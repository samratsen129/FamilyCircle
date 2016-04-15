package com.familycircle.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.familycircle.R;
import com.familycircle.manager.TeamManager;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LoginActivity extends Activity implements LoaderCallbacks<Cursor>, Handler.Callback {

    private static final String TAG = "LoginActivity";
    // UI references.
    private AutoCompleteTextView mEmailView;
    //private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private IChannelManagerClient channelManagerClient;
    private Handler mHandler;
    private Button mEmailSignInButton;
    private Button mNewUserProfileButton;
    public static LinkedHashMap<String,String> linkedHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        mHandler = new Handler(this);
        channelManagerClient = TeamManager.getInstance().getChannelManager();

        // FullScreen with transparent system bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setTransformationMethod(null);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mNewUserProfileButton = (Button) findViewById(R.id.create_new_user_button);
        mNewUserProfileButton.setTransformationMethod(null);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        AddReplyMessages();
    }

    public void AddReplyMessages() {
        if (linkedHashMap == null) {
            linkedHashMap = new LinkedHashMap<String,String>();
        }
        if (linkedHashMap.isEmpty()) {
            linkedHashMap.put("DND", "Do not disturb");
            linkedHashMap.put("Busy ", "I'm Busy, I'll call you later");
            linkedHashMap.put("Driving", "Can't talk now, call me later");
            linkedHashMap.put("Meeting", "I'm in a meeting, I'll call you later!");
}
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume(){
        super.onResume();
        TeamManager.getInstance().addControlManagerClientHandler(mHandler);
        PrefManagerBase prefManager = new PrefManagerBase();

        String action = getIntent().getStringExtra("ACTION");
        if (action!=null && action.equalsIgnoreCase("LOGOFF")){
            channelManagerClient.logOff();
            prefManager.setUserLoggedOff(true);
        }

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Device not connected to wifi or mobile network", Toast.LENGTH_LONG).show();

        }

        String u = prefManager.getUser();
        boolean isLoggedOut = prefManager.getUserLoggedOff();
        if (u!=null) mEmailView.setText(u.trim());
        if (u!=null && !isLoggedOut){
            List<ContactModel> contactModels = ContactsStaticDataModel.getAllContacts();
            if (contactModels!=null
                    && !contactModels.isEmpty()){
                startScreen();
            } else {
                showProgress(true);
                ContactModel contactModel = new ContactModel();
                contactModel.setIdTag(mEmailView.getText().toString());
                ///channelManagerClient.connectControlChannel(contactModel);
                moveToLogin();
            }
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        TeamManager.getInstance().removeControlManagerClientHandler(mHandler);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Unable to login, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        //mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid email address.
        if (TextUtils.isEmpty(email.trim())) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            PrefManagerBase prefManager = new PrefManagerBase();
            prefManager.setUser(mEmailView.getText().toString().trim());

            showProgress(true);
            ContactModel contactModel = new ContactModel();
            contactModel.setIdTag(mEmailView.getText().toString());
            Log.d(TAG, "Connect attemptLogin " + contactModel.getIdTag());
            //channelManagerClient.connectControlChannel(contactModel);

            moveToLogin();

        }
    }

    private void moveToLogin(){
        Intent intent = new Intent(LoginActivity.this, NewUserLoginActivity.class);
        intent.putExtra("EMAIL", mEmailView.getText().toString().trim());
        startActivity(intent);
        finish();
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
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
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "MainActivity from Controller Login handleMessage");

        String action = getIntent().getStringExtra("ACTION");
        if (action!=null && action.equalsIgnoreCase("LOGOFF")){
            return false;
        }

        if (msg.what == Constants.EventReturnState.GET_USERS_SUCCESS.value){
            Log.d(TAG, "Login Successful");
            showProgress(false);
            PrefManagerBase prefManager = new PrefManagerBase();
            prefManager.setUserLoggedOff(false);
            //startScreen();

        } else if (
                msg.what == Constants.EventReturnState.CRITICAL_ERROR.value
                || msg.what == Constants.EventReturnState.LOGIN_REQUIRED.value

                ) {
            showProgress(false);
            Log.d(TAG, "Login Un-Successful:" + msg.obj);
            mEmailView.setError("Unable to login with provided credentials");

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
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
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
}



