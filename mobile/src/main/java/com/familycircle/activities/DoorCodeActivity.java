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
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.manager.TeamManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.M2XCreateDoorTrigger;
import com.familycircle.utils.network.M2XCreateStreamValue;
import com.familycircle.utils.network.QueryDbUser;
import com.familycircle.utils.network.QueryDoorCode;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.Types;
import com.familycircle.utils.network.model.UserObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class DoorCodeActivity extends Activity implements ResponseListener{

    private static final String TAG = "LoginActivity";
    // UI references.
    private AutoCompleteTextView mEmailView, mEmailView2;
    //private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mEmailSignInButton;
    public static LinkedHashMap<String,String> linkedHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_door_code);

        // FullScreen with transparent system bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mEmailView2 = (AutoCompleteTextView) findViewById(R.id.email2);


        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptDooropen();
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
                attemptDooropen();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

    }


    @Override
    public void onResume(){
        super.onResume();
        PrefManagerBase prefManager = new PrefManagerBase();


        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Device not connected to wifi or mobile network", Toast.LENGTH_LONG).show();

        }


    }

    @Override
    public void onPause(){
        super.onPause();
    }


    public void attemptDooropen() {

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Unable to login, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mEmailView2.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String email2 = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid email address.
        if (TextUtils.isEmpty(email.trim())) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email2.trim())) {
            mEmailView2.setError(getString(R.string.error_field_required));
            focusView = mEmailView2;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            showProgress(true);

            QueryDoorCode queryDoorCode = new QueryDoorCode(email, email2, this);
            queryDoorCode.exec();

        }
    }

    private void moveToLogin(){
        Intent intent = new Intent(DoorCodeActivity.this, NewUserLoginActivity.class);
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
    public void onSuccess(Response response) {
        if (response.getRequestType() == Types.RequestType.QUERY_DOOR_CODE){
            if (response.getReponseCode()==204) {
                Toast.makeText(getApplicationContext(), "Door code does not exist", Toast.LENGTH_LONG).show();
            }

            QueryDbUser queryDbUser = new QueryDbUser(mEmailView2.getText().toString(), this);
            queryDbUser.exec();
        }

        if (response.getRequestType() == Types.RequestType.QUERY_DB_USER){
            UserObject userObject = (UserObject)response.getModel();
            if (userObject!=null) {
                Date date = new Date();
                M2XCreateStreamValue m2XCreateDoorTrigger = new M2XCreateStreamValue(this, userObject.m2x_id, "door","alphanumeric","door open request " + date.toString());
                m2XCreateDoorTrigger.exec();
            }
        }

        if (response.getRequestType() == Types.RequestType.M2X_CREATE_STREAM){
            Toast.makeText(getApplicationContext(), "Suucessful ! Door opening.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onFailure(Response response) {

    }
}



