package dev.nihongo.tracker

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

private val Bg = Color.Black
private val Fg = Color.White
private val Dim = Color(0xFF9A9A9A)
private val Faint = Color(0xFF3A3A3A)

enum class Screen { HOME, LOG, GOALS, NEW_GOAL, HEATMAP, HISTORY }

// Heatmap shades on black: empty, then four greens from dim to bright.
private val HeatColors = listOf(Color(0xFF1A1A1A), Color(0xFF0E4429), Color(0xFF006D32), Color(0xFF26A641), Color(0xFF39D353))
private val FutureCell = Color(0xFF0D0D0D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Bg, onBackground = Fg, onSurface = Fg, primary = Fg)) {
                App(viewModel())
            }
        }
    }
}

@Composable
fun App(vm: TrackerViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    // Check GitHub every time the app comes to the front, not only when the process starts.
    LifecycleStartEffect(Unit) {
        vm.checkForUpdate()
        onStopOrDispose { }
    }
    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.NEW_GOAL) Screen.GOALS else Screen.HOME
    }
    Box(Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        when (screen) {
            Screen.HOME -> HomeScreen(vm) { screen = it }
            Screen.LOG -> LogScreen(vm) { screen = it }
            Screen.GOALS -> GoalsScreen(vm) { screen = it }
            Screen.NEW_GOAL -> NewGoalScreen(vm) { screen = it }
            Screen.HEATMAP -> HeatmapScreen(vm)
            Screen.HISTORY -> HistoryScreen(vm)
        }
    }
}

// ---------- Screens ----------

@Composable
fun HomeScreen(vm: TrackerViewModel, go: (Screen) -> Unit) {
    val d by vm.dashboard.collectAsStateWithLifecycle()
    val upd by vm.update.collectAsStateWithLifecycle()
    val goalsDone = d.progress.count { it.done }
    Page {
        when (val u = upd) {
            is UpdateState.Available ->
                Entry("Update", u.note ?: "build ${u.release.versionCode} available · tap to install") { vm.installUpdate(u.release) }
            UpdateState.Downloading -> Entry("Updating…", "downloading") {}
            else -> Entry(
                "Check for updates",
                when (u) {
                    UpdateState.Checking -> "checking…"
                    UpdateState.UpToDate -> "up to date · build ${BuildConfig.VERSION_CODE}"
                    is UpdateState.Failed -> "${u.message} · tap to retry · build ${BuildConfig.VERSION_CODE}"
                    else -> "build ${BuildConfig.VERSION_CODE}"
                },
            ) { vm.checkForUpdate() }
        }
        Entry(
            title = if (d.ankiDoneToday) "Anki ✓" else "Anki",
            sub = "${d.ankiStreak} day streak · best ${d.ankiLongest}",
        ) { vm.toggleAnki(0) }
        Entry(
            title = if (d.ankiDoneYesterday) "Yesterday ✓" else "Yesterday",
            sub = "Anki, if you forgot",
        ) { vm.toggleAnki(1) }
        Entry("Log", "today ${d.todayChars} 文字 · ${formatDuration(d.todaySeconds)}") { go(Screen.LOG) }
        Entry("Goals", if (d.progress.isEmpty()) "none yet" else "$goalsDone of ${d.progress.size} done") { go(Screen.GOALS) }
        Entry("Heatmap") { go(Screen.HEATMAP) }
        Entry("History") { go(Screen.HISTORY) }
    }
}

@Composable
fun LogScreen(vm: TrackerViewModel, go: (Screen) -> Unit) {
    var medium by remember { mutableStateOf(Medium.READING) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var secs by remember { mutableStateOf("") }
    var chars by remember { mutableStateOf("") }
    // Any mix works: "90" in the minutes box alone is 1h 30m, "1" hours + "30" minutes is the same.
    val totalSeconds = (
        (hours.toLongOrNull() ?: 0L) * 3600 + (minutes.toLongOrNull() ?: 0L) * 60 + (secs.toLongOrNull() ?: 0L)
        ).coerceAtMost(360_000L).toInt() // cap at 100 hours per entry
    val c = chars.toIntOrNull() ?: 0
    Page {
        Label("Medium")
        Choice(Medium.entries, medium, { it.label }) { medium = it }
        Gap()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlainField("hours", hours, Modifier.weight(1f)) { hours = it }
            PlainField("min", minutes, Modifier.weight(1f)) { minutes = it }
            PlainField("sec", secs, Modifier.weight(1f)) { secs = it }
        }
        if (totalSeconds > 0) Text("= ${formatDuration(totalSeconds)}", color = Dim, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        PlainField("文字", chars) { chars = it }
        Gap()
        Action("Add", enabled = totalSeconds > 0 || c > 0) {
            vm.addLog(medium, totalSeconds, c)
            go(Screen.HOME)
        }
    }
}

@Composable
fun GoalsScreen(vm: TrackerViewModel, go: (Screen) -> Unit) {
    val d by vm.dashboard.collectAsStateWithLifecycle()
    Page {
        if (d.progress.isEmpty()) Text("No goals yet", color = Dim, fontSize = 18.sp, fontWeight = FontWeight.Light)
        d.progress.forEach { p -> GoalRow(p) { vm.deleteGoal(p.goal) } }
        Gap()
        Entry("New goal") { go(Screen.NEW_GOAL) }
    }
}

@Composable
fun NewGoalScreen(vm: TrackerViewModel, go: (Screen) -> Unit) {
    var metric by remember { mutableStateOf(Metric.CHARACTERS) }
    var medium by remember { mutableStateOf<Medium?>(null) }
    var period by remember { mutableStateOf(Period.DAILY) }
    var target by remember { mutableStateOf("") }
    val t = target.toIntOrNull() ?: 0
    Page {
        Label("Track")
        Choice(Metric.entries, metric, { it.label }) { metric = it }
        if (metric != Metric.ANKI_DAYS) {
            Gap()
            Label("Medium")
            Choice(listOf<Medium?>(null) + Medium.entries, medium, { it?.label ?: "All" }) { medium = it }
        }
        Gap()
        Label("Period")
        Choice(Period.entries, period, { it.label }) { period = it }
        Gap()
        PlainField("Target", target) { target = it }
        Gap()
        Action("Create", enabled = t > 0) {
            vm.addGoal(Goal(metric = metric, medium = if (metric == Metric.ANKI_DAYS) null else medium, period = period, target = t))
            go(Screen.GOALS)
        }
    }
}

/** Year-by-year grids like the Anki heatmap add-on: one column per week, one row per weekday (Mon at top). */
@Composable
fun HeatmapScreen(vm: TrackerViewModel) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    val anki by vm.ankiDays.collectAsStateWithLifecycle()
    var metric by remember { mutableStateOf(HeatMetric.ANKI) }
    val today = LocalDate.now().toEpochDay()
    val values = remember(metric, logs, anki) { heatValues(metric, logs, anki) }
    val summary = remember(values, today) { heatSummary(values, today) }
    val busiest = values.values.maxOrNull() ?: 0
    val thisYear = LocalDate.now().year
    val firstYear = values.keys.minOrNull()?.let { LocalDate.ofEpochDay(it).year } ?: thisYear

    val average = when (metric) {
        HeatMetric.ANKI -> null
        HeatMetric.TIME -> formatDuration(summary.averagePerActiveDay)
        HeatMetric.CHARS -> "${summary.averagePerActiveDay} 文字"
    }
    val summaryLine = listOfNotNull(
        average?.let { "Daily average: $it" },
        "Days active: ${summary.percentDaysActive}%",
        "Longest streak: ${summary.longestStreak} days",
        "Current streak: ${summary.currentStreak} days",
    ).joinToString(" · ")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        Choice(HeatMetric.entries, metric, { it.label }) { metric = it }
        Gap()
        Text(summaryLine, color = Dim, fontSize = 14.sp)
        Gap()
        for (year in thisYear downTo firstYear) YearHeatmap(year, values, busiest, today)
    }
}

@Composable
fun YearHeatmap(year: Int, values: Map<Long, Int>, busiest: Int, today: Long) {
    val jan1 = LocalDate.of(year, 1, 1)
    val length = jan1.lengthOfYear()
    val offset = jan1.dayOfWeek.value - 1 // Monday = row 0
    val cols = (offset + length + 6) / 7
    val start = jan1.toEpochDay()
    Text("$year", color = Dim, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
    Canvas(Modifier.fillMaxWidth().aspectRatio(cols / 7f)) {
        val gap = 1.dp.toPx()
        val cell = (size.width - gap * (cols - 1)) / cols
        for (i in 0 until length) {
            val epoch = start + i
            val idx = offset + i
            val color = if (epoch > today) FutureCell else HeatColors[heatLevel(values[epoch] ?: 0, busiest)]
            drawRect(color, Offset((idx / 7) * (cell + gap), (idx % 7) * (cell + gap)), Size(cell, cell))
        }
    }
    Gap()
}

@Composable
fun HistoryScreen(vm: TrackerViewModel) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().padding(start = 40.dp, end = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 32.dp),
    ) {
        if (logs.isEmpty()) item { Text("Nothing logged yet", color = Dim, fontSize = 18.sp, fontWeight = FontWeight.Light) }
        items(logs, key = { it.id }) { log ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${LocalDate.ofEpochDay(log.epochDay)} · ${log.medium.label}", color = Fg, fontSize = 20.sp, fontWeight = FontWeight.Light)
                    Text("${formatDuration(log.seconds)} · ${log.characters} 文字", color = Dim, fontSize = 14.sp)
                }
                Remove { vm.deleteLog(log) }
            }
        }
    }
}

// ---------- Building blocks ----------

/** Vertically centred, left-aligned column on the dark background, like a launcher list. */
@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 40.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
fun Entry(title: String, sub: String? = null, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(title, color = Fg, fontSize = 32.sp, fontWeight = FontWeight.Light)
        if (sub != null) Text(sub, color = Dim, fontSize = 14.sp)
    }
}

@Composable
fun GoalRow(p: GoalProgress, onRemove: () -> Unit) {
    val g = p.goal
    val title = if (g.metric == Metric.ANKI_DAYS) "Anki days" else "${g.medium?.label ?: "All media"} · ${g.metric.label}"
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Fg, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Text("${g.period.label} · ${p.current} / ${g.target}" + if (p.done) " ✓" else "", color = Dim, fontSize = 14.sp)
            }
            Remove(onRemove)
        }
        LinearProgressIndicator(
            progress = { p.fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp),
            color = Fg,
            trackColor = Faint,
        )
    }
}

@Composable
fun Remove(onClick: () -> Unit) {
    Text("remove", color = Dim, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClick).padding(8.dp))
}

@Composable
fun Label(text: String) {
    Text(text, color = Dim, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
fun Gap() {
    Box(Modifier.height(20.dp))
}

@Composable
fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = if (enabled) Fg else Faint,
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp),
    )
}

/** A row of plain words; the selected one is white, the rest grey. */
@Composable
fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(options) { o ->
            val on = o == selected
            Text(
                label(o),
                color = if (on) Fg else Dim,
                fontSize = 20.sp,
                fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
                modifier = Modifier.clickable { onSelect(o) }.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
fun PlainField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(7)) },
        modifier = modifier,
        label = { Text(label, color = Dim) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Fg,
            unfocusedTextColor = Fg,
            cursorColor = Fg,
            focusedIndicatorColor = Dim,
            unfocusedIndicatorColor = Faint,
        ),
    )
}
