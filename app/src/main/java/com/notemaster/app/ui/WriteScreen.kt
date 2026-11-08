package com.notemaster.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notemaster.app.audio.TonePlayer
import com.notemaster.app.data.MusicData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 「写歌」页：和弦进行库（一键试听）+ 八格旋律创作板（选音、试听）。
 * 状态均使用 rememberSaveable，切换页面后保持。
 */
@Composable
fun WriteScreen() {
    val listState = rememberLazyListState()
    var feedback by rememberSaveable { mutableStateOf("") }

    // 提示文字变化时自动滚到底部，确保完整可见
    LaunchedEffect(feedback) {
        if (feedback.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            runCatching { listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("✍️ 写歌工坊", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "第一步：选一个和弦进行循环播放，跟着哼；第二步：在创作板把你哼出来的音记下来并试听。",
                fontSize = 13.sp, color = Color(0xFF9A92B8), lineHeight = 20.sp
            )
        }
        item { ChordSection() }
        item { ComposerSection(onFeedback = { feedback = it }) }
    }
}

@Composable
private fun ChordSection() {
    var playJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var activeIdx by remember { mutableIntStateOf(-1) }
    var activeProg by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("🎸 万能和弦进行", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            MusicData.chordProgs.forEach { prog ->
                val active = activeProg == prog.name
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            prog.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (active) Color(0xFFFFD166) else Color.White
                        )
                        Text(prog.feel, fontSize = 12.sp, color = Color(0xFF9A92B8))
                    }
                    Text(
                        prog.roots.joinToString(" ") { MusicData.chordLabel(it) },
                        fontSize = 13.sp, color = Color(0xFF7BC7FF), modifier = Modifier.padding(end = 10.dp)
                    )
                    Button(
                        onClick = {
                            playJob?.cancel()
                            if (active) {
                                activeProg = ""; activeIdx = -1
                            } else {
                                activeProg = prog.name
                                playJob = scope.launch {
                                    var loop = 0
                                    while (loop < 2) {
                                        prog.roots.forEachIndexed { i, root ->
                                            activeIdx = i
                                            val notes = MusicData.chordNotes(root, MusicData.isMinorChord(root))
                                            notes.forEach { TonePlayer.play(it, 900) }
                                            delay(950)
                                        }
                                        loop++
                                    }
                                    activeProg = ""; activeIdx = -1
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
                        )
                    ) { Text(if (active) "⏸" else "▶") }
                }
                HorizontalDivider(color = Color(0xFF2E2947))
            }
            Text(
                "和弦名解读：C=135 大三和弦（明亮），Am=613 小三和弦（柔和）。写歌时让旋律尽量落在当前和弦的三个音上，就一定和谐。",
                fontSize = 12.sp, color = Color(0xFF77718F),
                lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ComposerSection(onFeedback: (String) -> Unit) {
    // 八格旋律：用字符串保存，切页不丢；oct 为统一八度
    var gridStr by rememberSaveable { mutableStateOf("1 2 3 4 5 6 7 1") }
    var oct by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val steps = remember(gridStr) { gridStr.split(" ").map { it.toIntOrNull() ?: 1 } }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("🎹 八格旋律创作板", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "点格子换音（数字 1-7 → 哆来咪发嗦拉西，8=休止）。写完点试听。",
                fontSize = 12.sp, color = Color(0xFF9A92B8), lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                steps.forEachIndexed { gi, step ->
                    Button(
                        onClick = {
                            val next = (step % 8) + 1   // 1..8，8 表示休止
                            val newSteps = steps.toMutableList().also { it[gi] = next }
                            gridStr = newSteps.joinToString(" ")
                            if (next != 8) TonePlayer.play(MusicData.stepToFreq(next, oct))
                        },
                        modifier = Modifier.weight(1f).height(76.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (step == 8) Color(0xFF2A2540) else Color(0xFF6750A4)
                        ),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (step == 8) "休" else if (oct > 0) "${step}′" else if (oct < 0) "_${step}_" else "$step",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                            Text(
                                if (step == 8) "—" else MusicData.solfege[step - 1],
                                fontSize = 11.sp, color = Color(0xFFFFD166)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = oct == 0, onClick = { oct = 0 }, label = { Text("中音区") })
                FilterChip(selected = oct == 1, onClick = { oct = 1 }, label = { Text("高音区") })
                FilterChip(selected = oct == -1, onClick = { oct = -1 }, label = { Text("低音区") })
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        for (step in steps) {
                            if (step != 8) TonePlayer.play(MusicData.stepToFreq(step, oct), 500)
                            delay(550)
                        }
                    }
                    onFeedback(
                        if (steps.none { it == 8 })
                            "🎵 好旋律秘诀：开头和结尾落在 1 或 5 上最稳"
                        else "🎵 休止符也是音乐的一部分"
                    )
                }) { Text("▶ 试听") }
                Button(onClick = {
                    gridStr = List(8) { (1..7).random() }.joinToString(" ")
                    onFeedback("🎲 已随机生成，试试听，说不定有惊喜")
                }) { Text("🎲 随机灵感") }
                Button(onClick = {
                    gridStr = "1 1 1 1 1 1 1 1"
                    onFeedback("已重置（全为 1/do）")
                }) { Text("🗑 重置") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "提示：好旋律秘诀——开头和结尾落在 1 或 5 上最稳；休止符也是音乐的一部分。",
                fontSize = 14.sp, color = Color(0xFFFFD166),
                lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
