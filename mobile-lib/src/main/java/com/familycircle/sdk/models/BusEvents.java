package com.familycircle.sdk.models;

/**
 * Created by samratsen on 2/26/15.
 */
public final class BusEvents {
    public static class ProcessPresenceEvent {

    }

    public static class IncomingCallEvent {
        public ContactModel contact;
        public IncomingCallEvent(ContactModel contact){
            this.contact = contact;
        }
    }

    public static class RejectIncomingCallEvent {
        public ContactModel contact;
        public RejectIncomingCallEvent(ContactModel contact){
            this.contact = contact;
        }
    }

    public static class ProcessCallCancelEvent {
        public ContactModel contact;
        public ProcessCallCancelEvent(ContactModel contact){
            this.contact = contact;
        }
    }

    public static class IncomingAudioSizeEvent {
        public double audioSize;
        public IncomingAudioSizeEvent(double size){
            this.audioSize = size;
        }
    }

    public static class IncomingVideoSizeEvent {
        public double videoSize;
        public IncomingVideoSizeEvent(double size){
            this.videoSize = size;
        }
    }

    public static class CompleteVideoProcessingEvent {
        public String fileName;
        public CompleteVideoProcessingEvent (String fileName){
            this.fileName = fileName;
        }
    }
}
