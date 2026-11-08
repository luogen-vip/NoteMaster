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
 */
@Composable
fun TrainScreen() {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("stats", Context.MODE_PRIVATE) }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            "今日战绩：答对 ${prefs.getInt("correct", 0)} 次 · 连击最高 ${prefs.getInt("best", 0)}",
            fontSize = 13.sp, color = Color(0xFF9A92B8)
        )
        when (mode) {
            0 -> ListenQuiz(prefs)
            1 -> NameQuiz(prefs)
            else -> EchoGame()
        }
    }
}

private fun record(context: Context?, prefs: SharedPreferences?, ok: Boolean) {
    if (prefs == null) return
    prefs.edit()
        .putInt("correct", prefs.getInt("correct", 0) + if (ok) 1 else 0)
        .putInt("total", prefs.getInt("total", 0) + 1)
        .putInt("best", prefs.getInt("best", 0).coerceAtLeast(prefs.getInt("streak", 0) + if (ok) 1 else 0))
        .putInt("streak", if (ok) prefs.getInt("streak", 0) + 1 else 0)
        .apply()
}

private typealias SharedPreferences = android.content.SharedPreferences

@Composable
private fun ListenQuiz(prefs: SharedPreferences?) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf((1..7).random()) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔊 听到的是哪个音？", fontSize = 15.sp, color = Color(0xFFB0A7C6))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { TonePlayer.play(MusicData.stepToFreq(step, 0)) }) {
                    Text("▶ 播放", fontSize = 18.sp)
                }
                if (feedback.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(feedback, fontSize = 16.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                }
            }
        }
        // 七个唱名按钮
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicData.solfege.forEachIndexed { idx, name ->
                Button(
                    onClick = {
                        val ok = idx + 1 == step
                        feedback = if (ok) "✅ 对！是 $name" else "❌ 是 ${MusicData.solfege[step - 1]}，再听一次"
                        record(context, prefs, ok)
                        if (ok) streak += 1 else streak = 0
                        scope.launch {
                            delay(900)
                            step = (1..7).random()
                            feedback = ""
                        }
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, fontSize = 16.sp)
                    Text("${idx + 1}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                } }
            }
        }
        Text("连击：$streak  🔥", fontSize = 15.sp, color = if (streak >= 5) Color(0xFFFF6B6B) else Color(0xFF9A92B8))
        Text(
            "小技巧：先在心里跟唱一遍，再选。答错时立刻重听一次，加深印象。",
            fontSize = 12.sp, color = Color(0xFF77718F)
        )
    }
}

@Composable
private fun NameQuiz(prefs: SharedPreferences?) {
    val context = LocalContext.current
    var letter by rememberSaveable { mutableStateOf(MusicData.letters.random()) }
    var feedback by rememberSaveable { mutableStateOf("") }
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
                        val target = MusicData.letters.indexOf(letter) + 1
                        val ok = idx + 1 == target
                        feedback = if (ok) "✅ $letter = $name = ${target}" else "❌ $letter = ${MusicData.solfege[target - 1]}"
                        record(context, prefs, ok)
                        scope.launch {
                            delay(700)
                            letter = MusicData.letters.random()
                            feedback = ""
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(name, fontSize = 15.sp) }
            }
        }
        if (feedback.isNotEmpty()) Text(feedback, fontSize = 15.sp, color = Color(0xFFFFD166))
        Text(
            "训练目标：看到字母 0.5 秒内脱口而出唱名。E→咪、G→嗦 是最容易混的，多刷。",
            fontSize = 12.sp, color = Color(0xFF77718F), lineHeight = 20.sp
        )
    }
}

@Composable
private fun EchoGame() {
    var seq by remember { mutableStateOf(listOf<Int>()) }
    var phase by remember { mutableStateOf("idle") } // idle / listen / input
    var input by remember { mutableStateOf(listOf<Int>()) }
    var feedback by remember { mutableStateOf("选择难度开始，先听旋律，再在键盘上复现") }
    var highlight by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()

    fun playSeq(s: List<Int>, done: () -> Unit) {
        scope.launch {
            for (n in s) {
                highlight = n
                TonePlayer.play(MusicData.stepToFreq(n, 0))
                delay(520)
            }
            highlight = -1
            done()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3 to "3 音", 5 to "5 音", 7 to "7 音").forEach { (len, label) ->
                Button(onClick = {
                    seq = List(len) { (1..7).random() }
                    input = emptyList()
                    feedback = "🔊 仔细听…"
                    phase = "listen"
                    playSeq(seq) {
                        phase = "input"
                        feedback = "轮到你了：把听到的按顺序弹出来"
                    }
                }) { Text(label) }
            }
            Button(onClick = {
                if (seq.isNotEmpty()) {
                    feedback = "🔊 再听一遍…"
                    playSeq(seq) { feedback = "轮到你了：把听到的按顺序弹出来" }
                }
            }) { Text("↻ 重听") }
        }
        Text(feedback, fontSize = 15.sp, color = Color(0xFFFFD166))
        // 8 键键盘（1..7 + 高1）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..7).forEach { n ->
                Button(
                    onClick = {
                        if (phase == "input") {
                            TonePlayer.play(MusicData.stepToFreq(n, 0))
                            input = input + n
                            val prefixOk = seq.take(input.size) == input
                            if (!prefixOk) {
                                feedback = "❌ 弹错了，重听一遍再来"
                                input = emptyList()
                            } else if (input.size == seq.size) {
                                feedback = "🎉 完全正确！你的耳朵升级了"
                                phase = "idle"
                                input = emptyList()
                            } else {
                                feedback = "✅ 第 ${input.size} 个对了，继续…"
                            }
                        } else TonePlayer.play(MusicData.stepToFreq(n, 0))
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
                    TonePlayer.play(MusicData.stepToFreq(1, 1))
                    if (phase == "input") {
                        input = input + 1
                        val prefixOk = seq.take(input.size) == input
                        if (!prefixOk) { feedback = "❌ 弹错了，重听一遍再来"; input = emptyList() }
                        else if (input.size == seq.size) { feedback = "🎉 完全正确！你的耳朵升级了"; phase = "idle"; input = emptyList() }
                        else feedback = "✅ 第 ${input.size} 个对了，继续…"
                    }
                },
                modifier = Modifier.weight(1f).height(70.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("哆", fontSize = 14.sp); Text("1̇", fontSize = 11.sp)
            } }
        }
        Text(
            "这是「听出旋律」的核心训练：听久了你就能像记住歌词一样记住音。",
            fontSize = 12.sp, color = Color(0xFF77718F), lineHeight = 20.sp
        )
    }
}
