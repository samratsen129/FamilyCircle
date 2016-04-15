package com.familycircle.sdk;

/**
 * Created by samratsen on 5/28/15.
 */
public class Constants {

    public enum EventReturnState
    {
        CONFIG_SETTINGS_ERROR(10),
        CRITICAL_ERROR(12),
        HOLD_RETIEVE_FAIL(7),
        MUTE_UNMUTE_FAIL(20),
        CONFIG_START(9),
        CONFIG_SUCCESS(13),
        NEW_OUT_CALL(1),
        NEW_INCOMING_CALL(2),
        NEW_ACTIVE_CONN(3),
        CALL_DISCONNECT(4),
        HOLD_SUCCESS(5),
        RETRIEVE_SUCCESS(6),
        CONFERENCE_ENABLED(8),
        SHUTDOWN_SUCCESS(11),
        HOLD_LOCAL_USER(14), // what does this mean, there is already a success status
        RETRIEVE_LOCAL_USER(15), // what does this mean
        CALL_FORWARD_SUCCESS(16),
        CONFERENCE_INVALID_START(17),//conference start after it has been disabled
        MUTE_ON_SUCCESS(18),
        MUTE_OFF_SUCCESS(19),
        FAULT_NOTIFICATION(21),
        CALL_END_STATS(22),
        CALL_PERIODIC_STATS(23),
        CALL_DISCONNECT_ACK(24),
        DTMF_DIGIT_NOT_PLAYED(25),
        CONFIG_PROXY_DISCOVERY_SUCCESS(26),
        SIP_UNREGISTER_SUCCESS(27),
        SIP_ENABLE_DIALPAD(28), // is this for incoming or outgoing and is this after or before 3
        SIP_TRANSPORT_LOSS(29), // is this network related
        SIP_CALL_STATS_TO_SERVER(30),
        SIP_RESTART_REQUIRED(31),
        NEW_IN_MESSAGE(32),
        NEW_OUT_MESSAGE(33),
        OTHER_MESSAGE(34),
        CONTROL_DISCONNECT(35),
        MESSAGE_SEND_ERROR(36),
        MESSAGE_SEND_SUCCESS(37),
        MESSAGE_SEND_CONNECTION_ERROR(38),
        MESSAGE_CRITICAL_ERROR(39),
        CONTROL_RETRIES_FAILED(40),
        LOGIN_REQUIRED(41),
        CREATE_USER_ERROR(42),
        CREATE_USER_SUCCESS(43),
        GET_USERS_ERROR(44),
        GET_USERS_SUCCESS(45),
        LOG_IN_EOF(46),
        INCOMING_CALL_SIGNAL(47),
        CALL_CANCEL_SIGNAL(48)
        ;
        public int value;
        EventReturnState(int v){
            value = v;
        }
    }

    public enum MESSAGE_STATUS_TYPE{
        READ("R"),
        UNREAD("U"),
        LOCKED("L");
        public String value;
        MESSAGE_STATUS_TYPE(String v){
            value = v;
        }
    }

    public enum NOTIFICATION_TYPE{
        CALL,
        SMS,
        OTHER;
    }

    public enum NetworkType
    {
        WIFI, MOBILE, NULL
    }

    public static final String USER_NAME    = "me.kg.androidrtc.SHARED_PREFS.USER_NAME";
    public static final String CALL_USER    = "me.kg.androidrtc.SHARED_PREFS.CALL_USER";
    public static final String STDBY_SUFFIX = "-stdby";
    public static final String STDBY_CHANNEL = "familycircle123";

    public static final String PUB_KEY = "pub-c-090357d1-f0ff-4d6f-8c22-4e792d4a19fc"; // Your Pub Key
    public static final String SUB_KEY = "sub-c-28471b50-f917-11e5-8916-0619f8945a4f"; // Your Sub Key

}
