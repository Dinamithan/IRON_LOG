package com.ironlog;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    static final String CH_REMINDER = "reminder";
    static final String EXTRA_DOW = "dow";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            scheduleAll(ctx);
            return;
        }
        ensureChannel(ctx);
        int dow = intent.getIntExtra(EXTRA_DOW, Calendar.MONDAY);

        Intent open = new Intent(ctx, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("IronLog")
            .setContentText("C'est l'heure de ta séance !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true);

        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE))
            .notify(dow, nb.build());

        scheduleOne(ctx, dow);
    }

    static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CH_REMINDER, "Rappels séance", NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
        }
    }

    static void scheduleAll(Context ctx) {
        Store store = new Store(ctx);
        if (!store.remindersEnabled()) {
            cancelAll(ctx);
            return;
        }
        for (int dow : store.reminderDays()) scheduleOne(ctx, dow);
    }

    static void scheduleOne(Context ctx, int dow) {
        Store store = new Store(ctx);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, store.reminderHour());
        cal.set(Calendar.MINUTE, store.reminderMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, dow);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 60_000)
            cal.add(Calendar.WEEK_OF_YEAR, 1);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingFor(ctx, dow));
        else
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingFor(ctx, dow));
    }

    static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (int dow = Calendar.SUNDAY; dow <= Calendar.SATURDAY; dow++)
            am.cancel(pendingFor(ctx, dow));
    }

    private static PendingIntent pendingFor(Context ctx, int dow) {
        Intent i = new Intent(ctx, ReminderReceiver.class).putExtra(EXTRA_DOW, dow);
        return PendingIntent.getBroadcast(ctx, dow, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
