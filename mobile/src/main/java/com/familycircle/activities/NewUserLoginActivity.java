package com.familycircle.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.LoaderManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.lib.utils.Logger;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.manager.M2XStreamManager;
import com.familycircle.manager.M2XTriggerManager;
import com.familycircle.manager.PubSubManager;
import com.familycircle.manager.TeamManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.Dvc;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.SecUtil;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateRequest;
import com.familycircle.utils.network.QueryDbInvite;
import com.familycircle.utils.network.QueryDbUser;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.Types;
import com.familycircle.utils.network.model.InviteModel;
import com.familycircle.utils.network.model.M2XDevice;
import com.familycircle.utils.network.model.UserObject;

import java.util.Date;
import java.util.UUID;

public class NewUserLoginActivity extends Activity implements Handler.Callback, ResponseListener, PubSubManager.OnPubNubMessage {

    private static final String TAG = "NewUserLoginActivity";
    // UI references.
    private AutoCompleteTextView invite_code, name, contact_number, password;
    //private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private IChannelManagerClient channelManagerClient;
    private Handler mHandler;
    private Button mEmailSignInButton;
    private String email;
    private UserObject userObject;
    private InviteModel inviteModel;
    private boolean userExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_login);
        invite_code = (AutoCompleteTextView)findViewById(R.id.invite_code);
        name = (AutoCompleteTextView)findViewById(R.id.name);
        contact_number = (AutoCompleteTextView)findViewById(R.id.contact_number );
        password = (AutoCompleteTextView)findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        email = getIntent().getStringExtra("EMAIL");
        if (email==null||email.isEmpty()) finish();

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setTransformationMethod(null);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mHandler = new Handler(this);
        channelManagerClient = TeamManager.getInstance().getChannelManager();

        showProgress(true);
        QueryDbUser queryDbUser = new QueryDbUser(email, this);
        queryDbUser.exec();

    }

    @Override
    public void onResume() {
        super.onResume();
        TeamManager.getInstance().addControlManagerClientHandler(mHandler);
        PubSubManager.getInstance().addListener(this);
        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Device not connected to wifi or mobile network", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        TeamManager.getInstance().removeControlManagerClientHandler(mHandler);
        PubSubManager.getInstance().removeListener(this);
    }

    public void attemptLogin() {

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Unable to login, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
            return;
        }


        boolean cancel = false;
        View focusView = null;

        if (invite_code.getVisibility() == View.VISIBLE){
            if (TextUtils.isEmpty(invite_code.getText().toString())) {
                invite_code.setError(getString(R.string.error_field_required));
                //focusView = name;
                cancel = true;
            }

            if (inviteModel==null || !invite_code.getText().toString().equals(inviteModel.invite_code)){
                invite_code.setError("Please check your invitation code.");
                cancel = true;
            }
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(name.getText().toString())) {
            name.setError(getString(R.string.error_field_required));
            //focusView = name;
            cancel = true;
        }

        if (TextUtils.isEmpty(contact_number.getText().toString())) {
            contact_number.setError(getString(R.string.error_field_required));
            //focusView = contact_number;
            cancel = true;
        }

        if (TextUtils.isEmpty(password.getText().toString())) {
            password.setError(getString(R.string.error_field_required));
            //focusView = password;
            cancel = true;
        }

        if (userObject!=null){
            if (TextUtils.isEmpty(password.getText().toString())) {
                String userPassword = password.getText().toString();
                final String hash;
                try {
                    hash = Base64.encodeToString(SecUtil.generateSHA1Hash(userPassword), Base64.NO_WRAP);
                    if (userPassword!=null && hash.equalsIgnoreCase(userObject.password)) {
                        name.setError("Credentials not correct");
                    }
                } catch (Exception e) {
                    Logger.e("Password not correct", e);
                    name.setError("Credentials not correct");
                }


            }

        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            //focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            PrefManagerBase prefManager = new PrefManagerBase();
            prefManager.setUser(email.toLowerCase());

            showProgress(true);
            ContactModel contactModel = new ContactModel();
            contactModel.setIdTag(email.toLowerCase());
            Log.d(TAG, "Connect attemptLogin " + contactModel.getIdTag());

            createUserObject();
            //channelManagerClient.connectControlChannel(contactModel);

        }
    }

    private void createUserObject(){

        if (userObject==null) {
            userObject = new UserObject();
        }
        userObject.contact_number = contact_number.getText().toString();
        userObject.email = email;
        userObject.is_head = (inviteModel!=null)?false:true;
        userObject.name = name.getText().toString();
        if (userObject.device_id==null) {
            userObject.device_id = Dvc.getDeviceUid(getApplicationContext());
        }
        if (userObject.family_id==null) {
            userObject.family_id = (inviteModel!=null)?inviteModel.familyId:UUID.randomUUID().toString();
        }
        userObject.password = password.getText().toString();

        if (userObject.m2x_id==null || userObject.m2x_id.isEmpty()) {
            M2XCreateRequest m2XCreateRequest = new M2XCreateRequest(this, userObject.device_id, userObject.family_id, userObject.name);
            m2XCreateRequest.exec();
        } else {
            PubSubManager.getInstance().subscribe(userObject.family_id, userObject.device_id);
            //LoginRequest loginRequest = new LoginRequest(userObject, this);
            //loginRequest.exec();
        }
    }

    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "NewUserLoginActivity from Controller Login handleMessage");

        String action = getIntent().getStringExtra("ACTION");
        if (action!=null && action.equalsIgnoreCase("LOGOFF")){
            return false;
        }

        if (msg.what == Constants.EventReturnState.GET_USERS_SUCCESS.value){
            Log.d(TAG, "Login Successful");

            //showProgress(false);
            M2XStreamManager m2XStreamManager = new M2XStreamManager();
            m2XStreamManager.checkAndCreateTriggers(this);


            /*PrefManagerBase prefManager = new PrefManagerBase();
            prefManager.setUserLoggedOff(false);
            startScreen();*/

        } else if (msg.what == Constants.EventReturnState.CONTROL_RETRIES_FAILED.value){
            showProgress(false);
            Log.d(TAG, "Login Un-Successful:" + msg.obj);
            Toast.makeText(this, "Unable to login after retries.", Toast.LENGTH_LONG).show();

        } else if (msg.what == Constants.EventReturnState.LOG_IN_EOF.value){
            showProgress(false);
            Log.d(TAG, "Logged off");
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        }else if (
                msg.what == Constants.EventReturnState.CONTROL_DISCONNECT.value
                        || msg.what == Constants.EventReturnState.CONTROL_RETRIES_FAILED.value
                        || msg.what == Constants.EventReturnState.FAULT_NOTIFICATION.value
                ){
            showProgress(false);
            Log.d(TAG, "Login Un-Successful:" + msg.obj);
            Toast.makeText(this, "There was a network issue, please re-try after some time", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void startScreen(){
        Log.d(TAG, "Login startScreen");

        ContactModel userContact = ContactsStaticDataModel.getLogInUser();
        userContact.setFirstName(userObject.name);
        userContact.setEmail(userContact.getIdTag());
        userContact.setPhoneNumber(userObject.contact_number);

        Intent intent = new Intent(NewUserLoginActivity.this, MainActivity.class);
        intent.putExtra("ACTION", "LOGIN");

        Intent receivedintent = getIntent();
        if (receivedintent!=null){
            String callNumber = receivedintent.getStringExtra(TEAMConstants.KEY_PHONE_NUMBER);
            String smsNumber = receivedintent.getStringExtra(TEAMConstants.KEY_SMS_NUMBER);
            int dialerExtra = receivedintent.getIntExtra(TEAMConstants.NOTIFICATION_GROUP_DIALER, 0);
            int smsExtra = receivedintent.getIntExtra(TEAMConstants.NOTIFICATION_GROUP_SMS, 0);
            if (callNumber!=null){
                String name = receivedintent.getStringExtra(TEAMConstants.KEY_CONTACT_NAME);
                intent.putExtra(TEAMConstants.KEY_PHONE_NUMBER, callNumber);
                intent.putExtra(TEAMConstants.KEY_CONTACT_NAME, name);

            } else if (smsNumber!=null){
                String name = receivedintent.getStringExtra(TEAMConstants.KEY_CONTACT_NAME);
                intent.putExtra(TEAMConstants.KEY_SMS_NUMBER, smsNumber);
                intent.putExtra(TEAMConstants.KEY_CONTACT_NAME, name);

            } else if (dialerExtra>0){
                intent.putExtra(TEAMConstants.NOTIFICATION_GROUP_DIALER, dialerExtra);

            } else if (smsExtra>0){
                intent.putExtra(TEAMConstants.NOTIFICATION_GROUP_SMS, smsExtra);

            }
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onSuccess(Response response) {

        if (response.getRequestType() == Types.RequestType.QUERY_DB_USER){
            showProgress(false);
            userObject = (UserObject)response.getModel();
            if (userObject!=null && userObject.name!=null){
                // User already e;;xists in db
                name.setText(userObject.name);
                contact_number.setText(userObject.contact_number);
                password.setText("");
                invite_code.setVisibility(View.GONE);

            } else {
                // check if there is an invite
                QueryDbInvite queryDbInvite = new QueryDbInvite(email, this);
                queryDbInvite.exec();
            }
        }

        if (response.getRequestType() == Types.RequestType.QUERY_DB_INVITE){
            if (response.getModel()!=null) {
                inviteModel = (InviteModel) response.getModel();
                invite_code.setVisibility(View.VISIBLE);
            }else {
                invite_code.setVisibility(View.GONE);
            }

        }

        if (response.getRequestType() == Types.RequestType.M2X_CREATE_STREAM_MGR){
            M2XTriggerManager m2XTriggerManager = new M2XTriggerManager();
            m2XTriggerManager.checkAndCreateTriggers(this);
        }

        if (response.getRequestType() == Types.RequestType.M2X_CREATE_TRIGGER_MGR){
            showProgress(false);
            PrefManagerBase prefManager = new PrefManagerBase();
            prefManager.setUserLoggedOff(false);
            startScreen();
        }

        if (response.getRequestType() == Types.RequestType.M2X_CREATE_DEVICE){

            M2XDevice m2XDevice = (M2XDevice) response.getModel();
            userObject.m2x_id = m2XDevice.id;

            PubSubManager.getInstance().subscribe(userObject.family_id, userObject.device_id);
        }

        if (response.getRequestType() == Types.RequestType.GET_LOGIN){
            ContactModel contactModel = new ContactModel();
            contactModel.setIdTag(email);
            channelManagerClient.connectControlChannel(contactModel);
        }

    }

    @Override
    public void onFailure(final Response response) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showProgress(false);
                Toast.makeText(NewUserLoginActivity.this, "Error while requesting " + response.getRequestType(), Toast.LENGTH_LONG).show();
                if (response.getRequestType() == Types.RequestType.QUERY_DB_USER) {

                    goBackToLogin();

                }
                if (response.getRequestType() == Types.RequestType.QUERY_DB_INVITE){
                    goBackToLogin();
                }
            }
        });


    }

    private void goBackToLogin(){
        Intent intent = new Intent(NewUserLoginActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPubNubMessage(String channel, Object message) {

    }

    @Override
    public void onConnect(String channel, Object message){
        // Now login
        LoginRequest loginRequest = new LoginRequest(userObject, this);
        loginRequest.exec();
    }

}
