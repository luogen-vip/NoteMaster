package com.notemaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.*
import com.notemaster.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 关键修复：忽略系统「超大字体」设置（封顶 1.0）。
            // 用户开了大字体后，固定宽度的按钮行会把「停止/随机灵感」等按钮
            // 挤成细竖条或圆形（文字完全挤没），底部导航也会折行。封顶后全 App 布局恢复稳定。
            val d = LocalDensity.current
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(
                    LocalDensity provides Density(d.density, fontScale = minOf(d.fontScale, 1.0f))
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val items = listOf(
        Triple("learn", "学乐理", "📚"),
        Triple("train", "练耳", "👂"),
        Triple("piano", "钢琴", "🎹"),
        Triple("songs", "歌曲", "🎵"),
        Triple("write", "写歌", "✍️")
    )
    Scaffold(
        containerColor = Color(0xFF1C1B22),
        bottomBar = {
            NavigationBar {
                val cur = nav.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { (route, label, emoji) ->
                    NavigationBarItem(
                        selected = cur == route,
                        onClick = {
                            nav.navigate(route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(emoji) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            nav,
            startDestination = "learn",
            modifier = Modifier.padding(pad).background(Color(0xFF1C1B22)),
            // 关闭切页过渡动画：切换导航栏时页面直接切换，避免闪烁
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("learn") { LearnScreen() }
            composable("train") { TrainScreen() }
            composable("piano") { PianoScreen() }
            composable("songs") { SongsScreen() }
            composable("write") { WriteScreen() }
        }
    }
}
