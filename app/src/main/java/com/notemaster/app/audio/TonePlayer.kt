package com.notemaster.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.exp
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
            val n = SAMPLE_RATE * durationMs / 1000
            val pcm = ShortArray(n)
            var phase = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                // 瞬时频率从 freq 线性滑向 freqEnd（用相位累加保证连续无爆音）
                val f = if (freqEnd == freq) freq
                else freq + (freqEnd - freq) * (t * 1000 / durationMs)
                phase += 2 * PI * f / SAMPLE_RATE
                // 基频 + 二三次泛音，接近拨弦/钢琴的亮色
                val v = sin(phase) +
                        0.45 * sin(2 * phase) +
                        0.18 * sin(3 * phase)
                // 快速起音 + 指数衰减
                val attack = if (i < SAMPLE_RATE / 100) i / (SAMPLE_RATE / 100.0) else 1.0
                val decay = exp(-3.5 * t)
                pcm[i] = (v * attack * decay * volume * Short.MAX_VALUE / 1.7).toInt().toShort()
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
