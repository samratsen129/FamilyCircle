package com.familycircle.lib.utils.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.familycircle.lib.utils.db.DBHelper.*;
import com.familycircle.lib.utils.Logger;

public class DialerDbAdapter {
	public final String TAG = "DialerDbAdapter";
	
/*	public long insertCallLogs(List<CallItem> calls)
	{
		Logger.d( "insertCallLogs .. start");
		long ret = 0, cnt = 0;
		SQLiteDatabase db = null;
		if (calls==null) return 0;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getWritableDatabase();
			db.beginTransaction(); // dont move from here or finally block will hang
			
			if (db != null && db.isOpen() && !calls.isEmpty())
			{
				for (CallItem call: calls){
					try {
						
						ContentValues val = new ContentValues();
						val.put(ColsCall.ID, call._id);
						val.put(ColsCall.CALL_ID, call.callID);
						val.put(ColsCall.IS_CONF_CALL, call.isConfCall?1:0);
						val.put(ColsCall.CALL_TYPE, call.callType.name());
						val.put(ColsCall.TN, call.phoneNum);
						val.put(ColsCall.TN_NAME, call.phoneNumName);
						val.put(ColsCall.USER_TN, call.userPhoneNum);
						val.put(ColsCall.USER_ACTION, call.userAction.name());
						val.put(ColsCall.CALL_STATUS, call.callStatus.name());
						val.put(ColsCall.CRE_DT, call.createDate);
						val.put(ColsCall.CALL_START_DT , call.startDate);
						val.put(ColsCall.CALL_END_DT , call.endDate);
						val.put(ColsCall.CALL_DURATION , call.duration);
						val.put(ColsCall.CALL_STATUS_INFO1 , call.callStats1);
						val.put(ColsCall.CALL_STATUS_INFO2 , call.callStats2);
						

						ret = db.insert(DBHelper.TABLE_CALL_LOGS,
							                        null,
							                        val);
						cnt++;
						Logger.d( "insertCallLogs " + call.phoneNum+" RC="+ret);
					
					} catch (Exception e){
						Logger.e( "insertCallLogs insert issue", e);
					}
					
				}
				
				db.setTransactionSuccessful();
				
				
			}
			
	
		}
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "insertCallLogs .. ending");
			if (db != null) {
				db.endTransaction();
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "insertCallLogs .. end");
		}
		
		return cnt;
	}
	
	public long updateCall(CallItem call)
	{
		Logger.d( "updateCall .. start");
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
				
				String where =
						ColsCall.ID
		                + " = ?";
				
				String[] whereArgs =
					    new String[]
					    {
					       call._id+""

					    };

				ContentValues val = new ContentValues();
				val.put(ColsCall.CALL_ID, call.callID);
				val.put(ColsCall.IS_CONF_CALL, call.isConfCall?1:0);
				val.put(ColsCall.CALL_TYPE, call.callType.name());
				val.put(ColsCall.TN, call.phoneNum);
				val.put(ColsCall.TN_NAME, call.phoneNumName);
				val.put(ColsCall.USER_TN, call.userPhoneNum);
				val.put(ColsCall.USER_ACTION, call.userAction.name());
				val.put(ColsCall.CALL_STATUS, call.callStatus.name());
				val.put(ColsCall.CRE_DT, call.createDate);
				val.put(ColsCall.CALL_START_DT , call.startDate);
				val.put(ColsCall.CALL_END_DT , call.endDate);
				val.put(ColsCall.CALL_DURATION , call.duration);
				val.put(ColsCall.CALL_STATUS_INFO1 , call.callStats1);
				val.put(ColsCall.CALL_STATUS_INFO2 , call.callStats2);
				
				ret = db.update(DBHelper.TABLE_CALL_LOGS, val, where,
			                			whereArgs);
				Logger.d( "updateCall updated  " + call.phoneNum +": RC="+ret);
				db.setTransactionSuccessful();
					
				db.endTransaction();
					
			}
			

		}
		catch (Exception e){
			Logger.d( e.toString(), e);
		}
		finally
		{
			Logger.d( "updateCall .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "updateCall .. end");
		}
		
		return ret;
	}
	
	public List<CallItem> getCallById(int id){
		
		Logger.d( "getCallById .. start");
		
		List<CallItem> calls = new ArrayList<CallItem>();
		String queryStr = "select "
				+ DBHelper.ColsCall.ID +","
				+ DBHelper.ColsCall.CALL_ID +","
				+ ColsCall.IS_CONF_CALL +","
				+ ColsCall.CALL_TYPE +","
				+ ColsCall.TN+","
				+ ColsCall.TN_NAME+","
				+ ColsCall.USER_TN+","
				+ ColsCall.USER_ACTION+","
				+ ColsCall.CALL_STATUS +","
				+ ColsCall.CRE_DT+","
				+ ColsCall.CALL_START_DT+","
				+ ColsCall.CALL_END_DT+","
				+ ColsCall.CALL_DURATION+","
				+ ColsCall.CALL_STATUS_INFO1+","
				+ ColsCall.CALL_STATUS_INFO2
				+ " FROM " + DBHelper.TABLE_CALL_LOGS
				+ " WHERE "+ ColsCall.ID+"="+id
				+ " order by " + ColsCall.CRE_DT +" asc";
		
		Logger.d( "getCallById .. start " + id);

		SQLiteDatabase db = null;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getReadableDatabase();
			
			Cursor mCursor = db.rawQuery(queryStr, null);
			
			while (mCursor.moveToNext()) {
				
				CallItem call = new CallItem();
				
				call._id = mCursor.getInt(0);
				if (!mCursor.isNull(1)) {
					call.callID = mCursor.getInt(1);
				} else {
					call.callID = -1;
				}
				call.isConfCall = mCursor.getInt(2) > 0;
				call.callType = CallType.valueOf(mCursor.getString(3));
				call.phoneNum = mCursor.getString(4);
				call.phoneNumName = mCursor.getString(5);
				call.userPhoneNum = mCursor.getString(6);
				call.userAction = CallUserAction.valueOf(mCursor.getString(7));
				call.callStatus = CallStatus.valueOf(mCursor.getString(8));
				call.createDate = mCursor.getLong(9);
				call.startDate = mCursor.getLong(10);
				call.endDate = mCursor.getLong(11);
				call.duration = mCursor.getLong(12);
				call.callStats1 = mCursor.getString(13);
				call.callStats2 = mCursor.getString(14);
								
				calls.add(call);
				
			}
			
		} 
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "getCallById .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "getCallById .. end");
		}
		
		return calls;
	}
	
	public List<CallItem> getCallByCallId(int callId){
		
		Logger.d( "getCallByCallId .. start");
		
		List<CallItem> calls = new ArrayList<CallItem>();
		String queryStr = "select "
				+ ColsCall.ID +","
				+ ColsCall.CALL_ID +","
				+ ColsCall.IS_CONF_CALL +","
				+ ColsCall.CALL_TYPE +","
				+ ColsCall.TN+","
				+ ColsCall.TN_NAME+","
				+ ColsCall.USER_TN+","
				+ ColsCall.USER_ACTION+","
				+ ColsCall.CALL_STATUS +","
				+ ColsCall.CRE_DT+","
				+ ColsCall.CALL_START_DT+","
				+ ColsCall.CALL_END_DT+","
				+ ColsCall.CALL_DURATION+","
				+ ColsCall.CALL_STATUS_INFO1+","
				+ ColsCall.CALL_STATUS_INFO2
				+ " FROM " + DBHelper.TABLE_CALL_LOGS
				+ " WHERE "+ ColsCall.ID+"="+callId
				+ " order by " + ColsCall.CRE_DT +" asc";
		
		Logger.d( "getCallByCallId .. start " + callId);

		SQLiteDatabase db = null;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getReadableDatabase();
			
			Cursor mCursor = db.rawQuery(queryStr, null);
			
			while (mCursor.moveToNext()) {
				
				CallItem call = new CallItem();
				
				call._id = mCursor.getInt(0);
				if (!mCursor.isNull(1)) {
					call.callID = mCursor.getInt(1);
				} else {
					call.callID = -1;
				}
				call.isConfCall = mCursor.getInt(2) > 0;
				call.callType = CallType.valueOf(mCursor.getString(3));
				call.phoneNum = mCursor.getString(4);
				call.phoneNumName = mCursor.getString(5);
				call.userPhoneNum = mCursor.getString(6);
				call.userAction = CallUserAction.valueOf(mCursor.getString(7));
				call.callStatus = CallStatus.valueOf(mCursor.getString(8));
				call.createDate = mCursor.getLong(9);
				call.startDate = mCursor.getLong(10);
				call.endDate = mCursor.getLong(11);
				call.duration = mCursor.getLong(12);
				call.callStats1 = mCursor.getString(13);
				call.callStats2 = mCursor.getString(14);
								
				calls.add(call);
				
			}
			
		} 
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "getCallByCallId .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "getCallByCallId .. end");
		}
		
		return calls;
	}
	
	public List<CallItem> getCallByTn(String tn){
		
		Logger.d( "getCallByTn .. start");
		
		List<CallItem> calls = new ArrayList<CallItem>();
		String queryStr = "select "
				+ ColsCall.ID +","
				+ ColsCall.CALL_ID +","
				+ ColsCall.IS_CONF_CALL +","
				+ ColsCall.CALL_TYPE +","
				+ ColsCall.TN+","
				+ ColsCall.TN_NAME+","
				+ ColsCall.USER_TN+","
				+ ColsCall.USER_ACTION+","
				+ ColsCall.CALL_STATUS +","
				+ ColsCall.CRE_DT+","
				+ ColsCall.CALL_START_DT+","
				+ ColsCall.CALL_END_DT+","
				+ ColsCall.CALL_DURATION+","
				+ ColsCall.CALL_STATUS_INFO1+","
				+ ColsCall.CALL_STATUS_INFO2
				+ " FROM " + DBHelper.TABLE_CALL_LOGS
				+ " WHERE "+ ColsCall.ID+"='"+tn.trim()+"'"
				+ " order by " + ColsCall.CRE_DT +" asc";
		
		Logger.d( "getCallByTn .. start " + tn);

		SQLiteDatabase db = null;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getReadableDatabase();
			
			Cursor mCursor = db.rawQuery(queryStr, null);
			
			while (mCursor.moveToNext()) {
				
				CallItem call = new CallItem();
				
				call._id = mCursor.getInt(0);
				if (!mCursor.isNull(1)) {
					call.callID = mCursor.getInt(1);
				} else {
					call.callID = -1;
				}
				call.isConfCall = mCursor.getInt(2) > 0;
				call.callType = CallType.valueOf(mCursor.getString(3));
				call.phoneNum = mCursor.getString(4);
				call.phoneNumName = mCursor.getString(5);
				call.userPhoneNum = mCursor.getString(6);
				call.userAction = CallUserAction.valueOf(mCursor.getString(7));
				call.callStatus = CallStatus.valueOf(mCursor.getString(8));
				call.createDate = mCursor.getLong(9);
				call.startDate = mCursor.getLong(10);
				call.endDate = mCursor.getLong(11);
				call.duration = mCursor.getLong(12);
				call.callStats1 = mCursor.getString(13);
				call.callStats2 = mCursor.getString(14);
								
				calls.add(call);
				
			}
			
		} 
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "getCallByTn .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "getCallByTn .. end");
		}
		
		return calls;
	}
	
	public List<CallItem> getAllCalls(){
		
		Logger.d( "getAllCalls .. start");
		
		List<CallItem> calls = new ArrayList<CallItem>();
		String queryStr = "select "
				+ ColsCall.ID +","
				+ ColsCall.CALL_ID +","
				+ ColsCall.IS_CONF_CALL +","
				+ ColsCall.CALL_TYPE +","
				+ ColsCall.TN+","
				+ ColsCall.TN_NAME+","
				+ ColsCall.USER_TN+","
				+ ColsCall.USER_ACTION+","
				+ ColsCall.CALL_STATUS +","
				+ ColsCall.CRE_DT+","
				+ ColsCall.CALL_START_DT+","
				+ ColsCall.CALL_END_DT+","
				+ ColsCall.CALL_DURATION+","
				+ ColsCall.CALL_STATUS_INFO1+","
				+ ColsCall.CALL_STATUS_INFO2
				+ " FROM " + DBHelper.TABLE_CALL_LOGS
				+ " order by " + ColsCall.CRE_DT +" asc";
		
		Logger.d( "getAllCalls .. start ");

		SQLiteDatabase db = null;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getReadableDatabase();
			
			Cursor mCursor = db.rawQuery(queryStr, null);
			
			while (mCursor.moveToNext()) {
				
				CallItem call = new CallItem();
				
				call._id = mCursor.getInt(0);
				if (!mCursor.isNull(1)) {
					call.callID = mCursor.getInt(1);
				} else {
					call.callID = -1;
				}
				call.isConfCall = mCursor.getInt(2) > 0;
				call.callType = CallType.valueOf(mCursor.getString(3));
				call.phoneNum = mCursor.getString(4);
				call.phoneNumName = mCursor.getString(5);
				call.userPhoneNum = mCursor.getString(6);
				call.userAction = CallUserAction.valueOf(mCursor.getString(7));
				call.callStatus = CallStatus.valueOf(mCursor.getString(8));
				call.createDate = mCursor.getLong(9);
				call.startDate = mCursor.getLong(10);
				call.endDate = mCursor.getLong(11);
				call.duration = mCursor.getLong(12);
				call.callStats1 = mCursor.getString(13);
				call.callStats2 = mCursor.getString(14);
								
				calls.add(call);
				
			}
			
		} 
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "getAllCalls .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "getAllCalls .. end");
		}
		
		return calls;
	}
	
	public List<CallItem> getLogsByUserAction(String action){
	Logger.d( "getCallByTn .. start");
		
		List<CallItem> calls = new ArrayList<CallItem>();
		String queryStr = "select "
				+ ColsCall.ID +","
				+ ColsCall.CALL_ID +","
				+ ColsCall.IS_CONF_CALL +","
				+ ColsCall.CALL_TYPE +","
				+ ColsCall.TN+","
				+ ColsCall.TN_NAME+","
				+ ColsCall.USER_TN+","
				+ ColsCall.USER_ACTION+","
				+ ColsCall.CALL_STATUS +","
				+ ColsCall.CRE_DT+","
				+ ColsCall.CALL_START_DT+","
				+ ColsCall.CALL_END_DT+","
				+ ColsCall.CALL_DURATION+","
				+ ColsCall.CALL_STATUS_INFO1+","
				+ ColsCall.CALL_STATUS_INFO2
				+ " FROM " + DBHelper.TABLE_CALL_LOGS;
				if (action!=null && action.equalsIgnoreCase(CallUserAction.Missed.name())){
					queryStr += " WHERE "+ ColsCall.USER_ACTION+"='"+CallUserAction.Missed.name()+"'";
				}
				queryStr +=  " order by " + ColsCall.CRE_DT +" desc";
		
		Logger.d( "getCallByUserAction .. start " + action);

		SQLiteDatabase db = null;
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getReadableDatabase();
			
			Cursor mCursor = db.rawQuery(queryStr, null);
			//Logger.e( "COUNT = "+mCursor.getCount());
			int count = 0;
			while (mCursor.moveToNext()) {
				//Logger.e( "while count =  "+count);
				count++;
				CallItem call = new CallItem();
				
				call._id = mCursor.getInt(0);
				if (!mCursor.isNull(1)) {
					call.callID = mCursor.getInt(1);
				} else {
					call.callID = -1;
				}
				call.isConfCall = mCursor.getInt(2) > 0;
				call.callType = CallType.valueOf(mCursor.getString(3));
				call.phoneNum = mCursor.getString(4);
				call.phoneNumName = mCursor.getString(5);
				call.userPhoneNum = mCursor.getString(6);
				call.userAction = CallUserAction.valueOf(mCursor.getString(7));
				call.callStatus = CallStatus.valueOf(mCursor.getString(8));
				call.createDate = mCursor.getLong(9);
				call.startDate = mCursor.getLong(10);
				call.endDate = mCursor.getLong(11);
				call.duration = mCursor.getLong(12);
				call.callStats1 = mCursor.getString(13);
				call.callStats2 = mCursor.getString(14);
								
				calls.add(call);	
				//Logger.e( "while end count =  "+count);
			}			
		} 
		catch (Exception e){
			Logger.e( e.toString(), e);
		}
		finally
		{
			Logger.d( "getCallByTn .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "getCallByTn .. end");
		}
		
		// Get the appropriate contacts from the database retrieved records
		if (calls!=null){
			List<ContactDetail> contacts = ApplicationClass.getContactList();
			if (contacts!=null && !contacts.isEmpty()){
				HashMap<String, ContactDetail> contactTable  = new HashMap<String, ContactDetail>();
				for (ContactDetail contact: contacts){
					if (contact.getPhoneNumbers()!=null){
						for (String phonenum: contact.getPhoneNumbers()){
							contactTable.put(phonenum, contact);
						}
					}
				}
				
				if (!contactTable.isEmpty()){
					for (CallItem call: calls){
						if (contactTable.containsKey(call.phoneNum)){
							ContactDetail contactDetail = contactTable.get(call.phoneNum);
							if (contactDetail!=null){
								call.contactId = contactDetail.getContactId();
								call.contactPhotoURI = contactDetail.getPhotoURL();
								call.contactName = contactDetail.getFirstName();
							}
						}
					}
				}
			}
		}
		
		return calls;
		
	}*/
	
	public boolean deleteAll(){
		SQLiteDatabase db = null;
		Logger.d( "deleteAll .. start");
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getWritableDatabase();
			
			if (db != null && db.isOpen()){
				db.beginTransaction();

				long	cnt = db
					        .delete(DBHelper.TABLE_CALL_LOGS, null,
					                null);
				
				db.setTransactionSuccessful();
				
				db.endTransaction();
			}
			
		}catch (Exception e){
			Logger.e( e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d( "deleteAll .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "deleteAll .. end");
		}
		
		return true;
	}
	
	public boolean deleteCallsById(int id){
		SQLiteDatabase db = null;
		Logger.d( "deleteCallsById .. start");
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getWritableDatabase();
			
			if (db != null && db.isOpen()){
				db.beginTransaction();
				String where =
						DBHelper.ColsCall.ID + " = " +  id;

				long	cnt = db
					        .delete(DBHelper.TABLE_CALL_LOGS, where,
					                null);
				
				db.setTransactionSuccessful();
				
				db.endTransaction();
			}
			
		}catch (Exception e){
			Logger.e( e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d( "deleteCallsById .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "deleteCallsById .. end");
		}
		
		return true;
	}
	
	public boolean deleteCallsByCallId(int callId){
		SQLiteDatabase db = null;
		Logger.d( "deleteCallsByCallId .. start");
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getWritableDatabase();
			
			if (db != null && db.isOpen()){
				db.beginTransaction();
				String where =
						DBHelper.ColsCall.ID + " = " +  callId;

				long	cnt = db
					        .delete(DBHelper.TABLE_CALL_LOGS, where,
					                null);
				
				db.setTransactionSuccessful();
				
				db.endTransaction();
			}
			
		}catch (Exception e){
			Logger.e( e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d( "deleteCallsByCallId .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "deleteCallsByCallId .. end");
		}
		
		return true;
	}
	
	public boolean deleteCallsByTn(String tn){
		SQLiteDatabase db = null;
		Logger.d( "deleteCallsByTn .. start");
		
		final DBHelper mDbHelper = DBHelper.getInstance();
		try
		{
			
			mDbHelper.acquireAccess();
			
			db = mDbHelper.getWritableDatabase();
			
			if (db != null && db.isOpen()){
				db.beginTransaction();
				String where =
						DBHelper.ColsCall.TN + " = '" +  tn.trim() + "'";

				long	cnt = db
					        .delete(DBHelper.TABLE_CALL_LOGS, where,
					                null);
				
				db.setTransactionSuccessful();
				
				db.endTransaction();
			}
			
		}catch (Exception e){
			Logger.e( e.toString(), e);
			return false;
		}
		finally
		{
			Logger.d( "deleteCallsByTn .. ending");
			if (db != null) {
				db.close();
			}
			mDbHelper.releaseAccess();
			Logger.d( "deleteCallsByTn .. end");
		}
		
		return true;
	}

}
