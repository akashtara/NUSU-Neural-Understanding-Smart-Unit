package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.util.Log
import com.example.api.SystemErrorState
import com.example.api.LogLevel

enum class SoundPack {
    SILENT,
    SOFT_PULSE,
    DIGITAL_RESONANCE,
    HOLOGRAPHIC_CHORD
}

object NusuSoundManager {
    private const val TAG = "NusuSoundManager"
    private const val SAMPLE_RATE = 44100
    
    var currentSoundPack: SoundPack = SoundPack.SOFT_PULSE
    var isHapticEnabled: Boolean = true

    fun playCue(context: Context, type: String) {
        if (currentSoundPack == SoundPack.SILENT) {
            triggerHaptic(context, type)
            return
        }

        triggerHaptic(context, type)

        try {
            val audioData = when (currentSoundPack) {
                SoundPack.SOFT_PULSE -> generateSoftPulse(type)
                SoundPack.DIGITAL_RESONANCE -> generateDigitalResonance(type)
                SoundPack.HOLOGRAPHIC_CHORD -> generateHolographicChord(type)
                else -> null
            }

            if (audioData != null) {
                val bufferSize = audioData.size
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(audioData, 0, audioData.size)
                audioTrack.play()
                
                SystemErrorState.log(
                    level = LogLevel.INFO,
                    tag = "SOUND_ENGINE",
                    message = "Played audio cue: $type",
                    details = "Pack: $currentSoundPack | Size: ${audioData.size} shorts"
                )

                // Stop and release to clean up resources after sound duration
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (e: Exception) {}
                }, 1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synthesize NusuSound: ${e.message}")
        }
    }

    private fun triggerHaptic(context: Context, type: String) {
        if (!isHapticEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (type) {
                        "start" -> VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                        "end" -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                        "error" -> VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60), intArrayOf(0, 180, 0, 180), -1)
                        else -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic vibration failed: ${e.message}")
        }
    }

    private fun generateSoftPulse(type: String): ShortArray {
        val duration = if (type == "start") 0.16 else 0.08
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        val frequency = if (type == "start") 880.0 else 440.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = Math.exp(-t * 22.0)
            val sine = Math.sin(2.0 * Math.PI * frequency * t)
            samples[i] = (sine * Short.MAX_VALUE * 0.35 * envelope).toInt().toShort()
        }
        return samples
    }

    private fun generateDigitalResonance(type: String): ShortArray {
        val duration = 0.22
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        
        val startFreq = if (type == "start") 440.0 else 1100.0
        val endFreq = if (type == "start") 1100.0 else 330.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = Math.sin(Math.PI * (i.toDouble() / numSamples))
            val currentFreq = startFreq + (endFreq - startFreq) * (i.toDouble() / numSamples)
            val sine = Math.sin(2.0 * Math.PI * currentFreq * t)
            samples[i] = (sine * Short.MAX_VALUE * 0.4 * envelope).toInt().toShort()
        }
        return samples
    }

    private fun generateHolographicChord(type: String): ShortArray {
        val duration = 0.32
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)
        
        val baseFreq = if (type == "start") 587.33 else 440.0 // D5 or A4 base
        val freqs = doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5, baseFreq * 1.875)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = Math.exp(-t * 12.0)
            var mixedWave = 0.0
            for (f in freqs) {
                mixedWave += Math.sin(2.0 * Math.PI * f * t)
            }
            mixedWave /= freqs.size
            samples[i] = (mixedWave * Short.MAX_VALUE * 0.35 * envelope).toInt().toShort()
        }
        return samples
    }
}
