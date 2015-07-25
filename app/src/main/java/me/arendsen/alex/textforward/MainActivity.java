package me.arendsen.alex.textforward;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {

    public static final int CONFIGURE_SERVER = 10;
    public static final String LOG_TAG = "TextForward";

    TextView tvServerAddressDisplay;
    Button bConfigureServer, bTestConnection;
    ToggleButton tbForwardToggle;
    boolean forwardingEnabled;

    private class TextForwardSettings {
        public String serverIP;
        public int serverPort;

        public TextForwardSettings() {
            serverIP = "";
            serverPort = -1;
        }

        public boolean isReady() {
            return !serverIP.isEmpty() && serverIP!=null && serverPort>-1;
        }
    }

    TextForwardSettings serverSettings = new TextForwardSettings();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure toggle button
        tbForwardToggle = (ToggleButton) findViewById(R.id.textForwardToggle);
        tbForwardToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!serverSettings.isReady()) {
                    tbForwardToggle.setChecked(false);
                } else {
                    forwardingEnabled = tbForwardToggle.isChecked();
                    Toast.makeText(getApplicationContext(),"Text Forwarding "+((forwardingEnabled)?"Enabled":"Disabled"),Toast.LENGTH_SHORT).show();

                    // Start / stop service here
                    Intent serviceIntent = new Intent(MainActivity.this, TextForwardService.class);
                    if(forwardingEnabled) {
                        serviceIntent.putExtra("serverIP",serverSettings.serverIP);
                        serviceIntent.putExtra("serverPort",serverSettings.serverPort);
                        startService(serviceIntent);
                    } else {
                        stopService(serviceIntent);
                    }
                }

            }
        });
        forwardingEnabled = tbForwardToggle.isChecked();

        // Configure configuration button
        bConfigureServer = (Button) findViewById(R.id.configureServerButton);
        bConfigureServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent configurationIntent = new Intent(MainActivity.this, ConfigureServerActivity.class);
                startActivityForResult(configurationIntent, CONFIGURE_SERVER);
            }
        });

        // Configure test connection button
        bTestConnection = (Button) findViewById(R.id.testConnectionButton);
        bTestConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("SMS_RECEIVED_ACTION");
                broadcastIntent.putExtra("sms-sender", "TextForward");
                broadcastIntent.putExtra("sms-message", "Test Message");
                getApplicationContext().sendBroadcast(broadcastIntent);
            }
        });

        // Initialize TextViews
        tvServerAddressDisplay = (TextView) findViewById(R.id.serverAddressDisplay);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == CONFIGURE_SERVER) {
            String serverIP = data.getStringExtra("serverIP");
            int serverPort = data.getIntExtra("serverPort",-1);
            try {
                if(serverIP != null && serverPort != -1) {
                    serverSettings.serverIP = serverIP;
                    serverSettings.serverPort = serverPort;
                } else {
                   throw new Exception("Bad arguments from configuration activity");
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to open socket", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                updateServerField();
            }

        }
    }

    private void updateServerField() {
        String serverIP = serverSettings.serverIP;
        String serverPort = ""+serverSettings.serverPort;

        tvServerAddressDisplay.setText(serverIP+":"+serverPort);

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

        return super.onOptionsItemSelected(item);
    }
}
