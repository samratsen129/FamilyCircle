/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.voiceengine;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AudioEffect.Descriptor;
import android.os.Process;
import android.util.Log;

import com.familycircle.lib.utils.AudioEncoder;
import com.familycircle.sdk.BaseApp;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class  WebRtcAudioRecord {
    private static final boolean DEBUG = false;

    private static final String TAG = "WebRtcAudioRecord";

    // Mono recording is default.
    private static final int CHANNELS = 1;

    // Default audio data format is PCM 16 bit per sample.
    // Guaranteed to be supported by all devices.
    private static final int BITS_PER_SAMPLE = 16;

    // Number of bytes per audio frame.
    // Example: 16-bit PCM in stereo => 2*(16/8)=4 [bytes/frame]
    private static final int BYTES_PER_FRAME = CHANNELS * (BITS_PER_SAMPLE / 8);

    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;

    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    private ByteBuffer byteBuffer;
    private final int bytesPerBuffer;
    private final int framesPerBuffer;
    private final int sampleRate;
    private int recordingSampleRate;
    private final long nativeAudioRecord;
    private final AudioManager audioManager;
    private final Context context;

    private AudioRecord audioRecord = null;
    private AudioRecordThread audioThread = null;

    private AcousticEchoCanceler aec = null;
    private boolean useBuiltInAEC = false;

    public int samples_per_frame = 980;


    /*public void recycleInputBuffer(byte[] buffer){
        audioThread.data_buffer.offer(buffer);
    }*/

    private void copyBytesDest0(byte[] srcByte, byte[]destByte, int startIndex, int len){
        int j=0;
        if (destByte.length < len) new RuntimeException("Size incompatible");
        for (int i=startIndex; i<len ; i++){
            destByte[j++] = srcByte[i];
        }
    }

    private void copyBytes2Src0(byte[] srcByte, byte[]destByte, int startIndex, int len){
        int j=startIndex;
        if (destByte.length < len) new RuntimeException("Size incompatible");
        for (int i=0; i<len ; i++){
            destByte[j++] = srcByte[i];
        }
    }

    /**
     * Audio thread which keeps calling ByteBuffer.read() waiting for audio
     * to be recorded. Feeds recorded data to the native counterpart as a
     * periodic sequence of callbacks using DataIsRecorded().
     * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
     */
    private class AudioRecordThread extends Thread {
        private volatile boolean keepAlive = true;
        private AudioEncoder audioEncoder;
        private boolean isRecording = false;
        //ArrayBlockingQueue<byte[]> data_buffer = new ArrayBlockingQueue<byte[]>(50);

        private int _stopCycles = 0;
        public AudioRecordThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Logd("AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());
            samples_per_frame = byteBuffer.capacity();
            try {
                audioRecord.startRecording();

            } catch (IllegalStateException e) {
                Loge("AudioRecord.startRecording failed: " + e.getMessage());
                return;
            }

            assertTrue(audioRecord.getRecordingState()
                    == AudioRecord.RECORDSTATE_RECORDING);

            /*for(int x=0; x < 25; x++)
                data_buffer.add(new byte[samples_per_frame]);*/

            long lastTime = System.nanoTime();
            long audioPresentationTimeNs=0;
            //byte[] remaining_buffer = new byte[samples_per_frame];
            //int remaining_buffer_length=0;
            boolean readyToSend = false;
            int bytesRead = 0;

            while (keepAlive) {
                readyToSend = false;
                bytesRead = 0;

                byte[] this_buffer = null;
                if (BaseApp.startAudioRecording) {
                    if (!isRecording) {
                        audioEncoder = new AudioEncoder(BaseApp.getContext(), recordingSampleRate);
                        audioEncoder.prepareEncoder(BaseApp.audioOutputFile);
                        isRecording = true;
                        _stopCycles=0;
                    }
                    /*if(data_buffer.isEmpty()){
                        this_buffer = new byte[samples_per_frame];
                    //  Log.i(TAG, "Audio buffer empty. added new buffer");
                    }else{
                      this_buffer = data_buffer.poll();
                    }*/
                } else {
                    /*this_buffer = new byte[samples_per_frame];*/
                }

                //bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
                this_buffer = new byte[samples_per_frame];
                readyToSend=true;
                audioPresentationTimeNs = System.nanoTime();
                bytesRead = audioRecord.read(this_buffer, 0, samples_per_frame);
                int capacity = byteBuffer.capacity();
                byteBuffer.clear();
                byteBuffer.put(this_buffer, 0, capacity);

                if (isRecording) {
                    /*int MIN_SIZE = samples_per_frame - bytesRead;

                    byte[] temp_buffer = byteBuffer.array();
                    if (bytesRead < samples_per_frame) { // bytes read < 1024
                        Log.d(TAG, "BYTES READ, REMAINING " + bytesRead + ", " + remaining_buffer_length);
                        if (remaining_buffer_length > 0) {
                            //TOSEND remaining_buffer_length + extra_bytes_length == 1024
                            int extra_bytes_length = samples_per_frame - remaining_buffer_length;
                            Log.d(TAG, "EXTRA required from NEW " + extra_bytes_length);
                            if (remaining_buffer_length < MIN_SIZE) {
                                //add all of the read bytes to the remaining_buffer
                                copyBytes2Src0(temp_buffer, remaining_buffer, remaining_buffer_length, bytesRead);
                                remaining_buffer_length = remaining_buffer_length + bytesRead;
                                Log.d(TAG, "EXTRA < min size, copying to REMAINING, new REMAINING " + remaining_buffer_length);

                            } else {
                                this_buffer = new byte[samples_per_frame];
                                copyBytesDest0(remaining_buffer, this_buffer, 0, remaining_buffer_length);
                                copyBytes2Src0(temp_buffer, this_buffer, remaining_buffer_length, extra_bytes_length);
                                Log.d(TAG, "SENDING ... " + remaining_buffer_length + " + " + extra_bytes_length);

                                // send it now
                                readyToSend = true;

                                remaining_buffer_length = bytesRead - extra_bytes_length;
                                copyBytesDest0(temp_buffer, remaining_buffer, extra_bytes_length, remaining_buffer_length);
                                Log.d(TAG, "REMAINING ... " + remaining_buffer_length);

                            }

                        } else {
                            //NOTHING TO SEND
                            Log.d(TAG, "INIT " + remaining_buffer_length);
                            copyBytesDest0(temp_buffer, remaining_buffer, 0, bytesRead);
                            remaining_buffer_length = bytesRead;

                        }

                    }*/

                    if ((audioEncoder != null)
                            && (bytesRead == this_buffer.length)
                                && readyToSend) {
                        audioEncoder.offerAudioEncoder(this_buffer, audioPresentationTimeNs);
                        audioPresentationTimeNs = 0;
                    }
                }

                if (!BaseApp.startAudioRecording) {
                    // go through one more cycle before disabling isRecording so  that
                    // encoders are drained
                    if (isRecording && _stopCycles==0){
                        _stopCycles = 1;
                        try {
                            audioEncoder.stop();
                        } catch (Exception e){

                        }
                    } else if (_stopCycles==1){
                        isRecording = false;
                        _stopCycles=0;
                    }
                }

                //if (bytesRead == byteBuffer.capacity()) {
               if (bytesRead == this_buffer.length) {
                    //nativeDataIsRecorded(bytesRead, nativeAudioRecord);
                    nativeDataIsRecorded(byteBuffer.capacity(), nativeAudioRecord);
                } else {
                    Loge("AudioRecord.read failed: " + bytesRead);
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        keepAlive = false;
                    }
                }

                if (DEBUG) {
                    long nowTime = System.nanoTime();
                    long durationInMs =
                            TimeUnit.NANOSECONDS.toMillis((nowTime - lastTime));
                    lastTime = nowTime;
                    Logd("bytesRead[" + durationInMs + "] " + bytesRead);
                }
            }

            try {
                audioRecord.stop();
            } catch (Exception e) {
                Loge("AudioRecord.stop failed: " + e.getMessage());
            }
        }

        public void joinThread() {

            if (audioEncoder!=null && isRecording) {

                try {
                    audioEncoder.stop();
                } catch (Exception e){

                }
            }
            keepAlive = false;

            while (isAlive()) {
                try {
                    join();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }

            isRecording = false;
        }
    }

    WebRtcAudioRecord(Context context, long nativeAudioRecord) {
        Logd("ctor" + WebRtcAudioUtils.getThreadInfo());
        this.context = context;
        this.nativeAudioRecord = nativeAudioRecord;
        audioManager = (AudioManager) context.getSystemService(
                Context.AUDIO_SERVICE);
        sampleRate = GetNativeSampleRate();
        Log.d(TAG, "Sample Rate " + sampleRate);
        bytesPerBuffer = BYTES_PER_FRAME * (sampleRate / BUFFERS_PER_SECOND);

        Log.d(TAG, "bytesPerBuffer " + byteBuffer);
        framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
        Log.d(TAG, "framesPerBuffer " + framesPerBuffer);

        byteBuffer = byteBuffer.allocateDirect(bytesPerBuffer);
        Logd("byteBuffer.capacity: " + byteBuffer.capacity());

        // Rather than passing the ByteBuffer with every callback (requiring
        // the potentially expensive GetDirectBufferAddress) we simply have the
        // the native class cache the address to the memory once.
        nativeCacheDirectBufferAddress(byteBuffer, nativeAudioRecord);

        if (DEBUG) {
            WebRtcAudioUtils.logDeviceInfo(TAG);
        }
    }

    private int GetNativeSampleRate() {
        return WebRtcAudioUtils.GetNativeSampleRate(audioManager);
    }

    public static boolean BuiltInAECIsAvailable() {
        // AcousticEchoCanceler was added in API level 16 (Jelly Bean).
        if (!WebRtcAudioUtils.runningOnJellyBeanOrHigher()) {
            return false;
        }
        // TODO(henrika): add black-list based on device name. We could also
        // use uuid to exclude devices but that would require a session ID from
        // an existing AudioRecord object.
        return AcousticEchoCanceler.isAvailable();
    }

    private boolean EnableBuiltInAEC(boolean enable) {
        Logd("EnableBuiltInAEC(" + enable + ')');
        // AcousticEchoCanceler was added in API level 16 (Jelly Bean).
        if (!WebRtcAudioUtils.runningOnJellyBeanOrHigher()) {
            return false;
        }
        // Store the AEC state.
        useBuiltInAEC = enable;
        // Set AEC state if AEC has already been created.
        if (aec != null) {
            int ret = aec.setEnabled(enable);
            if (ret != AudioEffect.SUCCESS) {
                Loge("AcousticEchoCanceler.setEnabled failed");
                return false;
            }
            Logd("AcousticEchoCanceler.getEnabled: " + aec.getEnabled());
        }
        return true;
    }

    private int InitRecording(int sampleRate) {
        Logd("InitRecording(sampleRate=" + sampleRate + ")");
        // Get the minimum buffer size required for the successful creation of
        // an AudioRecord object, in byte units.
        // Note that this size doesn't guarantee a smooth recording under load.
        // TODO(henrika): Do we need to make this larger to avoid underruns?
        int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Logd("AudioRecord.getMinBufferSize: " + minBufferSize);
        recordingSampleRate = sampleRate;
        if (aec != null) {
            aec.release();
            aec = null;
        }
        assertTrue(audioRecord == null);

        int bufferSizeInBytes = Math.max(byteBuffer.capacity(), minBufferSize);
        Logd("bufferSizeInBytes: " + bufferSizeInBytes);
        try {
            audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes);

        } catch (IllegalArgumentException e) {
            Logd(e.getMessage());
            return -1;
        }
        assertTrue(audioRecord.getState() == AudioRecord.STATE_INITIALIZED);

        Logd("AudioRecord " +
                "session ID: " + audioRecord.getAudioSessionId() + ", " +
                "audio format: " + audioRecord.getAudioFormat() + ", " +
                "channels: " + audioRecord.getChannelCount() + ", " +
                "sample rate: " + audioRecord.getSampleRate());
        Logd("AcousticEchoCanceler.isAvailable: " + BuiltInAECIsAvailable());
        if (!BuiltInAECIsAvailable()) {
            return framesPerBuffer;
        }

        aec = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
        if (aec == null) {
            Loge("AcousticEchoCanceler.create failed");
            return -1;
        }
        int ret = aec.setEnabled(useBuiltInAEC);
        if (ret != AudioEffect.SUCCESS) {
            Loge("AcousticEchoCanceler.setEnabled failed");
            return -1;
        }
        Descriptor descriptor = aec.getDescriptor();
        Logd("AcousticEchoCanceler " +
                "name: " + descriptor.name + ", " +
                "implementor: " + descriptor.implementor + ", " +
                "uuid: " + descriptor.uuid);
        Logd("AcousticEchoCanceler.getEnabled: " + aec.getEnabled());
        return framesPerBuffer;
    }

    private boolean StartRecording() {
        Logd("StartRecording");
        assertTrue(audioRecord != null);
        assertTrue(audioThread == null);

        audioThread = new AudioRecordThread("AudioRecordJavaThread");
        audioThread.start();
        return true;
    }

    private boolean StopRecording() {
        Logd("StopRecording");
        assertTrue(audioThread != null);
        Logd("StopRecording stopping encoder");
        //audioEncoder.stop();

        Logd("StopRecording stopping thread");
        audioThread.joinThread();
        Logd("thread stopped");
        audioThread = null;
        if (aec != null) {
            aec.release();
            aec = null;
        }
        audioRecord.release();

        audioRecord = null;
        return true;
    }

    /** Helper method which throws an exception  when an assertion has failed. */
    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    private static void Logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void Loge(String msg) {
        Log.e(TAG, msg);
    }

    private native void nativeCacheDirectBufferAddress(
            ByteBuffer byteBuffer, long nativeAudioRecord);

    private native void nativeDataIsRecorded(int bytes, long nativeAudioRecord);
}
