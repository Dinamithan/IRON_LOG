package com.ironlog;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.ironlog.Ui.*;

public class MainActivity extends AppCompatActivity {

    private Store store;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ensureNotifChannels();
        requestNotifPermission();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        root = col(this);
        int p = dp(this, 16);
        root.setPadding(p, dp(this, 20), p, p);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        root.removeAllViews();

        // Header
        LinearLayout head = rowh(this);
        head.addView(tv(this, "IRON", 24, TEXT, true));
        head.addView(tv(this, "LOG", 24, ACCENT, true));
        head.addView(hSpace());
        Button bell = button(this, "🔔", false);
        bell.setPadding(dp(this, 10), dp(this, 8), dp(this, 10), dp(this, 8));
        bell.setOnClickListener(v -> reminderDialog());
        head.addView(bell);
        root.addView(head);

        int wk = store.sessionsThisWeek();
        TextView sub = tv(this, wk + " séance" + (wk > 1 ? "s" : "") + " cette semaine", 13, MUTED, false);
        sub.setPadding(0, dp(this, 4), 0, dp(this, 12));
        root.addView(sub);

        // Active program row
        List<Models.Program> progs = store.getPrograms();
        int progIdx = Math.min(store.activeProgram(), progs.size() - 1);
        Models.Program prog = progs.get(progIdx);

        LinearLayout progRow = rowh(this);
        TextView progName = tv(this, prog.name, 13, TEXT, true);
        progRow.addView(progName, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button manageBtn = button(this, "Gérer", false);
        manageBtn.setOnClickListener(v -> startActivity(new Intent(this, ProgramsActivity.class)));
        progRow.addView(manageBtn);
        LinearLayout.LayoutParams prlp = lp(MATCH, WRAP);
        prlp.bottomMargin = dp(this, 12);
        root.addView(progRow, prlp);

        Button hist = button(this, "Voir l'historique", false);
        hist.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams hlp = lp(MATCH, WRAP);
        hlp.bottomMargin = dp(this, 16);
        root.addView(hist, hlp);

        TextView lbl = tv(this, "CHOISIS TA SÉANCE", 13, MUTED, true);
        lbl.setPadding(dp(this, 2), 0, 0, dp(this, 10));
        root.addView(lbl);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM", Locale.FRENCH);
        for (int i = 0; i < prog.days.size(); i++) {
            final Models.Day d = prog.days.get(i);
            LinearLayout card = col(this);
            card.setBackground(round(CARD, 14, BORDER, 1, this));
            int cp = dp(this, 15);
            card.setPadding(cp, cp, cp, cp);

            card.addView(tv(this, d.name, 16, TEXT, true));
            Long ld = store.lastDone(d.name);
            String when = (ld == null) ? "Jamais faite" : "Dernière fois : " + sdf.format(new Date(ld));
            card.addView(tv(this, d.ex.size() + " exercices · " + when, 12.5f, MUTED, false));

            final int idx = i;
            card.setOnClickListener(v -> {
                Intent it = new Intent(this, WorkoutActivity.class);
                it.putExtra("day", idx);
                startActivity(it);
            });

            LinearLayout.LayoutParams clp = lp(MATCH, WRAP);
            clp.bottomMargin = dp(this, 11);
            root.addView(card, clp);
        }

        if (prog.days.isEmpty()) {
            TextView empty = tv(this, "Ce programme n'a pas encore de séances.\nAppuie sur Gérer pour en ajouter.", 14, MUTED, false);
            empty.setPadding(0, dp(this, 20), 0, 0);
            root.addView(empty);
        }
    }

    // ---------------- reminder dialog ----------------

    private void reminderDialog() {
        final Set<Integer> selDays = new HashSet<>(store.reminderDays());
        final boolean[] enabled = {store.remindersEnabled()};

        LinearLayout box = col(this);
        int p = dp(this, 16);
        box.setPadding(p, p, p, p);

        LinearLayout enableRow = rowh(this);
        enableRow.addView(tv(this, "Rappels actifs", 14, TEXT, false),
            new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button toggleBtn = button(this, enabled[0] ? "ON" : "OFF", enabled[0]);
        toggleBtn.setOnClickListener(v -> {
            enabled[0] = !enabled[0];
            toggleBtn.setText(enabled[0] ? "ON" : "OFF");
            toggleBtn.setBackground(round(enabled[0] ? ACCENT : CARD, 10, BORDER, enabled[0] ? 0 : 1, this));
            toggleBtn.setTextColor(enabled[0] ? DARKTXT : TEXT);
        });
        enableRow.addView(toggleBtn);
        LinearLayout.LayoutParams erlp = lp(MATCH, WRAP);
        erlp.bottomMargin = dp(this, 14);
        box.addView(enableRow, erlp);

        TextView daysLabel = tv(this, "Jours", 13, MUTED, true);
        daysLabel.setPadding(0, 0, 0, dp(this, 8));
        box.addView(daysLabel);
        LinearLayout dayRow = rowh(this);
        String[] dayLabels = {"Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"};
        for (int i = 0; i < 7; i++) {
            final int dow = i + 1;
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(dayLabels[i]);
            b.setTextSize(11);
            boolean on = selDays.contains(dow);
            b.setBackground(round(on ? ACCENT : CARD2, 8, BORDER, 1, this));
            b.setTextColor(on ? DARKTXT : TEXT);
            b.setPadding(0, dp(this, 8), 0, dp(this, 8));
            b.setOnClickListener(v -> {
                if (selDays.contains(dow)) selDays.remove(dow);
                else selDays.add(dow);
                boolean s = selDays.contains(dow);
                b.setBackground(round(s ? ACCENT : CARD2, 8, BORDER, 1, this));
                b.setTextColor(s ? DARKTXT : TEXT);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            if (i < 6) lp.rightMargin = dp(this, 3);
            dayRow.addView(b, lp);
        }
        LinearLayout.LayoutParams drlp = lp(MATCH, WRAP);
        drlp.bottomMargin = dp(this, 14);
        box.addView(dayRow, drlp);

        TextView timeLabel = tv(this, "Heure du rappel", 13, MUTED, true);
        timeLabel.setPadding(0, 0, 0, dp(this, 8));
        box.addView(timeLabel);
        LinearLayout timeRow = rowh(this);
        EditText hourEt = numField("H");
        hourEt.setText(String.valueOf(store.reminderHour()));
        EditText minEt = numField("M");
        minEt.setText(String.format(Locale.US, "%02d", store.reminderMinute()));
        timeRow.addView(hourEt, new LinearLayout.LayoutParams(dp(this, 64), WRAP));
        timeRow.addView(tv(this, "  :  ", 20, TEXT, true));
        timeRow.addView(minEt, new LinearLayout.LayoutParams(dp(this, 64), WRAP));
        box.addView(timeRow);

        new AlertDialog.Builder(this)
            .setTitle("Rappels d'entraînement")
            .setView(box)
            .setPositiveButton("Enregistrer", (d, w) -> {
                int h = parseNum(hourEt.getText().toString());
                int m = parseNum(minEt.getText().toString());
                store.setRemindersEnabled(enabled[0]);
                store.setReminderDays(selDays);
                store.setReminderHour(Math.max(0, Math.min(23, h)));
                store.setReminderMinute(Math.max(0, Math.min(59, m)));
                ReminderReceiver.scheduleAll(this);
                Toast.makeText(this, enabled[0] ? "Rappels activés" : "Rappels désactivés", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ---------------- helpers ----------------

    private void ensureNotifChannels() {
        ReminderReceiver.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                "timer", "Fin de repos", android.app.NotificationManager.IMPORTANCE_HIGH);
            ((android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
        }
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private View hSpace() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private EditText numField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(16);
        e.setGravity(Gravity.CENTER);
        e.setBackground(round(CARD2, 9, BORDER, 1, this));
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setPadding(dp(this, 6), dp(this, 10), dp(this, 6), dp(this, 10));
        return e;
    }

    private static int parseNum(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
