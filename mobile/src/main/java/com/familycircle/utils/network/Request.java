package com.familycircle.utils.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.familycircle.TeamApp;
import com.familycircle.lib.utils.Logger;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.familycircle.utils.network.model.BaseModel;
import com.familycircle.utils.network.Types.NetworkResponseType;
import com.familycircle.utils.network.Types.RequestType;
import com.familycircle.utils.network.Types.DatabaseRequestType;
import com.familycircle.utils.network.Types.DeviceRequestType;
import com.familycircle.utils.network.Types.HttpRequestType;
import com.familycircle.utils.network.Types.NetworkResponseType;
import com.familycircle.utils.network.Types.RequestType;
import com.familycircle.utils.network.Types.XSIRequestType;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class Request {
	
	public static int HTTP_PORT = 80;
	public static int HTTPS_PORT = 443;
	public static int CLIENT_TIMEOUT = 20000;
	public static int DEFAULT_TIMEOUT = 20000;
	protected HttpRequestType operationType;
	protected DatabaseRequestType databaseOperationType;
	protected XSIRequestType xsiOperationType;
	protected DeviceRequestType deviceOperationType;
	protected StringBuffer url;
	protected String contentType;
	protected ArrayList<NameValuePair> headerParams;
	protected ResponseListener responseListener;
	public String requestObjectId; 
	private String requestTypeObjectId;
	
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	public Request(HttpRequestType operationType, String contentType, String urlString)
	{
		this.contentType = contentType;
		this.operationType = operationType;
		this.headerParams = new ArrayList<NameValuePair>();
		this.url = new StringBuffer();
		CLIENT_TIMEOUT = DEFAULT_TIMEOUT;
		url.append(urlString);
		
		headerParams.add(new BasicNameValuePair("Content-Type", contentType));
		
		//addHeaderParams("Content-Type", contentType);


	}

	public Request(final XSIRequestType xsiOperationType)
	{
		this.xsiOperationType = xsiOperationType;

	}

    public Request(final DeviceRequestType deviceOperationType)
    {
        this.deviceOperationType = deviceOperationType;

    }

	public Request(final DatabaseRequestType databaseOperationType)
	{
		this.databaseOperationType = databaseOperationType;

	}
	
	public HttpRequestType getOperationType() {
		return operationType;
	}
	public void setOperationType(HttpRequestType operationType) {
		this.operationType = operationType;
	}
	public String getUrl() {
		return url!=null?url.toString():null;
	}
	public void setUrl(StringBuffer url) {
		this.url = url;
	}
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public ArrayList<NameValuePair> getHeaderParams() {
		return headerParams;
	}
	public void setHeaderParams(ArrayList<NameValuePair> headerParams) {
		if (this.headerParams!=null){
			this.headerParams.addAll(headerParams);
		} else {
			this.headerParams = headerParams;
		}
	}
	public ResponseListener getResponseListener() {
		return responseListener;
	}
	public void setResponseListener(ResponseListener responseListener) {
		this.responseListener = responseListener;
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

	private void execRequest(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    ExecutorService executor = Executors.newSingleThreadExecutor();

                    Future<Response> future = executor.submit(new Callable<Response>() {
                        @Override
                        public Response call() throws Exception {
                            return parse(200, null, null);
                        }
                    });

                    try {
                        final Response response = future.get(CLIENT_TIMEOUT, TimeUnit.MILLISECONDS);

                        response.setRequestType(getRequestType());
                        //response.setRequest(Request.this);
                        response.setStatus(true);
                        if (response.getResponseType() == NetworkResponseType.FAILURE) {
                            response.setStatus(false);
                        }

                        if (handler!=null) {
                            // Send response object back on UI Thread
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    if (responseListener != null) {
                                        if (response.getStatus()) {
                                            responseListener.onSuccess(response);
                                        } else {
                                            responseListener.onFailure(response);
                                        }
                                    }
                                }
                            });
                        }

                    } catch (TimeoutException e) {
                        future.cancel(true);
                        Logger.e("Request Terminated due to timeout");

                        if (handler!=null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (responseListener != null) {
                                        Response response = new Response();
                                        //response.setRequest(Request.this);
                                        response.setRequestType(getRequestType());
                                        response.setStatus(false);
                                        response.setReponseCode(Response.TIMEOUT);
                                        response.setResponseType(NetworkResponseType.FAILURE);
                                        response.setErrorMessage("Request timeout");
                                        responseListener.onFailure(response);
                                    }
                                }
                            });

                        }

                    }

                    executor.shutdownNow();

                } catch (final Exception e){

                    if (handler!=null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (responseListener != null) {
                                    Response response = new Response();
                                    //response.setRequest(Request.this);
                                    response.setRequestType(getRequestType());
                                    response.setStatus(false);
                                    response.setReponseCode(500);
                                    response.setResponseType(NetworkResponseType.FAILURE);
                                    response.setErrorMessage("exception " + e.toString());
                                    responseListener.onFailure(response);
                                }
                            }
                        });

                    }

                }
            }
        });

        thread.start();
	}

	public void exec(){

		if (databaseOperationType!=null
                || xsiOperationType!=null
					|| deviceOperationType!=null){
            execRequest();
			return;
		}

		AsyncHttpClient client = new AsyncHttpClient(false, HTTP_PORT, HTTPS_PORT);
		client.setTimeout(CLIENT_TIMEOUT);
		
		try {
		
			if (headerParams!=null && !headerParams.isEmpty()){
				if (operationType != HttpRequestType.DELETE){
					for (NameValuePair nameValue:headerParams){
						client.addHeader(nameValue.getName(), nameValue.getValue());
						Logger.d("Request header " + nameValue.getName() + ":" + nameValue.getValue());
					}
				}
			}
			
			Logger.d(operationType +": Request to " + url.toString());
			
			if (operationType == HttpRequestType.GET){
				if (getBodyContent()!=null && !getBodyContent().isEmpty()){
					RequestParams params = new RequestParams(getBodyContent());
					client.get(url.toString(), params, asyncHttpResponseHandler);
					
				} else {
					
					client.get(url.toString(), asyncHttpResponseHandler);
				}
				
			}
			
			if (operationType == HttpRequestType.POST){
				
				if (getBodyContent()!=null && !getBodyContent().isEmpty()){
					RequestParams params = new RequestParams(getBodyContent());
					client.post(url.toString(), params, asyncHttpResponseHandler);
					
				} else if (getStringBodyContent()!=null){
					StringEntity entity = new StringEntity(getStringBodyContent());
					Logger.d("Request PUT string body " + getStringBodyContent());
					client.post(TeamApp.getContext(), url.toString(), entity, "application/json", asyncHttpResponseHandler);
					
				} else {
					client.post(url.toString(), asyncHttpResponseHandler);
				}
				
			}
			
			if (operationType == HttpRequestType.PUT){
				if (getBodyContent()!=null && !getBodyContent().isEmpty()){
					RequestParams params = new RequestParams(getBodyContent());
					client.put(url.toString(), params, asyncHttpResponseHandler);
					
				} else if (getStringBodyContent()!=null){
					StringEntity entity = new StringEntity(getStringBodyContent());
					Logger.d("Request PUT string body " + getStringBodyContent());
					//client.addHeader("Content-Type", "application/json");
					client.put(TeamApp.getContext(), url.toString(), entity, "application/json", asyncHttpResponseHandler);
					
				} else {
					client.put(url.toString(), asyncHttpResponseHandler);
				}
				
			}
			
			if (operationType == HttpRequestType.DELETE){
				
				Header [] headers = null;
				if (headerParams!=null && !headerParams.isEmpty()){
					int size = headerParams.size();
					headers = new Header[size];
					int i=0;
					for (NameValuePair nameValue:headerParams){
						headers[i++] = new BasicHeader(nameValue.getName(), nameValue.getValue());
					}
				}
				
				/*if (getBodyContent()!=null && !getBodyContent().isEmpty() && headers!=null && headers.length>0){
					RequestParams params = new RequestParams(getBodyContent());
					client.delete(GlobalApplicationClass.getContext(), url.toString(), headers, params, asyncHttpResponseHandler);
					
				} else*/ 
				if (headers!=null && headers.length>0){
					client.delete(TeamApp.getContext(), url.toString(), headers, asyncHttpResponseHandler);
					
				}  else {
					client.delete(url.toString(), asyncHttpResponseHandler);
				}
			}
			
		} catch (Exception e) {
			Log.e("Request", "Request creation error " + e.toString(), e);
			Response response = new Response();
			response.setRequest(Request.this);
			response.setRequestType(getRequestType());
			response.setStatus(false);
			response.setReponseCode(500);
			response.setErrorMessage(e.toString());
			if (responseListener!=null) responseListener.onFailure(response);
			
		}
	}
	
	/**
	 * This method parses the response from Server
	 * 
	 * @param responseCode
	 * @param response
	 * @param headers
	 * @return
	 */
	protected abstract Response parse(int responseCode, String response, Header[] headers);
				//List<NameValuePair> headers);

	/**
	 * This method returns the body content of any request.
	 * 
	 * @return
	 */
	public abstract List <NameValuePair> getBodyContent();

	public abstract String getStringBodyContent();
	
	public byte[] getByteContent()
	{
		return null;
	}

	public abstract boolean clearCookies();

	public abstract boolean urlRedirectionRequired();

	public abstract boolean useSessionCookies();

	public abstract RequestType getRequestType();

	public abstract boolean cacheResponse(final Response responseObject);

	public void addExtraToUrl(String name)
	{
		url.append(name);
	}

	public void addHeaderParams(String name, String value)
	{
		headerParams.add(new BasicNameValuePair(name, value));
	}
	
	protected String getValue(JSONObject jo, String nodeKey){
		if (jo==null) return null;
		if (!jo.isNull(nodeKey) && jo.has(nodeKey)){
			try {
				return jo.get(nodeKey).toString().trim();
			} catch (JSONException e) {
				return null;
			}
		}
		return null;
	}
	
	protected boolean getBool(JSONObject jo, String nodeKey){
		if (jo==null) return false;
		if (!jo.isNull(nodeKey) && jo.has(nodeKey)){
			try {
				return jo.getBoolean(nodeKey);
			} catch (JSONException e) {
				return false;
			}
		}
		return false;
	}
	
	protected JSONArray getArray(JSONObject jo, String nodeKey){
		if (jo==null) return null;
		if (!jo.isNull(nodeKey) && jo.has(nodeKey)){
			try {
				return jo.getJSONArray(nodeKey);
			} catch (JSONException e) {
				return null;
			}
		}
		return null;
	}
	
	protected JSONObject getWrapperObject(JSONObject jo, String nodeKey){
		if (jo==null) return null;
		if (!jo.isNull(nodeKey) && jo.has(nodeKey)){
			try {
				return jo.getJSONObject(nodeKey);
			} catch (JSONException e) {
				return null;
			}
		}
		return null;
	}
	
	public Date getDateTimeAsDate(String dateTime) {
		try {
			//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date dt = sdf.parse(dateTime);
			return dt;
		} catch (Exception e){
			return null;
		}
	}
	
 	private final JsonHttpResponseHandler asyncHttpResponseHandler = new JsonHttpResponseHandler() {

        @Override
        public void onStart() {
        }
        
        @Override
        public void onSuccess(final int statusCode, final Header[] headers, final JSONArray jsonArray) {
        	Log.d("Request", "onSuccess jsonarray: " + jsonArray.toString());
        	
        	try {
            	// assign to variable to avoid memory leak
            	// Handle possibly expensive stuff in a separate thread as onSuccess is called on UI Thread
            	Thread t = new Thread( new Runnable() {
            	    @Override
            	    public void run() {
            	    	try {
            	    		final Response response = parse(statusCode, jsonArray.toString(),
            	    										headers);
            	    		response.setRequestType(getRequestType());
            	    		response.setRequest(Request.this);
            	    		response.setStatus(true);
            				if (response.getResponseType() == NetworkResponseType.FAILURE) {
            					response.setStatus(false);
            				}
            				response.setReponseCode(statusCode);
            				// Send response object back on UI Thread
	            	    	handler.post(new Runnable()
	            			{
	
	            				@Override
	            				public void run()
	            				{
	            					if (responseListener!=null) {
	            						if (response.getStatus()){
	            							responseListener.onSuccess(response);
	            						} else {
	            							responseListener.onFailure(response);
	            						}
	            					}
	            				}
	            			});
	            	    	
            	    	} catch (Exception e){
            	    		Response response = new Response();
            				response.setRequest(Request.this);
            				response.setRequestType(getRequestType());
            				response.setStatus(false);
            				response.setReponseCode(500);
            				response.setResponseType(NetworkResponseType.FAILURE);
            				response.setErrorMessage("JSON parse exception");
            	    		if (responseListener!=null) responseListener.onFailure(response);
            	    		
            	    	}
            	    	
            	    }
            	});
            	t.start();
            	
            } catch (Exception e) {
                Log.e("Request", "JSON parse exception", e);
            }
        }


        @Override
        public void onSuccess(final int statusCode, final Header[] headers, final JSONObject jsonObject) {
            // called when response HTTP status is "200 OK"
            Log.d("Request", "on success json object " + jsonObject.toString());
            try {
            	// assign to variable to avoid memory leak
            	// Handle possibly expensive stuff in a separate thread as onSuccess is called on UI Thread
            	Thread t = new Thread( new Runnable() {
            	    @Override
            	    public void run() {
            	    	try {
            	    		final Response response = parse(statusCode, jsonObject.toString(),
            	    										headers);
            	    		response.setRequestType(getRequestType());
            	    		response.setRequest(Request.this);
            	    		response.setStatus(true);
            				if (response.getResponseType() == NetworkResponseType.FAILURE) {
            					response.setStatus(false);
            				}
            				response.setReponseCode(statusCode);
            				// Send response object back on UI Thread
	            	    	handler.post(new Runnable()
	            			{
	
	            				@Override
	            				public void run()
	            				{
	            					if (responseListener!=null) {
	            						if (response.getStatus()){
	            							responseListener.onSuccess(response);
	            						} else {
	            							responseListener.onFailure(response);
	            						}
	            					}
	            				}
	            			});
	            	    	
            	    	} catch (Exception e){
            	    		Response response = new Response();
            				response.setRequest(Request.this);
            				response.setRequestType(getRequestType());
            				response.setStatus(false);
            				response.setReponseCode(500);
            				response.setResponseType(NetworkResponseType.FAILURE);
            				response.setErrorMessage("JSON parse exception");
            	    		if (responseListener!=null) responseListener.onFailure(response);
            	    		
            	    	}
            	    	
            	    }
            	});
            	t.start();
            	
            } catch (Exception e) {
                Log.e("Request", "JSON parse exception", e);
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String msg, Throwable throwable) {
        	
        	Response response = new Response();
			response.setRequest(Request.this);
			response.setRequestType(getRequestType());
			response.setStatus(false);
			response.setResponseType(NetworkResponseType.FAILURE);
			response.setReponseCode(statusCode);
        	
        	if (msg!=null && msg.length()>0){
                Log.e("Request", "Http error " + statusCode + ": " + msg);
                response.setErrorMessage(msg);
            } else if (throwable!=null){
            	
				response.setErrorMessage(throwable.getMessage());
	    		if (responseListener!=null) responseListener.onFailure(response);
                
            } else {
            	response.setErrorMessage("Unhandled exception occured");
            }
        	
        	if (responseListener!=null) responseListener.onFailure(response);
            
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

        	Response response = new Response();
			response.setRequest(Request.this);
			response.setRequestType(getRequestType());
			response.setStatus(false);
			response.setResponseType(NetworkResponseType.FAILURE);
			response.setReponseCode(statusCode);
        	
        	if (errorResponse!=null && errorResponse.toString().length()>0){
                Log.e("Request", "Http error " + statusCode + ": " + errorResponse.toString());
                response.setErrorMessage(errorResponse.toString());
                
            } else if (throwable!=null){
            	
				response.setErrorMessage(throwable.getMessage());
	    		if (responseListener!=null) responseListener.onFailure(response);
                
            } else {
            	response.setErrorMessage("Unhandled exception occured");
            }
        	
        	if (responseListener!=null) responseListener.onFailure(response);
        }

        @Override
        public void onRetry(int retryNo) {
            // called when request is retried
        	Log.e("Request", "Retrying request " + getRequestType());
        }
    };
	
}
