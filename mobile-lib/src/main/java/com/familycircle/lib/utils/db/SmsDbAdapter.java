package com.familycircle.lib.utils.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;

import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.lib.utils.Utils;
import com.familycircle.lib.utils.db.DBHelper.*;
import com.familycircle.lib.utils.Logger;
import com.familycircle.sdk.BaseApp;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.sdk.models.MessageModel;

public class SmsDbAdapter
{

	public static final String TAG = "SmsDbAdapter";

	private final static String[] SmsProjection = { DBHelper.ColsSMS.MID, ColsSMS.TN, ColsSMS.TYPE, ColsSMS.NAME, ColsSMS.SMS_BODY, ColsSMS.SMS_DIR, ColsSMS.STATUS, ColsSMS.TIME, ColsSMS.IS_READ, ColsSMS.IS_FAILED_MSG };

	public long insertSmsLogs(List<MessageModel> messages)
	{
		Logger.d("insertSmsLogs for TEMP .. start");
		long ret = 0, cnt = 0;
		SQLiteDatabase db = null;
		if (messages == null)
			return 0;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();
			db.beginTransaction(); // dont move from here or finally block will hang

			if (db != null && db.isOpen() && !messages.isEmpty())
			{
				for (MessageModel message : messages)
				{
					try
					{
						if (message.getStatus().trim().equalsIgnoreCase("D")) continue; // dont load deleted messages
						ContentValues val = new ContentValues();
						val.put(ColsSMS.MID, message.getMid());
						val.put(ColsSMS.TN, message.getTn());
						val.put(ColsSMS.NAME, message.getName());
						val.put(ColsSMS.SMS_BODY, message.getBody());
						val.put(ColsSMS.SMS_DIR, message.getDir());
						if (message.getStatus()!=null) {
							val.put(ColsSMS.STATUS, message.getStatus().equalsIgnoreCase("R")?"R":"U");
						} else {
							val.put(ColsSMS.STATUS, "U");
						}
						val.put(ColsSMS.TIME, message.getTime());
                        val.put(ColsSMS.INTERNAL_ID, message.getServerId()==null?"":message.getServerId());
						// val.put(ColsSMS.UNREAD_CNT,
						// message.getUnreadCount());
						int isRead = 0;

						isRead = (message.getStatus() == null || !message.getStatus().trim().equalsIgnoreCase("R")) ? 0 : 1;
						val.put(ColsSMS.IS_READ, isRead);
						val.put(ColsSMS.IS_FAILED_MSG, message.isFailed());
						ret = db.insert(DBHelper.TABLE_SMS_LOGS, null, val);
						Logger.d("INSERTING " + message.getTn());
						cnt++;
						Logger.d("insertSmsLogs " + message.getMid() + " RC=" + ret);
						setLastReceiveTime(message);
					}
					catch (Exception e)
					{
						Logger.e("insertSmsLogs insert issue", e);
					}

				}

				db.setTransactionSuccessful();

			}

		}
		catch (Exception e)
		{
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d("insertSmsLogs .. ending");
			if (db != null)
			{
				db.endTransaction();
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("insertSmsLogs .. end");
		}

		return cnt;
	}

	public boolean insertSmsLog(MessageModel message)
	{
		Logger.d("insertSmsLog for TEMP .. start");
		long ret = 0, cnt = 0;
		SQLiteDatabase db = null;
		if (message == null)
			return false;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();
			db.beginTransaction(); // dont move from here or finally block will hang

			if (db != null && db.isOpen())
			{
				if (message.getStatus().trim().equalsIgnoreCase("D")) return false; // dont load deleted messages
				ContentValues val = new ContentValues();
				val.put(ColsSMS.MID, message.getMid());
				val.put(ColsSMS.TN, message.getTn());
				val.put(ColsSMS.NAME, message.getName());
				val.put(ColsSMS.SMS_BODY, message.getBody());
				val.put(ColsSMS.SMS_DIR, message.getDir());
				if (message.getStatus()!=null) {
					val.put(ColsSMS.STATUS, message.getStatus().equalsIgnoreCase("R")?"R":"U");
				} else {
					val.put(ColsSMS.STATUS, "U");
				}
				val.put(ColsSMS.TIME, message.getTime());
				val.put(ColsSMS.INTERNAL_ID, message.getServerId()==null?"":message.getServerId());
				// val.put(ColsSMS.UNREAD_CNT,
				// message.getUnreadCount());
				int isRead = 0;

				isRead = (message.getStatus() == null || !message.getStatus().trim().equalsIgnoreCase("R")) ? 0 : 1;
				val.put(ColsSMS.IS_READ, isRead);
				val.put(ColsSMS.IS_FAILED_MSG, message.isFailed());
				ret = db.insert(DBHelper.TABLE_SMS_LOGS, null, val);
				Logger.d("INSERTING " + message.getTn());
				cnt++;
				Logger.d("insertSmsLog " + message.getMid() + " RC=" + ret);
				setLastReceiveTime(message);


				db.setTransactionSuccessful();

			}

		}
		catch (Exception e)
		{
			Logger.e( e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d("insertSmsLog .. ending");
			if (db != null)
			{
				db.endTransaction();
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("insertSmsLog .. end");
		}

		return true;
	}

	private void setLastReceiveTime(MessageModel message){
		if (message==null) return;
		PrefManagerBase pfm = new PrefManagerBase();
		long t = message.getTime();
		pfm.setLastQueryTime(t);

	}

	public long updateMessageStatus(int mid, String messageStatusValue)
	{
		Logger.d("updateMessageStatus .. start");
		long ret = 0;
		SQLiteDatabase db = null;
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();

			if (db != null && db.isOpen())
			{
				db.beginTransaction();

				String where = ColsSMS.MID + " = ?";

				String[] whereArgs = new String[] { mid + ""

				};

				ContentValues val = new ContentValues();
				val.put(ColsSMS.STATUS, messageStatusValue);

				ret = db.update(DBHelper.TABLE_SMS_LOGS, val, where, whereArgs);
				Logger.d("updateMessageStatus updated  " + mid + ":" + messageStatusValue + " RC=" + ret);
				db.setTransactionSuccessful();

				db.endTransaction();

			}

		}
		catch (Exception e)
		{
			Logger.d(e.toString(), e);
		}
		finally
		{
			Logger.d("updateMessageStatus .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("updateMessageStatus .. end");
		}

		return ret;
	}

	public long updateMessageStatusByTn(String tn, String messageStatusValue)
	{
		Logger.d("updateMessageStatusByTn .. start");
		long ret = 0;
		SQLiteDatabase db = null;
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();

			if (db != null && db.isOpen())
			{
				db.beginTransaction();

				String where = ColsSMS.TN + " = ?";

				String[] whereArgs = new String[] { tn

				};

				ContentValues val = new ContentValues();
				val.put(ColsSMS.STATUS, messageStatusValue);

				ret = db.update(DBHelper.TABLE_SMS_LOGS, val, where, whereArgs);
				Logger.d("updateMessageStatusByTn updated  " + tn + ":" + messageStatusValue + " RC=" + ret);
				db.setTransactionSuccessful();

				db.endTransaction();

			}

		}
		catch (Exception e)
		{
			Logger.d(e.toString(), e);
		}
		finally
		{
			Logger.d("updateMessageStatusByTn .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("updateMessageStatusByTn .. end");
		}

		return ret;
	}

	// dir has values I or O for incoming or outgoing respectively
	public List<MessageModel> getGroupedMessage()
	{
		List<MessageModel> messages = new ArrayList<MessageModel>();
		List<MessageModel> _messages = new ArrayList<MessageModel>();
		String queryStr = "select  " + ColsSMS.MID + "," + ColsSMS.TN + "," + ColsSMS.NAME + "," + ColsSMS.SMS_BODY + "," + ColsSMS.SMS_DIR + "," + ColsSMS.STATUS + "," + ColsSMS.TIME + "," + ColsSMS.IS_READ + "," + ColsSMS.IS_FAILED_MSG + " FROM " + DBHelper.TABLE_SMS_LOGS
				// + " group by " + ColsSMS.TN
				/*+ " where " + ColsSMS.SMS_DIR + "='" + dir + "'"*/ + " order by " + ColsSMS.TN + ", " + ColsSMS.TIME + " asc";

		Logger.d("getGroupedMessage .. start");

		SQLiteDatabase db = null;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getReadableDatabase();

			Cursor mCursor = db.rawQuery(queryStr, null);

			while (mCursor.moveToNext())
			{
				//Logger.d(TAG, "getGroupedMessage0 " + mCursor.getString(1));
				MessageModel model = new MessageModel();
				// model.setUnreadCount(mCursor.getInt(0));
				if (!mCursor.isNull(0))
					model.setMid(mCursor.getInt(0));
				model.setTn(mCursor.getString(1));
				model.setName(mCursor.getString(2));
				model.setBody(mCursor.getString(3));
				model.setDir(mCursor.getString(4));
				model.setStatus(mCursor.getString(5));
				if (!mCursor.isNull(6))
					model.setTime(mCursor.getLong(6));
				if (!mCursor.isNull(8))
					model.setFailed(mCursor.getInt(8) > 0);
				else
					model.setFailed(false);
				
				
				if (!model.isFailed()) _messages.add(model);
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
		}
		finally
		{
			Logger.d("getGroupedMessage .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("getGroupedMessage .. end");
		}

		if (_messages != null)
		{

			String tn = "";
			MessageModel tempModel = null;
			long cnt = 0;
			long tnGroupCnt = 0;
			long unreadCnt = 0;

			long lastRecordCnt = _messages.size();
			for (MessageModel message : _messages)
			{
				//Logger.d(TAG, "getGroupedMessage1 " + message.getTn());
				if(message.getTn() != null)
				{
				if ((!message.getTn().equalsIgnoreCase(tn)) && cnt > 0)
				{
					// Add the last record of the previous tn
					//Logger.d(TAG, "getGroupedMessage1 add " + message.getTn());
					messages.add(tempModel);
					unreadCnt = 0;
				}
				}

				if (!message.getStatus().trim().equalsIgnoreCase("R"))
				{
					
					unreadCnt += 1;
					message.setUnreadCount(unreadCnt);
				}

				cnt++;
				tempModel = message;
				
				if (lastRecordCnt == cnt)
				{
					//Logger.d(TAG, "getGroupedMessage1 add " + tempModel.getTn());
					messages.add(tempModel);
				}
				
				tn = message.getTn();

			}

			try
			{
				Collections.sort(messages, new Comparator<MessageModel>()
						{
					public int compare(MessageModel result1, MessageModel result2)
					{
						return (result2.getTime() > result1.getTime()) ? 1 : -1;
					}
						});

			}
			catch (Exception e)
			{
				Logger.e("Error while sorting message models", e);
			}
		}

		
		// Get the appropriate contacts from the database retrieved records
		if (messages != null)
		{
			List<ContactModel> contacts = ContactsStaticDataModel.getContacts();
			Logger.d("total contacts " + (contacts==null?0:contacts.size()) );
			if (contacts != null && !contacts.isEmpty())
			{
				HashMap<String, ContactModel> contactTable = new HashMap<String, ContactModel>();
				for (ContactModel contact : contacts)
				{
					if (contact.getIdTag() != null)
					{
						contactTable.put(contact.getIdTag(), contact);

					}
				}

				if (!contactTable.isEmpty())
				{
					for (MessageModel message : messages)
					{
						if (contactTable.containsKey(message.getTn()))
						{
                            ContactModel contactDetail = contactTable.get(message.getTn());
							if (contactDetail != null)
							{
								message.setContactId(contactDetail.getContentId());
								message.setContactPhotoURI(contactDetail.getAvatarUrlSmall());
								message.setContactName(contactDetail.getName());
							}
							Logger.d("getGroupedMessage3 " + message.getTn() +  ", " + message.getName() +", "+ message.getContactPhotoURI()+"/"+contactDetail.getContentId());
						}
					}
				}
			}
		}
		return messages;
	}

	public List<MessageModel> getMessagesByTn(String tn)
	{
		if (tn == null)
			return null;
		List<MessageModel> messages = new ArrayList<MessageModel>();
		String queryStr = "select " + ColsSMS.MID + "," + ColsSMS.TN + "," + ColsSMS.NAME + "," + ColsSMS.SMS_BODY + "," + ColsSMS.SMS_DIR + "," + ColsSMS.STATUS + "," + ColsSMS.TIME + "," + ColsSMS.IS_READ + "," + ColsSMS.IS_FAILED_MSG + " FROM " + DBHelper.TABLE_SMS_LOGS + " WHERE " + ColsSMS.TN + "='" + tn.trim() + "'" + " order by " + ColsSMS.TIME + " asc";

		Logger.d("getMessagesByTn .. start " + tn);

		SQLiteDatabase db = null;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getReadableDatabase();

			Cursor mCursor = db.rawQuery(queryStr, null);
			String prevDateStr = null;
			String dateStr = null;
			int total = mCursor.getCount();
			int cnt = 0;
			while (mCursor.moveToNext())
			{
				cnt++;
				MessageModel model = new MessageModel();
				if (!mCursor.isNull(0))
					model.setMid(mCursor.getInt(0));
				model.setTn(mCursor.getString(1));
				model.setName(mCursor.getString(2));
				model.setBody(mCursor.getString(3));
				model.setDir(mCursor.getString(4));
				model.setStatus(mCursor.getString(5));

				dateStr = null;
				if (!mCursor.isNull(6))
				{
					model.setTime(mCursor.getLong(6));

					long t = mCursor.getLong(6);
					dateStr = DateUtils.formatDateTime(BaseApp.getContext(), t, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
					if (prevDateStr == null || !dateStr.equalsIgnoreCase(prevDateStr))
					{
						model.setDateBreakTime(mCursor.getLong(6));
					}
					else if (total == cnt && !dateStr.equalsIgnoreCase(prevDateStr))
					{
						model.setDateBreakTime(mCursor.getLong(6));
					}

				}
				prevDateStr = dateStr;

				if (!mCursor.isNull(8))
					model.setFailed(mCursor.getInt(8) > 0);
				else
					model.setFailed(false);
				messages.add(model);
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
		}
		finally
		{
			Logger.d("getMessagesByTn .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("getMessagesByTn .. end");
		}
		// Get the appropriate contacts from the database retrieved records
		if (messages != null)
		{
            List<ContactModel> contacts = ContactsStaticDataModel.getContacts();
            Logger.d("total contacts " + (contacts==null?0:contacts.size()) );
            if (contacts != null && !contacts.isEmpty())
            {
                HashMap<String, ContactModel> contactTable = new HashMap<String, ContactModel>();
                for (ContactModel contact : contacts)
                {
                    if (contact.getIdTag() != null)
                    {
                        contactTable.put(contact.getIdTag(), contact);

                    }
                }

                if (!contactTable.isEmpty())
                {
                    for (MessageModel message : messages)
                    {
                        if (contactTable.containsKey(message.getTn()))
                        {
                            ContactModel contactDetail = contactTable.get(message.getTn());
                            if (contactDetail != null)
                            {
                                message.setContactId(contactDetail.getContentId());
                                message.setContactPhotoURI(contactDetail.getAvatarUrlSmall());
                                message.setContactName(contactDetail.getName());
                            }
                            Logger.d("getGroupedMessage3 " + message.getTn() +  ", " + message.getName() +", "+ message.getContactPhotoURI()+"/"+contactDetail.getContentId());
                        }
                    }
                }
            }
		}

		return messages;
	}

	public List<MessageModel> getMessagesByMultipleTn(String[] tnArray)
	{
		if (tnArray == null)
			return null;

		List<MessageModel> messages = new ArrayList<MessageModel>();
		String queryStr = "select " + ColsSMS.MID + "," + ColsSMS.TN + "," + ColsSMS.NAME + "," + ColsSMS.SMS_BODY + "," + ColsSMS.SMS_DIR + "," + ColsSMS.STATUS + "," + ColsSMS.TIME + "," + ColsSMS.IS_READ + "," + ColsSMS.IS_FAILED_MSG + " FROM " + DBHelper.TABLE_SMS_LOGS

				+ " WHERE " + ColsSMS.TN + " IN ( " + makePlaceholders(tnArray.length) + ") "

	+ " order by " + ColsSMS.TIME + " asc";

		SQLiteDatabase db = null;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getReadableDatabase();

			Cursor mCursor = db.rawQuery(queryStr, tnArray);
			String prevDateStr = null;
			String dateStr = null;
			int total = mCursor.getCount();
			int cnt = 0;
			while (mCursor.moveToNext())
			{
				cnt++;
				MessageModel model = new MessageModel();
				if (!mCursor.isNull(0))
					model.setMid(mCursor.getInt(0));
				model.setTn(mCursor.getString(1));
				model.setName(mCursor.getString(2));
				model.setBody(mCursor.getString(3));
				model.setDir(mCursor.getString(4));
				model.setStatus(mCursor.getString(5));

				dateStr = null;
				if (!mCursor.isNull(6))
				{
					model.setTime(mCursor.getLong(6));

					long t = mCursor.getLong(6);
					dateStr = DateUtils.formatDateTime(BaseApp.getContext(), t, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH);
					if (prevDateStr == null || !dateStr.equalsIgnoreCase(prevDateStr))
					{
						model.setDateBreakTime(mCursor.getLong(6));
					}
					else if (total == cnt && !dateStr.equalsIgnoreCase(prevDateStr))
					{
						model.setDateBreakTime(mCursor.getLong(6));
					}

				}
				prevDateStr = dateStr;

				if (!mCursor.isNull(8))
					model.setFailed(mCursor.getInt(8) > 0);
				else
					model.setFailed(false);
				messages.add(model);
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
		}
		finally
		{
			Logger.d("getMessagesByMultipleTn .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("getMessagesByMultipleTn .. end");
		}
		// Get the appropriate contacts from the database retrieved records
		if (messages != null)
		{
            List<ContactModel> contacts = ContactsStaticDataModel.getContacts();
            Logger.d("total contacts " + (contacts==null?0:contacts.size()) );
            if (contacts != null && !contacts.isEmpty())
            {
                HashMap<String, ContactModel> contactTable = new HashMap<String, ContactModel>();
                for (ContactModel contact : contacts)
                {
                    if (contact.getIdTag() != null)
                    {
                        contactTable.put(contact.getIdTag(), contact);

                    }
                }

                if (!contactTable.isEmpty())
                {
                    for (MessageModel message : messages)
                    {
                        if (contactTable.containsKey(message.getTn()))
                        {
                            ContactModel contactDetail = contactTable.get(message.getTn());
                            if (contactDetail != null)
                            {
                                message.setContactId(contactDetail.getContentId());
                                message.setContactPhotoURI(contactDetail.getAvatarUrlSmall());
                                message.setContactName(contactDetail.getName());
                            }
                            Logger.d("getGroupedMessage3 " + message.getTn() +  ", " + message.getName() +", "+ message.getContactPhotoURI()+"/"+contactDetail.getContentId());
                        }
                    }
                }
            }
		}

		return messages;
	}

	String makePlaceholders(int len)
	{
		if (len < 1)
		{
			// It will lead to an invalid query anyway ..
			throw new RuntimeException("No placeholders");
		}
		else
		{
			StringBuilder sb = new StringBuilder(len * 2 - 1);
			sb.append("?");
			for (int i = 1; i < len; i++)
			{
				sb.append(",?");
			}
			return sb.toString();
		}
	}

	public List<MessageModel> getMessagesById(int mid)
	{
		List<MessageModel> messages = new ArrayList<MessageModel>();
		String queryStr = "select " + ColsSMS.MID + "," + ColsSMS.TN + "," + ColsSMS.NAME + "," + ColsSMS.SMS_BODY + "," + ColsSMS.SMS_DIR + "," + ColsSMS.STATUS + "," + ColsSMS.TIME + "," + ColsSMS.IS_READ + "," + ColsSMS.IS_FAILED_MSG + " FROM " + DBHelper.TABLE_SMS_LOGS + " WHERE " + ColsSMS.MID + "=" + mid + " order by " + ColsSMS.TIME + " desc";

		Logger.d("getMessagesById .. start");

		SQLiteDatabase db = null;

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getReadableDatabase();

			Cursor mCursor = db.rawQuery(queryStr, null);

			while (mCursor.moveToNext())
			{
				MessageModel model = new MessageModel();
				if (!mCursor.isNull(0))
					model.setMid(mCursor.getInt(0));
				model.setTn(mCursor.getString(1));
				model.setName(mCursor.getString(2));
				model.setBody(mCursor.getString(3));
				model.setDir(mCursor.getString(4));
				model.setStatus(mCursor.getString(5));
				if (!mCursor.isNull(6))
					model.setTime(mCursor.getLong(6));
				if (!mCursor.isNull(8))
					model.setFailed(mCursor.getInt(8) > 0);
				else
					model.setFailed(false);
				messages.add(model);
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
		}
		finally
		{
			Logger.d("getMessagesById .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("getMessagesById .. end");
		}

		return messages;
	}
	
	public int getUnreadCount()
	{
		String queryStr = "select " + ColsSMS.MID + " FROM " + DBHelper.TABLE_SMS_LOGS + " WHERE " + ColsSMS.STATUS + "!= 'R' OR " + ColsSMS.STATUS + " is null";

		Logger.d("getUnreadCountd .. start");

		SQLiteDatabase db = null;

		final DBHelper mDbHelper = DBHelper.getInstance();
		
		int cnt = 0;
		
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getReadableDatabase();

			Cursor mCursor = db.rawQuery(queryStr, null);

			cnt = mCursor.getCount();

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
		}
		finally
		{
			Logger.d("getUnreadCount .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("getUnreadCount .. end");
		}

		return cnt;
	}

	public boolean deleteAll()
	{
		SQLiteDatabase db = null;
		Logger.d("deleteAll .. start");

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();

			if (db != null && db.isOpen())
			{
				db.beginTransaction();

				long cnt = db.delete(DBHelper.TABLE_SMS_LOGS, null, null);

				db.setTransactionSuccessful();

				db.endTransaction();
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d("deleteAll .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("deleteAll .. end");
		}

		return true;
	}
	
	public boolean deleteMessageById(int mid)
	{
		SQLiteDatabase db = null;
		Logger.d("deleteMessageById .. start");

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();

			if (db != null && db.isOpen())
			{
				db.beginTransaction();
				String where = ColsSMS.MID + " = " + mid;

				long cnt = db.delete(DBHelper.TABLE_SMS_LOGS, where, null);

				db.setTransactionSuccessful();

				db.endTransaction();
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d("deleteMessageById .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("deleteMessageById .. end");
		}

		return true;
	}

	public boolean deleteMessagesByTn(String tn)
	{
		SQLiteDatabase db = null;
		Logger.d("deleteMessagesByTn .. start " + tn);

		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{

			mDbHelper.acquireAccess();

			db = mDbHelper.getWritableDatabase();

			if (db != null && db.isOpen())
			{
				db.beginTransaction();

				String where = ColsSMS.TN + " = '" + tn + "'";

				long cnt = db.delete(DBHelper.TABLE_SMS_LOGS, where, null);

				db.setTransactionSuccessful();

				db.endTransaction();
			}

		}
		catch (Exception e)
		{
			Logger.e(e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d("deleteMessagesByTn .. ending");
			if (db != null)
			{
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d("deleteMessagesByTn .. end");
		}

		return true;
	}
	
	public  void insertDummyMessage(String tn, String body) {
		
		List<MessageModel> list = new ArrayList<MessageModel>();
		MessageModel message = new MessageModel();
		message.setBody(body);
		message.setDir("O");
		message.setStatus("R");
		message.setTime(System.currentTimeMillis());
		message.setTn(Utils.getPhoneNumberDigits(tn));
		message.setFailed(true);
		message.setType("sms");
		message.setMid(Utils.generateMid(message));
		list.add(message);
		insertSmsLogs(list);
	}


}
