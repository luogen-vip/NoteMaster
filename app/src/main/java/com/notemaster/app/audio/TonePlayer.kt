package com.notemaster.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * 内置钢琴音色合成器：无需任何音频素材，实时合成带泛音和衰减包络的音。
 * 支持滑音：freqEnd != freq 时，音高在时长内从 freq 连续滑到 freqEnd（下滑音效果）。
 *
 * 并发模型：每个音一条独立 AudioTrack，登记在 active 列表里；
 * v1.6 的 bug 是新音被上一个未结束的音"误杀"（快弹时哑音、声音发怪），v1.7 修复：
 *  - 新音永远正常发声，互不干扰；
 *  - stopAll() 只用于离开页面，停掉当前所有在响的音。
 */
object TonePlayer {

    private const val SAMPLE_RATE = 44100

    /** 当前所有在响的轨道 */
    private val active = CopyOnWriteArrayList<AudioTrack>()

    /** 立即停止当前所有发声（离开页面时调用，避免切页后声音残留） */
    fun stopAll() {
        for (t in active) {
            runCatching { t.pause() }
            runCatching { t.flush() }
        }
        for (t in active) {
            runCatching { t.release() }
        }
        active.clear()
    }

    /** 播放一个音；freqEnd 不等于 freq 时做连续滑音 */
    fun play(freq: Double, durationMs: Int = 600, volume: Double = 0.8, freqEnd: Double = freq) {
        if (freq <= 0.0) return
        Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            // 低音区响度补偿：频率越低听感越弱，轻微提升增益（最多 +22%）
            val comp = 1.0 + (300.0 - freq).coerceAtLeast(0.0) / 300.0 * 0.22
            val vol = (volume * comp).coerceAtMost(1.0)
            val n = SAMPLE_RATE * durationMs / 1000
            val pcm = ShortArray(n)
            var phase = 0.0
            val durSec = durationMs / 1000.0
            // 衰减速率随音长自适应：保证音在整段时值内都保持可闻，
            // 结尾仍在 ~35% 响度（旧版固定 exp(-3.5t)，长音放到一半就没声了）
            val decayK = (-ln(0.35) / durSec).coerceIn(1.2, 4.5)
            // 末尾 45ms 线性淡出，消除音尾截断的「咔」声（破音来源）
            val fadeN = minOf(n, SAMPLE_RATE * 45 / 1000)
            val attackN = SAMPLE_RATE / 200   // 5ms 快速起音
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                // 瞬时频率：按指数（等程）从 freq 匀速滑向 freqEnd，
                // 人耳对音高是对数感知，指数滑音听起来才是匀速下滑
                val f = if (freqEnd == freq) freq
                else freq * Math.pow(freqEnd / freq, (t / durSec).coerceIn(0.0, 1.0))
                phase += 2 * PI * f / SAMPLE_RATE
                // 基频 + 四个泛音，更接近人声/钢琴的亮色
                val v = sin(phase) +
                        0.45 * sin(2 * phase) +
                        0.18 * sin(3 * phase) +
                        0.09 * sin(4 * phase) +
                        0.05 * sin(5 * phase)
                val attack = if (i < attackN) i / attackN.toDouble() else 1.0
                val decay = exp(-decayK * t)
                var env = attack * decay
                if (i >= n - fadeN) env *= (n - i).toDouble() / fadeN
                pcm[i] = (v * env * vol * Short.MAX_VALUE / 2.0).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            active.add(track)
            track.play()
            Thread.sleep(durationMs.toLong() + 150)
            // 播完自行收尾；若 stopAll 已释放过，重复释放会被 runCatching 兜住不崩溃
            active.remove(track)
            runCatching { track.stop() }
            runCatching { track.release() }
        }.start()
    }
}
