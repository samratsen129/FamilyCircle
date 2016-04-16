package com.familycircle.utils.network;

public class Types {
	public enum NetworkType
	{
		WIFI, MOBILE, NULL
	}

	public enum HttpRequestType
	{
		POST, GET, PUT, DELETE
	}

	public enum DatabaseRequestType
	{
		QUERY, UPDATE
	}

	public enum XSIRequestType
	{
		POST, GET, PUT, DELETE
	}

    public enum DeviceRequestType
    {
        POST, GET, PUT, DELETE
    }

	public enum NetworkResponseType
	{
		SUCCESS, FAILURE, SESSION_EXPIRED, SERVER_MAINTAINCE
	}
	
	public enum RequestType
	{
		GET_LOGIN("xsi_login"),
		LOGOUT("logout"),

		QUERY_DB_USER("query_database_user"),
		QUERY_DB_INVITE("query_db_invite"),
		QUERY_DB_USER_DEVICES("query_database_user_devices"),
		QUERY_INVITE_USER("query_invite_user"),
		QUERY_INSERT_DOOR_CODE("query_add_door_code"),
		QUERY_DOOR_CODE("query_door_code"),

		QUERY_DB_DEVICE("query_database_device"),
        QUERY_DB_DEVICE_UUID("query_database_device2"),
		UPDATE_DB_USER_DEVICES("update_database_user_devices"),
		REMOVE_DB_USER_ACCOUNT("remove_user_account"),
		UPDATE_DB_USER("update_database_user"),
		UPDATE_DB_DEVICE("update_database_device"),
		UPDATE_DB_BEACON_DATA("update_beacon_data"),
		QUERY_DB_LOGIN("database_validate_login"),
		QUERY_DB_DEVICE_INIT("database_validate_device_init"),
		XSI_ACTION_SETTINGS("xsi_action_settings"),
        DELETE_XSI_VOICE_MESSAGE("remove_xsi_voice_message"),
		XSI_CALL_LOGS("xsi_call_logs"),
        DEVICE_PAIR_REQUEST("device_pair_request"),
		COMMAND_XSI_LOCATION_SWITCH("command_location_switch"),
		COMMAND_DEVICE_USER_SWITCH("command_user_switch"),
        COMMAND_DEVICE_ACCOUNT_DROP("command_account_drop"),
		COMMAND_CALL_TRANSFER("command_call_transfer"),
		COMMAND_XMPP_START("command_xmpp_start"),
		UPDATE_DB_BW_USER("update_database_bw_user"),
		QUERY_DB_BW_USER("query_database_bw_user"),
		M2X_CREATE_DEVICE("create_device"),
		M2X_CREATE_STREAM("create_stream"),
		M2X_GET_STREAM("create_stream"),
		M2X_GET_ALL_STREAM("create_stream_stats"),
		M2X_GET_TRIGGERS("get_triggers"),
		M2X_CREATE_TRIGGERS("create_triggers"),
		M2X_CREATE_STREAM_MGR("stream_manager"),
		M2X_CREATE_TRIGGER_MGR("trigger_manager"),
		UNKNOWN("logout");

		String value;

		RequestType(String value)
		{
			this.value = value;
		}

		public String getParam()
		{
			return value;
		}
	}

    public enum ProximityStatus {
        OPEN,
        PAIR_WAIT_ACK,
        PAIRED,
        TIMEOUT,
        CLOSED,
        REJECTED
    }

    public enum ProximityCommand {
        PAIR,
        UNPAIR,
        PAIR_ACK,
        ACCOUNT_SWITCH,
		ACCOUNT_SWITCH_ACK,
        CALL_TRANSFER,
        CALL_TRANSFER_ACK,
        ACCOUNT_RECOVER,
        SET_CALL_TRANSFER,
        PING,
        PING_ACK,
        ACCOUNT_DISABLE,
        NONE
    }

}
