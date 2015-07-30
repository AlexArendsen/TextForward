package me.arendsen.alex.textforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by copper on 7/28/15.
 */
public class BadgerMessageBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "BadgerMessageRecvSuper";
    protected HashMap<String, OnBadgerMessageListener> commandLookup = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String messageSrc = intent.getStringExtra("badger-message");
        if(messageSrc!=null) {
            BadgerMessage message = new BadgerMessage(messageSrc);

            if(message.getCommand().equals("MALFORMED")) {
                Log.w(LOG_TAG,"Malformed message");
            }

            if(commandLookup.containsKey(message.getCommand())){
                commandLookup.get(message.getCommand()).onMessage(message);
            }
        } else {
            Log.e(LOG_TAG,"No message set");
        }
    }

    public boolean on(String command, OnBadgerMessageListener listener) {
        if(!commandLookup.containsKey(command)) {
            commandLookup.put(command, listener);
            return true;
        } else {
            Log.w(LOG_TAG, "Tried to assign overwrite callback for command " + command);
            return false;
        }
    }
}
