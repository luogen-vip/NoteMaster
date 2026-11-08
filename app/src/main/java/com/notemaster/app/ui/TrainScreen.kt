package com.notemaster.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notemaster.app.audio.TonePlayer
import com.notemaster.app.data.MusicData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 「练耳」页：三大肌肉记忆训练
 *  1. 听音辨唱名：播放随机音 → 选唱名
 *  2. 音名反应：显示音名(如 E) → 选唱名（条件反射）
 *  3. 旋律跟读：听一段音序 → 在键盘上复现
 *
 * 出题规则：洗牌轮换（deck）——把 7 个音洗成一叠，抽完再重新洗，
 * 保证每一轮 7 个音各出现一次，不会某个音一直不出现、也不会只出某几个。
 *
 * 今日战绩实时刷新：答题后立即更新，不需要切换页面。
 */
@Composable
fun TrainScreen() {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("stats", Context.MODE_PRIVATE) }

    // 战绩放进 Compose 状态：答题立刻刷新界面
    var correctTotal by rememberSaveable { mutableIntStateOf(prefs.getInt("correct", 0)) }
    var bestStreak by rememberSaveable { mutableIntStateOf(prefs.getInt("best", 0)) }

    // 记录一次答题：写入偏好存储 + 同步刷新状态
    val record: (Boolean) -> Unit = { ok ->
        val newStreak = prefs.getInt("streak", 0) + if (ok) 1 else 0
        prefs.edit()
            .putInt("correct", prefs.getInt("correct", 0) + if (ok) 1 else 0)
            .putInt("total", prefs.getInt("total", 0) + 1)
            .putInt("streak", newStreak)
            .putInt("best", prefs.getInt("best", 0).coerceAtLeast(newStreak))
            .apply()
        correctTotal = prefs.getInt("correct", 0)
        bestStreak = prefs.getInt("best", 0)
    }

    Column(
        Modifier.fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("① 听音辨名", "② 音名反应", "③ 旋律跟读").forEachIndexed { i, label ->
                FilterChip(
                    selected = mode == i,
                    onClick = { mode = i },
                    label = { Text(label) }
                )
            }
        }
        Text(
            "今日战绩：答对 $correctTotal 次 · 连击最高 $bestStreak（连续答对不中断）",
            fontSize = 13.sp, color = Color(0xFF9A92B8)
        )
        when (mode) {
            0 -> ListenQuiz(record)
            1 -> NameQuiz(record)
            else -> EchoGame()
        }
    }
}

/**
 * 洗牌抽音器：把 1..7 洗成一叠逐个抽，抽完自动重新洗牌。
 * 每一轮 7 个音各出现且只出现一次——任何音都不会被漏掉或霸屏。
 */
class NoteDeck {
    private var bag: MutableList<Int> = mutableListOf()

    fun draw(): Int {
        if (bag.isEmpty()) bag = (1..7).shuffled().toMutableList()
        return bag.removeAt(bag.size - 1)
    }
}

@Composable
private fun ListenQuiz(record: (Boolean) -> Unit) {
    val deck = remember { NoteDeck() }
    var step by rememberSaveable { mutableIntStateOf(deck.draw()) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    // 播放态不跨页保存：切走再回来=未播放，不会卡在「播放中」且按钮不可点
    var isPlaying by remember { mutableStateOf(false) }
    // 答题锁定：出结果后的 0.9 秒过渡期内不可再答，防止连点同一题重复计战绩/连击
    var answering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { TonePlayer.stopAll() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔊 听到的是哪个音？", fontSize = 15.sp, color = Color(0xFFB0A7C6))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (isPlaying) return@Button
                        isPlaying = true
                        TonePlayer.play(MusicData.stepToFreq(step, 0), 900)
                        scope.launch {
                            delay(1000)
                            isPlaying = false
                        }
                    },
                    enabled = !isPlaying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isPlaying) "❚❚ 播放中…" else "▶ 播放", fontSize = 18.sp)
                }
                // 固定高度的反馈区：没答题时也占位，答完弹提示不会把页面往下挤
                Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    if (feedback.isNotEmpty()) {
                        Text(feedback, fontSize = 16.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // 七个唱名按钮
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicData.solfege.forEachIndexed { idx, name ->
                Button(
                    onClick = {
                        if (answering) return@Button
                        answering = true
                        val ok = idx + 1 == step
                        feedback = if (ok) "✅ 对！是 $name" else "❌ 是 ${MusicData.solfege[step - 1]}，再听一次"
                        record(ok)
                        if (ok) streak += 1 else streak = 0
                        scope.launch {
                            delay(900)
                            step = deck.draw()   // 洗牌轮换：7 个音每轮各出现一次
                            feedback = ""
                            answering = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, fontSize = 16.sp)
                    Text("${idx + 1}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                } }
            }
        }
        Text(
            "连击：$streak  🔥（答错就清零，试试连续答对 5 个）",
            fontSize = 15.sp, color = if (streak >= 5) Color(0xFFFF6B6B) else Color(0xFF9A92B8)
        )
        Text(
            "训练目标：把 7 个唱名听成肌肉记忆；答错时立刻重听，加深印象。",
            fontSize = 12.sp, color = Color(0xFF77718F)
        )
    }
}

@Composable
private fun NameQuiz(record: (Boolean) -> Unit) {
    val deck = remember { NoteDeck() }
    var letter by rememberSaveable { mutableStateOf(MusicData.letters[deck.draw() - 1]) }
    var feedback by rememberSaveable { mutableStateOf("") }
    // 答题锁定：0.7 秒过渡期内不可再答，防止连点重复计分
    var answering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("看到音名，立刻说出唱名！", fontSize = 15.sp, color = Color(0xFFB0A7C6))
                Spacer(Modifier.height(8.dp))
                Text(letter, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicData.solfege.forEachIndexed { idx, name ->
                Button(
                    onClick = {
                        if (answering) return@Button
                        answering = true
                        val target = MusicData.letters.indexOf(letter) + 1
                        val ok = idx + 1 == target
                        feedback = if (ok) "✅ $letter = $name = ${target}" else "❌ $letter = ${MusicData.solfege[target - 1]}"
                        record(ok)
                        scope.launch {
                            delay(700)
                            letter = MusicData.letters[deck.draw() - 1]   // 洗牌轮换，A~B 都会出现
                            feedback = ""
                            answering = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(name, fontSize = 15.sp) }
            }
        }
        // 固定高度反馈区，避免提示出现时按钮上下跳动
        Box(Modifier.height(22.dp)) {
            if (feedback.isNotEmpty()) Text(feedback, fontSize = 15.sp, color = Color(0xFFFFD166))
        }
        Text(
            "训练目标：看到字母 0.5 秒内脱口而出唱名。E→咪、G→嗦 是最容易混的，多刷。",
            fontSize = 12.sp, color = Color(0xFF77718F), lineHeight = 20.sp
        )
    }
}

@Composable
private fun EchoGame() {
    // 切走再切回不丢进度：全部状态用 rememberSaveable 保存
    var seq by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var phase by remember { mutableStateOf("idle") } // idle / listen / input（不跨页保存，避免回来卡在“听中”）
    var progress by rememberSaveable { mutableIntStateOf(0) } // 已连对到第几个音（逐位判定）
    var feedback by rememberSaveable { mutableStateOf("选择难度开始，先听旋律，再在键盘上复现") }
    var highlight by rememberSaveable { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { TonePlayer.stopAll() }
    }

    // 旋律取值范围：1..7 中音 + 8 = 高音哆（键盘 8 个键都可能是答案，判定与琴键一一对应）
    val stepFreq: (Int) -> Double = { if (it == 8) MusicData.stepToFreq(1, 1) else MusicData.stepToFreq(it, 0) }

    // 旋律生成：洗牌袋——8 个音（1..7 + 高音哆）洗成一叠逐个抽，不够再洗一叠；
    // 不写死、不霸屏，每段旋律都尽量覆盖到所有音
    fun melodyOf(len: Int): List<Int> {
        val bag = mutableListOf<Int>()
        while (bag.size < len) bag.addAll((1..8).shuffled())
        return bag.take(len)
    }

    fun playSeq(s: List<Int>, done: () -> Unit) {
        scope.launch {
            for (n in s) {
                highlight = n
                TonePlayer.play(stepFreq(n))
                delay(440)
                // 每个音结束后先熄灭高亮再停一小段：连续相同的音也能看到两次独立的闪烁，
                // 用户能明确数出到底弹了几个音
                highlight = -1
                delay(140)
            }
            highlight = -1
            done()
        }
    }

    // 逐位判定：第 progress+1 个音必须完全正确才算对；
    // 弹错则明确提示「第几个弹错了，从头再来」并归零（逻辑透明，不会让人误会“点中答案里的音就算对”）
    fun submitKey(n: Int) {
        if (phase != "input" || seq.isEmpty()) return
        if (n == seq[progress]) {
            progress += 1
            if (progress == seq.size) {
                feedback = "🎉 全部 ${seq.size} 个音按顺序全对！你的耳朵升级了"
                phase = "idle"
                progress = 0
            } else {
                feedback = "✅ 第 ${progress} 个音对了（${progress}/${seq.size}），继续…"
            }
        } else {
            // 弹错：归零并停止本轮输入，是否重听由用户决定（点「再听一遍」），不自动循环播放
            feedback = "❌ 第 ${progress + 1} 个音弹错了，已归零——点「🔊 再听一遍」重听，或点难度开始新旋律"
            progress = 0
            phase = "idle"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 难度按钮 + 重听：各占权重，大屏小屏都不会被挤变形；重听按钮写清楚文字
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3 to "3 音", 5 to "5 音", 7 to "7 音").forEach { (len, label) ->
                Button(
                    onClick = {
                        // 正在播旋律时不接受重新开始/换难度，避免两次播放重叠打架
                        if (phase == "listen") return@Button
                        seq = melodyOf(len)
                        progress = 0
                        feedback = "🔊 仔细听…"
                        phase = "listen"
                        playSeq(seq) {
                            phase = "input"
                            feedback = "轮到你了：把听到的按顺序弹出来"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) { Text(label, maxLines = 1) }
            }
            Button(
                onClick = {
                    // 正在播旋律时不重复触发，避免两遍重叠
                    if (seq.isEmpty() || phase == "listen") return@Button
                    feedback = "🔊 再听一遍…"
                    playSeq(seq) { feedback = "轮到你了：把听到的按顺序弹出来" }
                },
                modifier = Modifier.weight(1.4f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) { Text("🔊 再听一遍", maxLines = 1) }
        }
        Text(feedback, fontSize = 15.sp, color = Color(0xFFFFD166))
        // 进度圆点：● = 已连对，○ = 还没对；弹错归零后能直观看到从头开始
        if (seq.isNotEmpty()) {
            Text(
                "进度：" + List(seq.size) { i -> if (i < progress) "●" else "○" }.joinToString(" "),
                fontSize = 14.sp, color = Color(0xFF7BC7FF)
            )
        }
        // 8 键键盘（1..7 + 高音哆）：高音哆也是合法答案，判定与琴键一一对应
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..7).forEach { n ->
                Button(
                    onClick = {
                        TonePlayer.play(stepFreq(n))
                        submitKey(n)
                    },
                    modifier = Modifier.weight(1f).height(70.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highlight == n) Color(0xFFFFD166) else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${MusicData.solfege[n - 1]}", fontSize = 14.sp)
                        Text("$n", fontSize = 11.sp)
                    }
                }
            }
            Button(
                onClick = {
                    TonePlayer.play(stepFreq(8))
                    submitKey(8)
                },
                modifier = Modifier.weight(1f).height(70.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("高音哆", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("高1", fontSize = 11.sp)
            } }
        }
        Text(
            "这是「听出旋律」的核心训练：必须按顺序逐个对上；弹错会归零停下，点「再听一遍」重听或点难度开始新旋律。听久了你就能像记住歌词一样记住音。",
            fontSize = 12.sp, color = Color(0xFF77718F), lineHeight = 20.sp
        )
    }
}
