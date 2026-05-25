package com.smseditor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Placeholder service declared in manifest (required by some Android versions). */
public class HeadsUpService extends Service {
    @Override public IBinder onBind(Intent intent) { return null; }
}
