package com.familycircle.adapters;

//import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.activities.MainActivity;
import com.familycircle.custom.views.CircularImageView;
import com.familycircle.utils.imagecache.ImageLoader;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.MessageModel;


public class MessageHistoryListAdapter extends BaseAdapter
{
	private final static String TAG = "MessageListAdapter";
	private List<MessageModel> messages;
	private ImageLoader imageLoader;

	public MessageHistoryListAdapter()
	{
		imageLoader = new ImageLoader(TeamApp.getContext());
	}

	public void loadData(List<MessageModel> messages)
	{

		this.messages = messages;
		notifyDataSetChanged();
	}

	@Override
	public int getCount()
	{
		if (messages == null)
			return 0;
		return messages.size();
	}

	@Override
	public Object getItem(int position)
	{
		if (messages == null)
			return null;
		return messages.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View view, final ViewGroup parent)
	{

		MessageModel d = (MessageModel) getItem(position);

		if (view == null)
		{
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			view = inflater.inflate(R.layout.message_history_row_view, parent, false);

		}

		// Ignoring view holder pattern for now
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.contact_addTochatCheckboxId);
		TextView name = (TextView) view.findViewById(R.id.chat_sender_name);
		// TextView tn = (TextView)view.findViewById(R.id.chat_sender_number);
		TextView dateValue = (TextView) view.findViewById(R.id.time_of_chat);
		TextView chatDuration = (TextView) view.findViewById(R.id.time_of_chat_in_hours);
		TextView message = (TextView) view.findViewById(R.id.message_received);
		TextView unreadCount = (TextView) view.findViewById(R.id.messageUnreadCount);
		CircularImageView senderImage = (CircularImageView) view.findViewById(R.id.sender_image);
		//final MainActivity activity = (MainActivity) parent.getContext();
		if (d != null)
		{
			final ContactModel contactModel = ContactsStaticDataModel.getContactByIdTag(d.getTn());
			final String tn = d.getTn();
			checkBox.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{

				}
			});

			checkBox.setChecked(false);

			//String titleText = d.getTn();
			String dateStr = "";
			if (d.getTime() > 0)
			{
				try
				{
					dateStr = DateUtils.formatDateTime(TeamApp.getContext(), d.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);

				}
				catch (Exception e)
				{
					Log.d(TAG, "List Adapter Exception while parsing date", e);
				}
			}

			dateValue.setText(dateStr);
			message.setText(d.getBody());
			String nm = d.getName();

			if (nm == null || nm.trim().isEmpty())
			{
				if(d.getTn() != null){
					if (contactModel!=null) {
						name.setText(contactModel.getName());
					}
				}
			}
			else
			{
				name.setText(nm);
			}

			if (d.getUnreadCount() > 0)
			{
				unreadCount.setText(d.getUnreadCount() + "");
				unreadCount.setVisibility(View.VISIBLE);
			}
			else
			{
				unreadCount.setVisibility(View.GONE);
			}


			if (contactModel!=null && contactModel.getAvatarUrlSmall()!=null && contactModel.getAvatarUrlSmall().indexOf("http")>=0){
				imageLoader.DisplayImage(contactModel.getAvatarUrlSmall(), senderImage);
			} else {
				senderImage.setImageResource(R.drawable.icon_avatar);
			}
		}
		return view;
	}
}
