package com.familycircle.utils.network;

import com.familycircle.utils.network.model.BaseModel;
import com.familycircle.utils.network.Types.NetworkResponseType;
import com.familycircle.utils.network.Types.RequestType;

public class Response
{
	// HTTP server response code.
	public static final int SUCCESS = 200;
	public static final int SUCCESS_NO_CONTENT = 204;
	public static final int SESSION_EXPIRED = 302;
	public static final int SESSION_UNAUTHORIZED = 401;

	// HTTP Maintanance Error Message

	public static final int SERVER_MAINTENANCE = 500;
	public static final int TIMEOUT = 599;
	public static final int SERVER_MAINTENANCE_1 = 800;
	public static final int SERVER_MAINTENANCE_2 = 900;
	public static final int OTT_SESSION_EXPIRED = 1025;

	// Custom Error Message
	public static final int UN_SUPPORTED_EXCEPTION = 1;
	public static final int ILLEGAL_STATE_EXCEPTION = 2;
	public static final int UN_KNOWN_HOST_EXCEPTION = 3;
	public static final int CLIENT_PROTOCOL_EXCEPTION = 4;
	public static final int IO_EXCEPTION = 5;
	public static final int NO_NETWORK = 6;
	public static final int INTERRUPTED_EXCEPTION = 7;
		
	public enum FailedReason
	{
		NO_NETWORK, SESSION_EXPIRED, SERVER_MAINTANCE, CANCELLED, UNAUTHORIZED
	}
	
	private RequestType requestType;
	private NetworkResponseType responseType;
	private String cacheDataPath;

	private boolean status;

	private BaseModel model;
	private FailedReason failedReason;

	private String errorMessage;
	private int reponseCode = 500;
	
	private String requestObjectId; 
	private String requestTypeObjectId;
	
	//NOTE: this is populated only if there is a failure
	// so that the request can be executed again on user permission
	private Request request;

	public int getReponseCode()
	{
		return reponseCode;
	}

	public void setReponseCode(int reponseCode)
	{
		this.reponseCode = reponseCode;
	}

	public RequestType getRequestType()
	{
		return requestType;
	}

	public void setRequestType(RequestType requestType)
	{
		this.requestType = requestType;
	}

	public NetworkResponseType getResponseType()
	{
		return responseType;
	}

	public void setResponseType(NetworkResponseType responseType)
	{
		this.responseType = responseType;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public boolean getStatus()
	{
		return status;
	}

	public void setStatus(boolean status)
	{
		this.status = status;
	}

	public BaseModel getModel()
	{
		return model;
	}

	public void setModel(BaseModel model)
	{
		this.model = model;
	}

	public String getCacheDataPath()
	{
		return cacheDataPath;
	}

	public void setCacheDataPath(String cacheDataPath)
	{
		this.cacheDataPath = cacheDataPath;
	}

	public FailedReason getFailedReason()
	{
		return failedReason;
	}

	public void setFailedReason(FailedReason failedReason)
	{
		this.failedReason = failedReason;
	}

	public String getRequestObjectId() {
		return requestObjectId;
	}

	public void setRequestObjectId(String requestObjectId) {
		this.requestObjectId = requestObjectId;
	}

	public String getRequestTypeObjectId() {
		return requestTypeObjectId;
	}

	public void setRequestTypeObjectId(String requestTypeObjectId) {
		this.requestTypeObjectId = requestTypeObjectId;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

}

