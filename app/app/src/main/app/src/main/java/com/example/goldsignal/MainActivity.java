package com.example.goldsignal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.List;

public class MainActivity extends Activity {

    private TextView statusView;
    private Button startBtn, stopBtn;
    private LinearLayout historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        requestNotifPermission();
        refreshHistory();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(40, 60, 40, 40);

        TextView title = new TextView(this);
        title.setText("GOLD SIGNAL");
        title.setTextSize(32);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, 80));

        TextView sub = new TextView(this);
        sub.setText("هشدار طلا — رایگان");
        sub.setTextSize(12);
        sub.setTextColor(Color.GRAY);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, lp(-1, 60));

        startBtn = new Button(this);
        startBtn.setText("▶ شروع پایش");
        startBtn.setTextSize(16);
        startBtn.setOnClickListener(v -> startService());
        root.addView(startBtn, lp(-1, 80));

        stopBtn = new Button(this);
        stopBtn.setText("■ توقف");
        stopBtn.setTextSize(16);
        stopBtn.setEnabled(false);
        stopBtn.setOnClickListener(v -> stopService());
        root.addView(stopBtn, lp(-1, 80));

        statusView = new TextView(this);
        statusView.setText("آماده");
        statusView.setGravity(Gravity.CENTER);
        statusView.setTextSize(13);
        statusView.setTextColor(Color.DKGRAY);
        statusView.setPadding(0, 20, 0, 20);
        root.addView(statusView, lp(-1, 60));

        View sep = new View(this);
        sep.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams sp = lp(-1, 1);
        sp.setMargins(0, 20, 0, 20);
        root.addView(sep, sp);

        TextView hTitle = new TextView(this);
        hTitle.setText("تاریخچه سیگنال‌ها");
        hTitle.setTextSize(15);
        hTitle.setTypeface(null, Typeface.BOLD);
        root.addView(hTitle, lp(-1, 50));

        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyList, lp(-1, -2));

        scroll.addView(root);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private void refreshHistory() {
        historyList.removeAllViews();
        List<SignalHistory.Item> items = SignalHistory.load(this);

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("هنوز سیگنالی ثبت نشده");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(12);
            empty.setTextColor(Color.GRAY);
            empty.setPadding(0, 20, 0, 20);
            historyList.addView(empty, lp(-1, 60));
            return;
        }

        for (SignalHistory.Item it : items) {
            TextView row = new TextView(this);
            row.setText(it.format());
            row.setTextSize(13);
            row.setPadding(15, 20, 15, 20);
            if ("CALL".equals(it.type)) row.setTextColor(Color.rgb(0, 130, 60));
            else row.setTextColor(Color.rgb(190, 30, 40));
            historyList.addView(row, lp(-1, -2));

            View line = new View(this);
            line.setBackgroundColor(Color.rgb(230, 230, 230));
            historyList.addView(line, lp(-1, 1));
        }
    }

    private void startService() {
        Intent i = new Intent(this, SignalService.class);
        i.setAction(SignalService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        statusView.setText("✅ فعال");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
    }

    private void stopService() {
        Intent i = new Intent(this, SignalService.class);
        i.setAction(SignalService.ACTION_STOP);
        startService(i);
        statusView.setText("متوقف شد");
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistory();
    }
}
