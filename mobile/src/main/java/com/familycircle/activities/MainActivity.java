package com.familycircle.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.familycircle.utils.NotificationMgr;
import com.familycircle.utils.network.LoginRequest;
import com.familycircle.utils.network.M2XCreateStreamValue;
import com.familycircle.utils.network.model.UserObject;
import com.mikepenz.actionitembadge.library.ActionItemBadge;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.familycircle.R;
//import com.familycircle.auto.MessagingService;
import com.familycircle.fragments.ContactsFragment;
import com.familycircle.fragments.MessageHistoryFragment;
import com.familycircle.fragments.MyRecordingsFragment;
import com.familycircle.fragments.NavigationDrawerFragment;
import com.familycircle.manager.TeamManager;
import com.familycircle.utils.TEAMUtils;
import com.familycircle.lib.utils.PrefManagerBase;
import com.familycircle.sdk.Constants;
import com.familycircle.sdk.models.MessageModel;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, Handler.Callback{

    private static final String TAG = "MainActivity";
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private Toolbar toolbar;

    private TextView toolbarTitle;

    private Handler mHandler;

    private RelativeLayout mainContainer;

    private int badgeCount;

    private MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mainContainer = (RelativeLayout) findViewById(R.id.main_activity_container);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            //toolbar.setNavigationIcon(R.drawable.ic_drawer);
            toolbarTitle = (TextView)toolbar.findViewById(R.id.toolbar_header);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //startService(new Intent(MainActivity.this, MessagingService.class));
    }

    @Override
    public void onResume(){
        super.onResume();
        TeamManager.getInstance().addMessagingClientHandler(mHandler);

        badgeCount = TEAMUtils.updateSmsAppBadge();
        invalidateOptionsMenu();

    }

    @Override
    public void onPause(){
        TeamManager.getInstance().removeMessagingClientHandler(mHandler);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(MainActivity.this, MessagingService.class));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (badgeCount>0) {
            ActionItemBadge.update(this, menu.findItem(R.id.item_messagebadge), FontAwesome.Icon.faw_comment_o, ActionItemBadge.BadgeStyle.DARKGREY, badgeCount);
        } else {
            ActionItemBadge.hide(menu.findItem(R.id.item_messagebadge));
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, 1);
            return true;

        } else if (item.getItemId() == R.id.item_messagebadge){
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = new MessageHistoryFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;
        boolean isLogOff = false;
        boolean isInviteUser = false;
        boolean isPanic = false;
        switch (position){
            case 1:
                isInviteUser = true;
                break;
            case 0:
            case 2:
                fragment = new ContactsFragment();
                break;
            case 3:
                fragment = new MessageHistoryFragment();
                break;
            case 4:
                fragment = new MyRecordingsFragment();
                break;
            case 5:
                isPanic=true;
                panicTrigger();
                break;
            case 6:
                isLogOff = true;
                break;
        }

        if (isLogOff){
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("ACTION", "LOGOFF");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        } else if (isInviteUser){
            Intent intent = new Intent(MainActivity.this, AddUserLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else if (isPanic){
            Toast.makeText(getApplicationContext(), "Panic Triggered", Toast.LENGTH_SHORT).show();
            NotificationMgr notificationMgr = new NotificationMgr();
            notificationMgr.showNotification(1234, "Alert", "ssen@mp.com", "panic received");
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    public Fragment getFragment(){
        return getSupportFragmentManager().findFragmentById(R.id.container);
    }

    private void panicTrigger(){
        UserObject userObject = LoginRequest.getUserObject();
        M2XCreateStreamValue m2XCreateStream = new M2XCreateStreamValue(null, userObject.m2x_id, "panic", "alphanumeric", "I am panic-ing");
        m2XCreateStream.exec();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        //actionBar.setDisplayShowTitleEnabled(true);
        //actionBar.setTitle(mTitle);
        setToolBarTitle(mTitle.toString());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_drawer);
    }

    public void setToolBarTitle(String title) {
        if (toolbar != null) {
            toolbarTitle.setText(title);
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage " + msg);
        if (msg.what == Constants.EventReturnState.NEW_IN_MESSAGE.value){

            if (getFragment() instanceof MessageHistoryFragment){
                MessageHistoryFragment fragment = (MessageHistoryFragment) getFragment();
                fragment.loadView();
            } else {
                badgeCount = TEAMUtils.updateSmsAppBadge();
                invalidateOptionsMenu();
            }

            MessageModel messageModel = (MessageModel)msg.obj;
            String message = messageModel.getBody();
            message = message.length()<=24?message:message.substring(0,24);
            Snackbar.make(mainContainer, messageModel.getName() +": " + message+"...", Snackbar.LENGTH_LONG)
                    .setAction("READ", new SnackBarOnClickListener(messageModel.getTn()))
                    .setActionTextColor(Color.parseColor("#f37120"))
                    .show();


        }
        return false;
    }

    public class SnackBarOnClickListener implements View.OnClickListener{
        private final String _tn;

        public SnackBarOnClickListener(final String tn){
            _tn = tn;
        }

        @Override
        public void onClick(View v){
            Intent intent = new Intent(MainActivity.this, MessageDetailActivity.class);
            intent.putExtra("TAG_ID", _tn);
            startActivityForResult(intent, 0);
        }
    }

}
