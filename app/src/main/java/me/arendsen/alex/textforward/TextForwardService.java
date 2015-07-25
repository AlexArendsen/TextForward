package me.arendsen.alex.textforward;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by copper on 7/25/15.
 */
public class TextForwardService extends Service {

    Server textForwardServer;
    IntentFilter smsIntentFilter;

    public static final String LOG_TAG = "TextForwardService";

    private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG,"onReceive");
            if(textForwardServer!=null && textForwardServer.isReady()) {
                try {
                    textForwardServer.send(
                            intent.getStringExtra("sms-message"),
                            intent.getStringExtra("sms-sender")
                    );
                } catch (Exception e) {
                    Log.e(LOG_TAG,"Unexpected error while sending message to server: "+e);
                    e.printStackTrace();
                    stopSelf();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(LOG_TAG,"onCreate");

        textForwardServer = null;

        smsIntentFilter = new IntentFilter();
        smsIntentFilter.addAction("SMS_RECEIVED_ACTION");
        registerReceiver(smsIntentReceiver, smsIntentFilter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG,"onStartCommand");

        /* Called every time startService is called. */
        if(textForwardServer==null && intent!=null) {
            String serverIP = intent.getStringExtra("serverIP");
            int serverPort = intent.getIntExtra("serverPort",-1);

            try {
                textForwardServer = new Server(serverIP,serverPort);
                Log.i(LOG_TAG,"Starting TextForward service on "+serverIP+":"+serverPort);
            } catch (Exception e) {
                Log.e(LOG_TAG,"Error while connecting to TextForward server: "+e);
                e.printStackTrace();
                stopSelf();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG,"onDestroy");
        unregisterReceiver(smsIntentReceiver);
        textForwardServer.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TextForwardService is started, not bound
        return null;
    }
}