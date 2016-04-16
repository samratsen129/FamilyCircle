package com.familycircle.lib.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;


import com.familycircle.sdk.BaseApp;

import java.io.IOException;
import java.util.HashMap;

public class AudioPlayer {
	private final static String TAG = "AudioPlayer";
	private static AudioPlayer audioPlayer = null;
	private int originalAudioManagerMode = AudioManager.MODE_NORMAL;

	private static MediaPlayer mPlayer = null;

	private int voicemailTotlDuration = 0;

	private IMediaStatusListener listner = null;

	private boolean isPaused = false;
	
	private static Vibrator mVibrator;
	
	private boolean isVibrate = false;
	
	private PrefManagerBase prefManager = new PrefManagerBase();

	public static AudioPlayer getInstance() {
		if (mPlayer == null) {
			audioPlayer = new AudioPlayer();
		}
		return audioPlayer;
	}

	public static Vibrator getVibrator()
	{
		if(mVibrator == null)
		{
			mVibrator = (Vibrator) BaseApp.getContext().getSystemService(Context.VIBRATOR_SERVICE);
		}
		return mVibrator;
	}

    public void playAudio(final String audioFileName,
                          final IMediaStatusListener mediaStatusListner){
        //return;
		playAudio2(audioFileName,mediaStatusListner);
    }
/*	public void playAudio(final Context context, final String audioUrl,
			final IMediaStatusListener mediaStatusListner,
			final HolderView holder) */
	public void playAudio2(final String audioFileName,
			final IMediaStatusListener mediaStatusListner){
		if (audioFileName == null) {
			return;
		}
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}

		this.listner = mediaStatusListner;

		final AudioManager audioManager = (AudioManager)BaseApp.getContext().getSystemService(Context.AUDIO_SERVICE);
		
		originalAudioManagerMode = audioManager.getMode();
		
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT){
	        Log.i(TAG, "AudioPlayer Silent mode");
	        return;
		}
	
				
		new Thread(new Runnable() {

			@Override
			public void run() {
				mPlayer = new MediaPlayer();
				if (mPlayer != null) {
					try {
						
						if (prefManager.getVibrateWhenRingingSetting())
						{
							if (mVibrator == null)
							{
							   getVibrator(); 
							}
							if (mVibrator != null)
						    {
							long[] patern = { 0, 1000, 1000 };
							mVibrator.vibrate(patern, 1);
						    }
						}
						if (audioFileName.startsWith("android.resource:")) {
							// mPlayer.reset();

							if (Utils.isTabletDevice(BaseApp.getContext())) {
								mPlayer.setAudioStreamType(AudioManager.STREAM_RING);

							} else {
								mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);

							}
							mPlayer.setDataSource(audioFileName);

							try {
								mPlayer.prepare();
							} catch (Exception e) {
								e.printStackTrace();
							}
							mPlayer.setLooping(true);
							
						} else if (!audioFileName.startsWith("http")) {
							/*File file = new File(audioFileName);
							FileInputStream inputStream = new FileInputStream(
									file);
							mPlayer.setDataSource(inputStream.getFD());*/
							AssetFileDescriptor descriptor = BaseApp.getContext().getAssets().openFd(audioFileName);
							mPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
					        descriptor.close();
							if (Utils.isTabletDevice(BaseApp.getContext())) {

							} else {
								mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);

							}
							mPlayer.prepare();
							mPlayer.setLooping(true);
							
						} else {
							//mPlayer.reset();
							Uri uri = Uri.parse(audioFileName);
							HashMap<String, String> header = new HashMap<String, String>();
							header.put("Content-Type", "application/json");
							mPlayer.setDataSource(
                                    BaseApp.getContext(), uri,
									header);
							
							if (Utils.isTabletDevice(BaseApp.getContext())) {
								mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

							} else {
								mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);

							}
							
							//mPlayer.prepareAsync();
							mPlayer.prepare();
							mPlayer.setLooping(true);
							
						}

						
						mPlayer.setOnPreparedListener(new OnPreparedListener() {

							@Override
							public void onPrepared(MediaPlayer mp) {
								if (listner != null) {
									listner.onMediaReadyListner();
								}
								/*if (listner != null
										&& listner instanceof VoiceMailView) {
									((VoiceMailView) listner)
									.updateVoicemailTag();
								}*/
								
								isPaused = false;
								

							}
						});
						
						mPlayer.setOnCompletionListener(new OnCompletionListener() {
							@Override
							public void onCompletion(MediaPlayer mp) {
								if (listner != null) {
									isPaused = false;
									if (listner != null) {
										listner.onCompleteListner();
									}

								}
								audioManager.setMode(originalAudioManagerMode);
								audioManager.setSpeakerphoneOn(false);
								releasePlayer();

							}
						});

						mPlayer.setOnErrorListener(new OnErrorListener() {

							@Override
							public boolean onError(MediaPlayer mp, int what,
									int extra) {
								isPaused = false;
								if (listner != null) {
									listner.onMediaErrorListner();
								}
								audioManager.setMode(originalAudioManagerMode);
								audioManager.setSpeakerphoneOn(false);
								return true;
							}
						});
						
						//AudioManager audioManager = (AudioManager)TeamApp.getContext().getSystemService(Context.AUDIO_SERVICE);
						audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
						audioManager.setSpeakerphoneOn(true);
						
						mPlayer.start();
						
						

					} catch (IllegalArgumentException e) {
						if (listner != null) {
							listner.onMediaErrorListner();
						}
						audioManager.setMode(originalAudioManagerMode);
						audioManager.setSpeakerphoneOn(false);
						Log.e(TAG, "music player error 1", e);
						
					} catch (SecurityException e) {
						if (listner != null) {
							listner.onMediaErrorListner();
						}
						audioManager.setMode(originalAudioManagerMode);
						audioManager.setSpeakerphoneOn(false);
						Log.e(TAG, "music player error 2", e);
						
					} catch (IllegalStateException e) {
						if (listner != null) {
							listner.onMediaErrorListner();
						}
						audioManager.setMode(originalAudioManagerMode);
						audioManager.setSpeakerphoneOn(false);
						Log.d(TAG, "music player error 3", e);
						
					} catch (IOException e) {
						if (listner != null) {
							listner.onMediaErrorListner();
						}
						audioManager.setMode(originalAudioManagerMode);
						audioManager.setSpeakerphoneOn(false);
						Log.e(TAG, "music player error 4", e);
						
					}
				}
				
				

			}
		}).start();
	}

	public boolean isPlaying() {
		if (mPlayer != null) {
			return mPlayer.isPlaying();
		}
		return false;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public int getAudioCurrentPosition() {
		if (mPlayer != null) {
			return mPlayer.getCurrentPosition();
		}
		return 0;
	}

	public void startAudio() {
		if (mPlayer != null) {
			isPaused = false;
			mPlayer.start();
		}
	}

	public void pauseAudio() {
		if (mPlayer != null) {
			if (mPlayer.isPlaying()) {
				isPaused = true;
				mPlayer.pause();
				if (listner != null) {
					listner.onMediaPausedListner();
				}
			}
		}
	}

	public void stopAudioPlay() {
		if (mPlayer != null) {
			isPaused = false;
			mPlayer.stop();
			
			final AudioManager audioManager = (AudioManager)BaseApp.getContext().getSystemService(Context.AUDIO_SERVICE);
			audioManager.setMode(originalAudioManagerMode);
			audioManager.setSpeakerphoneOn(false);
		}
	}

	public int getTotalDuration() {
		if (mPlayer != null) {
			return mPlayer.getDuration();
		}
		return 0;
	}

	public void setSeekTo(int mSecond) {
		if (mPlayer != null) {
			mPlayer.seekTo(mSecond);
		}
	}
	
	public void setVibration() 	{
		if(mVibrator == null)
		{
			getVibrator();
		}
		long[] patern = { 0, 1000, 1000 };
		mVibrator.vibrate(patern, 1);		
	}

	public void releasePlayer() {
		
		final AudioManager audioManager = (AudioManager)BaseApp.getContext().getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(originalAudioManagerMode);
		audioManager.setSpeakerphoneOn(false);
		
		if (listner != null) {
			listner = null;
		}
		
		if (mVibrator != null) {
			mVibrator.cancel();
			mVibrator = null;
		}
		
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}
	}

}
