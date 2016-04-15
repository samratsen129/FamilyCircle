package com.familycircle.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import com.familycircle.R;
import com.familycircle.adapters.AutoReplyAdapter;

/**
 * Created by saran.s on 8/4/2015.
 */
public class AutoReplyMessagesActivity extends ActionBarActivity {

    private Toolbar toolbar;
    private Button add_more_messages;
    private AutoReplyAdapter myAdapter;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_reply);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            //toolbar.setNavigationIcon(R.drawable.ic_drawer);
            TextView toolbarTitle = (TextView)toolbar.findViewById(R.id.toolbar_header);
            toolbarTitle.setText("AUTO CANNED RESPONSES");
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        }

        add_more_messages = (Button)findViewById(R.id.add_more_reply_button);
        add_more_messages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater li = LayoutInflater.from(AutoReplyMessagesActivity.this);
                View promptsView = li.inflate(R.layout.auto_reply_prompts, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        AutoReplyMessagesActivity.this);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText reply_key = (EditText) promptsView
                        .findViewById(R.id.editText_key);
                final EditText reply_message = (EditText) promptsView
                        .findViewById(R.id.editText_message);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        if(!reply_key.getText().toString().trim().equalsIgnoreCase("") && !reply_message.getText().toString().trim().equalsIgnoreCase("")){
                                            LoginActivity.linkedHashMap.put(reply_key.getText().toString().trim(), reply_message.getText().toString().trim());
                                            myAdapter.notifyDataSetChanged();
                                        }else{
                                            Toast.makeText(getApplicationContext(), "Please enter your Auto reply key and message", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();


            }
        });

        myAdapter = new AutoReplyAdapter(this, LoginActivity.linkedHashMap);
        listview = (ListView) findViewById(R.id.list);
        listview.setAdapter(myAdapter);

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
}
