package com.example.goldsignal;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PriceFeed {

    public interface Listener {
        void onPrice(double price, long timestamp, String source);
        void onError(String message);
    }

    private static final String LBANK_URL =
        "https://api.lbkex.com/v2/ticker.do?symbol=paxg_usdt";

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;

    private boolean running = false;
    private long intervalMs = 10000L;

    public PriceFeed(Listener listener) {
        this.listener = listener;
    }

    public void setInterval(long ms) { this.intervalMs = ms; }

    public void start() {
        if (running) return;
        running = true;
        fetch();
    }

    public void stop() {
        running = false;
        main.removeCallbacksAndMessages(null);
    }

    private void fetch() {
        if (!running) return;

        Request req = new Request.Builder()
            .url(LBANK_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
            .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {
                main.post(() -> listener.onError("اتصال: " + e.getMessage()));
                next();
            }

            @Override public void onResponse(Call c, Response r) throws IOException {
                try {
                    String body = r.body() != null ? r.body().string() : "";
                    JSONObject j = new JSONObject(body);
                    JSONArray data = j.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        JSONObject ticker = data.getJSONObject(0).getJSONObject("ticker");
                        double price = Double.parseDouble(ticker.getString("latest"));
                        main.post(() -> listener.onPrice(price, System.currentTimeMillis(), "LBank"));
                    } else {
                        main.post(() -> listener.onError("پاسخ نامعتبر"));
                    }
                } catch (Exception e) {
                    main.post(() -> listener.onError("parse"));
                }
                next();
            }
        });
    }

    private void next() {
        if (!running) return;
        main.postDelayed(this::fetch, intervalMs);
    }
}
