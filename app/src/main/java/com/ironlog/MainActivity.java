package com.ironlog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.ironlog.Ui.*;

public class MainActivity extends AppCompatActivity {

    private Store store;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

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

        LinearLayout head = rowh(this);
        head.addView(tv(this, "IRON", 24, TEXT, true));
        head.addView(tv(this, "LOG", 24, ACCENT, true));
        root.addView(head);

        int wk = store.sessionsThisWeek();
        TextView sub = tv(this, wk + " séance" + (wk > 1 ? "s" : "") + " cette semaine", 13, MUTED, false);
        sub.setPadding(0, dp(this, 4), 0, dp(this, 16));
        root.addView(sub);

        Button hist = button(this, "Voir l'historique", false);
        hist.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams hlp = lp(MATCH, WRAP);
        hlp.bottomMargin = dp(this, 16);
        root.addView(hist, hlp);

        TextView lbl = tv(this, "CHOISIS TA SÉANCE", 13, MUTED, true);
        lbl.setPadding(dp(this, 2), 0, 0, dp(this, 10));
        root.addView(lbl);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM", Locale.FRENCH);

        for (int i = 0; i < Models.PROGRAM.size(); i++) {
            final Models.Day d = Models.PROGRAM.get(i);
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
    }
}
