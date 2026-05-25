package com.smseditor;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Edit an existing SMS message.
 *
 * Fields editable:
 *  - body (text)
 *  - address (sender/recipient number)
 *  - date (timestamp → shown as date + time pickers)
 *  - read status (0 = unread, 1 = read)
 *  - type (1=received, 2=sent, 3=draft, 4=outbox, 5=failed, 6=queued)
 *
 * This works WITHOUT root because:
 *   Android grants WRITE to content://sms only when the calling app
 *   is the default SMS application. The user has set us as default.
 */
public class EditSmsActivity extends AppCompatActivity {

    private long smsId;
    private EditText etBody, etAddress;
    private TextView tvDate, tvTime;
    private Spinner spType, spRead;
    private long currentTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sms);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("ویرایش پیام");

        etBody    = findViewById(R.id.et_body);
        etAddress = findViewById(R.id.et_address);
        tvDate    = findViewById(R.id.tv_date);
        tvTime    = findViewById(R.id.tv_time);
        spType    = findViewById(R.id.sp_type);
        spRead    = findViewById(R.id.sp_read);

        smsId = getIntent().getLongExtra("sms_id", -1);
        if (smsId == -1) { finish(); return; }

        loadSms(smsId);
        setupDateTimePickers();

        findViewById(R.id.btn_save).setOnClickListener(v -> saveSms());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    // ─── Load from content://sms ──────────────────────────────────────────────

    private void loadSms(long id) {
        Cursor c = getContentResolver().query(
            Uri.parse("content://sms/" + id),
            new String[]{"_id", "address", "body", "date", "read", "type"},
            null, null, null
        );

        if (c == null || !c.moveToFirst()) {
            Toast.makeText(this, "پیام یافت نشد", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String address = c.getString(c.getColumnIndexOrThrow("address"));
        String body    = c.getString(c.getColumnIndexOrThrow("body"));
        long date      = c.getLong(c.getColumnIndexOrThrow("date"));
        int read       = c.getInt(c.getColumnIndexOrThrow("read"));
        int type       = c.getInt(c.getColumnIndexOrThrow("type"));
        c.close();

        etBody.setText(body);
        etAddress.setText(address);
        currentTimestamp = date;
        updateDateTimeDisplay(date);

        // Type spinner: 0=Inbox(1), 1=Sent(2), 2=Draft(3), 3=Outbox(4)
        int typePos = type > 0 ? type - 1 : 0;
        if (typePos < spType.getCount()) spType.setSelection(typePos);

        // Read spinner: 0=خوانده شده, 1=خوانده نشده
        spRead.setSelection(read == 1 ? 0 : 1);
    }

    // ─── Date & Time Pickers ─────────────────────────────────────────────────

    private void setupDateTimePickers() {
        tvDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(currentTimestamp);
            new android.app.DatePickerDialog(this,
                (view, year, month, day) -> {
                    cal.set(year, month, day);
                    currentTimestamp = cal.getTimeInMillis();
                    updateDateTimeDisplay(currentTimestamp);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        tvTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(currentTimestamp);
            new android.app.TimePickerDialog(this,
                (view, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    currentTimestamp = cal.getTimeInMillis();
                    updateDateTimeDisplay(currentTimestamp);
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show();
        });
    }

    private void updateDateTimeDisplay(long ts) {
        Date d = new Date(ts);
        tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(d));
        tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d));
    }

    // ─── Save to content://sms ────────────────────────────────────────────────

    private void saveSms() {
        String body    = etBody.getText().toString();
        String address = etAddress.getText().toString().trim();

        if (body.isEmpty()) {
            etBody.setError("متن پیام نمی‌تواند خالی باشد");
            return;
        }

        // Type: spinner position 0→1(inbox), 1→2(sent), 2→3(draft), 3→4(outbox)
        int type = spType.getSelectedItemPosition() + 1;
        // Read: 0=خوانده شده(1), 1=خوانده نشده(0)
        int read = spRead.getSelectedItemPosition() == 0 ? 1 : 0;

        ContentValues cv = new ContentValues();
        cv.put("body",    body);
        cv.put("address", address);
        cv.put("date",    currentTimestamp);
        cv.put("type",    type);
        cv.put("read",    read);

        int rows = getContentResolver().update(
            Uri.parse("content://sms/" + smsId), cv, null, null
        );

        if (rows > 0) {
            Toast.makeText(this, "✓ پیام ذخیره شد", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this,
                "خطا در ذخیره — آیا اپ به عنوان پیام‌رسان پیش‌فرض تنظیم شده؟",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
