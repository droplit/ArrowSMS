package com.droplit.texcellent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.droplit.texcellent.services.ChatHeadService;
import com.klinker.android.logger.Log;
import com.klinker.android.send_message.ApnUtils;
import com.klinker.android.send_message.DeliveredReceiver;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;
import com.klinker.android.send_message.Utils;


import com.nispok.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    private Settings settings;

    private RecipientEditTextView toField;
    private ImageView imageToSend;
    private RecyclerView log;
    private Button sendButton;
    private EditText messageField;
    private TextView textView;

    private LogAdapter logAdapter;

    private boolean imageEnabled = false;

    private static int RESULT_LOAD_IMAGE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSettings();
        initViews();
        initActions();
        initLogging();
        toField =
                (RecipientEditTextView) findViewById(R.id.to);
        toField.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter adapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this);
        adapter.setShowMobileOnly(true);
        toField.setAdapter(adapter);
        toField.dismissDropDownOnItemSelected(true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawableRecipientChip[] chips = toField.getSortedRecipients();
                for (DrawableRecipientChip chip : chips) {
                    Log.v("DrawableChip", chip.getEntry().getDisplayName() + " " + chip.getEntry().getDestination());
                }
            }
        }, 5000);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(),"Settings", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_default_text:
                Toast.makeText(getApplicationContext(),"Set as Default", Toast.LENGTH_SHORT).show();
                setDefaultSmsApp();
                return true;
            case R.id.action_init_apns:
                Toast.makeText(getApplicationContext(),"Select Apns", Toast.LENGTH_SHORT).show();
                initApns();
                return true;
            case R.id.action_dialog:
                showDialog();
            case R.id.action_msg:
                Intent i = new Intent(getApplicationContext(),MessagingActivity.class);
                startActivity(i);
            case R.id.action_head:
                startService(new Intent(getApplicationContext(), ChatHeadService.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void showDialog() {
        DialogFragment newFragment = QuickReply.newInstance(
                R.string.quick_reply);
        newFragment.show(getFragmentManager(), "dialog");
    }
    public static class QuickReply extends DialogFragment {

        public static QuickReply newInstance(int title) {
            QuickReply frag = new QuickReply();
            Bundle args = new Bundle();
            args.putInt("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.quick_reply, null));
            return builder.create();
        }
    }

    private void initSettings() {
        settings = Settings.get(this);

        if (TextUtils.isEmpty(settings.getMmsc())) {
            initApns();
        }
    }

    private void initApns() {
        ApnUtils.initDefaultApns(this, new ApnUtils.OnApnFinishedListener() {
            @Override
            public void onFinished() {
                settings = Settings.get(MainActivity.this, true);
            }
        });
    }

    private void initViews() {
        //toField = (EditText) findViewById(R.id.to);
        imageToSend = (ImageView) findViewById(R.id.image);
        log = (RecyclerView) findViewById(R.id.log);
        sendButton = (Button) findViewById(R.id.sendButton);
        messageField = (EditText) findViewById(R.id.messageBodyField);
        textView = (TextView) findViewById(R.id.toggleImageText);
    }

    private void initActions() {

        //toField.setText(Utils.getMyPhoneNumber(this));

        imageToSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSendImage();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        log.setHasFixedSize(false);
        log.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(new ArrayList<String>());
        log.setAdapter(logAdapter);
    }

    private void initLogging() {
        Log.setDebug(true);
        Log.setPath("messenger_log.txt");
    }

    private void setDefaultSmsApp() {
        //setDefaultAppButton.setVisibility(View.GONE);
        Intent intent =
                new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                getPackageName());
        startActivity(intent);
        Log.d("Default", "Set as default SMS app");
    }

    private void toggleSendImage() {
        if (imageEnabled) {
            imageToSend.setAlpha(0.3f);
            imageEnabled = false;
            textView.setText("Disabled");
        } else{
            imageToSend.setAlpha(1.0f);
            imageEnabled = true;
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
            textView.setText("Enabled");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.image);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }


    }
    public void sendMessage() {

        /* (toField.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter a number", Toast.LENGTH_LONG).show();
            Log.d("Message", "No phone number was inputted");
            return;
        }*/

        if (messageField.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter a message", Toast.LENGTH_LONG).show();
            Log.d("Message", "It was empty.");
            return;
        }

        if (Utils.isDefaultSmsApp(this)) {
            // TODO: Use SmsManager to send SMS and then record the message in the system SMS
            // ContentProvider
            Snackbar.with(getApplicationContext()) // context
                    .text("Text Sent") // text to display
                    .show(this); // activity where it is displayed

            logAdapter.addItem("Sent: " + messageField.getText().toString());

            new Thread(new Runnable() {
                @Override
                public void run() {

                    com.klinker.android.send_message.Settings sendSettings = new com.klinker.android.send_message.Settings();
                    sendSettings.setMmsc(settings.getMmsc());
                    sendSettings.setProxy(settings.getMmsProxy());
                    sendSettings.setPort(settings.getMmsPort());

                    Transaction transaction = new Transaction(MainActivity.this, sendSettings);

                    Message message = new Message(messageField.getText().toString(), toField.getText().toString().replace(",","").replace("(", "").replace(")", "")
                                                                .replace("-", "").replace(" ",""));
                    message.setType(Message.TYPE_SMSMMS);

                    if (imageEnabled) {
                        message.setImage(BitmapFactory.decodeResource(getResources(), R.id.image));
                    }

                    transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
                    Log.d("Message", "Sent to "+ toField.getText().toString().replace(",","").replace("(","").replace(")","")
                                                            .replace("-", "").replace(" ", ""));
                    DeliveredReceiver deliveredReceiver = new DeliveredReceiver();
                }
            }).start();

            messageField.setText("");
        } else {
            // TODO: Notify the user the app is not default and provide a way to trigger
            // Utils.setDefaultSmsApp() so they can set it.
            new MaterialDialog.Builder(this)
                    .title("Default SMS")
                    .content("%AppName needs to be your default SMS app to do that.")
                    .positiveText("Set Now")
                    .negativeText("Later")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            setDefaultSmsApp();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            Toast.makeText(getApplicationContext(),
                                    "You cannot send texts until default is set.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .show();
        }
    }


}