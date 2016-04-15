package com.familycircle.sdk;

/**
 * Created by samratsen on 5/28/15.
 */
public final class CallConstants {
    public enum CallStatus
    {
        Completed("Completed"),
        Receiving("Receiving"),
        Calling("Calling"),
        Hold("Hold"),
        ActiveSingle("Active Single"),
        ActiveConf("Active Conference");

        public String value;
        CallStatus(String v){
            value = v;
        }
    };
    public enum CallType
    {
        Idle("Idle"),
        Incoming("Receiving"),
        Outgoing("Outgoing");

        public String value;
        CallType(String v){
            value = v;
        }
    };
    // This represents the last user action on the call transaction
    public enum CallUserAction
    {
        None("None"),
        Accepted("Accepted"),
        Rejected("Rejected"),
        Hold("Hold"),
        Missed("Missed");

        public String value;
        CallUserAction(String v){
            value = v;
        }
    };
}
