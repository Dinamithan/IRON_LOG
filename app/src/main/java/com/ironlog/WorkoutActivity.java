package com.ironlog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.ironlog.Ui.*;

public class WorkoutActivity extends AppCompatActivity {

    private static final String TIMER_CHANNEL = "timer";

    private Store store;
    private Models.Day day;
    private int dayIndex;
    private LinearLayout content, restBar;
    private TextView restTime;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private int remaining;
    private long restEndTimeMs = 0;

    private final List<ExState> states = new ArrayList<>();

    private final BroadcastReceiver timerDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (TimerService.ACTION_DONE.equals(intent.getAction())) {
                if (ticker != null) handler.removeCallbacks(ticker);
                remaining = 0;
                restEndTimeMs = 0;
                paintRest();
                beep();
                handler.postDelayed(() -> restBar.setVisibility(View.GONE), 1500);
            }
        }
    };

    private static class SetRow {
        EditText w, reps;
        Button check;
        TextView no;
        GradientDrawable noDefaultBg;
        boolean done;
    }

    private static class ExState {
        Models.Ex ex;
        LinearLayout setsBox;
        TextView info;
        final List<SetRow> rows = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        dayIndex = getIntent().getIntExtra("day", 0);
        List<Models.Program> progs = store.getPrograms();
        Models.Program prog = progs.get(Math.min(store.activeProgram(), progs.size() - 1));
        dayIndex = Math.max(0, Math.min(dayIndex, prog.days.size() - 1));
        day = prog.days.get(dayIndex);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ensureTimerChannel();

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);

        ScrollView sv = new ScrollView(this);
        content = col(this);
        int p = dp(this, 16);
        content.setPadding(p, dp(this, 16), p, dp(this, 96));
        sv.addView(content);
        frame.addView(sv, new FrameLayout.LayoutParams(MATCH, MATCH));

        restBar = buildRestBar();
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(MATCH, WRAP);
        rlp.gravity = Gravity.BOTTOM;
        restBar.setLayoutParams(rlp);
        restBar.setVisibility(View.GONE);
        frame.addView(restBar);

        setContentView(frame);
        buildContent();
        restoreWorkoutState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(TimerService.ACTION_DONE);
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(timerDoneReceiver, filter, RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(timerDoneReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(timerDoneReceiver); } catch (Exception ignored) {}
        saveWorkoutState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticker != null) handler.removeCallbacks(ticker);
    }

    // ---------------- state persistence ----------------

    private void saveWorkoutState() {
        try {
            JSONObject state = new JSONObject();
            state.put("prog", store.activeProgram());
            state.put("day", dayIndex);
            if (restEndTimeMs > System.currentTimeMillis())
                state.put("rest_end", restEndTimeMs);
            JSONArray exArr = new JSONArray();
            for (ExState st : states) {
                JSONArray setArr = new JSONArray();
                for (SetRow sr : st.rows) {
                    JSONObject s = new JSONObject();
                    s.put("w", sr.w.getText().toString());
                    s.put("r", sr.reps.getText().toString());
                    s.put("done", sr.done);
                    setArr.put(s);
                }
                exArr.put(setArr);
            }
            state.put("ex", exArr);
            store.saveTempWorkout(state);
        } catch (Exception ignored) {}
    }

    private void restoreWorkoutState() {
        JSONObject state = store.getTempWorkout();
        if (state == null) return;
        try {
            if (state.getInt("prog") != store.activeProgram()) return;
            if (state.getInt("day") != dayIndex) return;

            // Restore rest timer if still active
            long endMs = state.optLong("rest_end", 0);
            if (endMs > System.currentTimeMillis()) {
                int sec = (int) ((endMs - System.currentTimeMillis()) / 1000);
                restEndTimeMs = endMs;
                remaining = sec;
                restBar.setVisibility(View.VISIBLE);
                paintRest();
                startLocalTicker();
                // Service should already be running; restart in case it was killed
                startTimerService(sec);
            }

            // Restore set data
            JSONArray exArr = state.getJSONArray("ex");
            for (int i = 0; i < Math.min(exArr.length(), states.size()); i++) {
                JSONArray setArr = exArr.getJSONArray(i);
                ExState st = states.get(i);
                while (st.rows.size() < setArr.length()) addSetRow(st);
                for (int j = 0; j < Math.min(setArr.length(), st.rows.size()); j++) {
                    JSONObject s = setArr.getJSONObject(j);
                    SetRow sr = st.rows.get(j);
                    sr.w.setText(s.optString("w", ""));
                    sr.reps.setText(s.optString("r", ""));
                    if (s.optBoolean("done", false) && !sr.done) {
                        sr.done = true;
                        sr.check.setBackground(round(GOOD, 10, GOOD, 1, this));
                        sr.check.setTextColor(DARKTXT);
                        sr.no.setBackground(ovalAccent());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // ---------------- build UI ----------------

    private void buildContent() {
        LinearLayout head = rowh(this);
        Button back = button(this, "‹ Retour", false);
        back.setBackgroundColor(0x00000000);
        back.setTextColor(ACCENT);
        back.setPadding(0, dp(this, 4), 0, dp(this, 4));
        back.setOnClickListener(v -> finish());
        head.addView(back);
        head.addView(space());
        Button setBtn = button(this, "⚙", false);
        setBtn.setOnClickListener(v -> settingsDialog());
        head.addView(setBtn);
        content.addView(head);

        TextView title = tv(this, day.name.toUpperCase(), 13, MUTED, true);
        title.setPadding(dp(this, 2), dp(this, 6), 0, dp(this, 10));
        content.addView(title);

        boolean first = true;
        for (Models.Ex ex : day.ex) {
            content.addView(exerciseCard(ex, first));
            first = false;
        }

        Button finish = button(this, "Terminer la séance", true);
        finish.setOnClickListener(v -> finishSession());
        LinearLayout.LayoutParams flp = lp(MATCH, WRAP);
        flp.topMargin = dp(this, 6);
        content.addView(finish, flp);
    }

    private View space() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private LinearLayout exerciseCard(final Models.Ex ex, boolean isFirst) {
        final ExState st = new ExState();
        st.ex = ex;
        states.add(st);

        LinearLayout card = col(this);
        card.setBackground(round(CARD, 14, BORDER, 1, this));
        int cp = dp(this, 14);
        card.setPadding(cp, cp, cp, cp);
        LinearLayout.LayoutParams clp = lp(MATCH, WRAP);
        clp.bottomMargin = dp(this, 13);
        card.setLayoutParams(clp);

        LinearLayout top = rowh(this);
        TextView nm = tv(this, ex.name, 15, TEXT, true);
        top.addView(nm, new LinearLayout.LayoutParams(0, WRAP, 1f));
        top.addView(tv(this, ex.scheme, 12, ACCENT, true));
        card.addView(top);

        st.info = tv(this, "", 12, MUTED, false);
        st.info.setPadding(0, dp(this, 2), 0, dp(this, 8));
        updateInfo(st);
        card.addView(st.info);

        LinearLayout chips = rowh(this);
        if (isFirst) {
            Button wu = chip("Échauffement");
            wu.setOnClickListener(v -> warmupDialog(ex, currentWeight(st)));
            chips.addView(wu);
        }
        Button pl = chip("Plaques");
        pl.setOnClickListener(v -> platesDialog(currentWeight(st)));
        chips.addView(pl);
        LinearLayout.LayoutParams chlp = lp(MATCH, WRAP);
        chlp.bottomMargin = dp(this, 6);
        card.addView(chips, chlp);

        st.setsBox = col(this);
        card.addView(st.setsBox);
        for (int i = 0; i < ex.sets(); i++) addSetRow(st);

        Button add = button(this, "+ série", false);
        add.setBackgroundColor(0x00000000);
        add.setTextColor(ACCENT);
        add.setPadding(0, dp(this, 8), 0, 0);
        add.setOnClickListener(v -> addSetRow(st));
        card.addView(add);

        return card;
    }

    private Button chip(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(TEXT);
        b.setBackground(round(CARD2, 8, BORDER, 1, this));
        b.setPadding(dp(this, 12), dp(this, 7), dp(this, 12), dp(this, 7));
        LinearLayout.LayoutParams p = lp(WRAP, WRAP);
        p.rightMargin = dp(this, 8);
        b.setLayoutParams(p);
        return b;
    }

    private void addSetRow(final ExState st) {
        final SetRow sr = new SetRow();
        final int num = st.rows.size() + 1;

        LinearLayout box = rowh(this);
        box.setPadding(0, dp(this, 6), 0, dp(this, 6));

        GradientDrawable nb = new GradientDrawable();
        nb.setShape(GradientDrawable.OVAL);
        nb.setColor(CARD2);

        TextView no = tv(this, String.valueOf(num), 11, MUTED, true);
        no.setGravity(Gravity.CENTER);
        no.setBackground(nb);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(dp(this, 22), dp(this, 22));
        nlp.rightMargin = dp(this, 6);
        box.addView(no, nlp);

        sr.no = no;
        sr.noDefaultBg = nb;

        Button minus = smallBtn("−");
        final EditText w = numEdit("kg");
        Button plus = smallBtn("+");
        double pre = preWeight(st);
        if (pre > 0) w.setText(Plates.trim(pre));
        minus.setOnClickListener(v -> adjust(w, -2.5));
        plus.setOnClickListener(v -> adjust(w, 2.5));

        box.addView(minus);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, WRAP, 1.3f);
        wlp.leftMargin = dp(this, 4);
        wlp.rightMargin = dp(this, 4);
        box.addView(w, wlp);
        box.addView(plus);

        final EditText reps = numEdit("reps");
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        rlp.leftMargin = dp(this, 6);
        rlp.rightMargin = dp(this, 6);
        box.addView(reps, rlp);

        final Button ck = smallBtn("✓");
        sr.check = ck;
        ck.setOnClickListener(v -> {
            sr.done = !sr.done;
            if (sr.done) {
                double ww = parse(w.getText().toString());
                double rr = parse(reps.getText().toString());
                if (ww > 0 && rr > 0) {
                    store.setLast(st.ex.name, ww, rr);
                    updateInfo(st);
                    int orm = Progression.oneRM(ww, rr);
                    if (orm > 0)
                        Toast.makeText(this, "1RM estimé ≈ " + orm + " kg", Toast.LENGTH_SHORT).show();
                }
                ck.setBackground(round(GOOD, 10, GOOD, 1, this));
                ck.setTextColor(DARKTXT);
                no.setBackground(ovalAccent());
                startRest(store.restDefault());
            } else {
                ck.setBackground(round(CARD2, 10, BORDER, 1, this));
                ck.setTextColor(TEXT);
                no.setBackground(nb);
            }
        });
        box.addView(ck);

        sr.w = w;
        sr.reps = reps;
        st.rows.add(sr);
        st.setsBox.addView(box);
    }

    private GradientDrawable ovalAccent() {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(ACCENT);
        return g;
    }

    private Button smallBtn(String t) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(t);
        b.setTextSize(16);
        b.setTextColor(TEXT);
        b.setBackground(round(CARD2, 10, BORDER, 1, this));
        b.setPadding(0, 0, 0, 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 42), dp(this, 44)));
        return b;
    }

    private EditText numEdit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(16);
        e.setGravity(Gravity.CENTER);
        e.setBackground(round(CARD2, 9, BORDER, 1, this));
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setPadding(dp(this, 6), dp(this, 10), dp(this, 6), dp(this, 10));
        return e;
    }

    private void adjust(EditText w, double delta) {
        double v = Math.max(0, parse(w.getText().toString()) + delta);
        w.setText(Plates.trim(v));
    }

    private double preWeight(ExState st) {
        double[] l = store.last(st.ex.name);
        if (l != null) return Progression.suggest(l[0], l[1], st.ex.topReps());
        return parse(st.ex.planned);
    }

    private double currentWeight(ExState st) {
        if (!st.rows.isEmpty()) {
            double w = parse(st.rows.get(0).w.getText().toString());
            if (w > 0) return w;
        }
        double[] l = store.last(st.ex.name);
        if (l != null) return l[0];
        return parse(st.ex.planned);
    }

    private void updateInfo(ExState st) {
        double[] l = store.last(st.ex.name);
        if (l == null) { st.info.setText("Première fois sur cet exo"); return; }
        double sug = Progression.suggest(l[0], l[1], st.ex.topReps());
        String s = "Dernière fois : " + Plates.trim(l[0]) + " kg × " + (int) l[1];
        if (sug > l[0]) s += "   →   essaie " + Plates.trim(sug) + " kg";
        st.info.setText(s);
    }

    // ---------------- rest timer ----------------

    private LinearLayout buildRestBar() {
        LinearLayout bar = col(this);
        bar.setBackgroundColor(BG2);
        int p = dp(this, 14);
        bar.setPadding(p, p, p, p);

        LinearLayout r = rowh(this);
        LinearLayout left = col(this);
        left.addView(tv(this, "REPOS", 11, MUTED, true));
        restTime = tv(this, "2:00", 30, TEXT, true);
        left.addView(restTime);
        r.addView(left);
        r.addView(space());

        Button skip = button(this, "Passer", true);
        skip.setOnClickListener(v -> stopRest());

        r.addView(skip);
        bar.addView(r);
        return bar;
    }

    private void startRest(int sec) {
        if (ticker != null) handler.removeCallbacks(ticker);
        remaining = sec;
        restEndTimeMs = System.currentTimeMillis() + sec * 1000L;
        restBar.setVisibility(View.VISIBLE);
        paintRest();
        startTimerService(sec);
        startLocalTicker();
    }

    private void startLocalTicker() {
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker = new Runnable() {
            @Override public void run() {
                if (remaining > 0) {
                    remaining--;
                    paintRest();
                }
                if (remaining > 0) handler.postDelayed(this, 1000);
                // When remaining reaches 0, the service broadcasts ACTION_DONE which we handle
            }
        };
        handler.postDelayed(ticker, 1000);
    }

    private void stopRest() {
        if (ticker != null) handler.removeCallbacks(ticker);
        restBar.setVisibility(View.GONE);
        restEndTimeMs = 0;
        stopTimerService();
    }

    private void startTimerService(int sec) {
        Intent i = new Intent(this, TimerService.class);
        i.setAction(TimerService.ACTION_START);
        i.putExtra(TimerService.EXTRA_SECONDS, sec);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private void stopTimerService() {
        Intent i = new Intent(this, TimerService.class);
        i.setAction(TimerService.ACTION_STOP);
        startService(i);
    }

    private void paintRest() {
        restTime.setText(fmt(Math.max(0, remaining)));
    }

    private static String fmt(int s) {
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }

    private void beep() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 350);
        } catch (Exception ignored) {}
        try {
            Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vb != null) {
                if (Build.VERSION.SDK_INT >= 26)
                    vb.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
                else
                    vb.vibrate(400);
            }
        } catch (Exception ignored) {}
    }

    private void ensureTimerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                TIMER_CHANNEL, "Fin de repos", NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    // ---------------- dialogs ----------------

    private void warmupDialog(Models.Ex ex, double workW) {
        List<String> ws = Progression.warmups(workW, store.bar());
        StringBuilder sb = new StringBuilder();
        for (String s : ws) sb.append("•  ").append(s).append("\n");
        String msg = sb.length() == 0 ? "Renseigne d'abord une charge de travail." : sb.toString().trim();
        new AlertDialog.Builder(this)
            .setTitle("Échauffement — " + ex.name).setMessage(msg).setPositiveButton("OK", null).show();
    }

    private void platesDialog(double pre) {
        final EditText in = numEdit("Poids cible (kg)");
        if (pre > 0) in.setText(Plates.trim(pre));
        LinearLayout box = col(this);
        int p = dp(this, 18);
        box.setPadding(p, p, p, p);
        box.addView(in);
        final TextView out = tv(this, "", 15, TEXT, false);
        out.setPadding(0, dp(this, 14), 0, 0);
        box.addView(out);
        Runnable calc = () -> out.setText(Plates.perSide(parse(in.getText().toString()), store.bar()));
        in.addTextChangedListener(new SimpleWatcher(calc));
        calc.run();
        new AlertDialog.Builder(this)
            .setTitle("Plaques (barre " + Plates.trim(store.bar()) + " kg)")
            .setView(box).setPositiveButton("OK", null).show();
    }

    private void settingsDialog() {
        LinearLayout box = col(this);
        int p = dp(this, 18);
        box.setPadding(p, p, p, p);
        box.addView(tv(this, "Temps de repos (secondes)", 13, MUTED, true));
        final EditText rest = numEdit("120");
        rest.setText(String.valueOf(store.restDefault()));
        box.addView(rest);
        TextView bl = tv(this, "Poids de la barre (kg)", 13, MUTED, true);
        bl.setPadding(0, dp(this, 14), 0, 0);
        box.addView(bl);
        final EditText bar = numEdit("20");
        bar.setText(Plates.trim(store.bar()));
        box.addView(bar);
        new AlertDialog.Builder(this).setTitle("Réglages").setView(box)
            .setPositiveButton("Enregistrer", (d, w) -> {
                int r = (int) parse(rest.getText().toString());
                if (r > 0) store.setRestDefault(r);
                double bv = parse(bar.getText().toString());
                if (bv > 0) store.setBar(bv);
            }).setNegativeButton("Annuler", null).show();
    }

    // ---------------- finish ----------------

    private void finishSession() {
        JSONArray entries = new JSONArray();
        try {
            for (ExState st : states) {
                JSONArray sets = new JSONArray();
                for (SetRow sr : st.rows) {
                    if (sr.done) {
                        double ww = parse(sr.w.getText().toString());
                        double rr = parse(sr.reps.getText().toString());
                        if (ww > 0 || rr > 0) {
                            JSONObject o = new JSONObject();
                            o.put("w", ww); o.put("reps", rr);
                            sets.put(o);
                        }
                    }
                }
                if (sets.length() > 0) {
                    JSONObject e = new JSONObject();
                    e.put("name", st.ex.name); e.put("sets", sets);
                    entries.put(e);
                }
            }
        } catch (Exception ignored) {}

        if (entries.length() > 0) store.addSession(day.name, entries);
        store.clearTempWorkout();
        stopTimerService();
        Toast.makeText(this, "Séance enregistrée", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static double parse(String s) {
        if (s == null) return 0;
        s = s.trim().replace(",", ".");
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private static class SimpleWatcher implements TextWatcher {
        final Runnable r;
        SimpleWatcher(Runnable r) { this.r = r; }
        public void afterTextChanged(Editable e) { r.run(); }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
    }
}
