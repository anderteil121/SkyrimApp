package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.ShoutId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val activeTracks = java.util.concurrent.atomic.AtomicInteger(0)

    private fun generateBuffer(
        durationSeconds: Float,
        sampleRate: Int = 22050,
        generator: (time: Float, duration: Float) -> Float
    ): ShortArray {
        val numSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val time = i.toFloat() / sampleRate
            val sampleVal = generator(time, durationSeconds).coerceIn(-1.0f, 1.0f)
            buffer[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun playBuffer(
        buffer: ShortArray,
        durationSeconds: Float,
        sampleRate: Int = 22050
    ) {
        if (activeTracks.get() >= 4) return
        scope.launch {
            if (activeTracks.incrementAndGet() > 4) {
                activeTracks.decrementAndGet()
                return@launch
            }
            var audioTrack: AudioTrack? = null
            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                kotlinx.coroutines.delay((durationSeconds * 1000).toLong() + 50)
            } catch (_: Exception) {
                // Ignore audio generation failures gracefully
            } finally {
                try {
                    audioTrack?.release()
                } catch (_: Exception) {}
                activeTracks.decrementAndGet()
            }
        }
    }

    private fun playTone(
        durationSeconds: Float,
        sampleRate: Int = 22050,
        generator: (time: Float, duration: Float) -> Float
    ) {
        val buffer = generateBuffer(durationSeconds, sampleRate, generator)
        playBuffer(buffer, durationSeconds, sampleRate)
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 120) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    // Precomputed deterministic PCM audio buffers for high-frequency game sounds
    private val swordClashBuffer by lazy {
        generateBuffer(0.28f) { time, _ ->
            val env = exp(-time * 18f)
            val metal = sin(2f * PI.toFloat() * 1420f * time) * 0.4f +
                    sin(2f * PI.toFloat() * 2840f * time) * 0.3f +
                    sin(2f * PI.toFloat() * 880f * time) * 0.3f
            metal * env
        }
    }

    private val warHornBuffer by lazy {
        generateBuffer(1.2f) { time, _ ->
            val env = if (time < 0.25f) time / 0.25f else exp(-(time - 0.25f) * 2.2f)
            val freq = 220f + 10f * sin(2f * PI.toFloat() * 4f * time)
            val horn = sin(2f * PI.toFloat() * freq * time) * 0.6f +
                    sin(2f * PI.toFloat() * (freq * 1.5f) * time) * 0.3f +
                    sin(2f * PI.toFloat() * (freq * 2f) * time) * 0.1f
            horn * env * 0.8f
        }
    }

    private val goldCoinsBuffer by lazy {
        generateBuffer(0.3f) { time, _ ->
            val env1 = exp(-time * 20f)
            val env2 = if (time > 0.08f) exp(-(time - 0.08f) * 20f) else 0f
            val ping1 = sin(2f * PI.toFloat() * 1960f * time) * env1 * 0.5f
            val ping2 = sin(2f * PI.toFloat() * 2637f * (time - 0.08f)) * env2 * 0.5f
            ping1 + ping2
        }
    }

    private val conquestBuffer by lazy {
        generateBuffer(1.4f) { time, _ ->
            val noteFreq = when {
                time < 0.22f -> 261.6f
                time < 0.44f -> 329.6f
                time < 0.70f -> 392.0f
                else -> 523.2f
            }
            val noteTime = when {
                time < 0.22f -> time
                time < 0.44f -> time - 0.22f
                time < 0.70f -> time - 0.44f
                else -> time - 0.70f
            }
            val env = exp(-noteTime * 2.8f)
            val brass = sin(2f * PI.toFloat() * noteFreq * time) * 0.6f +
                    sin(2f * PI.toFloat() * (noteFreq * 2f) * time) * 0.3f
            brass * env * 0.75f
        }
    }

    // 1. Звон стали / мечей (Sword clash)
    fun playSwordClash() {
        vibrate(35, 160)
        playBuffer(swordClashBuffer, 0.28f)
    }

    // 2. Нордский боевой рог (Nordic War Horn)
    fun playWarHorn() {
        vibrate(120, 200)
        playBuffer(warHornBuffer, 1.2f)
    }

    // 3. Драконий Крик Ту'ума (Dovahkiin Shout)
    fun playShout(shoutId: ShoutId) {
        vibrate(220, 255)
        val baseFreq = shoutId.voiceAudioTone
        playTone(durationSeconds = 1.0f) { time, _ ->
            val pitch = baseFreq * (1.2f - time * 0.4f)
            val env = exp(-time * 2.5f)
            val wave = sin(2f * PI.toFloat() * pitch * time) * 0.7f +
                    sin(2f * PI.toFloat() * (pitch * 0.5f) * time) * 0.5f +
                    (Math.random().toFloat() * 0.2f - 0.1f)
            wave * env
        }
    }

    // 4. Золотые септимы (Coins)
    fun playGoldCoins() {
        vibrate(25, 80)
        playBuffer(goldCoinsBuffer, 0.3f)
    }

    // 5. Захват крепости / Победный аккорд (Victory / Conquest)
    fun playConquest() {
        vibrate(180, 240)
        playBuffer(conquestBuffer, 1.4f)
    }
}
