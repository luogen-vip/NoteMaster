package com.notemaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * 「歌曲」页：热门歌曲简化简谱，逐句歌词 + 数字谱，可整曲播放并高亮当前句。
 */
@Composable
fun SongsScreen() {
    var openSong by remember { mutableStateOf<MusicData.Song?>(null) }
    if (openSong != null) {
        SongDetail(openSong!!) { openSong = null }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("🎵 歌曲读谱训练", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "点开一首歌：先看歌词对应的数字谱，再点播放——播放时数字会逐句点亮。看 10 遍，你就「看得懂谱」了。（谱为教学简化版，1=C）",
                    fontSize = 13.sp, color = Color(0xFF9A92B8)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(MusicData.songs) { song ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { openSong = song }
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
    var playingLine by remember { mutableStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var playJob by remember { mutableStateOf<Job?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                playJob?.cancel()
                playing = false
                playingLine = -1
                onBack()
            }) { Text("← 返回") }
            Spacer(Modifier.weight(1f))
        }
        Text("${song.title} · ${song.artist}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(song.note, fontSize = 12.sp, color = Color(0xFF9A92B8))
        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                if (playing) {
                    playJob?.cancel()
                    playing = false
                    playingLine = -1
                } else {
                    playing = true
                    playJob = scope.launch {
                        val beatMs = (60000f / song.bpm) * 2 // 每音 2 拍感觉放缓，便于跟读
                        for ((li, line) in song.lines.withIndex()) {
                            if (!playing) break
                            playingLine = li
                            val notes = MusicData.parseMelody(line.melody)
                            for (n in notes) {
                                if (!playing) break
                                val freq = MusicData.stepToFreq(n.step, n.octave)
                                if (freq > 0) TonePlayer.play(freq, (n.beats * beatMs * 0.9).toInt())
                                delay((n.beats * beatMs).toLong())
                            }
                            delay(300)
                        }
                        playing = false
                        playingLine = -1
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (playing) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
            )
        ) { Text(if (playing) "⏸ 停止" else "▶ 逐句播放（边听边看）") }

        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(song.lines) { li, line ->
                val active = li == playingLine
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (active) Color(0xFF3A2F63) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        line.lyric,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) Color(0xFFFFD166) else Color(0xFFDDD6EE)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        line.melody.replace("|", ""),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else Color(0xFF7BC7FF),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
