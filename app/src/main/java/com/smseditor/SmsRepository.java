package com.smseditor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

/**
 * All raw ContentProvider operations on content://sms.
 *
 * ══ HOW THIS WORKS WITHOUT ROOT ══════════════════════════════════════════════
 *
 * Android's TelephonyProvider (which backs content://sms) uses
 * permission checks at runtime:
 *
 *   • READ_SMS   → any app with this permission can query
 *   • WRITE_SMS  → ONLY the default SMS app can insert/update/delete
 *
 * The "default SMS app" protection is enforced in TelephonyProvider.java:
 *
 *     private boolean isSelf(int uid) { ... }
 *     private boolean isDefaultSmsApp(int callingUid) {
 *         return Telephony.Sms.getDefaultSmsPackage(mContext)
 *                .equals(getCallingPackageName());
 *     }
 *
 * So when the user sets our app as default, these operations succeed
 * without any root/system privileges.
 */
public class SmsRepository {

    private static final Uri SMS_URI = Uri.parse("content://sms");

    /**
     * Insert a new SMS into the database.
     *
     * @param type  1=received (inbox), 2=sent, 3=draft, 4=outbox
     */
    public static Uri insert(ContentResolver cr,
                             String address,
                             String body,
                             int type,
                             long dateMs) {
        ContentValues cv = new ContentValues();
        cv.put("address", address);
        cv.put("body",    body);
        cv.put("date",    dateMs);
        cv.put("type",    type);
        cv.put("read",    1);
        cv.put("seen",    1);
        return cr.insert(SMS_URI, cv);
    }

    /**
     * Update an existing SMS.
     *
     * @param smsId  the _id from content://sms
     * @return number of rows updated (0 = failed / not default app)
     */
    public static int update(ContentResolver cr,
                             long smsId,
                             String address,
                             String body,
                             long dateMs,
                             int type,
                             int read) {
        ContentValues cv = new ContentValues();
        cv.put("address", address);
        cv.put("body",    body);
        cv.put("date",    dateMs);
        cv.put("type",    type);
        cv.put("read",    read);
        return cr.update(
            Uri.parse("content://sms/" + smsId), cv, null, null
        );
    }

    /**
     * Delete an SMS by _id.
     *
     * @return number of rows deleted
     */
    public static int delete(ContentResolver cr, long smsId) {
        return cr.delete(
            Uri.parse("content://sms/" + smsId), null, null
        );
    }

    /**
     * Mark a message as read/unread.
     */
    public static void setRead(ContentResolver cr, long smsId, boolean read) {
        ContentValues cv = new ContentValues();
        cv.put("read", read ? 1 : 0);
        cr.update(Uri.parse("content://sms/" + smsId), cv, null, null);
    }
}
