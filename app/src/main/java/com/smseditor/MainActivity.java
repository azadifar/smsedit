package com.smseditor;

import android.app.role.RoleManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Main inbox. Lists all SMS threads.
 * Key insight: being the DEFAULT SMS app gives WRITE_SMS access without root.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DEFAULT_SMS = 101;
    private ListView listView;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.list_sms);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Cursor c = (Cursor) parent.getItemAtPosition(position);
            long smsId = c.getLong(c.getColumnIndexOrThrow("_id"));
            openEdit(smsId);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Cursor c = (Cursor) parent.getItemAtPosition(position);
            long smsId = c.getLong(c.getColumnIndexOrThrow("_id"));
            confirmDelete(smsId);
            return true;
        });

        checkDefaultSmsApp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    // ─── Check & Request Default SMS App ─────────────────────────────────────

    private void checkDefaultSmsApp() {
        if (!isDefaultSmsApp()) {
            new AlertDialog.Builder(this)
                .setTitle("تنظیم به عنوان پیام‌رسان پیش‌فرض")
                .setMessage("برای ویرایش پیام‌ها، این اپ باید به عنوان پیام‌رسان پیش‌فرض تنظیم شود. بدون root کار می‌کند.")
                .setPositiveButton("تنظیم", (d, w) -> requestDefaultSmsApp())
                .setNegativeButton("بعداً", null)
                .show();
        }
    }

    private boolean isDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = getSystemService(RoleManager.class);
            return rm.isRoleHeld(RoleManager.ROLE_SMS);
        }
        String def = Telephony.Sms.getDefaultSmsPackage(this);
        return getPackageName().equals(def);
    }

    private void requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = getSystemService(RoleManager.class);
            Intent intent = rm.createRequestRoleIntent(RoleManager.ROLE_SMS);
            startActivityForResult(intent, REQUEST_DEFAULT_SMS);
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, REQUEST_DEFAULT_SMS);
        }
    }

    // ─── Load SMS Messages ────────────────────────────────────────────────────

    private void loadMessages() {
        if (cursor != null) cursor.close();

        Uri uri = Uri.parse("content://sms");
        cursor = getContentResolver().query(
            uri,
            new String[]{"_id", "address", "body", "date", "read", "type"},
            null, null,
            "date DESC"
        );

        if (cursor == null || cursor.getCount() == 0) {
            showEmpty();
            return;
        }

        String[] from = {"address", "body", "date"};
        int[] to = {R.id.tv_address, R.id.tv_body, R.id.tv_date};

        SmsCursorAdapter adapter = new SmsCursorAdapter(this, cursor);
        listView.setAdapter(adapter);
        listView.setVisibility(View.VISIBLE);
        findViewById(R.id.tv_empty).setVisibility(View.GONE);
    }

    private void showEmpty() {
        listView.setVisibility(View.GONE);
        findViewById(R.id.tv_empty).setVisibility(View.VISIBLE);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void openEdit(long smsId) {
        Intent i = new Intent(this, EditSmsActivity.class);
        i.putExtra("sms_id", smsId);
        startActivity(i);
    }

    private void confirmDelete(long smsId) {
        new AlertDialog.Builder(this)
            .setTitle("حذف پیام")
            .setMessage("این پیام حذف شود؟")
            .setPositiveButton("حذف", (d, w) -> deleteSms(smsId))
            .setNegativeButton("انصراف", null)
            .show();
    }

    private void deleteSms(long smsId) {
        int rows = getContentResolver().delete(
            Uri.parse("content://sms/" + smsId), null, null
        );
        if (rows > 0) {
            Toast.makeText(this, "پیام حذف شد", Toast.LENGTH_SHORT).show();
            loadMessages();
        } else {
            Toast.makeText(this, "خطا: ابتدا اپ را به عنوان پیام‌رسان پیش‌فرض تنظیم کنید", Toast.LENGTH_LONG).show();
        }
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_compose) {
            startActivity(new Intent(this, ComposeActivity.class));
            return true;
        } else if (id == R.id.action_default) {
            requestDefaultSmsApp();
            return true;
        } else if (id == R.id.action_inject) {
            showInjectDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Inject a fake SMS - inserts directly into the SMS database.
     * Works without root when this app is the default SMS app.
     */
    private void showInjectDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_inject, null);
        new AlertDialog.Builder(this)
            .setTitle("افزودن پیام جدید")
            .setView(v)
            .setPositiveButton("افزودن", (d, w) -> {
                android.widget.EditText etAddr = v.findViewById(R.id.et_inject_address);
                android.widget.EditText etBody = v.findViewById(R.id.et_inject_body);
                android.widget.Spinner spType = v.findViewById(R.id.sp_inject_type);

                String address = etAddr.getText().toString().trim();
                String body = etBody.getText().toString().trim();
                int type = spType.getSelectedItemPosition() == 0 ? 1 : 2; // 1=inbox, 2=sent

                if (!address.isEmpty() && !body.isEmpty()) {
                    SmsRepository.insert(getContentResolver(), address, body, type,
                        System.currentTimeMillis());
                    Toast.makeText(this, "پیام افزوده شد", Toast.LENGTH_SHORT).show();
                    loadMessages();
                }
            })
            .setNegativeButton("انصراف", null)
            .show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQUEST_DEFAULT_SMS) {
            if (isDefaultSmsApp()) {
                Toast.makeText(this, "✓ اپ به عنوان پیش‌فرض تنظیم شد", Toast.LENGTH_SHORT).show();
                loadMessages();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) cursor.close();
    }
}
