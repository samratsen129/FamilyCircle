package com.familycircle.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.TeamApp;
import com.familycircle.custom.views.CircularImageView;
import com.familycircle.manager.TeamManager;
import com.familycircle.utils.NetworkUtils;
import com.familycircle.utils.TEAMUtils;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.IChannelManagerClient;
import com.familycircle.sdk.models.ContactModel;

import java.io.File;


public class CreateProfileActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private static final String TAG = "CreateProfileAct";
    private static int PICK_FROM_CAMERA_RC = 0;
    private static int PICK_FROM_FILE_RC = 1;

    private Uri mImageCaptureUri;
    private ImageView mUserImage;
    private IChannelManagerClient channelManagerClient;
    private Handler mHandler;
    private AutoCompleteTextView nameTextView;
    private AutoCompleteTextView phoneTextView;
    private Button backButton;
    private Button nextButton;
    private CircularImageView userImageView;
    private LinearLayout llProfileContainer;
    private LinearLayout llProfileImageContainer;
    private View mProgressView;

    private ContactModel contactModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
        nameTextView = (AutoCompleteTextView)findViewById(R.id.nameTextView);
        phoneTextView = (AutoCompleteTextView)findViewById(R.id.phoneTextView);
        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
        nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(this);
        userImageView = (CircularImageView) findViewById(R.id.user_image_avatar);
        userImageView.setOnClickListener(this);
        llProfileContainer = (LinearLayout) findViewById(R.id.profile_form);
        llProfileImageContainer = (LinearLayout) findViewById(R.id.profile_image_form);
        mProgressView = findViewById(R.id.login_progress);
        mHandler = new Handler(this);
        channelManagerClient = TeamManager.getInstance().getChannelManager();
        contactModel = new ContactModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        TeamManager.getInstance().addControlManagerClientHandler(mHandler);
        if (!NetworkUtils.IsNetworkAvailable(this)){
            Toast.makeText(getApplicationContext(), "Device not connected to wifi or mobile network", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        TeamManager.getInstance().removeControlManagerClientHandler(mHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.message_settings) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void createImageDialog(){

        Toast.makeText(getApplicationContext(), "This is a dummy dialog, your slection will NOT BE USED", Toast.LENGTH_LONG).show();

        final String [] items           = new String [] {"From Camera", "From SD Card", "Cancel"};
        ArrayAdapter<String> adapter  = new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,items);
        AlertDialog.Builder builder     = new AlertDialog.Builder(this);
        builder.setTitle("Select Image from ...");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    /*Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);*/
                    File file = new File(Environment.getExternalStorageDirectory(),
                            "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    mImageCaptureUri = Uri.fromFile(file);

                    try {
                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                        intent.putExtra("return-data", true);

                        startActivityForResult(intent, PICK_FROM_CAMERA_RC);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    dialog.cancel();

                } else if (item == 1) {
                    Intent intent = new Intent();

                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE_RC);

                    dialog.cancel();

                } else {
                    dialog.cancel();
                }
            }
        });
        builder.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        Bitmap bitmap   = null;
        String path     = "";

        if (requestCode == PICK_FROM_FILE_RC) {
            mImageCaptureUri = data.getData();
            path = getRealPathFromURI(mImageCaptureUri); //from Gallery

            if (path == null)
                path = mImageCaptureUri.getPath(); //from File Manager

            if (path != null)
                bitmap  = BitmapFactory.decodeFile(path);

        } else if (requestCode == PICK_FROM_CAMERA_RC) {
            path    = mImageCaptureUri.getPath();
            bitmap  = BitmapFactory.decodeFile(path);
        }

        onSubmit();
        //mUserImage.setImageBitmap(bitmap);
    }

    public String getRealPathFromURI(Uri contentUri) {
        String [] proj      = {MediaStore.Images.Media.DATA};
        Cursor cursor       = getContentResolver().query( contentUri, proj, null, null,null);//managedQuery

        if (cursor == null) return null;

        int column_index    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "CreateUserActivity from Controller Login handleMessage");

        if (msg.what == Constants.EventReturnState.CREATE_USER_SUCCESS.value) {
            showProgress(false);
            Toast.makeText(getApplicationContext(), "User created successfully.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (msg.what == Constants.EventReturnState.CREATE_USER_ERROR.value) {
            showProgress(false);
            Toast.makeText(getApplicationContext(), "User could not be created." + ((String)msg.obj), Toast.LENGTH_LONG).show();
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.backButton:
                finish();
                break;
            case R.id.nextButton:
                onNextButtonPressed();
                break;
            case R.id.user_image_avatar:
                createImageDialog();
                break;
        }
    }

    @Override
    public void onBackPressed(){
        if (llProfileImageContainer.isShown()){
            llProfileImageContainer.setVisibility(View.GONE);
            llProfileContainer.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    private void onSubmit(){
        channelManagerClient.requestCreateNewUser(contactModel);
        showProgress(true);
    }

    private void onNextButtonPressed(){
        if (checkUserProfile()) {
            llProfileContainer.setVisibility(View.GONE);
            llProfileImageContainer.setVisibility(View.VISIBLE);
        }

    }

    private boolean checkUserProfile(){

        nameTextView.setError(null);
        phoneTextView.setError(null);

        String nameFull = nameTextView.getText().toString();

        if (nameFull==null||nameFull.trim().isEmpty()){
            nameTextView.setError("First and Last Name required");
            return false;
        }

        String [] names = nameFull.trim().split(" ");
        if (names.length < 2){
            nameTextView.setError("Last Name missing, please specify both First and Last names");
            return false;
        }

        if(phoneTextView.getText().toString().trim().isEmpty()){
            phoneTextView.setError("Please specify phone number");
            return false;
        }

        contactModel.setFirstName(TEAMUtils.capitalize(names[0].trim()));
        contactModel.setLastName(TEAMUtils.capitalize(names[names.length - 1].trim()));
        contactModel.setIdTag(contactModel.getFirstName().substring(0, 1) + contactModel.getLastName() + "@" + TeamApp.getGroupId());
        contactModel.setEmail(contactModel.getIdTag());
        contactModel.setPhoneNumber(phoneTextView.getText().toString().trim());

        return true;
    }

    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            llProfileImageContainer.setVisibility(show ? View.GONE : View.VISIBLE);
            llProfileImageContainer.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    llProfileImageContainer.setVisibility(show ? View.GONE : View.VISIBLE);
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
            llProfileImageContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
