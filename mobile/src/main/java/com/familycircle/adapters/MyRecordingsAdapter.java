package com.familycircle.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.familycircle.R;
import com.familycircle.custom.views.CircularImageView;
import com.familycircle.lib.utils.BitmapOptimizer;
import com.familycircle.sdk.models.RecordingsModel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by samratsen on 4/22/15.
 */
public class MyRecordingsAdapter extends RecyclerView.Adapter<MyRecordingsAdapter.ViewHolder> {

    private List<RecordingsModel> data;
    private Context ctx;
    public MyRecordingsAdapter(Context ctx){

        this.ctx = ctx;
    }

    public void loadData(List<RecordingsModel> data){
        this.data=data;
        notifyDataSetChanged();
    }

    public List<RecordingsModel> getData(){
        return data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.recordings_card_row, viewGroup, false);
        return new ViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        RecordingsModel contact = data.get(i);
        viewHolder.textViewName.setText(contact.getName());
        long fileSize=contact.getSize();

        if (fileSize==0){
            File f = new File(contact.getFileName());
            if (f.exists()){
                fileSize = f.length();
            }
        }
        viewHolder.textViewDate.setText(getGetTimeFromMilli(contact.getModifyDate()) + ", " + getSizeInMb(fileSize));
        viewHolder.tag = contact.getId();
        final String fileName = contact.getFileName();

        Log.d("MyRecordingsAdapter", "Recording bitmap name " + fileName);
        viewHolder.imageIcon.setImageResource(R.drawable.icon_avatar);
        if (fileName!=null && fileName.length() > 10) {
            File f = new File(fileName);
            if (f.exists()) {
                new BitmapTask(contact.getId()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, viewHolder);

            }
        }

        MediaController mc = new MediaController(ctx);
        mc.setAnchorView(viewHolder.videoView);
        mc.setMediaPlayer(viewHolder.videoView);

        viewHolder.imageIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:/"+fileName));
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                File f = new File(fileName);
                if (!f.exists()) return;
                String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(f).toString());
                String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                intent.setDataAndType(Uri.parse("file:/"+fileName), mimetype);
                //intent.setDataAndType(Uri.parse("file:/"+fileName), "video*//*");
                try {
                    ctx.startActivity(intent);
                }catch(Exception e){

                } catch (Error e){

                }
            }
        });

        viewHolder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = v.getContext();
                if (ctx == null) return;
                try {
                    /*Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:/"+fileName));
                    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    File f = new File(fileName);
                    if (!f.exists()) return;
                    String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(f).toString());
                    String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    //intent.setDataAndType(Uri.parse("file:/"+fileName), mimetype);
                    intent.setDataAndType(Uri.parse("file:/"+fileName), "video*//*");
                    ctx.startActivity(intent);*/

                    File f = new File(fileName);
                    if (!f.exists()) return;
                    final VideoView vv = (VideoView)v.findViewById(R.id.video_recording);
                    final CircularImageView civ = (CircularImageView)v.findViewById(R.id.image_recording);

                    civ.setVisibility(View.GONE);
                    vv.setVideoURI(Uri.parse(fileName));
                    if (!vv.isShown()){
                        vv.setVisibility(View.VISIBLE);
                        vv.start();
                        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                vv.setVisibility(View.GONE);
                                civ.setVisibility(View.VISIBLE);
                            }
                        });
                    } else if (vv.isShown()){
                        vv.stopPlayback();
                        vv.setVisibility(View.GONE);
                        civ.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e){
                    Log.e("MyRecordingsAdapter", "File Open issue " + fileName, e);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        if (data==null) return 0;
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewName;
        public TextView textViewDate;
        public CircularImageView imageIcon;
        public VideoView videoView;
        public int tag=-1;
        public View container;
        public ViewHolder(View v) {
            super(v);

            /*v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context ctx = v.getContext();
                    if (ctx==null) return;
                    *//**//*
                    int i = getPosition();


                }
            });*/
            textViewName = (TextView)v.findViewById(R.id.text_view_name);
            textViewDate = (TextView)v.findViewById(R.id.text_view_date);
            imageIcon = (CircularImageView)v.findViewById(R.id.image_recording);
            videoView = (VideoView)v.findViewById(R.id.video_recording);
            container=v;
        }
    }

    public class BitmapTask extends AsyncTask<ViewHolder, Integer, Bitmap> {
        private static final String TAG = "MediaTask";
        private WeakReference<ViewHolder> viewHolder;
        private int id = -1;

        public BitmapTask(int viewTag){
               this.id = viewTag;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bitmap doInBackground(ViewHolder... params) {

            try {
                viewHolder = new WeakReference<ViewHolder>(params[0]);


                Bitmap thumb =
                        MediaStore.Video.Thumbnails.getThumbnail(
                                ctx.getContentResolver(), id,
                                MediaStore.Video.Thumbnails.MICRO_KIND,
                                BitmapOptimizer.getDLBitmapOptions());
                return thumb;

            } catch (Exception e) {
                Log.e(TAG, "Exception in MediaTask ", e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //Log.d(TAG, "progress " + values[0]);

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            try {
                if (bitmap!=null && viewHolder.get()!=null && viewHolder.get().tag == id) {
                    viewHolder.get().imageIcon.setImageBitmap(bitmap);
                }

            } catch (Error e){

            } catch (Exception e){

            } finally {
            }
        }


        @Override
        protected void onCancelled(){
            try {

            } catch (Error e){

            } catch (Exception e){

            } finally {

            }
        }
    }

    public String getGetTimeFromMilli(String Time_MilliSec) {
        try {
            long millisTime = Long.valueOf(Time_MilliSec).longValue();
            DateFormat monthFormatter = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a");
            return monthFormatter.format(new Date(millisTime * 1000));
        } catch (Exception e){
            return "";
        }
    }

    public static String getSizeInMb(long sizeInBytes)
    {
        String fileSize = "";
        if (sizeInBytes < 1024)
        {
            fileSize = "" + sizeInBytes + " B";
        }
        else if ((sizeInBytes >= 1024) && (sizeInBytes < 1048576))
        {
            fileSize = "" + ((float)sizeInBytes / 1024) + " KB";
        }
        else if (sizeInBytes >= 1048576)
        {
            fileSize = "" + ((float)sizeInBytes / 1048576) + " MB";
        }
        return fileSize;
    }

}
