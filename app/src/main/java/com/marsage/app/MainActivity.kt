package com.marsage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

val BG = Color(0xFF0E0B0A)
val ACCENT = Color(0xFFC1440E)
val TXT = Color(0xFFF5EDE6)
val DIM = Color(0xFF9A8F87)
val MONO = FontFamily.Monospace

fun ru(x: Double, d: Int = 2): String = String.format(Locale("ru"), "%.${d}f", x)

@Composable
fun everySecond() {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); t++ } }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = ProfilesRepo(this)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = ACCENT, background = BG, surface = BG,
                    onBackground = TXT, onSurface = TXT
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = BG) { App(repo) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App(repo: ProfilesRepo) {
    val profiles = remember { repo.load() }
    if (profiles.isEmpty()) return
    var pIdx by remember { mutableIntStateOf(0) }
    val birth = remember(pIdx) { repo.parse(profiles[pIdx].birthISO) }
    val pagerState = rememberPagerState { 4 }

    Column(
        Modifier.fillMaxSize().background(BG).systemBarsPadding().padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MARSAGE", color = ACCENT, fontSize = 20.sp, fontFamily = MONO)
            Spacer(Modifier.width(12.dp))
            Text("треугольная туба: свайп = поворот", color = DIM, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            profiles.forEachIndexed { i, p ->
                TextButtonLite(p.name.take(12), selected = i == pIdx) { pIdx = i }
            }
        }
        Spacer(Modifier.height(8.dp))

        HorizontalPager(state = pagerState) { page ->
            Box(Modifier.fillMaxSize().graphicsLayer {
                val rel = (page - (pagerState.currentPage + pagerState.currentPageOffsetFraction))
                    .coerceIn(-1f, 1f)
                rotationY = -50f * rel
                cameraDistance = 16f * density
                val a = abs(rel)
                scaleX = 1f - 0.22f * a
                scaleY = 1f - 0.22f * a
                alpha = 1f - 0.55f * a
            }) {
                when (page) {
                    0 -> SolCounterScreen(birth)
                    1 -> ConverterScreen(birth)
                    2 -> BirthdayScreen(birth)
                    else -> FlightsScreen(birth)
                }
            }
        }
    }
}

@Composable
fun TextButtonLite(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) ACCENT else DIM,
        fontSize = 12.sp,
        fontFamily = MONO,
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .clickable(onClick = onClick)
    )
}

/* ---------- Экран 1: Сол-счётчик ---------- */

@Composable
fun SolCounterScreen(birth: ZonedDateTime) {
    everySecond()
    val now = Instant.now()
    val sols = MarsCore.solsBetween(birth.toInstant(), now)
    val age = MarsCore.ageYears(birth.toInstant(), now)
    val ey = MarsCore.fullEarthYears(birth, now.atZone(birth.zone))
    val frac = MarsCore.solOfDay(birth.toInstant(), now)
    val (mh, mm, ms) = MarsCore.marsClock(frac)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("СОЛ-СЧЁТЧИК", color = DIM, fontSize = 12.sp)
        Text(ru(sols), color = TXT, fontSize = 52.sp, fontFamily = MONO)
        Text("прожитых солов", color = DIM, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Text("${ru(age)} марс. года · идёт ${floor(age).toInt() + 1}-й", color = ACCENT, fontSize = 18.sp)
        Text("земной возраст: $ey полн. лет", color = TXT, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Text("сол №${floor(sols).toLong() + 1}", color = DIM, fontSize = 13.sp)
        Text(
            "марсианские часы: %02d:%02d:%02d".format(mh, mm, ms),
            color = TXT, fontSize = 16.sp, fontFamily = MONO
        )
        Text("сутки на Марсе = 24,6231 ч", color = DIM, fontSize = 11.sp)
    }
}

/* ---------- Экран 2: Конвертер ---------- */

@Composable
fun ConverterScreen(birth: ZonedDateTime) {
    var reverse by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf("2027-02-19 15:44") }
    var solInput by remember { mutableStateOf("10000") }
    var out by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        Text("КОНВЕРТЕР", color = DIM, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedButton(onClick = { reverse = false }) {
                Text("Дата → солы", color = if (!reverse) ACCENT else DIM, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { reverse = true }) {
                Text("Солы → дата", color = if (reverse) ACCENT else DIM, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!reverse) {
            OutlinedTextField(
                value = dateInput, onValueChange = { dateInput = it },
                label = { Text("ГГГГ-ММ-ДД ЧЧ:ММ (МСК)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                runCatching {
                    val t = LocalDateTime.parse(dateInput.trim().replace(" ", "T"))
                        .atZone(MarsCore.MSK).toInstant()
                    val sols = MarsCore.solsBetween(birth.toInstant(), t)
                    out = ru(sols) + " сола · " + ru(sols / MarsCore.YEAR_SOLS) +
                        " марс. года от рождения\n" + MarsCore.darian(t).toString() +
                        "\nMars Year: MY " + MarsCore.marsYear(t).first
                }.onFailure { out = "Не разобрал дату. Формат: 2027-02-19 15:44" }
            }) { Text("Пересчитать") }
        } else {
            OutlinedTextField(
                value = solInput, onValueChange = { solInput = it },
                label = { Text("Соловы от рождения") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                runCatching {
                    val sols = solInput.trim().replace(",", ".").toDouble()
                    val t = birth.toInstant()
                        .plusSeconds((sols * MarsCore.SOL_SEC).roundToLong())
                    out = t.atZone(MarsCore.MSK)
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")) +
                        " МСК\n" + MarsCore.darian(t).toString() +
                        "\nMars Year: MY " + MarsCore.marsYear(t).first
                }.onFailure { out = "Введите число, например 14545" }
            }) { Text("Пересчитать") }
        }

        if (out.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(out, color = TXT, fontSize = 14.sp, fontFamily = MONO)
        }
    }
}

/* ---------- Экран 3: Година ---------- */

@Composable
fun BirthdayScreen(birth: ZonedDateTime) {
    everySecond()
    val now = Instant.now()
    val (n, inst) = MarsCore.nextBirthday(birth.toInstant(), now)
    val solsLeft = MarsCore.solsBetween(now, inst)
    val progress = (1.0 - solsLeft / MarsCore.YEAR_SOLS).toFloat().coerceIn(0f, 1f)
    val dateStr = inst.atZone(MarsCore.MSK)
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
    val prevStr = MarsCore.birthday(birth.toInstant(), n - 1).atZone(MarsCore.MSK)
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val secs = Duration.between(now, inst).seconds
    val days = secs / 86400
    val h = secs / 3600 % 24
    val m = secs / 60 % 60

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ГОДИНА", color = DIM, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(230.dp)) {
                drawArc(Color(0x55C1440E), -90f, 360f, false, style = Stroke(20f, cap = StrokeCap.Round))
                drawArc(ACCENT, -90f, progress * 360f, false, style = Stroke(20f, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ru(solsLeft, 1), fontSize = 34.sp, fontFamily = MONO, color = TXT)
                Text("сола до праздника", color = DIM, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("$n-я марсианская година", color = ACCENT, fontSize = 18.sp)
        Text(dateStr + " МСК", color = TXT, fontSize = 14.sp)
        Text("земного времени: $days сут %02d:%02d".format(h, m), color = DIM, fontSize = 12.sp)
        Text("предыдущая: $prevStr", color = DIM, fontSize = 11.sp)
    }
}

/* ---------- Экран 4: Ближайшие рейсы ---------- */

@Composable
fun FlightsScreen(birth: ZonedDateTime) {
    everySecond()
    val now = Instant.now()
    val dOpp = MarsCore.darian(MarsCore.OPPOSITION)
    val dClose = MarsCore.darian(MarsCore.CLOSEST)
    val durE = Duration.between(now, MarsCore.OPPOSITION)
    val solsM = MarsCore.solsBetween(now, MarsCore.OPPOSITION)
    val secs = durE.seconds
    val days = secs / 86400
    val h = secs / 3600 % 24
    val m = secs / 60 % 60

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        Text("БЛИЖАЙШИЕ РЕЙСЫ", color = ACCENT, fontSize = 18.sp, fontFamily = MONO)
        Text("табло планетного аэропорта", color = DIM, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))

        Text("РЕЙС ОППОЗИЦИЯ", color = TXT, fontSize = 16.sp, fontFamily = MONO)
        Text("отправление 19.02.2027, 18:44 МСК", color = DIM, fontSize = 13.sp)
        Text("СТАТУС: ПО СТОЯНИЮ", color = ACCENT, fontSize = 13.sp, fontFamily = MONO)
        Spacer(Modifier.height(8.dp))
        Text("СБЛИЖЕНИЕ 20.02.2027, 03:07 · 101,4 млн км", color = TXT, fontSize = 13.sp, fontFamily = MONO)
        if (dOpp.sol == dClose.sol && dOpp.month == dClose.month) {
            Text("с точки зрения Марса это один день: ${dOpp}", color = DIM, fontSize = 11.sp)
        }
        Spacer(Modifier.height(16.dp))

        Text("ТАЙМЕР ЗЕМНОЙ: $days сут %02d:%02d:%02d".format(h, m, secs % 60),
            color = TXT, fontSize = 14.sp, fontFamily = MONO)
        Text("ТАЙМЕР МАРСИАНСКИЙ: ${ru(solsM)} сола",
            color = TXT, fontSize = 14.sp, fontFamily = MONO)
        Spacer(Modifier.height(16.dp))

        Text("СЛЕДУЮЩИЙ: 25.03.2029 · РЕГЛЯРНЫЙ*", color = DIM, fontSize = 12.sp, fontFamily = MONO)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = {}) {
            Text("КУПИТЬ БИЛЕТ", color = ACCENT, fontSize = 14.sp, fontFamily = MONO)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "рейсы пока не выполняются. планеты просто стоят в одну линию. Марс ближе обычного.",
            color = DIM, fontSize = 10.sp
        )
    }
}
