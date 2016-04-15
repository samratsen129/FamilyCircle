package com.familycircle.lib.utils;

import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.os.Trace;
import android.util.Log;

import com.familycircle.lib.services.MuxerIntentService;
import com.familycircle.sdk.BaseApp;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created by samratsen on 4/20/15.
 */
public class AVRecorder {

    private static final String TAG = "AVRecorder";
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    public static int VIDEO_WIDTH = 1280;            // 720p
    public static int VIDEO_HEIGHT = 720;
    private static final int BIT_RATE = 6000000;            // 6Mbps

    private static final Object sSyncObj = new Object();
    private static volatile AVRecorder sInstance;

    private File mOutputFile;
    private MediaCodec mEncoder;
    private InputSurface mInputSurface;
    private MediaCodec.BufferInfo mBufferInfo;

    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted = false;
    private int mNumTracksAdded = 0;

    private int mViewportWidth, mViewportHeight;
    private int mViewportXoff, mViewportYoff;
    private final float mProjectionMatrix[] = new float[16];
    private String outputFileName;

    boolean fullStopReceived = false;
    long startWhen;
    boolean firstFrameReady = false;

    private boolean enableAudio = true;
    private boolean enableVideo = true;

    private AudioSoftwarePoller audioPoller;
    private AudioPollerEncoder aEncoder;
    private double videoSize = 0;
    private int sizeUpdateCounter=0;
    private AVRecorder() {
    }

    /**
     * Retrieves the singleton, creating the instance if necessary.
     */
    public static AVRecorder getInstance() {
        if (sInstance == null) {
            synchronized (sSyncObj) {
                if (sInstance == null) {
                    sInstance = new AVRecorder();
                }
            }
        }
        //sInstance = new AVRecorder();
        return sInstance;
    }

    /**
     * Creates a new encoder, and prepares the output file.
     * <p/>
     * We can't set up the InputSurface yet, because we want the EGL contexts to share stuff,
     * and the primary context may not have been configured yet.
     */
    private void prepareEncoder(final String fileName) {
        MediaCodec encoder=null;
        MediaMuxer muxer;

        if (mEncoder != null || mInputSurface != null) {
            throw new RuntimeException("prepareEncoder called twice?");
        }

        // mOutputFile = new File(context.getFilesDir(), "video.mp4");
        mOutputFile = new File(fileName);
        Log.d(TAG, "Video recording to file " + mOutputFile);
        mBufferInfo = new MediaCodec.BufferInfo();

        try {

            if (enableVideo) {
                // Create and configure the MediaFormat.
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,
                        VIDEO_WIDTH, VIDEO_HEIGHT);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

                // Create a MediaCodec encoder, and configure it with our format.
                encoder = MediaCodec.createEncoderByType(MIME_TYPE);
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

                mEncoder = encoder;
            }

            // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
            // because our MediaFormat doesn't have the Magic Goodies.  These can only be
            // obtained from the encoder after it has started processing data.
            muxer = new MediaMuxer(mOutputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxerStarted=false;

            mMuxer = muxer;
        } catch (Exception ex) {
            Log.w(TAG, "Something failed during recorder init: " + ex);
            releaseEncoder();
        }

        configureViewport();
    }

    /**
     * Finishes setup.  Call with the primary EGL context set.
     */
    public void firstTimeSetup() {
        if (!isRecording() || mInputSurface != null) {
            // not recording, or already initialized
            return;
        }
        Log.d(TAG, "firstTimeSetup");
        InputSurface inputSurface;

        try {
            if (enableVideo) {
                inputSurface = new InputSurface(mEncoder.createInputSurface());
                mEncoder.start();
                mInputSurface = inputSurface;
            }

        } catch (Exception ex) {
            Log.w(TAG, "Something failed during recorder init: " + ex);
            releaseEncoder();
        }
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseEncoder() {
        Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;

        }
    }

    /**
     * Returns true if a recording is in progress.
     */
    public boolean isRecording() {
        return (mEncoder != null);
    }

    /**
     * Configures our viewport projection matrix.
     * <p/>
     * We always render at the surface's resolution, which matches the video encoder resolution.
     * The resolution and orientation of the device itself are irrelevant -- we're not recording
     * what's on screen, but rather what would be on screen if the device resolution matched our
     * video parameters.
     */
    private void configureViewport() {
        float arenaRatio = ViewPortState.ARENA_HEIGHT / ViewPortState.ARENA_WIDTH;
        int x, y, viewWidth, viewHeight;

        if (VIDEO_HEIGHT > (int) (VIDEO_WIDTH * arenaRatio)) {
            // limited by narrow width; restrict height
            viewWidth = VIDEO_WIDTH;
            viewHeight = (int) (VIDEO_WIDTH * arenaRatio);
        } else {
            // limited by short height; restrict width
            viewHeight = VIDEO_HEIGHT;
            viewWidth = (int) (VIDEO_HEIGHT / arenaRatio);
        }
        x = (VIDEO_WIDTH - viewWidth) / 2;
        y = (VIDEO_HEIGHT - viewHeight) / 2;
        //1080 * 1920
        Log.d(TAG, "configureViewport w=" + VIDEO_WIDTH + " h=" + VIDEO_HEIGHT);
        Log.d(TAG, " --> x=" + x + " y=" + y + " gw=" + viewWidth + " gh=" + viewHeight);

        mViewportXoff = x;
        mViewportYoff = y;
        mViewportWidth = viewWidth;
        mViewportHeight = viewHeight;
        if (enableVideo) {
            //Matrix.orthoM(mProjectionMatrix, 0, 0, ViewPortState.ARENA_WIDTH,
            //        0, ViewPortState.ARENA_HEIGHT, -1, 1);
        }
    }

    /**
     * Returns the projection matrix.
     *
     * @param dest a float[16]
     */
    public void getProjectionMatrix(float[] dest) {
        if (enableVideo) {
            System.arraycopy(dest, 0, mProjectionMatrix, 0, mProjectionMatrix.length);
        }
    }

    /**
     * Sets the viewport for video.
     */
    public void setViewport() {
        if (enableVideo) {
            GLES20.glViewport(mViewportXoff, mViewportYoff, mViewportWidth, mViewportHeight);
        }
    }

    /**
     * Configures EGL to output to our InputSurface.
     */
    public void makeCurrent() {
        if (enableVideo) {
            firstTimeSetup();
            mInputSurface.makeCurrent();

        }
        firstFrameReady = true;
    }

    /**
     * Sends the current frame to the recorder.  Before doing so, we drain any pending output.
     */
    public void swapBuffers() {
        if (enableVideo) {
            if (!isRecording()) {
                return;
            }
            drainEncoder(false);
            mInputSurface.setPresentationTime(System.nanoTime());
            mInputSurface.swapBuffers();
        }
    }

    /**
     * Extracts all pending data from the encoder.
     * <p/>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     */
    private void drainEncoder(boolean endOfStream) {
        if (!isRecording() && !enableVideo) {
            return;
        }

        Trace.beginSection("drainEncoder");

        //        if (endOfStream) {
        //            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
        //            mEncoder.signalEndOfInputStream();
        //        }
        try {
            //Log.d(TAG, "AVRecorder drainEncoder " + endOfStream);
            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();

            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    break;      // out of while
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    if (mMuxerStarted) {
                        throw new RuntimeException("format changed twice");
                    }
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "encoder output format changed: " + newFormat);
                    mTrackIndex = mMuxer.addTrack(newFormat);
                    mNumTracksAdded++;
                    Log.i(TAG, "going to start muxer " + mNumTracksAdded);
                    //if (mMuxerStarted.compareAndSet(false, true) && tracks==2) {
                    if (!mMuxerStarted){
                        Log.d(TAG, "Muxer started : " + newFormat);
                        mMuxerStarted=true;
                        mMuxer.start();

                    }
                    Log.d(TAG, "End of Muxer started : " + newFormat);
                    //mMuxerStarted = true;
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // the codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        if (!mMuxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }

                        // Adjust the ByteBuffer values to match BufferInfo.  (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                        mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);

                        /*videoSize += mBufferInfo.size;

                        sizeUpdateCounter++;
                        if (sizeUpdateCounter==200){
                            sizeUpdateCounter=0;
                            //TeamApp.videoBytes = videoSize;
                        }*/

                        //Log.d(TAG, "wrote " + mBufferInfo.size + " bytes");
                    }

                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            Log.d(TAG, "end of stream reached");
                        }
                        break;      // out of while
                    }
                }
            }
        } catch (Exception e){

        } catch (Error e){

        }

        Trace.endSection();
    }

    /**
     * Handles activity pauses (could be leaving the view, could be switching back to the
     * main activity).  Stop recording, shut the codec down.
     */
/*    public void viewPaused() {
        Log.d(TAG, "AVRecorder Paused");
        fullStopReceived = true;
        if (enableVideo) {
            drainEncoder(true);
            releaseEncoder();
        }

    }*/

    public void stopRecording(){
        Log.d(TAG, "AVRecorder Paused");
        fullStopReceived = true;
        if (enableVideo) {
            drainEncoder(true);
            releaseEncoder();

            //TeamApp.audioOutputFile = null;
            BaseApp.startAudioRecording = false;

            Intent cmdIntent = new Intent(BaseApp.getContext(), MuxerIntentService.class);
            cmdIntent.putExtra("CMD", 1);
            cmdIntent.putExtra("FILE_NAME_VIDEO", outputFileName);
            BaseApp.getContext().startService(cmdIntent);
        }

        Log.d(TAG, "Video Size " + videoSize);

    }


    public void startRecording(String outputFileName){
        enableAudio = true;
        enableVideo = true;
        mNumTracksAdded=0;
        BaseApp.videoBytes =0;
        this.outputFileName=outputFileName;
        prepareEncoder(outputFileName);
        if (enableAudio) {
            /*aEncoder = new AudioPollerEncoder(TeamApp.getContext());
            aEncoder.prepareEncoder(outputFileName);
            audioPoller = new AudioSoftwarePoller();
            audioPoller.setAudioEncoder(aEncoder);
            aEncoder.setAudioSoftwarePoller(audioPoller);
            audioPoller.startPolling();*/

            BaseApp.audioOutputFile = outputFileName;
            BaseApp.startAudioRecording = true;
        }
        startWhen = System.nanoTime();
    }

    public static class ViewPortState {
        public static float ARENA_HEIGHT;
        public static float ARENA_WIDTH;
    }

}
