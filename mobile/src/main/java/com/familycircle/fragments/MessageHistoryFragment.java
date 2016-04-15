package com.familycircle.fragments;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.familycircle.R;
import com.familycircle.activities.MessageDetailActivity;
import com.familycircle.adapters.MessageHistoryListAdapter;
import com.familycircle.lib.utils.db.SmsDbAdapter;
import com.familycircle.sdk.models.MessageModel;

import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MessageHistoryFragment extends Fragment implements OnClickListener, OnItemClickListener{

    private MessageLoadingTask messageLoadTask = null;
    private TextView textViewNoContent;
    private ListView chatHistoryList;
    private MessageHistoryListAdapter adapter;
    private Context context;

    public MessageHistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_message_history, container, false);
        context = getActivity();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        // TODO Auto-generated method stub
        super.onViewCreated(view, savedInstanceState);
        initializeView(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadView();
    }

    public void loadView(){
        if (messageLoadTask==null){
            messageLoadTask = new MessageLoadingTask();
            messageLoadTask.execute((Void) null);
        }
    }

    //ChatLogFilter chatLogFilter;

    protected void initializeView(View view, Bundle savedInstanceState)
    {

        chatHistoryList = (ListView) view.findViewById(R.id.chat_history_list);
        chatHistoryList.addFooterView(new View(context), null, true);
        chatHistoryList.setOnItemClickListener(this);
        textViewNoContent = (TextView) view.findViewById(R.id.empty_view);
        chatHistoryList.setEmptyView(textViewNoContent);

        adapter = new MessageHistoryListAdapter();
        chatHistoryList.setAdapter(adapter);

        boolean ret = false;

        showProgress(true, "Info", "Checking Session..."); // after this onResume is going to be shown

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MessageModel messageModel = (MessageModel)parent.getItemAtPosition(position);
        if (messageModel!=null){
            Intent intent = new Intent(getActivity(), MessageDetailActivity.class);
            intent.putExtra("TAG_ID", messageModel.getTn());
            intent.putExtra("unreadCnt", 1);
            getActivity().startActivityForResult(intent, 0);
        }

    }

    public class MessageLoadingTask extends AsyncTask<Void, Void, Boolean> {

        private static final String TAG = "MessageHistoryFrag";
        private List<MessageModel> messages;

        public MessageLoadingTask (){
            if (messages!=null){
                messages.clear();
                messages=null;
            }
        }

        @Override
        protected void onPreExecute (){
            try {
				/*if (pd!=null && pd.isShowing()){
					pd.cancel();
				}*/

            } catch (Exception e){

            }

            showProgress(true, "Info", "Refreshing...");

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (getActivity()==null) return false;
            try {

                    SmsDbAdapter dbAdapter = new SmsDbAdapter();
                    messages = dbAdapter.getGroupedMessage();
                    if (messages==null){
                        Log.d(TAG, "MessageLoadingTask is null");
                    } else {
                        Log.d(TAG, "MessageLoadingTask # " + messages.size());
                    }
                    return true;

            } catch (Exception e){
                Log.e(TAG, "doinbackground " + e.toString(), e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            try {
                showProgress(false, null, null);
                if (success){
                    if (messages!=null){
                        adapter.loadData(messages);
                    }
                }
            } catch (Exception e){
                Log.e(TAG, "onPostExecute " + e.toString(), e);
            } finally {
                messageLoadTask = null;
            }
        }

        @Override
        protected void onCancelled() {
            messageLoadTask = null;
            showProgress(false, null, null);
        }
    }

    private void showProgress(final boolean show, String title, String msg) {

    }
}
