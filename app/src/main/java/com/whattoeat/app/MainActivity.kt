package com.whattoeat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF29251F)
private val Cream = Color(0xFFFFF9EF)
private val Orange = Color(0xFFC64625)
private val Palette = listOf(0xFFF8BA55, 0xFFF08C68, 0xFF93BFA3, 0xFFB4A0CF, 0xFF7BBAC6,
    0xFFE5A7B7, 0xFFDFCE84, 0xFFA3B6DC, 0xFFC8C48B).map { Color(it) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Orange, onPrimary = Color.White,
                background = Cream, surface = Color.White, onBackground = Ink, onSurface = Ink,
                secondary = Color(0xFF356A50))) { FoodApp() }
        }
    }
}

@Composable
private fun FoodApp(vm: GameViewModel = viewModel()) {
    val state = vm.state
    val zh = vm.chinese
    fun t(en: String, cn: String) = if (zh) cn else en
    val scrollState = rememberScrollState()
    LaunchedEffect(state.screen, state.current) { scrollState.scrollTo(0) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.screen != Screen.SETUP) { confirmExit = true }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text(t("Leave this game?", "離開今次遊戲？")) },
        text = { Text(t("The current round will be cleared.", "今次回合嘅資料會清除。")) },
        confirmButton = { TextButton(onClick = { confirmExit = false; vm.reset() }) { Text(t("New game", "開新遊戲")) } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(t("Keep playing", "繼續玩")) } }
    )
    Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◉  SPIN • MATCH • EAT", style = MaterialTheme.typography.labelMedium, color = Orange)
                TextButton(onClick = vm::toggleLanguage, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (zh) "English" else "繁體中文")
                }
            }
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f)
                .verticalScroll(scrollState).padding(horizontal = 24.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (state.screen) {
                    Screen.SETUP -> Setup(vm)
                    Screen.ROULETTE -> key(state.current) { Roulette(vm) }
                    Screen.PASS -> {
                        Spacer(Modifier.height(36.dp))
                        Text("↗", fontSize = 80.sp, color = Orange)
                        Title(t("Pass the phone", "傳畀下一位"), t("Your choices stay private.", "你嘅選擇已隱藏。"))
                        InfoCard {
                            Text(t("${state.player!!.name} has finished.", "${state.player!!.name} 已完成。"))
                            Spacer(Modifier.height(12.dp))
                            Text(t("Next up", "下一位"), style = MaterialTheme.typography.labelLarge, color = Orange)
                            Text(state.players[state.current + 1].name, style = MaterialTheme.typography.headlineLarge)
                        }
                        Primary(t("I'm ready — continue", "準備好，開始！"), vm::nextPlayer)
                    }
                    Screen.RESULTS -> Results(vm)
                }
            }
        }
    }
}

@Composable
private fun Title(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Ink.copy(alpha = .7f))
    }
}
@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}
@Composable
private fun Primary(label: String, action: () -> Unit, enabled: Boolean = true) {
    Button(onClick = action, enabled = enabled, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Setup(vm: GameViewModel) {
    val zh = vm.chinese
    fun t(en: String, cn: String) = if (zh) cn else en
    var custom by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    Title(t("What to Eat?", "今餐食乜？"), t("Less debating. More eating.", "唔使再諗，轉個盤就食得！"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(Modifier.size(132.dp).clip(CircleShape).background(Color(0xFFF8BA55)), contentAlignment = Alignment.Center) {
            Text("🍜", fontSize = 72.sp)
        }
    }
    Text(t("01  /  WHO'S HUNGRY?", "01  /  邊個肚餓？"), color = Orange, style = MaterialTheme.typography.labelLarge)
    Text(t("Players · one shared phone", "玩家人數 · 共用一部手機"))
    (1..8).chunked(4).forEach { row ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { count ->
            FilterChip(selected = vm.playerCount == count, onClick = { vm.playerCount = count },
                label = { Text("$count", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp))
        }
      }
    }
    (0 until vm.playerCount).forEach { i ->
        OutlinedTextField(value = vm.names[i], onValueChange = { value -> vm.setName(i, value.take(30)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            label = { Text(t("Player ${i + 1}", "玩家 ${i + 1}")) },
            placeholder = { Text(t("Name (optional)", "名字（可留空）")) })
    }
    Text(t("02  /  ON THE MENU", "02  /  轉盤有咩食？"), color = Orange, style = MaterialTheme.typography.labelLarge)
    InfoCard {
        Text(t("${vm.setupFoods.size} food options", "${vm.setupFoods.size} 個美食選項"), fontWeight = FontWeight.Bold)
        Text(vm.setupFoods.joinToString(" · ") { it.label(zh) }, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = { editing = !editing }) { Text(t("Edit menu", "修改選項")) }
        if (editing) {
            Text(t("Keep 1–12 options. Custom names are used in both languages.", "保留 1–12 個選項；自訂名稱適用於兩種語言。"), style = MaterialTheme.typography.bodySmall)
            vm.setupFoods.forEach { food ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(food.symbol + " " + food.label(zh), modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.removeFood(food.id) }, enabled = vm.setupFoods.size > 1) { Text(t("Remove", "移除")) }
                }
            }
            OutlinedTextField(value = custom, onValueChange = { custom = it.take(24) }, singleLine = true,
                label = { Text(t("Add a food", "加入美食")) }, modifier = Modifier.fillMaxWidth())
            val duplicate = vm.setupFoods.any { it.english.equals(custom.trim(), true) || it.chinese == custom.trim() }
            if (duplicate) Text(t("Already on the menu", "已有呢個選項"), color = Orange)
            TextButton(onClick = { vm.addFood(custom.trim()); custom = "" },
                enabled = custom.isNotBlank() && !duplicate && vm.setupFoods.size < 12) { Text(t("+ Add option", "+ 加入選項")) }
            TextButton(onClick = vm::restoreFoods) { Text(t("Restore default menu", "還原預設選項")) }
        }
    }
    Primary(t("Let's spin →", "開始轉盤 →"), {
        vm.start((0 until vm.playerCount).map { vm.names[it].trim().ifBlank { t("Player ${it + 1}", "玩家 ${it + 1}") } }, vm.setupFoods)
    })
    Text(t("Up to 3 spins each · Like to finish · Entirely offline", "每人最多轉 3 次 · Like 即可完成 · 全程離線"),
        style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = .7f))
}

@Composable
private fun Roulette(vm: GameViewModel) {
    val s = vm.state
    val zh = vm.chinese
    fun t(en: String, cn: String) = if (zh) cn else en
    Text(t("PLAYER ${s.current + 1} OF ${s.players.size}", "玩家 ${s.current + 1} / ${s.players.size}"), color = Orange)
    Title(t("${s.player!!.name}'s turn", "${s.player!!.name}，到你喇！"),
        t("Trust your taste. Give it a spin.", "跟住你嘅口味，轉一轉！"))
    Text(t("Spin ${s.spins} / ${s.spinLimit}", "已轉 ${s.spins} / ${s.spinLimit} 次"), fontWeight = FontWeight.Bold)
    FoodWheel(s.foods, zh, s.pendingId, s.spinning, vm::animationFinished)
    when {
        s.spinning -> Text(t("Something delicious is coming…", "好嘢食轉緊過嚟…"), modifier = Modifier.alignCenter())
        s.pendingId != null -> {
            val food = s.foods.first { it.id == s.pendingId }
            Text(food.symbol + " " + food.label(zh), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Primary(t("♥ Like  +3", "♥ 鍾意  +3"), { vm.vote(Vote.LIKE) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.vote(Vote.SKIP) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(t("Skip  0", "跳過  0")) }
                OutlinedButton(onClick = { vm.vote(Vote.REJECT) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text(t("Reject  −3", "唔想食  −3")) }
            }
        }
        s.turnDone -> {
            Text(t("Your votes are saved for this round.", "已記低你今次回合嘅選擇。"))
            Primary(t("Finish turn →", "完成回合 →"), vm::finishTurn)
        }
        else -> Primary(t("Spin the wheel", "轉動轉盤"), vm::spin, s.canSpin)
    }
    if (s.canFollow) {
        OutlinedButton(onClick = vm::follow, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(t("Follow the Crowd", "跟大隊"))
        }
        Text(t("Happy with anything? Sit this round out.", "食咩都得？可以直接跟大隊。"), style = MaterialTheme.typography.bodySmall)
    }
    if (s.spins > 0 && !s.turnDone) Text(t("Like ends your turn. Skip or Reject lets you spin again, up to 3 times.",
        "Like 後可完成；跳過或唔想食可再轉，每人最多 3 次。"), style = MaterialTheme.typography.bodySmall)
}
private fun Modifier.alignCenter() = fillMaxWidth()

/** Target is chosen once by the engine; animation lands its segment exactly under the fixed pointer. */
@Composable
private fun FoodWheel(foods: List<FoodOption>, zh: Boolean, targetId: String?, spinning: Boolean, onFinished: () -> Unit) {
    // Recreated activities must still show the selected wedge at the pointer.
    val rotation = remember {
        Animatable(if (!spinning && targetId != null) WheelMath.landingAngle(foods.indexOfFirst { it.id == targetId }, foods.size) else 0f)
    }
    val finished by rememberUpdatedState(onFinished)
    val step = 360f / foods.size
    LaunchedEffect(spinning, targetId) {
        if (spinning && targetId != null) {
            val index = foods.indexOfFirst { it.id == targetId }
            rotation.animateTo(WheelMath.spinDestination(rotation.value, index, foods.size),
                tween(3200, easing = CubicBezierEasing(.12f, .72f, .16f, 1f)))
            finished()
        }
    }
    val description = if (zh) "美食轉盤：" else "Food wheel: "
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f)
            .semantics { contentDescription = description + foods.joinToString { it.label(zh) } }) {
            val radius = size.minDimension / 2 - 16.dp.toPx()
            val origin = center
            drawCircle(Color(0xFFEEDFCB), radius + 7.dp.toPx())
            rotate(rotation.value, origin) {
                foods.forEachIndexed { i, food ->
                    drawArc(Palette[i % Palette.size], -90f + step * i, step, true,
                        topLeft = Offset(origin.x - radius, origin.y - radius), size = Size(radius * 2, radius * 2))
                    val angle = (-90f + step * (i + .5f)) * Math.PI / 180
                    val x = origin.x + cos(angle).toFloat() * radius * .66f
                    val y = origin.y + sin(angle).toFloat() * radius * .66f
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(41, 37, 31)
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = (if (foods.size > 9) 10 else 12).sp.toPx()
                        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    }
                    // Short labels fit wedges; full selected names appear beneath the wheel.
                    val name = food.label(zh).let { if (it.length > 10) it.take(9) + "…" else it }
                    drawContext.canvas.nativeCanvas.drawText(name, x, y - (paint.ascent() + paint.descent()) / 2, paint)
                }
            }
            drawCircle(Cream, radius * .17f)
            drawCircle(Orange, radius * .09f)
            val pointer = Path().apply {
                moveTo(origin.x - 12.dp.toPx(), origin.y - radius - 6.dp.toPx())
                lineTo(origin.x + 12.dp.toPx(), origin.y - radius - 6.dp.toPx())
                lineTo(origin.x, origin.y - radius + 20.dp.toPx()); close()
            }
            drawPath(pointer, Orange)
        }
    }
}

@Composable
private fun Results(vm: GameViewModel) {
    val s = vm.state
    val zh = vm.chinese
    fun t(en: String, cn: String) = if (zh) cn else en
    val result = s.result!!
    Title(t("Dinner, decided.", "今晚食呢啲！"), t("Your table's best matches.", "你哋嘅美食配對結果。"))
    if (result.randomFood != null) {
        var revealed by rememberSaveable { mutableStateOf(false) }
        Text(t("Everyone followed the crowd. Let the wheel decide!", "大家都跟大隊，交畀轉盤決定！"))
        FoodWheel(s.foods, zh, result.randomFood.id, !revealed, { revealed = true })
        if (!revealed) return
        InfoCard {
            Text(t("RANDOM PICK", "隨機之選"), color = Orange)
            Text(result.randomFood.symbol + " " + result.randomFood.label(zh), style = MaterialTheme.typography.headlineMedium)
            Text(t("No preference votes were cast.", "今次冇人作出喜好投票。"))
        }
    } else {
        result.top.forEachIndexed { i, rec ->
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
                containerColor = if (i == 0) Color(0xFFFFE3B2) else Color.White)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (i == 0) t("01  /  TOP PICK", "01  /  首選推薦") else "0${i + 1}  /  ${t("ALSO ON THE TABLE", "其他推薦")}", color = Orange, fontWeight = FontWeight.Bold)
                    Text(rec.food.symbol + " " + rec.food.label(zh), style = MaterialTheme.typography.headlineMedium)
                    Text(t("${rec.likes} Like · ${rec.rejects} Reject", "${rec.likes} 人鍾意 · ${rec.rejects} 人唔想食"), fontWeight = FontWeight.Bold)
                    Text(t("${rec.skips} Skip · ${rec.unknown} not reviewed", "${rec.skips} 人跳過 · ${rec.unknown} 人未評價"), style = MaterialTheme.typography.bodySmall)
                    Text(when {
                        rec.compromise -> t("Compromise — someone rejected this option.", "折衷推薦：有玩家唔想食呢個選項。")
                        rec.unanimous -> t("Approved by every voting player.", "所有參與投票嘅玩家都明確表示鍾意。")
                        rec.likes == 0 -> t("No Rejects; no explicit Likes yet.", "冇人反對，亦未有人明確表示鍾意。")
                        else -> t("No Rejects; not everyone has approved it.", "冇人反對，但未獲所有人明確贊成。")
                    }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val follows = s.players.count { it.follows }
        if (follows > 0) Text(t("$follows followed the crowd and did not vote.", "$follows 人跟大隊，並無參與投票。"), style = MaterialTheme.typography.bodySmall)
        Text(t("Zero Rejects first, then Likes. Ties follow menu order. Unreviewed means unknown.",
            "優先推薦冇人反對嘅選項，再按鍾意人數排序。同票按選項次序排列；未評價代表未知。"), style = MaterialTheme.typography.bodySmall)
    }
    Primary(t("Play again · same group", "再玩一次 · 同一班人"), vm::replay)
    OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(t("New game", "開新遊戲")) }
}
