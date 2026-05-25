package com.smseditor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * REQUIRED for default SMS app.
 * Android will only grant default SMS status if the app declares
 * a receiver for SMS_DELIVER (and MMS_DELIVER for MmsReceiver).
 *
 * This receiver handles incoming SMS delivery to the inbox.
 * Without it Android won't let us be the default app.
 */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        StringBuilder body = new StringBuilder();
        String address = messages[0].getDisplayOriginatingAddress();
        long timestamp = messages[0].getTimestampMillis();

        for (SmsMessage msg : messages) {
            body.append(msg.getMessageBody());
        }

        // Write to inbox — this is the canonical way default SMS apps store messages
        SmsRepository.insert(
            context.getContentResolver(),
            address,
            body.toString(),
            1, // type = inbox
            timestamp
        );

        // Show a notification
        NotificationHelper.showIncoming(context, address, body.toString());
    }
}
