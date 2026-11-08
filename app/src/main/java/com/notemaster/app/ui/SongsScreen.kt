package com.notemaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
 *  逐句跟读模式（慢速，默认）/ 原速播放模式（按歌曲 BPM）
 *  播放时逐句逐音高亮；点击任意一句可从该句开始播放。
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
                    "《青花瓷》《童话》简谱已对照原曲逐音校准。逐句跟读=慢速练耳跟读；原速播放=按原歌速度。播放时蓝色数字会逐个点亮。点任意一句可从那句开始播。",
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
                            Text(song.note, fontSize = 12.sp, color = Color(0xFF9A92B8))
                        }
                        Text("▶", fontSize = 20.sp, color = Color(0xFFFFD166))
                    }
                }
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
            val beatMs = if (mode == 0) (60000f / song.bpm) * 1.8f else 60000f / song.bpm
            for (li in fromLine until song.lines.size) {
                if (!playing) break
                activeLine = li
                val notes = noteLines[li]
                for (ni in notes.indices) {
                    if (!playing) break
                    val n = notes[ni]
                    activeNote = ni
                    val freq = MusicData.stepToFreq(n.step, n.octave)
                    if (freq > 0) TonePlayer.play(freq, (n.beats * beatMs * 0.9f).toInt())
                    delay((n.beats * beatMs).toLong())
                }
                delay(200)
            }
            playing = false
            activeLine = -1
            activeNote = -1
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { stop(); onBack() }) { Text("← 返回") }
            Spacer(Modifier.weight(1f))
        }
        Text("${song.title} · ${song.artist}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(song.note, fontSize = 12.sp, color = Color(0xFF9A92B8))
        Spacer(Modifier.height(10.dp))

        // 播放模式切换
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { stop(); mode = 0 },
                label = { Text("🐢 逐句跟读(慢)") }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { stop(); mode = 1 },
                label = { Text("🎵 原速播放") }
            )
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { if (playing) stop() else start(0) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (playing) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
            )
        ) { Text(if (playing) "⏸ 停止播放" else "▶ 从头播放") }
        Spacer(Modifier.height(4.dp))
        Text("💡 点任意一句歌词，就从那一句开始播放", fontSize = 12.sp, color = Color(0xFF77718F))
        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
