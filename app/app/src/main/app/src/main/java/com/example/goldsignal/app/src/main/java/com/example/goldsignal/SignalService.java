package com.example.goldsignal;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class SignalService extends Service implements PriceFeed.Listener {

    public static final String ACTION_START = "START";
    public static final String ACTION_STOP  = "STOP";
    public static final String CH_SIGNAL     = "gold_signal";

    private static final int ID_FG = 1001;
    private static final int ID_SIGNAL = 1002;

    private PriceFeed feed;
    private final SignalEngine engine = new SignalEngine();

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopEverything();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            createChannel();
            startForeground(ID_FG, buildNotif("Gold Signal فعال", "در حال پایش طلا...", false));
            engine.reset();
            if (feed != null) feed.stop();
            feed = new PriceFeed(this);
            feed.setInterval(10000);
            feed.start();
        }
        return START_STICKY;
    }

    @Override
    public void onPrice(double price, long ts, String source) {
        Signal s = engine.evaluate(price);
        if (s == null) return;
        if (s.type == Signal.Type.CALL || s.type == Signal.Type.PUT) {
            SignalHistory.add(this, s);
            notifySignal(s, source);
        }
    }

    @Override
    public void onError(String msg) {}

    private void notifySignal(Signal s, String source) {
        boolean isCall = s.type == Signal.Type.CALL;
        String title = isCall ? "CALL — خرید" : "PUT — فروش";
        String body  = String.format("%s @ %.2f  (%s)", s.type.name(), s.price, source);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_SIGNAL, buildNotif(title, body, true));

        vibrate(isCall);
        playSound();
    }

    private Notification buildNotif(String title, String text, boolean alert) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CH_SIGNAL);
        } else {
            b = new Notification.Builder(this);
        }

        b.setSmallIcon(R.drawable.ic_signal)
         .setContentTitle(title)
         .setContentText(text)
         .setAutoCancel(true);

        if (alert) {
            b.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        }
        return b.build();
    }

    private void vibrate(boolean isCall) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        long[] pattern = isCall ? new long[]{0, 300, 150, 300} : new long[]{0, 700};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            v.vibrate(pattern, -1);
        }
    }

    private void playSound() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(this, uri);
            if (r != null) r.play();
        } catch (Exception ignored) {}
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
            CH_SIGNAL, "هشدار سیگنال",
            NotificationManager.IMPORTANCE_HIGH);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    private void stopEverything() {
        if (feed != null) { feed.stop(); feed = null; }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (feed != null) feed.stop();
    }
}
