
package com.familycircle.manager;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.familycircle.sdk.models.ContactModel;

public abstract class BaseController
{

	private final List<Handler> baseHandlerList = new ArrayList<Handler>();

	abstract public boolean handleMessage(int what, final ContactModel userContactModel, final String msg, final Object extra);

	public final void addHandler(Handler handler)
	{
		if (handler==null) return;
		for (Handler _handler:baseHandlerList){
			if (handler==_handler) return;
		}
		baseHandlerList.add(handler);
	}

	public final int getQueueSize()
	{
		return baseHandlerList.size();
	}

	public final void removeHandler(Handler handler)
	{
		baseHandlerList.remove(handler);
	}

	protected final void notifyHandlers(int what, int arg1, int arg2, Object obj)
	{
		Log.d("BaseController", "notifyHandlers");
		if (!baseHandlerList.isEmpty())
		{
			for (Handler handler : baseHandlerList)
			{
				if (handler==null) continue;
				Log.d("BaseController", "notify handler");
				Message msg = Message.obtain(handler, what, arg1, arg2, obj);
				msg.sendToTarget();
			}
		}
	}
}
