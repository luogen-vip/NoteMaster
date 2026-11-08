package com.notemaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/**
 * 「钢琴」页：一个八度白键 + 黑键，白键标注 唱名/数字/音名。
 * 结构：Box 叠黑白键，简单稳定的双行布局。
 */
@Composable
fun PianoScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🎹 互动钢琴", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "点按键发声。记住：白键从左到右 = C D E F G A B = 哆来咪发嗦拉西 = 1234567，越靠右越高。",
            fontSize = 13.sp, color = Color(0xFF9A92B8)
        )

        // 低八度键盘
        KeyboardRow(baseOctave = 0)
        Spacer(Modifier.height(4.dp))
        Text("高八度（唱名相同，声音更亮）", fontSize = 13.sp, color = Color(0xFF77718F))
        KeyboardRow(baseOctave = 1)

        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("🎵 试一试：跟着弹 C 大调音阶", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "从最左边的 哆(1) 依次弹到最右边，再弹回来。你会听到 3→4、7→高1 之间「贴得最紧」——那就是半音。",
                    fontSize = 13.sp, color = Color(0xFFB0A7C6), lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun KeyboardRow(baseOctave: Int) {
    Column {
        // 黑键行
        Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..7).forEach { n ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (n in listOf(1, 2, 4, 5, 6)) {
                        val freq = MusicData.stepToFreq(n, baseOctave) * 1.0595 // 升半音
                        val label = MusicData.letters[n - 1] + "♯"
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(0.85f)
                                .width(34.dp)
                                .background(Color(0xFF111118), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF3A3350), RoundedCornerShape(6.dp))
                                .clickable { TonePlayer.play(freq, 500) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 10.sp, color = Color(0xFF8888A0))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // 白键行
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..7).forEach { n ->
                Button(
                    onClick = { TonePlayer.play(MusicData.stepToFreq(n, baseOctave), 700) },
                    modifier = Modifier.weight(1f).height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2EEFA)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            MusicData.solfege[n - 1],
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2350)
                        )
                        Text(
                            if (baseOctave > 0) "${n}′" else "$n",
                            fontSize = 13.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold
                        )
                        Text(
                            MusicData.letters[n - 1],
                            fontSize = 11.sp, color = Color(0xFF8888A0)
                        )
                    }
                }
            }
        }
    }
}
