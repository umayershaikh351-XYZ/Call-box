package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.abs

class AudioCaptureEngine(private val context: Context) {

    private val tag = "AudioCaptureEngine"

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _activeAppLabel = MutableStateFlow("Manual Call")
    val activeAppLabel: StateFlow<String> = _activeAppLabel.asStateFlow()

    private val _activeAppPackage = MutableStateFlow("com.android.phone")
    val activeAppPackage: StateFlow<String> = _activeAppPackage.asStateFlow()

    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Fallback MediaRecorder instance if MediaCodec pipeline encounters device codec restriction
    private var mediaRecorder: MediaRecorder? = null
    private var isUsingMediaRecorder = false

    fun startRecording(
        outputFile: File,
        appLabel: String = "Manual Call",
        appPackage: String = "com.android.phone",
        bitrate: Int = 96000,
        preferVoiceCallSource: Boolean = true
    ): Boolean {
        if (_isRecording.value) {
            Log.w(tag, "Recording is already active")
            return false
        }

        currentOutputFile = outputFile
        _activeAppLabel.value = appLabel
        _activeAppPackage.value = appPackage
        _durationMs.value = 0L
        _amplitude.value = 0f

        val started = try {
            startMediaCodecRecording(outputFile, bitrate, preferVoiceCallSource)
        } catch (e: Exception) {
            Log.w(tag, "MediaCodec pipeline failed, falling back to MediaRecorder: ${e.message}")
            startMediaRecorderFallback(outputFile, bitrate, preferVoiceCallSource)
        }

        if (started) {
            _isRecording.value = true
            startTicker()
        }
        return started
    }

    private fun startMediaCodecRecording(
        outputFile: File,
        bitrate: Int,
        preferVoiceCallSource: Boolean
    ): Boolean {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val channelCount = 2
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize <= 0) {
            bufferSize = 4096 * 4
        } else {
            bufferSize *= 2
        }

        // Priority order of audio sources
        val sources = if (preferVoiceCallSource) {
            intArrayOf(
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC
            )
        } else {
            intArrayOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC
            )
        }

        var audioRecord: AudioRecord? = null
        for (src in sources) {
            try {
                val candidate = AudioRecord(
                    src,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord = candidate
                    Log.i(tag, "Initialized AudioRecord with source: $src")
                    break
                } else {
                    candidate.release()
                }
            } catch (e: Exception) {
                Log.d(tag, "Source $src not supported on this device/ROM: ${e.message}")
            }
        }

        // Try mono if stereo failed
        if (audioRecord == null) {
            for (src in sources) {
                try {
                    val candidate = AudioRecord(
                        src,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        audioFormat,
                        bufferSize
                    )
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord = candidate
                        Log.i(tag, "Initialized mono AudioRecord with source: $src")
                        break
                    } else {
                        candidate.release()
                    }
                } catch (e: Exception) {
                    Log.d(tag, "Mono source $src not supported: ${e.message}")
                }
            }
        }

        if (audioRecord == null) {
            throw IllegalStateException("Unable to initialize any AudioRecord source")
        }

        val recordInstance = audioRecord
        isUsingMediaRecorder = false

        recordingJob = scope.launch {
            var mediaCodec: MediaCodec? = null
            var mediaMuxer: MediaMuxer? = null
            var muxerStarted = false
            var trackIndex = -1

            try {
                val format = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    sampleRate,
                    channelCount
                ).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
                }

                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                mediaCodec.start()

                mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                recordInstance.startRecording()
                val audioBuffer = ByteArray(bufferSize)
                val bufferInfo = MediaCodec.BufferInfo()
                var presentationTimeUs = 0L
                val bytesPerSample = 2 * channelCount // 16-bit stereo

                while (isActive && _isRecording.value) {
                    val readBytes = recordInstance.read(audioBuffer, 0, audioBuffer.size)
                    if (readBytes > 0) {
                        // Calculate peak amplitude for visualizer
                        var maxAmp = 0
                        var i = 0
                        while (i < readBytes - 1) {
                            val sample = ((audioBuffer[i + 1].toInt() shl 8) or (audioBuffer[i].toInt() and 0xFF)).toShort()
                            val absSample = abs(sample.toInt())
                            if (absSample > maxAmp) {
                                maxAmp = absSample
                            }
                            i += 2
                        }
                        _amplitude.value = (maxAmp / 32768f).coerceIn(0f, 1f)

                        // Feed to MediaCodec
                        val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer: ByteBuffer? = mediaCodec.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(audioBuffer, 0, readBytes)
                            mediaCodec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                readBytes,
                                presentationTimeUs,
                                0
                            )
                            val samplesRead = readBytes / bytesPerSample
                            presentationTimeUs += (samplesRead * 1_000_000L) / sampleRate
                        }
                    }

                    // Drain encoder
                    var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    while (outputBufferIndex >= 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                            continue
                        }

                        if (!muxerStarted) {
                            val newFormat = mediaCodec.outputFormat
                            trackIndex = mediaMuxer.addTrack(newFormat)
                            mediaMuxer.start()
                            muxerStarted = true
                        }

                        val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in audio encoding loop: ${e.message}")
            } finally {
                try {
                    recordInstance.stop()
                    recordInstance.release()
                } catch (ignored: Exception) {}

                try {
                    mediaCodec?.stop()
                    mediaCodec?.release()
                } catch (ignored: Exception) {}

                try {
                    if (muxerStarted) {
                        mediaMuxer?.stop()
                    }
                    mediaMuxer?.release()
                } catch (ignored: Exception) {}
            }
        }
        return true
    }

    private fun startMediaRecorderFallback(
        outputFile: File,
        bitrate: Int,
        preferVoiceCallSource: Boolean
    ): Boolean {
        return try {
            val recorder = MediaRecorder()
            mediaRecorder = recorder
            isUsingMediaRecorder = true

            // Configure audio source
            val audioSource = if (preferVoiceCallSource) {
                try {
                    recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                } catch (e: Exception) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    MediaRecorder.AudioSource.MIC
                }
            } else {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                MediaRecorder.AudioSource.MIC
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(bitrate)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioChannels(2)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            // Amplitude sampler loop for visualizer
            recordingJob = scope.launch {
                while (isActive && _isRecording.value) {
                    delay(100)
                    try {
                        val maxAmp = recorder.maxAmplitude
                        _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (ignored: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "MediaRecorder failed: ${e.message}")
            false
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        val startTime = System.currentTimeMillis()
        tickerJob = scope.launch {
            while (isActive && _isRecording.value) {
                delay(200)
                _durationMs.value = System.currentTimeMillis() - startTime
            }
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        tickerJob?.cancel()
        tickerJob = null

        if (isUsingMediaRecorder) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.w(tag, "Exception stopping mediaRecorder: ${e.message}")
            }
            mediaRecorder = null
            isUsingMediaRecorder = false
        }

        recordingJob?.cancel()
        recordingJob = null

        _amplitude.value = 0f
        val result = currentOutputFile
        currentOutputFile = null
        return result
    }
}
