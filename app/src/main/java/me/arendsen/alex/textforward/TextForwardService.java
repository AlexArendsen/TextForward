package me.arendsen.alex.textforward;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by copper on 7/25/15.
 */
public class TextForwardService extends Service {

    /*Constants*/
    public static final String LOG_TAG = "TextForwardService";
    public static final String[] CONTACT_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                    ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                    : ContactsContract.Contacts.DISPLAY_NAME
    };
    public static final int CONTACT_PROJECTION_COLUMN_ID = 0;
    public static final int CONTACT_PROJECTION_COLUMN_KEY = 1;

    /* Fields */
    SSLSocket sock;
    BufferedWriter out;
    BufferedReader in;
    String host;
    int port;
    boolean started;
    Thread listenerThread;
    IntentFilter smsIntentFilter, badgerMessageIntentFilter;
    private BadgerMessageBroadcastReceiver badgerMessageReceiver = new BadgerMessageBroadcastReceiver();

    private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG,"onReceive");

            if(isReady()) {

                HashMap<String, String> headers = new HashMap<>();
                headers.put("sender",intent.getStringExtra("sms-sender"));
                BadgerMessage message = new BadgerMessage("SMS_NEW",headers,intent.getStringExtra("sms-message"));

                new SendMessageTask().execute(new MessageRequest(out, message));
            } else {
                Log.w(LOG_TAG, "Tried to send SMS notification but socket was not ready.");
            }

        }
    };

    /* Hook Overrides */
    @Override
    public void onCreate() {
        Log.d(LOG_TAG,"onCreate");

        this.started = false;

        // Set up SMS receiver (sends notifications when a new text arrives)
        smsIntentFilter = new IntentFilter();
        smsIntentFilter.addAction("SMS_RECEIVED_ACTION");
        registerReceiver(smsIntentReceiver, smsIntentFilter);

        // Set up Badger Message receiver (when client or server sends a Badger message)
        badgerMessageIntentFilter = new IntentFilter();
        badgerMessageIntentFilter.addAction("BADGER_RECEIVED_ACTION");
        registerReceiver(badgerMessageReceiver, badgerMessageIntentFilter);

        // Apply Badger message bindings

        badgerMessageReceiver.on("GET_MESSAGES", new OnBadgerMessageListener() {
            @Override
            public void onMessage(BadgerMessage message) {
                Log.d(LOG_TAG, "Message dump request received");
                new SendMessageTask().execute(new MessageRequest(out, new BadgerMessage("NOT_IMPLEMENTED", null, null)));
            }
        });

        badgerMessageReceiver.on("SMS_SEND", new OnBadgerMessageListener() {
            @Override
            public void onMessage(BadgerMessage message) {
                String number = message.getHeader("to");
                String msg = message.getMessage();

                if(number!=null && msg!=null) {
                    Log.d(LOG_TAG, "Sending message on behalf of agent...");
                    SmsManager manager = SmsManager.getDefault();
                    manager.sendTextMessage(number, null, msg, null, null);
                } else {
                    Log.e(LOG_TAG, "Invalid sender or message");
                }

            }
        });

        badgerMessageReceiver.on("GET_CONTACTS", new OnBadgerMessageListener() {
            @Override
            public void onMessage(BadgerMessage message) {

            }
        });

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        if(!this.started) {
            this.started = true;
            this.host = intent.getStringExtra("serverIP");
            this.port = intent.getIntExtra("serverPort", -1);

            // Create Socket
            SSLConnectionInitializationTask task = new SSLConnectionInitializationTask();
            task.execute(this);
            try {
                if(!task.get()) {
                    Log.e(LOG_TAG, "Failed to start socket.");
                    stopSelf();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while waiting for socket task");
                e.printStackTrace();
                stopSelf();
            }

            // Create + start listening thread
            this.listenerThread = new Thread(new Runnable() {

                private static final String LOG_TAG = "TextForwardListenThread";

                @Override
                public void run() {
                    String m;
                    try {
                        String digest = "";
                        while((m=in.readLine())!=null) {
                            // TODO -- Rewrite to consume percent symbol
                            if(!m.equals("%")) {
                                digest += m+"\n";
                            } else {

                                Log.d(LOG_TAG, "--- Received message\n"+digest);

                                // TODO -- Check if message should be forwarded, or should be handled by service.
                                Intent broadcastIntent = new Intent();
                                broadcastIntent.setAction("BADGER_RECEIVED_ACTION");
                                broadcastIntent.putExtra("badger-message",digest);
                                Context ctx = getApplicationContext();
                                ctx.sendBroadcast(broadcastIntent);

                                digest = "";
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception in listener thread: "+e);
                        e.printStackTrace();
                    }
                }
            });

            this.listenerThread.start();

            // Send initial BADGER CONNECT message
            if(this.isReady()) {
                String password = intent.getStringExtra("password");

                HashMap<String, String> headers = new HashMap<>();
                headers.put("type", "provider");
                headers.put("password", password);
                headers.put("handle", "paranoid");

                new SendMessageTask().execute(new MessageRequest(this.out, new BadgerMessage("CONNECT",headers,"")));
            }
        } else {
            Log.w(LOG_TAG, "Tried to start TextForwardService, but was already running");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        unregisterReceiver(smsIntentReceiver);
        unregisterReceiver(badgerMessageReceiver);
        new SendMessageTask().execute(new MessageRequest(out,new BadgerMessage("DISCONNECT",null,null)));
        try {
            this.out.close();
            this.sock.close();
        } catch(Exception e) {
            Log.i(LOG_TAG, "Failed to close network resources onDestroy. Oh well, I guess.");
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TextForwardService is started, not bound
        return null;
    }

    // TODO -- Change to isWriteReady
    public boolean isReady() {
        return this.sock != null && this.out != null;
    }

    private class MessageRequest {
        public BufferedWriter out;
        public BadgerMessage message;

        MessageRequest(BufferedWriter out, BadgerMessage message) {
            this.out = out;
            this.message = message;
        }
    }

    /* Classes and Interfaces */
    private class SendMessageTask extends AsyncTask<MessageRequest, Void, Boolean> {

        private static final String LOG_TAG = "TextForwardSendTask";

        @Override
        protected Boolean doInBackground(MessageRequest... params) {
            if(params.length != 1) {
                Log.e(LOG_TAG, "More or less than one message request provided");
                return false;
            }

            BufferedWriter out = params[0].out;
            BadgerMessage message = params[0].message;

            try {
                out.write(message.toString());
                out.flush();
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to send message");
                e.printStackTrace();
                return false;
            }
        }
    }

    private class SSLConnectionInitializationTask extends AsyncTask<TextForwardService, Void, Boolean> {

        private static final String LOG_TAG = "SSLConnectionTask";

        @Override
        protected Boolean doInBackground(TextForwardService... params) {
            if (params.length != 1) {
                Log.e(LOG_TAG, "More or less than one service instance provided");
                return false;
            }

            TextForwardService serv = params[0];

            try {
                String host = serv.host;
                int port = serv.port;
                serv.sock = makeInsecureSocket(host, port);
                serv.out = new BufferedWriter(new OutputStreamWriter(serv.sock.getOutputStream()));
                serv.in = new BufferedReader(new InputStreamReader(serv.sock.getInputStream()));
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while initializing connection: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        // TODO -- Add secure connection method, figure out how to add cert to trust store

        private SSLSocket makeInsecureSocket(String host, int port) throws Exception {

            // Disable certificate verification
            TrustManager[] allCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {/*No op*/}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {/*No op*/}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {return null;}
                    }
            };

            // Install Trust Manager
            SSLContext sslctx = SSLContext.getInstance("SSL");
            sslctx.init(null, allCerts, new java.security.SecureRandom());

            SSLSocketFactory sockFactory = sslctx.getSocketFactory();
            SSLSocket out = (SSLSocket) sockFactory.createSocket(host, port);

            Log.d(LOG_TAG, "Socket created");

            return out;
        }
    }

}