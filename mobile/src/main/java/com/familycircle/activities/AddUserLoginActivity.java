package com.familycircle.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.manager.PubSubManager;
import com.familycircle.manager.TeamManager;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.utils.Dvc;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.TEAMConstants;
import com.familycircle.utils.network.InviteUserRequest;
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

import java.util.UUID;

public class AddUserLoginActivity extends Activity implements ResponseListener {

    private static final String TAG = "NewUserLoginActivity";
    // UI references.
    private AutoCompleteTextView invite_code, email;
    //private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mEmailSignInButton;
    private UserObject userObject;
    private InviteModel inviteModel;
    private boolean userExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user_login);
        invite_code = (AutoCompleteTextView)findViewById(R.id.invite_code);
        email = (AutoCompleteTextView)findViewById(R.id.name);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setTransformationMethod(null);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendInvite();
            }
        });

    }

    private boolean validate(){
        boolean cancel = true;
        if (TextUtils.isEmpty(email.getText().toString())) {
            email.setError(getString(R.string.error_field_required));
            //focusView = name;
            cancel = false;
        }

        if (email.getText().toString().indexOf("@")<0) {
            email.setError("Please enter a valid email");
            //focusView = name;
            cancel = false;
        }

        if (TextUtils.isEmpty(invite_code.getText().toString())) {
            invite_code.setError(getString(R.string.error_field_required));
            cancel = false;
        }

        return cancel;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Device not connected to wifi or mobile network", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    public void sendInvite() {

        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Unable to login, not connected to wifi or mobile network", Toast.LENGTH_LONG).show();
            return;
        }

        if (validate()){

            UserObject currentUser = LoginRequest.getUserObject();

            InviteModel inviteModel = new InviteModel();

            inviteModel.familyId = currentUser.family_id;
            inviteModel.fromUser = currentUser.email;
            inviteModel.toUser = email.getText().toString();
            inviteModel.invite_code = invite_code.getText().toString();
            inviteModel.is_pending = true;

            showProgress(true);
            InviteUserRequest inviteUserRequest = new InviteUserRequest(inviteModel, this);
            inviteUserRequest.exec();

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
    public void onSuccess(Response response) {

        if (response.getRequestType() == Types.RequestType.QUERY_INVITE_USER){
            showProgress(false);
            if (response.getReponseCode()==200){
                Toast.makeText(AddUserLoginActivity.this, "Awesome! We were able to add " + email.getText().toString() + " to your Family Circle", Toast.LENGTH_LONG).show();
            } else if (response.getReponseCode()==204){
                Toast.makeText(AddUserLoginActivity.this, "Your invite request was already sent before to " + email.getText().toString(), Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onFailure(final Response response) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showProgress(false);
                Toast.makeText(AddUserLoginActivity.this, "Sorry, your invite could not be sent. " + response.getRequestType(), Toast.LENGTH_LONG).show();
                if (response.getRequestType() == Types.RequestType.QUERY_INVITE_USER) {



                }
            }
        });


    }

    private void goBackToLogin(){
        Intent intent = new Intent(AddUserLoginActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
