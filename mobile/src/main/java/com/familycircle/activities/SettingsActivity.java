package com.familycircle.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.familycircle.BuildConfig;
import com.familycircle.R;
import com.familycircle.lib.utils.Logger;
import com.familycircle.manager.MBand2Manager;
import com.familycircle.manager.PubSubManager;
import com.familycircle.utils.TEAMUtils;
import com.familycircle.utils.imagecache.ImageLoader;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.microsoft.band.BandException;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;

public class SettingsActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener{

    private ImageView userImage;
    private TextView firstName;
    private TextView lastName;
    private TextView userPhone;
    private Toolbar toolbar;
    private Button logoutButton, auto_messages;
    private TextView versionText;
    private CheckBox auto_reply;
    public final String AUTOPREFERENCES = "Auto_prefs" ;
    private SwitchCompat locationSwitch, band2Switch;
    SharedPreferences sharedpreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            //toolbar.setNavigationIcon(R.drawable.ic_drawer);
            TextView toolbarTitle = (TextView)toolbar.findViewById(R.id.toolbar_header);
            toolbarTitle.setText("SETTINGS");
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        }

        userImage = (ImageView)findViewById(R.id.image_user);
        firstName = (TextView)findViewById(R.id.first_name);
        lastName  = (TextView)findViewById(R.id.last_name);
        userPhone = (TextView)findViewById(R.id.user_phone);
        logoutButton = (Button)findViewById(R.id.logout_button);
        versionText = (TextView)findViewById(R.id.app_version);
        auto_messages = (Button)findViewById(R.id.reply_button);
        auto_reply = (CheckBox)findViewById(R.id.click_to_enable_reply);
        locationSwitch = (SwitchCompat)findViewById(R.id.switch_location);
        locationSwitch.setSwitchPadding(40);
        locationSwitch.setOnCheckedChangeListener(this);
        band2Switch = (SwitchCompat)findViewById(R.id.switch_microsoft_band);
        band2Switch.setSwitchPadding(40);
        band2Switch.setOnCheckedChangeListener(this);
        if (MBand2Manager.getInstance().isStarted){
            band2Switch.setChecked(true);
        }
        if (PubSubManager.getInstance().isStarted){
            locationSwitch.setChecked(true);
        }

        sharedpreferences = getSharedPreferences(AUTOPREFERENCES, Context.MODE_PRIVATE);

        versionText.setText("Version " + BuildConfig.VERSION_NAME);
        ContactModel contactModel = ContactsStaticDataModel.getLogInUser();

        ImageLoader imageLoader = new ImageLoader(getApplicationContext());
        imageLoader.DisplayImage(contactModel.getAvatarUrlSmall(), userImage);

        firstName.setText(TEAMUtils.capitalize(contactModel.getFirstName()));
        lastName.setText(TEAMUtils.capitalize(contactModel.getLastName()));
        userPhone.setText(contactModel.getPhoneNumber());

        if(sharedpreferences.getBoolean("Auto_reply", false)) {
            auto_reply.setChecked(true);
        }
        auto_reply.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                if (((CheckBox) v).isChecked()) {
                SharedPreferences.Editor editor = sharedpreferences.edit();

                editor.putBoolean("Auto_reply", auto_reply.isChecked());
                editor.commit();
//                }
            }
        });

        auto_messages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AutoReplyMessagesActivity.class);
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.putExtra("ACTION", "LOGOFF");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                PrefManagerBase prefManager = new PrefManagerBase();
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.global, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_location:
                Logger.d("Location switch_compat " + isChecked + "");
                if (isChecked){
                    PubSubManager.getInstance().startSharingLocation();
                } else {
                    PubSubManager.getInstance().stopSharingLocation();
                }
                break;

            case R.id.switch_microsoft_band:
                Logger.d("Band2 switch_compat " + isChecked + "");
                if (isChecked){
                    final WeakReference<Activity> reference = new WeakReference<Activity>(this);
                    MBand2Manager.getInstance().startSubscriptionTask();
                    new HeartRateConsentTask().execute(reference);
                } else {
                    //MBand2Manager.getInstance().stopSubscriptionTask();
                    MBand2Manager.getInstance().pauseTasks();
                }
                break;
        }
    }

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (MBand2Manager.getInstance().getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        MBand2Manager.getInstance().getBandClient().getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    Logger.d("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
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
                Logger.d(exceptionMessage);

            } catch (Exception e) {
                Logger.d(e.getMessage());
            }
            return null;
        }
    }
}
