package com.smseditor;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ComposeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("پیام جدید");

        EditText etTo  = findViewById(R.id.et_to);
        EditText etMsg = findViewById(R.id.et_message);
        Button btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(v -> {
            String to  = etTo.getText().toString().trim();
            String msg = etMsg.getText().toString().trim();

            if (to.isEmpty()) { etTo.setError("شماره گیرنده را وارد کنید"); return; }
            if (msg.isEmpty()) { etMsg.setError("متن پیام را وارد کنید"); return; }

            try {
                SmsManager.getDefault().sendTextMessage(to, null, msg, null, null);
                // Also save to sent box
                SmsRepository.insert(getContentResolver(), to, msg, 2, System.currentTimeMillis());
                Toast.makeText(this, "پیام ارسال شد", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "خطا در ارسال: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
