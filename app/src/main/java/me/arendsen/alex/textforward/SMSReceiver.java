package me.arendsen.alex.textforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Created by copper on 7/25/15.
 */
public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        SmsMessage[] messages;

        if(bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            messages = new SmsMessage[pdus.length];

            for(int i = 0; i<messages.length; ++i) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                String sms = "("+messages[i].getDisplayOriginatingAddress()+") "+messages[i].getMessageBody();

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("SMS_RECEIVED_ACTION");
                broadcastIntent.putExtra("sms-sender", messages[i].getDisplayOriginatingAddress());
                broadcastIntent.putExtra("sms-message", messages[i].getMessageBody());
                context.sendBroadcast(broadcastIntent);

            }
        } else {
            Log.i("SMSReceiver","Bundle was empty");
        }
    }
}
