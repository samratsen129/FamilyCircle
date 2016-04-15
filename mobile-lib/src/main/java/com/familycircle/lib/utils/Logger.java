package com.familycircle.lib.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;

import android.os.Environment;
import android.util.Log;

import com.familycircle.sdk.BaseApp;

public class Logger {
	
	/**
	 * Logger tag.
	 */
	
	private final static String mOutFile = BaseApp.getLoggerTag()+"App.log";
	
	/**
	 * Defines the dashes to differentiate lines. 
	 */
	private static final String DASHES = "-----";
	
	/**
	 * Indicates the logging level.
	 */
	private static int mLevel = BaseApp.getDebugMode();
	
	/**
	 * Sets logging level.
	 * @param level the logging level to use.
	 */
	public static void setLoggerLevel(int level) {
		mLevel = level;
	}
	
	/**
	 * Logs debug message.
	 */
	public static void d(String inMsg) {
		if (shouldLog(Log.DEBUG)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.d(BaseApp.getLoggerTag(), msg);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("DEBUG : " + msg);
			}
		}
	}
	
	/**
	 * Logs debug message.
	 * @param tr the exception to log.
	 */
	public static void d(String inMsg, Throwable tr) {
		if (shouldLog(Log.DEBUG)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.d(BaseApp.getLoggerTag(), msg, tr);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("DEBUG : " + msg + " : " + Log.getStackTraceString(tr));
			}
		}
	}
	
	/**
	 * Logs information message.
	 */
	public static void i(String inMsg) {
		if (shouldLog(Log.INFO)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.i(BaseApp.getLoggerTag(), msg);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("INFO : " + msg);
			}
		}
	}
	
	/**
	 * Logs information message.
	 * @param tr the exception to log. 
	 */
	public static void i(String inMsg, Throwable tr) {
		if (shouldLog(Log.INFO)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.i(BaseApp.getLoggerTag(), msg, tr);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("INFO : " + msg + " : " + Log.getStackTraceString(tr));
			}
		}
	}
	
	/**
	 * Logs verbose message.
	 */
	public static void v(String inMsg) {
		if (shouldLog(Log.VERBOSE)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.v(BaseApp.getLoggerTag(), msg);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("VERBOSE : " + msg);
			}
		}
	}
	
	/**
	 * Logs verbose message.
	 * @param tr the exception to log. 
	 */
	public static void v(String inMsg, Throwable tr) {
		if (shouldLog(Log.VERBOSE)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.v(BaseApp.getLoggerTag(), msg, tr);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("VERBOSE : " + msg + " : " + Log.getStackTraceString(tr));
			}
		}
	}
	
	/**
	 * Logs error message.
	 */
	public static void e(String inMsg) {
		if (shouldLog(Log.ERROR)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.e(BaseApp.getLoggerTag(), msg);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("ERROR : " + msg);
			}
		}
	}
	
	/**
	 * Logs error message.
	 * @param tr the exception to log. 
	 */
	public static void e(String inMsg, Throwable tr) {
		if (shouldLog(Log.ERROR)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.e(BaseApp.getLoggerTag(), msg, tr);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("ERROR : " + msg + " : " + Log.getStackTraceString(tr));
			}
		}
	}
	
	/**
	 * Logs warning message.
	 */
	public static void w(String inMsg) {
		if (shouldLog(Log.WARN)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.w(BaseApp.getLoggerTag(), msg);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("WARN : " + msg);
			}
		}
	}
	
	/**
	 * Logs warning message.
	 * @param tr the exception to log.
	 */
	public static void w(String inMsg, Throwable tr) {
		if (shouldLog(Log.WARN)) {
			String msg = (inMsg==null)?"null":inMsg;
			Log.w(BaseApp.getLoggerTag(), msg, tr);
			if (BaseApp.isLOG_CAPTURE()){
				writeLogToSD("WARN : " + msg + " : " + Log.getStackTraceString(tr));
			}
		}
	}
	
	/**
	 * Logs a method's entry.
	 * @param classAndMethod the name of the class and method, commonly 'Class.method'.
	 */
	public static void entry(final String classAndMethod) {
		d(String.format("%s %s %s", DASHES, classAndMethod, " entered."));
	}
	
	/**
	 * Logs a method's exit.
	 * @param classAndMethod the name of the class and method, commonly 'Class.method'.
	 */
	public static void exit(final String classAndMethod) {
		d(String.format("%s %s %s", DASHES, classAndMethod, " exited."));
	}
	
	/**
	 * Helper method to indicate whether or not the message should be logged.
	 * @param logLevel the log level of the message.
	 * @return true if the message should be logged.
	 */
	private static boolean shouldLog(int logLevel) {
		if (logLevel >= mLevel) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void writeLogToSD(String msg){
		try {
			java.io.File extStore = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			String mPath = extStore.getAbsolutePath();
			java.io.File f = new java.io.File(mPath);
			if (f.isDirectory()){
				BufferedWriter bos = new BufferedWriter(new FileWriter(mPath + "/" + mOutFile, true));
	            bos.write("\n" + (new java.util.Date()).toString() + " : " + msg);
	            bos.write("\n");
	            bos.flush();
	            bos.close();
			}
		} catch (Exception e){
			Log.d(BaseApp.getLoggerTag(), "Error while writing", e);
		} catch (Error e){
			Log.d(BaseApp.getLoggerTag(), "Error while writing", e);
		}
	}
}

