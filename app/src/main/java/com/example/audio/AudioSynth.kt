package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin


object AudioSynth {
    private const val TAG = "AudioSynth"
    private const val SAMPLE_RATE = 22050 // Use 22050Hz for optimized overhead and retro feels
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a target note frequency for a specific duration with a brassy retro envelope.
     */
    fun playFreq(frequencyHz: Float, durationMs: Int = 400) {
        if (frequencyHz <= 0f) return
        scope.launch {
            try {
                val numSamples = (durationMs / 1000f * SAMPLE_RATE).toInt()
                val generatedSnd = ByteArray(2 * numSamples)

                // Generate audio samples
                // Mix Sine and Triangle/Saw waves for a trumpet brassy sound
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val angle = 2.0 * Math.PI * frequencyHz * t
                    
                    // Core wave mixture
                    val sine = sin(angle)
                    val triangle = if ( ( (angle / Math.PI) % 2.0 ) >= 1.0 ) {
                        2.0 * ( (angle / Math.PI) % 1.0 ) - 1.0
                    } else {
                        1.0 - 2.0 * ( (angle / Math.PI) % 1.0 )
                    }
                    
                    // Combine waves to give it some brass harmonic grit
                    var rawVal = (sine * 0.55 + triangle * 0.45)

                    // ADSR Envelope
                    // Attack: first 5%, Decay: next 5%, Sustain: 70% level, Release: last 15%
                    val progress = i.toFloat() / numSamples
                    val amp = when {
                        progress < 0.05f -> progress / 0.05f // Attack
                        progress < 0.10f -> 1.0f - (progress - 0.05f) / 0.05f * 0.3f // Decay to 0.7 sustain
                        progress < 0.85f -> 0.7f // Sustain
                        else -> 0.7f * (1.0f - (progress - 0.85f) / 0.15f) // Release
                    }

                    // Scaled value to 16bit PCM limits (-32768 to 32767)
                    val sampleVal = (rawVal * amp * 32767).toInt().coerceIn(-32768, 32767)

                    // 16 bit PCM needs low and high bytes
                    generatedSnd[2 * i] = (sampleVal and 0x00ff).toByte()
                    generatedSnd[2 * i + 1] = ((sampleVal and 0xff00) ushr 8).toByte()
                }

                // Play Audio using AudioTrack in streaming or static mode
                val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(generatedSnd.size)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.size,
                        AudioTrack.MODE_STATIC
                    )
                }

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                
                // Allow completion then release audio resource to prevent system leaks
                kotlinx.coroutines.delay(durationMs + 100L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing synthesized note", e)
            }
        }
    }

    /**
     * Plays standard game sound effects using programmatic FM synthesis
     */
    fun playSFX(type: SFXType) {
        scope.launch {
            when (type) {
                SFXType.HIT_SUCCESS -> {
                    // Quick triumphant arpeggio C major
                    playFreq(261.63f, 100)
                    kotlinx.coroutines.delay(100)
                    playFreq(329.63f, 100)
                    kotlinx.coroutines.delay(100)
                    playFreq(392.00f, 250)
                }
                SFXType.HIT_FAIL -> {
                    // Sad downward pitch bend
                    playFreq(220f, 150)
                    kotlinx.coroutines.delay(150)
                    playFreq(147f, 300)
                }
                SFXType.LEVEL_UP -> {
                    // Level up power chord
                    playFreq(261.63f, 80)
                    kotlinx.coroutines.delay(80)
                    playFreq(392.00f, 80)
                    kotlinx.coroutines.delay(80)
                    playFreq(523.25f, 400)
                }
                SFXType.ITEM_BUY -> {
                    // Coin chiming
                    playFreq(880f, 80)
                    kotlinx.coroutines.delay(90)
                    playFreq(1174f, 200)
                }
            }
        }
    }

    enum class SFXType {
        HIT_SUCCESS,
        HIT_FAIL,
        LEVEL_UP,
        ITEM_BUY
    }
}
