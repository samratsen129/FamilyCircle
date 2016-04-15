package com.familycircle.lib.utils.db;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.familycircle.sdk.BaseApp;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper{
	private static DBHelper _instance;
	private static final String DB_NAME = "teamdb";
	private static final int DB_VERSION = 3;
	
	private static Lock lock = null;
	
	public synchronized static DBHelper getInstance(){
		if (_instance==null){
			_instance = new DBHelper();
		}
		return _instance;
	}
	
	//public final static String TABLE_SMS_SENDERS = "sms_senders";
	public final static String TABLE_SMS_LOGS = "message_logs";
	public final static String TABLE_CALL_LOGS = "call_logs";
	public final static String TABLE_VMAIL_LOGS = "vmail_logs";
	
	public static class ColsSMS
	{
		public static final String MID = "mid";
		public static final String TN = "tn";
		public static final String TYPE = "type";
		public static final String NAME = "name";
		public static final String SMS_BODY = "sms_body";
		public static final String SMS_DIR = "sms_dir";
		public static final String STATUS = "status";
		public static final String TIME = "date_time";
		public static final String IS_READ = "is_read";
		public static final String IS_FAILED_MSG = "is_failed_msg";
        public static final String INTERNAL_ID= "server_id";
		public static final String UNREAD_CNT= "unread_cnt";
	}
	
	public static class ColsCall
	{
		public static final String ID = "id";
		public static final String CALL_ID = "call_id"; // call id from the mcdv library
		public static final String IS_CONF_CALL = "is_conf_call";
		public static final String CALL_TYPE = "call_type"; // indicates if its an outgoing or incoming call
		public static final String TN = "tn"; // tn of the user at the other end
		public static final String TN_NAME = "tn_name"; // user name of the user at the other end
		public static final String USER_TN = "user_tn"; // user tn of the smartcalling logged in user
		public static final String USER_ACTION = "user_action"; // indicates the last user action on the call, Accepted, Rejected, Hold, Missed SConstants.CallUserAction
		public static final String CALL_STATUS = "call_status"; // indicates the current/last status, receiving, connected, completed etc from SCConstants.CallStatus enum
		public static final String CRE_DT = "create_date"; // when this request was created
		public static final String CALL_START_DT = "call_start_date"; // call start date
		public static final String CALL_END_DT = "call_end_date"; // call end date
		public static final String CALL_DURATION = "call_duration"; // duration of the call, end - start
		public static final String CALL_STATUS_INFO1= "call_info_extra1"; // call stats 1
		public static final String CALL_STATUS_INFO2= "call_info_extra2"; // call stats 2
	}
	
	
	public static class ColsVMAIL
	{
		public static final String VMID = "vmid";
		public static final String TN = "tn";
		public static final String CALLER_NAME = "caller_nm";
		public static final String CALLER_TN = "caller_tn";
		public static final String HAS_TRANSCRIPT = "has_transcript";
		public static final String TRANSCRIPT_TEXT = "transcript";
		public static final String FOLDER = "vm_folder";
		public static final String STATUS = "flags"; // r or u
		public static final String CREATE_DT = "date_time";
		public static final String DURATION = "duration";
		public static final String FILE_NAME = "vmail_file";
	}
	
	private final String SMS_LOGS_DDL = "CREATE TABLE IF NOT EXISTS "
	        + TABLE_SMS_LOGS + " ( " +
	        ColsSMS.MID
	        + " INTEGER PRIMARY KEY, " +
	        ColsSMS.TN
	        + " VARCHAR , " +
	        ColsSMS.NAME + " VARCHAR, " +
	        ColsSMS.SMS_BODY + " VARCHAR, " +
	        ColsSMS.SMS_DIR + " VARCHAR, " +
	        ColsSMS.STATUS + " VARCHAR, " +
	        ColsSMS.TIME + " INTEGER, " +
	        ColsSMS.IS_READ + " BOOLEAN, " +
            ColsSMS.INTERNAL_ID + " VARCHAR, "+
	        ColsSMS.IS_FAILED_MSG + " BOOLEAN); ";
	
	private final String CALL_LOGS_DDL = "CREATE TABLE IF NOT EXISTS "
	        + TABLE_CALL_LOGS + " ( " +
	        ColsCall.ID
	        + " INTEGER PRIMARY KEY, " +
	        ColsCall.CALL_ID + " INTEGER, " +
	        ColsCall.IS_CONF_CALL + " BOOLEAN, " +
	        ColsCall.CALL_TYPE + " VARCHAR, " +
	        ColsCall.TN + " VARCHAR, " +
	        ColsCall.TN_NAME + " VARCHAR, " +
	        ColsCall.USER_TN + " VARCHAR, " +
	        ColsCall.USER_ACTION + " VARCHAR, " +
	        ColsCall.CALL_STATUS + " VARCHAR, " +
	        ColsCall.CRE_DT + " INTEGER, " +
	        ColsCall.CALL_START_DT + " INTEGER, " +
	        ColsCall.CALL_END_DT + " INTEGER, " +
	        ColsCall.CALL_DURATION + " INTEGER, " +
	        ColsCall.CALL_STATUS_INFO1 + " TEXT, " +
	        ColsCall.CALL_STATUS_INFO2 + " TEXT); ";
	
	private final String VMAIL_LOGS_DDL = "CREATE TABLE IF NOT EXISTS "
	        + TABLE_VMAIL_LOGS + " ( " +
	        ColsVMAIL.VMID
	        + " INTEGER PRIMARY KEY, " +
	        ColsVMAIL.TN + " VARCHAR , " +
	        ColsVMAIL.CALLER_NAME + " VARCHAR, " +
	        ColsVMAIL.CALLER_TN + " VARCHAR, " +
	        ColsVMAIL.FOLDER + " VARCHAR, " +
	        ColsVMAIL.STATUS + " VARCHAR, " +
	        ColsVMAIL.DURATION + " INTEGER, " +
	        ColsVMAIL.CREATE_DT + " INTEGER, " +
	        ColsVMAIL.FILE_NAME + " VARCHAR, " +
	        ColsVMAIL.HAS_TRANSCRIPT + " BOOLEAN, " +
	        ColsVMAIL.TRANSCRIPT_TEXT + " VARCHAR); ";
	
	private final String SMS_LOGS_IDX1_DDL = "CREATE INDEX dir_idx ON "+TABLE_SMS_LOGS + "(" + ColsSMS.SMS_DIR + ")";
	//private final String SMS_LOGS_IDX2_DDL = "CREATE INDEX tn_idx ON "+TABLE_SMS_LOGS + "(" + ColsSMS.TN + ")";
	

	private DBHelper()
	{

		super(BaseApp.getContext(), DB_NAME, null, DB_VERSION);
		Log.d("DBHelper", "DBHelper constructor");
		if (lock==null){
			lock = new ReentrantLock();
		}
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			Log.d("DBHelper", "DBHelper onCreate");
			db.execSQL(SMS_LOGS_DDL);
			db.execSQL(CALL_LOGS_DDL);
			db.execSQL(VMAIL_LOGS_DDL);
			db.execSQL(SMS_LOGS_IDX1_DDL);
			Log.d("DBHelper", "DBHelper created");
		} catch (Exception e){
			Log.e("DBHelper","DBHelper " + e.toString(), e);
		}
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		if (oldVersion<=2){
			try {;
				db.execSQL(VMAIL_LOGS_DDL);
				Log.d("DBHelper","DBHelper vmail log table created" );
			} catch (Exception e){
                Log.e("DBHelper","DBHelper "+ e.toString(), e);
			}
			
		}
		
	}
	
	public void acquireAccess(){
		//MPLogger.d("DBHelper", "acquiring lock" + Thread.currentThread().getId());
		lock.lock();
		//MPLogger.d("DBHelper", "acquired lock" + Thread.currentThread().getId());
	}

	public void releaseAccess(){
		//MPLogger.d("DBHelper", "releasing lock " + Thread.currentThread().getId());
		lock.unlock();
		//MPLogger.d("DBHelper", "released lock" + Thread.currentThread().getId());
	}
	
}
