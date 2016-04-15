package com.familycircle.manager;

import android.os.AsyncTask;
import android.util.Log;

import com.familycircle.TeamApp;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateStream;
import com.familycircle.utils.network.M2XCreateStreamValue;
import com.familycircle.utils.network.model.UserObject;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.MotionType;

public class MBand2Manager {

    private static MBand2Manager _instance = new MBand2Manager();
    public static final String TAG = "MBand2Manager";

    long rate1=0, rate2=0, rate3=0;

    private BandClient client = null;
    public boolean isStarted = false;

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                Log.d(TAG, String.format("Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()));

                /*if (!event.getQuality().toString().equalsIgnoreCase("locked")){
                    Log.d(TAG, "Ignoring heartbeat not locked");
                    return;
                }*/
                ContactModel userContact = ContactsStaticDataModel.getLogInUser();
                userContact.heartRate = event.getHeartRate()+"";

                UserObject userObject = LoginRequest.getUserObject();
                M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue (null, userObject.m2x_id, "heartbeat", "numeric", event.getHeartRate()+"");
                m2XCreateStream.exec();
            }
        }
    };

    private BandSkinTemperatureEventListener mSkinTempEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
            if (event != null) {
                ContactModel userContact = ContactsStaticDataModel.getLogInUser();
                userContact.temperature = String.format("%.2f", event.getTemperature());
                Log.d(TAG, String.format("Skin Temperature = %.2f \n", event.getTemperature()));

                UserObject userObject = LoginRequest.getUserObject();
                M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue (null, userObject.m2x_id, "temperature", "numeric", "temperature: " + event.getTemperature());
                m2XCreateStream.exec();
            }
        }
    };

    private BandDistanceEventListener mBandDistanceEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent) {
            if (bandDistanceEvent != null) {
                ContactModel userContact = ContactsStaticDataModel.getLogInUser();
                userContact.montionType = bandDistanceEvent.getMotionType().toString();
                Log.d(TAG, "Motion Type = " + userContact.montionType);
                try {
                    Log.d(TAG, "Distance Today = " + bandDistanceEvent.getDistanceToday());
                    userContact.distanceToday = bandDistanceEvent.getDistanceToday() + "";
                } catch (InvalidBandVersionException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Speed = " + bandDistanceEvent.getSpeed());
                userContact.speed = bandDistanceEvent.getSpeed()+"";

                UserObject userObject = LoginRequest.getUserObject();
                M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue (null, userObject.m2x_id, "distance", "alphanumeric", "motion: " + userContact.montionType +", speed:" + userContact.speed +", distance:" + userContact.distanceToday);
                m2XCreateStream.exec();
            }
        }
    };


    private MBand2Manager(){

    }

    public void startSubscriptionTask(){
        if (isStarted) return;
        new HeartRateSubscriptionTask().execute();
        isStarted = true;
    }

    public void stopSubscriptionTask(){
        try {
            if (!isStarted) return;
            if (client==null) return;
            client.disconnect().await();
            isStarted = false;
        } catch (InterruptedException e) {
            // Do nothing as this is happening during destroy
        } catch (BandException e) {
            // Do nothing as this is happening during destroy
        }
    }

    public void pauseTasks(){
        if (client==null) return;
        try {

            client.disconnect().await();
            client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
        } catch (Exception e) {
            // Do nothing as this is happening during destroy
        }

        try {

            client.disconnect().await();
            client.getSensorManager().unregisterDistanceEventListener(mBandDistanceEventListener);
        } catch (Exception e) {
            // Do nothing as this is happening during destroy
        }

        try {

            client.disconnect().await();
            client.getSensorManager().unregisterSkinTemperatureEventListener(mSkinTempEventListener);
        } catch (Exception e) {
            // Do nothing as this is happening during destroy
        }
    }

    public static MBand2Manager getInstance(){
        return _instance;
    }

    public BandClient getBandClient(){
        return client;
    }



    public boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.d(TAG, "Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(TeamApp.getContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        Log.d(TAG, "Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    client.getSensorManager().registerSkinTemperatureEventListener(mSkinTempEventListener);
                    client.getSensorManager().registerDistanceEventListener(mBandDistanceEventListener);
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        isStarted = false;
                        Log.d(TAG, "You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    isStarted = false;
                    Log.d(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.e(TAG, exceptionMessage);
                isStarted=false;

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                isStarted=false;

            }
            return null;
        }
    }

}
