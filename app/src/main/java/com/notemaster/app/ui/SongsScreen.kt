package com.notemaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 「歌曲」页：逐句歌词 + 数字谱。
 *  逐句跟读模式（慢速，默认）/ 原速播放模式（严格按原曲 BPM）
 *  播放时逐句逐音高亮；点击任意一句可从该句开始播放；歌词自动跟随滚动。
 */
@Composable
fun SongsScreen() {
    // 用索引保存当前打开的歌曲，切换页面后保持
    var openIndex by rememberSaveable { mutableIntStateOf(-1) }
    if (openIndex >= 0 && openIndex < MusicData.songs.size) {
        SongDetail(MusicData.songs[openIndex]) { openIndex = -1 }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("🎵 歌曲读谱训练", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "六首歌的简谱与原曲速度均已对照权威简谱逐句校准。逐句跟读=慢速练耳；⚡原速=严格按原曲 BPM 播放。播放时蓝色数字逐个点亮，歌词自动跟随。点任意一句可从那句开始播。",
                    fontSize = 13.sp, color = Color(0xFF9A92B8), lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            items(MusicData.songs) { song ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { openIndex = MusicData.songs.indexOf(song) }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${song.title} · ${song.artist}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("原曲 ${song.bpm} BPM · ${song.note}", fontSize = 12.sp, color = Color(0xFF9A92B8))
                        }
                        Text("▶", fontSize = 20.sp, color = Color(0xFFFFD166))
                    }
                }
            }
            item {
                Text(
                    "音准大脑 · 作者 罗根 ❤ 用心做的音乐学习工具",
                    fontSize = 12.sp, color = Color(0xFF8888A0), fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun SongDetail(song: MusicData.Song, onBack: () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableIntStateOf(0) }   // 0=逐句跟读(慢) 1=原速
    var activeLine by remember { mutableIntStateOf(-1) }
    var activeNote by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    var playJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    // 预解析每句的音符
    val noteLines = remember(song) { song.lines.map { MusicData.parseMelody(it.melody) } }

    fun stop() {
        playJob?.cancel()
        playJob = null
        playing = false
        activeLine = -1
        activeNote = -1
    }

    fun start(fromLine: Int) {
        playJob?.cancel()
        playing = true
        playJob = scope.launch {
            // 逐句跟读 = 慢速教学；原速 = 按原曲 BPM × 每字拍数，精确还原发行速度
            val beatMs = if (mode == 0)
                (60000f / song.bpm) * song.noteBeats * 1.8f
            else
                (60000f / song.bpm) * song.noteBeats
            for (li in fromLine until song.lines.size) {
                if (!playing) break
                activeLine = li
                activeNote = -1
                val notes = noteLines[li]
                for (ni in notes.indices) {
                    if (!playing) break
                    val n = notes[ni]
                    activeNote = ni
                    val freq = MusicData.stepToFreq(n.step, n.octave)
                    if (freq > 0) TonePlayer.play(freq, (n.beats * beatMs * 0.9f).toInt())
                    delay((n.beats * beatMs).toLong())
                }
                // 逐句跟读模式句间停顿帮助消化；原速模式不停顿，保持原曲连续性
                if (mode == 0) delay(300)
            }
            playing = false
            activeLine = -1
            activeNote = -1
        }
    }

    // 播放到哪一句，歌词自动滚到哪一句
    LaunchedEffect(activeLine) {
        if (activeLine >= 0) {
            runCatching { listState.animateScrollToItem(activeLine) }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // 紧凑标题行：返回 + 歌名 + BPM
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { stop(); onBack() },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("← 返回") }
            Text(
                "${song.title} · ${song.artist}",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 1
            )
            Text("原曲 ${song.bpm} BPM", fontSize = 11.sp, color = Color(0xFF7BC7FF))
        }

        // 控制行：模式切换 + 播放按钮放一行，把屏幕尽量留给歌词
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == 0,
                onClick = { stop(); mode = 0 },
                label = { Text("🐢 逐句跟读") }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { stop(); mode = 1 },
                label = { Text("⚡ 原速") }
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (playing) stop() else start(0) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (playing) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
                )
            ) { Text(if (playing) "⏸ 停止" else "▶ 从头播放") }
        }
        Text(
            if (mode == 0)
                "慢速教学：逐句播放、句间停顿，点任意一句歌词就从那一句开始。"
            else
                "原速模式：严格按原曲发行速度（${song.bpm} BPM）连续播放，蓝色数字随音点亮。",
            fontSize = 11.sp, color = Color(0xFF77718F), lineHeight = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
        )

        // 歌词 + 数字谱：占据剩余全部空间，能看到多句歌词并自由滑动
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(song.lines) { li, line ->
                val active = li == activeLine
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (active) Color(0xFF3A2F63) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { stop(); start(li) }
                        .padding(10.dp)
                ) {
                    Text(
                        line.lyric,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) Color(0xFFFFD166) else Color(0xFFDDD6EE)
                    )
                    Spacer(Modifier.height(6.dp))
                    // 逐音数字谱：播放到哪个音就点亮哪个
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        noteLines[li].forEachIndexed { ni, n ->
                            val lit = active && ni == activeNote
                            Text(
                                MusicData.displayToken(n),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lit) Color(0xFFFFD166) else if (active) Color.White else Color(0xFF7BC7FF),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(
                                        if (lit) Color(0xFF6750A4) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
