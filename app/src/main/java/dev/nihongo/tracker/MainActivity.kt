package dev.nihongo.tracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            val ctx = LocalContext.current
            // Dynamic color needs Android 12+ (API 31); minSdk is 26, so fall back below that.
            val colors = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = colors) {
                App(viewModel())
            }
        }
    }
}

@Composable
fun App(vm: TrackerViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Today", "History", "Goals").forEachIndexed { i, title ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(title) })
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> TodayScreen(vm)
                1 -> HistoryScreen(vm)
                else -> GoalsScreen(vm)
            }
        }
    }
}

// ---------- Today ----------

@Composable
fun TodayScreen(vm: TrackerViewModel) {
    val d by vm.dashboard.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("今日", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Anki streak: ${d.ankiStreak} days", style = MaterialTheme.typography.titleLarge)
                    Text("Longest: ${d.ankiLongest} days")
                    Text("Today: ${d.todayChars} 文字 · ${d.todayMinutes} min")
                }
            }
        }
        item { AnkiTodayCard(reviewsToday = d.ankiToday, onSave = vm::setAnkiToday) }
        if (d.progress.isNotEmpty()) {
            item { Text("Goals", style = MaterialTheme.typography.titleMedium) }
            items(d.progress) { GoalCard(it, onDelete = null) }
        }
        item { LogForm(onAdd = { m, min, ch -> vm.addLog(m, min, ch) }) }
        if (d.weekMinutesByMedium.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Last 7 days by medium", style = MaterialTheme.typography.titleMedium)
                        d.weekMinutesByMedium.forEach { (m, min) -> Text("${m.label}: $min min") }
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
fun AnkiTodayCard(reviewsToday: Int, onSave: (Int) -> Unit) {
    var text by remember(reviewsToday) { mutableStateOf(if (reviewsToday > 0) reviewsToday.toString() else "") }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Anki reviews today", text, { text = it }, Modifier.weight(1f))
            Button(onClick = { onSave(text.toIntOrNull() ?: 0) }) { Text("Save") }
        }
    }
}

@Composable
fun LogForm(onAdd: (Medium, Int, Int) -> Unit) {
    var medium by remember { mutableStateOf(Medium.READING) }
    var minutes by remember { mutableStateOf("") }
    var chars by remember { mutableStateOf("") }
    val m = minutes.toIntOrNull() ?: 0
    val c = chars.toIntOrNull() ?: 0
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Log study", style = MaterialTheme.typography.titleMedium)
            Chips(Medium.entries, medium, { it.label }) { medium = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Minutes", minutes, { minutes = it }, Modifier.weight(1f))
                NumberField("文字", chars, { chars = it }, Modifier.weight(1f))
            }
            Button(
                onClick = { onAdd(medium, m, c); minutes = ""; chars = "" },
                enabled = m > 0 || c > 0,
            ) { Text("Add") }
        }
    }
}

// ---------- History ----------

@Composable
fun HistoryScreen(vm: TrackerViewModel) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp)) }
        if (logs.isEmpty()) item { Text("Nothing logged yet.") }
        items(logs, key = { it.id }) { log ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${LocalDate.ofEpochDay(log.epochDay)} · ${log.medium.label}")
                        Text("${log.minutes} min · ${log.characters} 文字", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.deleteLog(log) }) { Text("Delete") }
                }
            }
        }
    }
}

// ---------- Goals ----------

@Composable
fun GoalsScreen(vm: TrackerViewModel) {
    val d by vm.dashboard.collectAsStateWithLifecycle()
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Goals", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp)) }
        items(d.progress, key = { it.goal.id }) { GoalCard(it, onDelete = { vm.deleteGoal(it.goal) }) }
        item { NewGoalForm(onCreate = vm::addGoal) }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
fun GoalCard(p: GoalProgress, onDelete: (() -> Unit)?) {
    val g = p.goal
    val scope = if (g.metric == Metric.ANKI_REVIEWS) "" else " · ${g.medium?.label ?: "All media"}"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text("${g.metric.label}$scope · ${g.period.label}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
            }
            LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
            Text("${p.current} / ${g.target}" + if (p.done) "  ✓" else "")
        }
    }
}

@Composable
fun NewGoalForm(onCreate: (Goal) -> Unit) {
    var metric by remember { mutableStateOf(Metric.CHARACTERS) }
    var medium by remember { mutableStateOf<Medium?>(null) }
    var period by remember { mutableStateOf(Period.DAILY) }
    var target by remember { mutableStateOf("") }
    val t = target.toIntOrNull() ?: 0
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("New goal", style = MaterialTheme.typography.titleMedium)
            Chips(Metric.entries, metric, { it.label }) { metric = it }
            if (metric != Metric.ANKI_REVIEWS) {
                Chips(listOf<Medium?>(null) + Medium.entries, medium, { it?.label ?: "All media" }) { medium = it }
            }
            Chips(Period.entries, period, { it.label }) { period = it }
            NumberField("Target", target, { target = it }, Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onCreate(Goal(metric = metric, medium = if (metric == Metric.ANKI_REVIEWS) null else medium, period = period, target = t))
                    target = ""
                },
                enabled = t > 0,
            ) { Text("Create goal") }
        }
    }
}

// ---------- Shared bits ----------

@Composable
fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o -> FilterChip(selected = selected == o, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(7)) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
