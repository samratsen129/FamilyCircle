package com.familycircle.utils.network;

public interface ResponseListener {
	public void onSuccess(Response response);

	public void onFailure(Response response);
}
