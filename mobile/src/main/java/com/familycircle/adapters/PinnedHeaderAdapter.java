// @author Bhavya Mehta
package com.familycircle.adapters;

import java.util.ArrayList;
import java.util.Locale;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.familycircle.R;
import com.familycircle.activities.InCallActivity;
import com.familycircle.activities.MainActivity;
import com.familycircle.activities.Maps2Activity;
import com.familycircle.activities.MapsActivity;
import com.familycircle.activities.MessageDetailActivity;
import com.familycircle.custom.views.CircularImageView;
import com.familycircle.custom.views.IPinnedHeader;
import com.familycircle.custom.views.PinnedHeaderListView;
import com.familycircle.fragments.ContactsFragment;
import com.familycircle.fragments.MessageHistoryFragment;
import com.familycircle.fragments.PhoneDataFragment;
import com.familycircle.utils.imagecache.ImageLoader;
import com.familycircle.lib.utils.Logger;
import com.familycircle.sdk.models.ContactModel;

// Customized adaptor to populate data in PinnedHeaderListView
public class PinnedHeaderAdapter extends BaseAdapter implements OnScrollListener, IPinnedHeader, Filterable {

	private static final int TYPE_ITEM = 0;
	private static final int TYPE_SECTION = 1;
	private static final int TYPE_MAX_COUNT = TYPE_SECTION + 1;

	LayoutInflater mLayoutInflater;
	int mCurrentSectionPosition = 0, mNextSectionPostion = 0;

	// array list to store section positions
	ArrayList<Integer> mListSectionPos;

	// array list to store list view data
	ArrayList<ContactModel> mListItems;

	// context object
	Context mContext;

	private ImageLoader imageLoader;

	public PinnedHeaderAdapter(Context context, ArrayList<ContactModel> listItems,ArrayList<Integer> listSectionPos) {
		this.mContext = context;
		this.mListItems = listItems;
		this.mListSectionPos = listSectionPos;

		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		imageLoader = new ImageLoader(mContext.getApplicationContext());
	}

	@Override
	public int getCount() {
		return mListItems.size();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return !mListSectionPos.contains(position);
	}

	@Override
	public int getViewTypeCount() {
		return TYPE_MAX_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		return mListSectionPos.contains(position) ? TYPE_SECTION : TYPE_ITEM;
	}

	@Override
	public Object getItem(int position) {
		return mListItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mListItems.get(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		ViewHolder holder = null;

		int type = getItemViewType(position);

		if (convertView == null) {
			holder = new ViewHolder();


			switch (type) {
                case TYPE_ITEM:
                    convertView = mLayoutInflater.inflate(R.layout.contacts_row_view, null);
                    break;
                case TYPE_SECTION:
                    convertView = mLayoutInflater.inflate(R.layout.contacts_section_row_view, null);
                    break;
			}

			holder.buttonCotainer = (LinearLayout) convertView.findViewById(R.id.buttonLL);
			holder.textView = (TextView) convertView.findViewById(R.id.row_title);
			holder.statusTextView = (TextView) convertView.findViewById(R.id.text_view_contact_tag);
			holder.imageView = (CircularImageView) convertView.findViewById(R.id.profileImage);
			holder.statusIndicatorView = (ImageView)convertView.findViewById(R.id.status_indicator);
			holder.videoCallButton = (ImageButton) convertView.findViewById(R.id.videoButtonCard);
			holder.messageCallButton = (ImageButton) convertView.findViewById(R.id.messageRtcButtonCard);

			holder.phoneButton = (ImageButton)convertView.findViewById(R.id.phoneButtonCard);
			holder.mapButton = (ImageButton)convertView.findViewById(R.id.mapButtonCard);
			holder.vitalsButton = (ImageButton)convertView.findViewById(R.id.vitalsButtonCard);
			holder.hrButton = (ImageButton)convertView.findViewById(R.id.hrButtonCard);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();

		}

		final ContactModel contact = mListItems.get(position);

		if (type == TYPE_ITEM) {
			if (contact.getStatus().equalsIgnoreCase("Online")) {
				holder.statusIndicatorView.setBackgroundResource(R.drawable.rounded_status_indicator_on_bg);
				holder.videoCallButton.setEnabled(true);
				holder.videoCallButton.setAlpha(1.0f);
			} else {
				holder.statusIndicatorView.setBackgroundResource(R.drawable.rounded_status_indicator_off_bg);
				holder.videoCallButton.setEnabled(false);
				holder.videoCallButton.setAlpha(0.7f);
			}
			holder.statusTextView.setText(contact.getStatus().toUpperCase());

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//Logger.d("onItemClick " + ll.isShown());
					final MainActivity mainActivity = (MainActivity) v.getContext();
					final Fragment fragment = mainActivity.getFragment();
					LinearLayout ll = (LinearLayout) v.findViewById(R.id.buttonLL);

					if (ll.isShown()) {
						ll.setVisibility(View.GONE);
						toggleIndexBar(fragment, true);
					} else {
						ll.setVisibility(View.VISIBLE);
						toggleIndexBar(fragment, false);
					}

				}
			});

			holder.videoCallButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final MainActivity mainActivity = (MainActivity) v.getContext();
					if (mainActivity != null) {
						Intent mainIntent = new Intent(mainActivity, InCallActivity.class);
						//mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mainIntent.putExtra("CALL_TYPE", "call");
						// this is like a call from current user to the user who is calling
						mainIntent.putExtra("CALLEE_ID", contact.getIdTag());
						mainIntent.putExtra("incomingCallInfo", "ContactsFragment");
						mainIntent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
						mainActivity.startActivityForResult(mainIntent, 0);
					}

				}
			});

			holder.messageCallButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final MainActivity mainActivity = (MainActivity) v.getContext();
					if (mainActivity != null) {
						final Fragment fragment = mainActivity.getFragment();
						if (fragment instanceof ContactsFragment) {
							Intent intent = new Intent(mainActivity, MessageDetailActivity.class);
							intent.putExtra("TAG_ID", contact.getIdTag());
							intent.putExtra("unreadCnt", 1);
							mainActivity.startActivityForResult(intent, 0);

						}
					}
				}
			});

			holder.mapButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final MainActivity mainActivity = (MainActivity) v.getContext();
					if (mainActivity != null) {
						final Fragment fragment = mainActivity.getFragment();
						if (fragment instanceof ContactsFragment) {
							Intent intent = new Intent(mainActivity, Maps2Activity.class);
							intent.putExtra("TAG_ID", contact.getIdTag());
							mainActivity.startActivityForResult(intent, 0);

						}
					}
				}
			});

			holder.phoneButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final MainActivity mainActivity = (MainActivity) v.getContext();
					if (mainActivity != null) {
						android.support.v4.app.FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
						Fragment fragment = new PhoneDataFragment();

						Bundle args = new Bundle();
						args.putString("TAG_ID", contact.getIdTag());
						fragment.setArguments(args);

						fragmentManager.beginTransaction()
								.replace(R.id.container, fragment)
								.addToBackStack(null)
								.commit();
					}
				}
			});

			if (contact.getAvatarUrlSmall()!=null && contact.getAvatarUrlSmall().indexOf("http")>=0){
				imageLoader.DisplayImage(contact.getAvatarUrlSmall(), holder.imageView);
			} else {
				holder.imageView.setImageResource(R.drawable.icon_avatar);
			}
		}

		holder.textView.setText(contact.getName());


		return convertView;
	}

	private void toggleIndexBar(Fragment fragment, boolean visible){
		if (fragment instanceof ContactsFragment) {
			ContactsFragment contactsFragment = (ContactsFragment)fragment;
			contactsFragment.setIndexBarViewVisibility(visible?"":"dummy");
		}
	}

	@Override
	public int getPinnedHeaderState(int position) {
		// hide pinned header when items count is zero OR position is less than
		// zero OR
		// there is already a header in list view
		if (getCount() == 0 || position < 0 || mListSectionPos.indexOf(position) != -1) {
			return PINNED_HEADER_GONE;
		}

		// the header should get pushed up if the top item shown
		// is the last item in a section for a particular letter.
		mCurrentSectionPosition = getCurrentSectionPosition(position);
		mNextSectionPostion = getNextSectionPosition(mCurrentSectionPosition);
		if (mNextSectionPostion != -1 && position == mNextSectionPostion - 1) {
			return PINNED_HEADER_PUSHED_UP;
		}

		return PINNED_HEADER_VISIBLE;
	}

	public int getCurrentSectionPosition(int position) {
		//String listChar = mListItems.get(position).toString().substring(0, 1).toUpperCase(Locale.getDefault());
		//return mListItems.indexOf(listChar);

		String listChar = mListItems.get(position).getName().substring(0, 1).toUpperCase(Locale.getDefault());
		int _pos = -1;
		for (ContactModel contact:mListItems) {
			if (contact.getName().indexOf(listChar) >= 0) {
				_pos = contact.getName().indexOf(listChar);
				break;
			}
		}
		return _pos;
	}

	public int getNextSectionPosition(int currentSectionPosition) {
		int index = mListSectionPos.indexOf(currentSectionPosition);
		if ((index + 1) < mListSectionPos.size()) {
			return mListSectionPos.get(index + 1);
		}
		return mListSectionPos.get(index);
	}

	@Override
	public void configurePinnedHeader(View v, int position) {
		// set text in pinned header
		TextView header = (TextView) v;
		mCurrentSectionPosition = getCurrentSectionPosition(position);
		header.setText(mListItems.get(mCurrentSectionPosition).getName());
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {
		if (view instanceof PinnedHeaderListView) {
			((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
	}

	@Override
	public Filter getFilter() {
        Fragment fragment = ((MainActivity) mContext).getFragment();
		//return ((MainActivity) mContext).new ListFilter();
        if (fragment instanceof ContactsFragment){
            return ((ContactsFragment) fragment).new ListFilter();
        }
		return null;
	}

	public static class ViewHolder {
		public TextView textView;
		public TextView statusTextView;
		public CircularImageView imageView;
		public ImageView statusIndicatorView;
		public ImageButton videoCallButton;
		public ImageButton messageCallButton;
		public LinearLayout buttonCotainer;
		public ImageView mapButton;
		public ImageView vitalsButton;
		public ImageView hrButton;
		public ImageButton phoneButton;
	}
}
