package com.familycircle.sdk.models;

import com.familycircle.sdk.CallConstants;
import com.familycircle.sdk.CallConstants.*;

public class CallModel {
	
	private ContactModel fromContact;
	private ContactModel toContact;
	private String callTagId;

    public String getUserCallTagId() {
        return userCallTagId;
    }

    public void setUserCallTagId(String userCallTagId) {
        this.userCallTagId = userCallTagId;
    }

    private String userCallTagId;
	private String roomId; // for webrtc

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    private String phoneNum; // for SIP
    private CallType callType; // indicates if its an outgoing or incoming call
    private CallUserAction userAction; // indicates the last user action on the call, Accepted, Rejected, Hold, Missed SConstants.CallUserAction
    private CallStatus callStatus; // indicates the current/last status, receiving, connected, completed etc from Constants.CallStatus enum
    private long createDate=0; // when this request was created

    public CallUserAction getUserAction() {
        return userAction;
    }

    public void setUserAction(CallUserAction userAction) {
        this.userAction = userAction;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getCallStats() {
        return callStats;
    }

    public void setCallStats(String callStats) {
        this.callStats = callStats;
    }

    private long startDate=0; // call start date
    private long endDate=0; // call end date
    private long duration=0; // duration of the call, end - start
    private String callStats;

	public CallModel() {
		// TODO Auto-generated constructor stub
	}
	public ContactModel getFromContact() {
		return fromContact;
	}
	public void setFromContact(ContactModel fromContact) {
		this.fromContact = fromContact;
	}
	public ContactModel getToContact() {
		return toContact;
	}
	public void setToContact(ContactModel toContact) {
		this.toContact = toContact;
	}
	public String getCallTagId() {
		return callTagId;
	}
	public void setCallTagId(String callTagId) {
		this.callTagId = callTagId;
	}
	public CallType getCallType() {
		return callType;
	}
	public void setCallType(CallType callType) {
		this.callType = callType;
	}
	public String getRoomId() {
		return roomId;
	}
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

}
