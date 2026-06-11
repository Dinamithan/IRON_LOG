package com.ironlog;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import static com.ironlog.Ui.*;

public class DayEditorActivity extends AppCompatActivity {

    private Store store;
    private int progIdx, dayIdx;
    private EditText nameEdit;
    private LinearLayout exList;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        progIdx = getIntent().getIntExtra("prog_idx", 0);
        dayIdx = getIntent().getIntExtra("day_idx", 0);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = col(this);
        int p = dp(this, 16);
        root.setPadding(p, dp(this, 20), p, p);
        sv.addView(root);
        setContentView(sv);

        // Header
        LinearLayout head = rowh(this);
        Button back = button(this, "‹", false);
        back.setBackgroundColor(0);
        back.setTextColor(ACCENT);
        back.setPadding(0, dp(this, 4), dp(this, 8), dp(this, 4));
        back.setOnClickListener(v -> finish());
        head.addView(back);

        nameEdit = new EditText(this);
        nameEdit.setTextColor(TEXT);
        nameEdit.setTextSize(18);
        nameEdit.setTypeface(Typeface.DEFAULT_BOLD);
        nameEdit.setBackground(null);
        nameEdit.setSingleLine(true);
        nameEdit.setHint("Nom de la séance");
        nameEdit.setHintTextColor(MUTED);
        head.addView(nameEdit, new LinearLayout.LayoutParams(0, WRAP, 1f));
        root.addView(head);

        // Section header
        LinearLayout secHead = rowh(this);
        secHead.setPadding(0, dp(this, 20), 0, dp(this, 10));
        secHead.addView(tv(this, "EXERCICES", 12, MUTED, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button addEx = button(this, "+ Ajouter", true);
        addEx.setOnClickListener(v -> exerciseDialog(-1));
        secHead.addView(addEx);
        root.addView(secHead);

        exList = col(this);
        root.addView(exList);

        Models.Day day = getDay();
        nameEdit.setText(day.name);
        nameEdit.addTextChangedListener(new NameWatcher());
        buildExList();
    }

    private Models.Day getDay() {
        List<Models.Program> progs = store.getPrograms();
        Models.Program prog = progs.get(Math.min(progIdx, progs.size() - 1));
        return prog.days.get(Math.min(dayIdx, prog.days.size() - 1));
    }

    private void buildExList() {
        exList.removeAllViews();
        Models.Day day = getDay();

        for (int i = 0; i < day.ex.size(); i++) {
            final int ei = i;
            Models.Ex ex = day.ex.get(i);

            LinearLayout row = rowh(this);
            row.setBackground(round(CARD, 12, BORDER, 1, this));
            int rp = dp(this, 14);
            row.setPadding(rp, rp, rp, rp);

            LinearLayout info = col(this);
            info.addView(tv(this, ex.name, 14, TEXT, true));
            String detail = ex.scheme + (ex.planned != null && !ex.planned.isEmpty() ? "  ·  " + ex.planned + " kg" : "");
            info.addView(tv(this, detail, 12, MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, WRAP, 1f));

            Button edit = iconBtn("✏");
            edit.setOnClickListener(v -> exerciseDialog(ei));
            row.addView(edit);

            Button del = iconBtn("🗑");
            del.setTextColor(0xFFFF6B6B);
            del.setOnClickListener(v -> confirmDeleteEx(ei));
            row.addView(del);

            LinearLayout.LayoutParams lp = lp(MATCH, WRAP);
            lp.bottomMargin = dp(this, 8);
            exList.addView(row, lp);
        }

        if (day.ex.isEmpty()) {
            TextView empty = tv(this, "Aucun exercice. Appuie sur + Ajouter.", 13, MUTED, false);
            empty.setPadding(0, dp(this, 8), 0, 0);
            exList.addView(empty);
        }
    }

    private void exerciseDialog(int exIdx) {
        Models.Ex existing = (exIdx >= 0) ? getDay().ex.get(exIdx) : null;

        LinearLayout box = col(this);
        int p = dp(this, 16);
        box.setPadding(p, p, p, p);

        EditText nameEt = field("Nom de l'exercice");
        if (existing != null) nameEt.setText(existing.name);
        box.addView(nameEt);

        EditText schemeEt = field("Schéma (ex: 4×6-8)");
        if (existing != null) schemeEt.setText(existing.scheme);
        LinearLayout.LayoutParams slp = lp(MATCH, WRAP);
        slp.topMargin = dp(this, 8);
        box.addView(schemeEt, slp);

        EditText plannedEt = field("Charge prévue (kg, optionnel)");
        plannedEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) plannedEt.setText(existing.planned);
        LinearLayout.LayoutParams plp = lp(MATCH, WRAP);
        plp.topMargin = dp(this, 8);
        box.addView(plannedEt, plp);

        new AlertDialog.Builder(this)
            .setTitle(exIdx < 0 ? "Ajouter un exercice" : "Modifier l'exercice")
            .setView(box)
            .setPositiveButton("Enregistrer", (d, w) -> {
                String n = nameEt.getText().toString().trim();
                if (n.isEmpty()) { Toast.makeText(this, "Le nom est requis", Toast.LENGTH_SHORT).show(); return; }
                String scheme = schemeEt.getText().toString().trim();
                String planned = plannedEt.getText().toString().trim();

                List<Models.Program> progs = store.getPrograms();
                Models.Program prog = progs.get(Math.min(progIdx, progs.size() - 1));
                Models.Day day = prog.days.get(Math.min(dayIdx, prog.days.size() - 1));

                if (exIdx < 0) {
                    day.ex.add(new Models.Ex(n, scheme, planned));
                } else {
                    Models.Ex e = day.ex.get(exIdx);
                    e.name = n;
                    e.scheme = scheme;
                    e.planned = planned;
                }
                store.savePrograms(progs);
                buildExList();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void confirmDeleteEx(int ei) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer cet exercice ?")
            .setPositiveButton("Supprimer", (d, w) -> {
                List<Models.Program> progs = store.getPrograms();
                Models.Program prog = progs.get(Math.min(progIdx, progs.size() - 1));
                prog.days.get(Math.min(dayIdx, prog.days.size() - 1)).ex.remove(ei);
                store.savePrograms(progs);
                buildExList();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(15);
        e.setBackground(round(CARD2, 9, BORDER, 1, this));
        e.setPadding(dp(this, 10), dp(this, 10), dp(this, 10), dp(this, 10));
        e.setLayoutParams(lp(MATCH, WRAP));
        return e;
    }

    private Button iconBtn(String icon) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(icon);
        b.setTextSize(16);
        b.setTextColor(TEXT);
        b.setBackground(round(CARD2, 8, BORDER, 1, this));
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(this, 40), dp(this, 40));
        lp.leftMargin = dp(this, 6);
        b.setLayoutParams(lp);
        return b;
    }

    private class NameWatcher implements TextWatcher {
        public void afterTextChanged(Editable e) {
            String n = e.toString().trim();
            if (n.isEmpty()) return;
            List<Models.Program> progs = store.getPrograms();
            Models.Program prog = progs.get(Math.min(progIdx, progs.size() - 1));
            prog.days.get(Math.min(dayIdx, prog.days.size() - 1)).name = n;
            store.savePrograms(progs);
        }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
    }
}
