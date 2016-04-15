package com.familycircle.fragments;



import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.adapters.MyRecordingsAdapter;
import com.familycircle.sdk.models.RecordingsModel;

import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MyRecordingsFragment extends Fragment {

    private RecyclerView recList;
    private MyRecordingsAdapter adapter;
    private static final String TAG = "MyRecordingsFragment";
    private static AsyncTask<Void, Integer, List<RecordingsModel>> mediaTask = null;

    private static final Uri[] uriVideo =
            { MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.INTERNAL_CONTENT_URI, };
    private static final String[] videoProjection =
            {
                    MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE,
                    MediaStore.Images.Media.SIZE, MediaStore.Video.Media.DATE_MODIFIED,
                    MediaStore.Video.Media.DATE_TAKEN, MediaStore.Video.Media.MIME_TYPE,
                    MediaStore.Video.Media.DATE_ADDED
            };

    public MyRecordingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_my_recordings, container, false);
        recList = (RecyclerView) view.findViewById(R.id.recordingsList);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);
        adapter = new MyRecordingsAdapter(getActivity());
        recList.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mediaTask == null) {
            mediaTask = new MediaTask().execute();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void getMediaItems(){

    }

    public static List<RecordingsModel> getPhotosOrVideos()
    {
        Cursor cursor = null;
        List<RecordingsModel> itemList = new ArrayList<RecordingsModel>();

        try
        {
            String where = null;
            where = MediaStore.Video.VideoColumns.DATA + " LIKE ?";

            // May be have to get the DATA and have to strip out the file name for an accurate query
            String[] whereArgs = new String[]{"%/DCIM/%"};
            Log.d(TAG, "getPhotosOrVideos where," + whereArgs[0]);
            cursor = TeamApp.getContext().getContentResolver().query(uriVideo[0], videoProjection, where, whereArgs,
                    null);
            int count = cursor.getCount();
            Log.d(TAG, "getPhotosOrVideos # " + count);



            if (cursor != null && cursor.moveToFirst())
            {
                do
                {
                    int id = cursor.getColumnIndex(MediaColumns._ID);
                    int pathColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
                    int mimeColumn = cursor
                            .getColumnIndexOrThrow(MediaColumns.MIME_TYPE);
                    int sizeColumn = cursor
                            .getColumnIndex(MediaColumns.SIZE);
                    int titleColumn = cursor
                            .getColumnIndex(MediaColumns.TITLE);

                    int dateColumn = cursor.getColumnIndex(MediaColumns.DATE_ADDED);
                    int dateModifiedColumn = cursor.getColumnIndex(MediaColumns.DATE_MODIFIED);



                    String filePath = cursor.getString(pathColumn);
                    Log.d(TAG, "File " + filePath.toString() + ":" + cursor.getLong(sizeColumn));
                    if (cursor.getLong(sizeColumn) >= 0L)
                    {


                        RecordingsModel model = new RecordingsModel();
                        model.setName(cursor.getString(titleColumn));
                        if (model.getName().indexOf("video_capturing")==-1) continue;
                        model.setFileName(filePath.toString());
                        model.setSize(cursor.getLong(sizeColumn));
                        model.setId(cursor.getInt(id));
                        if (dateColumn!=-1){
                            model.setModifyDate(cursor.getString(dateColumn));
                        } else if (dateModifiedColumn!=-1) {
                            model.setModifyDate(cursor.getString(dateModifiedColumn));
                        } else {
                            model.setModifyDate("");
                        }


                        itemList.add(model);

                    }

                }
                while (cursor.moveToNext());
            }

        } catch (Exception e)
        {
            Log.e(TAG, e.toString(), e);

        } finally {
            if (cursor!=null) cursor.close();

            return itemList;
        }



    }

    public class MediaTask extends AsyncTask<Void, Integer, List<RecordingsModel>> {
        private static final String TAG = "MediaTask";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected List<RecordingsModel> doInBackground(Void... params) {

            try {
                return getPhotosOrVideos();
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
        protected void onPostExecute(List<RecordingsModel> results) {
            try {
                adapter.loadData(results);

            } catch (Error e){
                Log.e(TAG, "Exception in MediaTask ", e);
            } catch (Exception e){
                Log.e(TAG, "Error in MediaTask ", e);
            } finally {
                mediaTask = null;
            }
        }


        @Override
        protected void onCancelled(){
            try {

            } catch (Error e){

            } catch (Exception e){

            } finally {
                mediaTask = null;
            }
        }
    }


}

