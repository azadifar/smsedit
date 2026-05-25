package com.smseditor;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsCursorAdapter extends CursorAdapter {

    private static final int[] AVATAR_COLORS = {
        0xFF9FE1CB, 0xFFB5D4F4, 0xFFF5C4B3,
        0xFFFAC775, 0xFFF4C0D1, 0xFFCECBF6
    };
    private static final int[] AVATAR_TEXT_COLORS = {
        0xFF085041, 0xFF0C447C, 0xFF4A1B0C,
        0xFF412402, 0xFF4B1528, 0xFF26215C
    };

    private final LayoutInflater inflater;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd", Locale.getDefault());

    public SmsCursorAdapter(Context ctx, Cursor c) {
        super(ctx, c, 0);
        inflater = LayoutInflater.from(ctx);
    }

    @Override
    public View newView(Context ctx, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.item_sms, parent, false);
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {
        TextView tvAvatar  = view.findViewById(R.id.tv_avatar);
        TextView tvAddress = view.findViewById(R.id.tv_address);
        TextView tvBody    = view.findViewById(R.id.tv_body);
        TextView tvDate    = view.findViewById(R.id.tv_date);
        TextView tvBadge   = view.findViewById(R.id.tv_type_badge);
        View unreadDot     = view.findViewById(R.id.unread_dot);

        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String body    = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        long   date    = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        int    read    = cursor.getInt(cursor.getColumnIndexOrThrow("read"));
        int    type    = cursor.getInt(cursor.getColumnIndexOrThrow("type"));

        // Address display
        String display = (address != null && !address.isEmpty()) ? address : "ناشناس";
        tvAddress.setText(display);
        tvBody.setText(body != null ? body : "");

        // Date: show time if today, else show date
        Date msgDate = new Date(date);
        Date now = new Date();
        String dateStr;
        if (isSameDay(msgDate, now)) {
            dateStr = sdf.format(msgDate);
        } else {
            dateStr = sdfDate.format(msgDate);
        }
        tvDate.setText(dateStr);

        // Avatar: first char of address, colored by hash
        String initial = (display.length() > 0)
            ? String.valueOf(display.charAt(0)).toUpperCase()
            : "?";
        if (initial.matches("[0-9+]")) initial = "📱"; // number — use phone icon char
        tvAvatar.setText(initial);
        int colorIdx = Math.abs(display.hashCode()) % AVATAR_COLORS.length;
        tvAvatar.setBackgroundDrawable(null);
        tvAvatar.setBackgroundColor(AVATAR_COLORS[colorIdx]);
        tvAvatar.setTextColor(AVATAR_TEXT_COLORS[colorIdx]);
        // Make it circular
        tvAvatar.setBackground(ctx.getDrawable(R.drawable.circle_avatar));
        // tint programmatically
        android.graphics.drawable.GradientDrawable circle =
            new android.graphics.drawable.GradientDrawable();
        circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circle.setColor(AVATAR_COLORS[colorIdx]);
        tvAvatar.setBackground(circle);
        tvAvatar.setTextColor(AVATAR_TEXT_COLORS[colorIdx]);

        // Type badge
        switch (type) {
            case 1: tvBadge.setText("دریافتی"); tvBadge.setVisibility(View.GONE); break;
            case 2: tvBadge.setText("ارسالی"); tvBadge.setVisibility(View.VISIBLE); break;
            case 3: tvBadge.setText("پیش‌نویس"); tvBadge.setVisibility(View.VISIBLE); break;
            default: tvBadge.setVisibility(View.GONE);
        }

        // Unread dot
        unreadDot.setVisibility(read == 0 ? View.VISIBLE : View.GONE);

        // Bold address if unread
        tvAddress.setTypeface(null, read == 0
            ? android.graphics.Typeface.BOLD
            : android.graphics.Typeface.NORMAL);
    }

    private boolean isSameDay(Date a, Date b) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return fmt.format(a).equals(fmt.format(b));
    }
}
