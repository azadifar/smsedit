package com.smseditor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * REQUIRED for default SMS app status.
 * We must declare this even if we don't fully handle MMS.
 * Without it Android won't let us register as the default SMS app.
 */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // MMS handling placeholder
        // A full implementation would parse the WAP push PDU here
        // and write to content://mms
    }
}
