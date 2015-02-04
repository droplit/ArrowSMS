package com.droplit.texcellent;

import android.content.Intent;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {

    private LayoutInflater layoutInflater;
    private ArrayList<String> texts = new ArrayList<String>(10);

    Settings sendSettings = new Settings();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        sendSettings.setMmsc("http://mms.vtext.com/servlets/mms");
        sendSettings.setProxy("");
        sendSettings.setPort("80");
        sendSettings.setGroup(true);
        sendSettings.setDeliveryReports(true);
        sendSettings.setSplit(true);
        sendSettings.setSplitCounter(true);
        sendSettings.setStripUnicode(false);
        sendSettings.setSignature("");
        sendSettings.setSendLongAsMms(true);
        sendSettings.setSendLongAsMmsAfter(3);
        sendSettings.setAccount("julian.pinzer@gmail.com");
        sendSettings.setRnrSe(null);

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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_default) {
            Intent intent =
                    new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    getPackageName());
            startActivity(intent);

            Toast.makeText(getApplicationContext(),"Set as Default texting app.", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.messaging, container, false);
            return rootView;
        }
    }
    public void send(View view) {

        EditText messageField = (EditText) findViewById(R.id.messageBodyField);
        String text = messageField.getText().toString();
        ListView listView = (ListView) findViewById(R.id.listMessages);

        ViewGroup parent = (ViewGroup) findViewById(R.id.mainView);

        View viewR = LayoutInflater.from(this).inflate(R.layout.message_right, null);
        //View viewL = LayoutInflater.from(this).inflate(R.layout.message_left, null);
        parent.addView(viewR);
        TextView txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtMessage.setText(text);

        Transaction transaction = new Transaction(MainActivity.this, sendSettings);

        Message message = new Message(messageField.getText().toString(), "9785023741");
        message.setType(Message.TYPE_SMSMMS);

        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
        Log.d("TEXT", "Sent");
    }
}
