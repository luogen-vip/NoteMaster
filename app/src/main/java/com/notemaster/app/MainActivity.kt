package com.notemaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.notemaster.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppRoot()
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
        NavHost(nav, startDestination = "learn", modifier = Modifier.padding(pad)) {
            composable("learn") { LearnScreen() }
            composable("train") { TrainScreen() }
            composable("piano") { PianoScreen() }
            composable("songs") { SongsScreen() }
            composable("write") { WriteScreen() }
        }
    }
}
