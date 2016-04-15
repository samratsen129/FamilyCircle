package com.familycircle.lib.services;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.familycircle.sdk.BaseApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;

public class MuxerIntentService extends IntentService {

    public static final String TAG = "MuxerIntentService";

    public MuxerIntentService() {
        super("MuxerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        int cmd = intent.getIntExtra("CMD", 0);

        switch (cmd){
            case 1:
                // Join a video and audio file

                String fileNameVideo = intent.getStringExtra("FILE_NAME_VIDEO");
                String fileNameAudio = fileNameVideo.replaceAll(".mp4", "_audio.mp4");
                String fileNameOut1 = fileNameVideo.replaceAll(".mp4", "_1.mp4");
                if (mux(fileNameVideo, fileNameAudio, fileNameOut1)){
                    File f1 = new File(fileNameAudio);
                    if (f1.exists()) f1.delete();
                    File f2 = new File(fileNameVideo);
                    if (f2.exists()) f2.delete();
                    MediaScannerConnection.scanFile(BaseApp.getContext(), new String[]{fileNameOut1}, null, null);
                    //WebSocketManager.getInstance().getBus().post(new BusEvents.CompleteVideoProcessingEvent(getFileName("fileNameOut1")));
                    //showToast("Successfully saved video,"+ getFileName("fileNameOut1"));
                } else {
                    MediaScannerConnection.scanFile(BaseApp.getContext(), new String[]{fileNameVideo}, null, null);
                    showToast("Failed to merge video,"+ getFileName("fileNameVideo"));
                }
                break;

            case 2:
                // Crop a video file
                String fileName = intent.getStringExtra("FILE_NAME");
                String outFile = fileName.replaceAll(".mp4", "_trim.mp4");
                if (crop(fileName, outFile)){
                    File f1 = new File(fileName);
                    f1.delete();
                    MediaScannerConnection.scanFile(BaseApp.getContext(), new String[]{outFile}, null, null);

                } else {
                    MediaScannerConnection.scanFile(BaseApp.getContext(), new String[]{fileName}, null, null);
                }
                break;

            case 3:
                // Append two files
                String fileName1 = intent.getStringExtra("FILE_NAME1");
                String fileName2 = intent.getStringExtra("FILE_NAME2");
                String fileNameOut2 = intent.getStringExtra("FILE_NAME_OUT");
                break;

        }
                String fileName = intent.getStringExtra("FILE_NAME");
        Log.d(TAG, "File Name " + fileName);


    }

    private String getFileName(String fullFileName){
        try {
            int pos = fullFileName.lastIndexOf("/");
            return fullFileName.substring(pos+1);
        } catch (Exception e){
            return fullFileName;
        }
    }

    private void showToast(String message){
        try {
            Toast.makeText(BaseApp.getContext(), message, Toast.LENGTH_LONG).show();
        } catch (Exception e){

        } catch (Error e){

        }
    }

    public boolean mux(String videoFile, String audioFile, String outputFile) {
        Movie video;
        try {
            video = new MovieCreator().build(videoFile);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            return false;
        }

        Movie audio;
        try {
            audio = new MovieCreator().build(audioFile);

        } catch (IOException e) {
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        Track audioTrack = audio.getTracks().get(0);
        video.addTrack(audioTrack);

        Container out = new DefaultMp4Builder().build(video);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);
        try {
            out.writeContainer(byteBufferByteChannel);
            byteBufferByteChannel.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static class BufferedWritableFileByteChannel implements WritableByteChannel {
        private static final int BUFFER_CAPACITY = 1000000;

        private boolean isOpen = true;
        private final OutputStream outputStream;
        private final ByteBuffer byteBuffer;
        private final byte[] rawBuffer = new byte[BUFFER_CAPACITY];

        private BufferedWritableFileByteChannel(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.byteBuffer = ByteBuffer.wrap(rawBuffer);
        }

        @Override
        public int write(ByteBuffer inputBuffer) throws IOException {
            int inputBytes = inputBuffer.remaining();

            if (inputBytes > byteBuffer.remaining()) {
                dumpToFile();
                byteBuffer.clear();

                if (inputBytes > byteBuffer.remaining()) {
                    throw new BufferOverflowException();
                }
            }

            byteBuffer.put(inputBuffer);

            return inputBytes;
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        @Override
        public void close() throws IOException {
            dumpToFile();
            isOpen = false;
        }
        private void dumpToFile() {
            try {
                outputStream.write(rawBuffer, 0, byteBuffer.position());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean crop(String inputFileName, String outputFileName) {
        try {
            //String filePath = Environment.getExternalStorageDirectory() + "/sample1.mp4";
            Movie originalMovie = MovieCreator.build(inputFileName);

            Track track = originalMovie.getTracks().get(0);
            Movie movie = new Movie();
            movie.addTrack(new AppendTrack(new CroppedTrack(track, 200, 400)));

            Container out = new DefaultMp4Builder().build(movie);
            //String outputFilePath = Environment.getExternalStorageDirectory() + "/output_crop.mp4";
            FileOutputStream fos = new FileOutputStream(new File(outputFileName));
            out.writeContainer(fos.getChannel());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean append(String fileName1, String fileName2, String outputFileName) {
        try {
            String f1 = fileName1;
            String f2 = fileName2;
            Movie[] inMovies = new Movie[]{
                    MovieCreator.build(f1),
                    MovieCreator.build(f2)};

            List<Track> videoTracks = new LinkedList<Track>();
            List<Track> audioTracks = new LinkedList<Track>();
            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                }
            }
            Movie result = new Movie();
            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }

            Container out = new DefaultMp4Builder().build(result);
            //String outputFilePath = Environment.getExternalStorageDirectory() + "/output_append.mp4";
            FileOutputStream fos = new FileOutputStream(new File(outputFileName));
            out.writeContainer(fos.getChannel());
            fos.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
