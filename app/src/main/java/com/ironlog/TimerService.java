package com.ironlog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TimerService extends Service {

    static final String ACTION_START = "ironlog.TIMER_START";
    static final String ACTION_STOP  = "ironlog.TIMER_STOP";
    static final String ACTION_DONE  = "ironlog.TIMER_DONE";
    static final String EXTRA_SECONDS = "sec";

    private static final int NOTIF_ID = 77;
    private static final String CH_TIMER = "timer_rest";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable endTask;
    private long endTimeMs;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        int seconds = intent.getIntExtra(EXTRA_SECONDS, 120);
        endTimeMs = System.currentTimeMillis() + seconds * 1000L;

        ensureChannels();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, buildRestNotif(endTimeMs),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, buildRestNotif(endTimeMs));
        }

        if (endTask != null) handler.removeCallbacks(endTask);
        endTask = () -> {
            sendBroadcast(new Intent(ACTION_DONE));
            postDoneNotif();
            handler.postDelayed(this::stopSelf, 1500);
        };
        handler.postDelayed(endTask, seconds * 1000L);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (endTask != null) handler.removeCallbacks(endTask);
        NotificationManagerCompat.from(this).cancel(NOTIF_ID);
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }

    // Build the ongoing rest notification with native countdown
    private Notification buildRestNotif(long endMs) {
        Intent stopIntent = new Intent(this, TimerService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, WorkoutActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CH_TIMER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("IronLog — Repos")
            .setContentText("Prêt à repartir dans")
            .setWhen(endMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPi)
            .addAction(0, "Passer ›", stopPi)
            .build();
    }

    private void postDoneNotif() {
        NotificationManagerCompat.from(this).cancel(NOTIF_ID);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, "timer")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("IronLog")
            .setContentText("Repos terminé — c'est reparti !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify(42, nb.build());
    }

    private void ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            // Low-importance channel for the ongoing countdown (no sound)
            nm.createNotificationChannel(new NotificationChannel(
                CH_TIMER, "Timer repos", NotificationManager.IMPORTANCE_LOW));
            // High-importance channel for "rest done" notification
            nm.createNotificationChannel(new NotificationChannel(
                "timer", "Fin de repos", NotificationManager.IMPORTANCE_HIGH));
        }
    }
}
