package com.notemaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.notemaster.app.data.MusicData

/**
 * 「学乐理」页：欢迎卡片 + 8 节零基础乐理课，点开折叠内容。
 */
@Composable
fun LearnScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("音准大脑 🎼", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "目标：把「哆来咪发嗦拉西」练成 1234567 一样脱口而出。\n\n用法：① 先把第1课读完并记住对照表 → ② 每天 10 分钟去「练耳」刷题 → ③ 在「钢琴」上找音 → ④ 跟着「歌曲」读谱 → ⑤ 用「写歌」写出第一句旋律。",
                        fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("🎹 作者：罗根", fontSize = 13.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E))) {
                Column(Modifier.padding(16.dp)) {
                    Text("核心对照表（背到脱口而出）", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    // 7 列等宽自适应：数字 / 唱名 / 音名 一一竖向对齐，保证第 7 列不被裁剪
                    Row(Modifier.fillMaxWidth()) {
                        MusicData.solfege.forEachIndexed { i, name ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${i + 1}", fontSize = 16.sp, color = Color(0xFFB0A7C6), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166), maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(MusicData.letters[i], fontSize = 14.sp, color = Color(0xFF7BC7FF), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // 三行名字各是什么：数字、唱名、音名逐一说明（恢复 1.0 版本的讲解）
                    Text(
                        "三行分别是什么：\n" +
                        "· 数字（简谱）：写谱用的 1 2 3 4 5 6 7，最方便快速记录；\n" +
                        "· 唱名（怎么唱）：哆来咪发嗦拉西，是你嘴里唱出来的名字；\n" +
                        "· 音名（身份证）：C D E F G A B，是每个音固定不变的官方名字。",
                        fontSize = 12.sp, color = Color(0xFF9A92B8), lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "三者一一对应：1=C、2=D、3=E、4=F、5=G、6=A、7=B（以 1=C 的 C 大调为准）。看到任何一个数字、汉字或字母，都要能立刻报出另外两个。",
                        fontSize = 12.sp, color = Color(0xFF8888A0), lineHeight = 19.sp
                    )
                }
            }
        }
        itemsIndexed(MusicData.lessons) { _, lesson ->
            var open by rememberSaveable(lesson.title) { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { open = !open }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            lesson.title,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(if (open) "▲" else "▼", color = Color(0xFFB0A7C6))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(lesson.summary, fontSize = 13.sp, color = Color(0xFF9A92B8), lineHeight = 19.sp)
                    if (open) {
                        Spacer(Modifier.height(10.dp))
                        Text(lesson.body, fontSize = 14.sp, lineHeight = 23.sp, color = Color(0xFFDDD6EE))
                    }
                }
            }
        }
        item {
            Text(
                "学习没有捷径，但正确顺序是：听懂 → 认出 → 唱出 → 写出。每天 10 分钟，两周后你会明显感觉自己「开窍」。",
                fontSize = 12.sp, color = Color(0xFF77718F)
            )
        }
        item {
            Text(
                "音准大脑 · 作者 罗根 ❤ 用心做的音乐学习工具",
                fontSize = 12.sp, color = Color(0xFF8888A0), fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
