package com.familycircle.fragments;


import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.activities.MainActivity;
import com.familycircle.custom.views.IndexBarView;
import com.familycircle.adapters.PinnedHeaderAdapter;
import com.familycircle.custom.views.PinnedHeaderListView;
import com.familycircle.manager.TeamManager;
import com.familycircle.lib.utils.Logger;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment implements Handler.Callback {

    // unsorted list items
    ArrayList<ContactModel> mItems;

    // array list to store section positions
    ArrayList<Integer> mListSectionPos;

    // array list to store listView data
    ArrayList<ContactModel> mListItems;

    // custom list view with pinned header
    PinnedHeaderListView mListView;

    // custom adapter
    PinnedHeaderAdapter mAdaptor;

    // search box
    EditText mSearchView;

    // loading view
    ProgressBar mLoadingView;

    // empty view
    TextView mEmptyView;

    TextView mTabHeader1TextView;
    TextView mTabHeader2TextView;
    private ImageView mStatusIndicator;

    private IChannelManagerClient channelManagerClient;
    private Handler mHandler;

    public ContactsFragment() {
        // Required empty public constructor
        //setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // set up the view
        mSearchView = (EditText) view.findViewById(R.id.search_view);
        mLoadingView = (ProgressBar) view.findViewById(R.id.loading_view);
        mListView = (PinnedHeaderListView) view.findViewById(R.id.list_view);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);
        mTabHeader1TextView = (TextView) view.findViewById(R.id.tab1Header);
        mTabHeader2TextView = (TextView) view.findViewById(R.id.tab2Header);
        mStatusIndicator = (ImageView) view.findViewById(R.id.status_indicator);

        /*SpannableString spanString = new SpannableString("Members");
        spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
        spanString.setSpan(new StyleSpan(Color.RED), 0, spanString.length(), 0);
        mTabHeader1TextView.setText(spanString);*/

        // Array to ArrayList
        mItems = (ArrayList)ContactsStaticDataModel.getAllUniqueContacts();
        mListSectionPos = new ArrayList<Integer>();
        mListItems = new ArrayList<ContactModel>();

        mHandler = new Handler(this);
        channelManagerClient = TeamManager.getInstance().getChannelManager();

        ((MainActivity) getActivity()).restoreActionBar();

        new Poplulate().execute(mItems);

        mTabHeader1TextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItems!=null){
                    mItems.clear();
                    mItems=null;
                }
                if (mStatusIndicator.isShown()){
                    mItems = (ArrayList)ContactsStaticDataModel.getAllUniqueContacts();
                    mStatusIndicator.setVisibility(View.GONE);
                } else {
                    mItems = (ArrayList)ContactsStaticDataModel.getAllActiveContacts();
                    mStatusIndicator.setVisibility(View.VISIBLE);
                }
                new Poplulate().execute(mItems);
            }
        });

        // for handling configuration change
        if (savedInstanceState != null) {
            //mListItems = savedInstanceState.getStringArrayList("mListItems");
            mListSectionPos = savedInstanceState.getIntegerArrayList("mListSectionPos");

            //if (mListItems != null && mListItems.size() > 0 && mListSectionPos != null && mListSectionPos.size() > 0) {
            //    setListAdaptor();
            //}

            String constraint = savedInstanceState.getString("constraint");
            if (constraint != null && constraint.length() > 0) {
                mSearchView.setText(constraint);
                setIndexBarViewVisibility(constraint);
            }
        } /*else {
            new Poplulate().execute(mItems);
        }*/

        return view;
    }


    @Override
    public void onResume(){
        super.onResume();
        TeamManager.getInstance().addControlManagerClientHandler(mHandler);
    }

    @Override
    public void onPause(){

        TeamManager.getInstance().removeControlManagerClientHandler(mHandler);
        super.onPause();
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        /*if (mListItems != null && mListItems.size() > 0) {
            outState.put.putStringArrayList("mListItems", mListItems);
        }*/
        if (mListSectionPos != null && mListSectionPos.size() > 0) {
            outState.putIntegerArrayList("mListSectionPos", mListSectionPos);
        }
        String searchText = mSearchView.getText().toString();
        if (searchText != null && searchText.length() > 0) {
            outState.putString("constraint", searchText);
        }
        super.onSaveInstanceState(outState);
    }

    // http://stackoverflow.com/questions/6028218/android-retain-callback-state-after-configuration-change/6029070#6029070
    // http://stackoverflow.com/questions/5151095/textwatcher-called-even-if-text-is-set-before-adding-the-watcher

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mSearchView.addTextChangedListener(filterTextWatcher);
        super.onActivityCreated(savedInstanceState);
    }

    private void setListAdaptor() {
        // create instance of PinnedHeaderAdapter and set adapter to list view
        mAdaptor = new PinnedHeaderAdapter(getActivity(), mListItems, mListSectionPos);
        mListView.setAdapter(mAdaptor);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);

        // set header view
        View pinnedHeaderView = inflater.inflate(R.layout.contacts_section_row_view, mListView, false);
        mListView.setPinnedHeaderView(pinnedHeaderView);

        // set index bar view
        IndexBarView indexBarView = (IndexBarView) inflater.inflate(R.layout.index_bar_view, mListView, false);
        indexBarView.setData(mListView, mListItems, mListSectionPos);
        mListView.setIndexBarView(indexBarView);

        // set preview text view
        View previewTextView = inflater.inflate(R.layout.preview_view, mListView, false);
        mListView.setPreviewView(previewTextView);

        // for configure pinned header view on scroll change
        mListView.setOnScrollListener(mAdaptor);
    }


    public class SortIgnoreCase implements Comparator<ContactModel> {
        public int compare(ContactModel s1, ContactModel s2) {
            return s1.getName().compareToIgnoreCase(s2.getName());
        }
    }

    public void setIndexBarViewVisibility(String constraint) {
        // hide index bar for search results
        if (constraint != null && constraint.length() > 0) {
            mListView.setIndexBarVisibility(false);
        } else {
            mListView.setIndexBarVisibility(true);
        }
    }

    // sort array and extract sections in background Thread here we use
    // AsyncTask
    private class Poplulate extends AsyncTask<ArrayList<ContactModel>, Void, Void> {

        private void showLoading(View contentView, View loadingView, View emptyView) {
            contentView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        private void showContent(View contentView, View loadingView, View emptyView) {
            contentView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }

        private void showEmptyText(View contentView, View loadingView, View emptyView) {
            contentView.setVisibility(View.GONE);
            loadingView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            // show loading indicator
            showLoading(mListView, mLoadingView, mEmptyView);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(ArrayList<ContactModel>... params) {
            mListItems.clear();
            mListSectionPos.clear();
            ArrayList<ContactModel> contacts = params[0];
            Logger.d("pre mListItems candidates " + contacts.size());
            if (mItems.size() > 0) {

                Collections.sort(contacts, new SortIgnoreCase());

                String prev_section = "";
                for (ContactModel contact : contacts) {

                    String current_item = contact.getName();
                    String current_section = current_item.substring(0, 1).toUpperCase(Locale.getDefault());

                    if (!prev_section.equals(current_section)) {
                        ContactModel dummyHeaderModel = new ContactModel();
                        dummyHeaderModel.setFirstName(current_section);
                        mListItems.add(dummyHeaderModel);
                        // NEW array list of section positions
                        mListSectionPos.add(mListItems.size()-1);
                        mListItems.add(contact);

                        // OLD array list of section positions
                        // mListSectionPos.add(mListItems.indexOf(current_section));


                        prev_section = current_section;

                    } else {
                        mListItems.add(contact);

                    }
                }
                Logger.d("pre mListItems candidates " + contacts.size());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                Logger.d("mListItems.size() " + mListItems.size());
                if (mListItems.size() <= 0) {
                    showEmptyText(mListView, mLoadingView, mEmptyView);
                } else {
                    setListAdaptor();
                    showContent(mListView, mLoadingView, mEmptyView);
                }
            }
            super.onPostExecute(result);
        }
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            String str = s.toString();
            if (mAdaptor != null && str != null)
                mAdaptor.getFilter().filter(str);
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    };

    public class ListFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // NOTE: this function is *always* called from a background thread,
            // and
            // not the UI thread.
            String constraintStr = constraint.toString().toLowerCase(Locale.getDefault());
            FilterResults result = new FilterResults();

            if (constraint != null && constraint.toString().length() > 0) {
                ArrayList<ContactModel> filterItems = new ArrayList<ContactModel>();

                synchronized (this) {
                    for (ContactModel contact : mItems) {
                        String item = contact.getName();
                        if (item.toLowerCase(Locale.getDefault()).startsWith(constraintStr)) {
                            filterItems.add(contact);
                        }
                    }
                    result.count = filterItems.size();
                    result.values = filterItems;
                }
            } else {
                synchronized (this) {
                    result.count = mItems.size();
                    result.values = mItems;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            ArrayList<ContactModel> filtered = (ArrayList<ContactModel>) results.values;
            setIndexBarViewVisibility(constraint.toString());
            // sort array and extract sections in background Thread
            new Poplulate().execute(filtered);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {

        if (msg.what == Constants.EventReturnState.GET_USERS_SUCCESS.value){
            mSearchView.setText("");
            if (mItems!=null) mItems.clear();
            mItems=null;
            mItems = (ArrayList)ContactsStaticDataModel.getAllUniqueContacts();
            new Poplulate().execute(mItems);
        }
        return false;
    }


}
