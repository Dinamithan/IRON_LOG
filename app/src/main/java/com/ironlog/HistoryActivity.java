package com.ironlog;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.ironlog.Ui.*;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        Store store = new Store(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = col(this);
        int p = dp(this, 16);
        root.setPadding(p, dp(this, 20), p, p);
        sv.addView(root);
        setContentView(sv);

        Button back = button(this, "‹ Retour", false);
        back.setBackgroundColor(0x00000000);
        back.setTextColor(ACCENT);
        back.setPadding(0, 0, 0, dp(this, 6));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        root.addView(tv(this, "HISTORIQUE", 13, MUTED, true));

        JSONArray h = store.getHistory();
        if (h.length() == 0) {
            TextView e = tv(this, "Aucune séance enregistrée pour l'instant.", 14, MUTED, false);
            e.setPadding(0, dp(this, 30), 0, 0);
            root.addView(e);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM", Locale.FRENCH);

        for (int i = h.length() - 1; i >= 0; i--) {
            try {
                JSONObject s = h.getJSONObject(i);
                int sets = 0;
                double vol = 0;
                JSONArray en = s.getJSONArray("entries");
                for (int j = 0; j < en.length(); j++) {
                    JSONArray ss = en.getJSONObject(j).getJSONArray("sets");
                    sets += ss.length();
                    for (int k = 0; k < ss.length(); k++) {
                        JSONObject o = ss.getJSONObject(k);
                        vol += o.optDouble("w", 0) * o.optDouble("reps", 0);
                    }
                }
                LinearLayout c = col(this);
                c.setBackground(round(CARD, 12, BORDER, 1, this));
                int cp = dp(this, 14);
                c.setPadding(cp, cp, cp, cp);
                LinearLayout.LayoutParams clp = lp(MATCH, WRAP);
                clp.topMargin = dp(this, 10);
                c.setLayoutParams(clp);

                c.addView(tv(this, s.getString("day"), 14, TEXT, true));
                c.addView(tv(this, sdf.format(new Date(s.getLong("date")))
                        + " · " + sets + " séries · " + Math.round(vol) + " kg de volume", 12.5f, MUTED, false));
                root.addView(c);
            } catch (Exception ignored) {}
        }
    }
}
