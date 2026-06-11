package com.ironlog;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
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

public class ProgramEditorActivity extends AppCompatActivity {

    private Store store;
    private int progIdx;
    private EditText nameEdit;
    private LinearLayout dayList;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        progIdx = getIntent().getIntExtra("prog_idx", 0);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = col(this);
        int p = dp(this, 16);
        root.setPadding(p, dp(this, 20), p, p);
        sv.addView(root);
        setContentView(sv);

        // Header: back + editable program name
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
        nameEdit.setHint("Nom du programme");
        nameEdit.setHintTextColor(MUTED);
        head.addView(nameEdit, new LinearLayout.LayoutParams(0, WRAP, 1f));
        root.addView(head);

        // Section header
        LinearLayout secHead = rowh(this);
        secHead.setPadding(0, dp(this, 20), 0, dp(this, 10));
        secHead.addView(tv(this, "SÉANCES", 12, MUTED, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button addDay = button(this, "+ Ajouter", true);
        addDay.setOnClickListener(v -> addDay());
        secHead.addView(addDay);
        root.addView(secHead);

        dayList = col(this);
        root.addView(dayList);

        // Load program name (days loaded in onResume)
        Models.Program prog = getProgram();
        nameEdit.setText(prog.name);
        nameEdit.addTextChangedListener(new NameWatcher());
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildDayList();
    }

    private Models.Program getProgram() {
        List<Models.Program> progs = store.getPrograms();
        return progs.get(Math.min(progIdx, progs.size() - 1));
    }

    private void buildDayList() {
        dayList.removeAllViews();
        Models.Program prog = getProgram();
        for (int i = 0; i < prog.days.size(); i++) {
            final int di = i;
            Models.Day day = prog.days.get(i);

            LinearLayout row = rowh(this);
            row.setBackground(round(CARD, 12, BORDER, 1, this));
            int rp = dp(this, 14);
            row.setPadding(rp, rp, rp, rp);

            LinearLayout info = col(this);
            info.addView(tv(this, day.name, 14, TEXT, true));
            String sub = day.ex.size() + " exercice" + (day.ex.size() > 1 ? "s" : "");
            info.addView(tv(this, sub, 12, MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, WRAP, 1f));

            Button edit = iconBtn("✏");
            edit.setOnClickListener(v -> openDayEditor(di));
            row.addView(edit);

            Button del = iconBtn("🗑");
            del.setTextColor(0xFFFF6B6B);
            del.setOnClickListener(v -> confirmDeleteDay(di));
            row.addView(del);

            LinearLayout.LayoutParams lp = lp(MATCH, WRAP);
            lp.bottomMargin = dp(this, 8);
            dayList.addView(row, lp);
        }

        if (prog.days.isEmpty()) {
            TextView empty = tv(this, "Aucune séance. Appuie sur + Ajouter.", 13, MUTED, false);
            empty.setPadding(0, dp(this, 8), 0, 0);
            dayList.addView(empty);
        }
    }

    private void addDay() {
        List<Models.Program> progs = store.getPrograms();
        Models.Program prog = progs.get(Math.min(progIdx, progs.size() - 1));
        int newDayIdx = prog.days.size();
        prog.days.add(new Models.Day("Nouvelle séance"));
        store.savePrograms(progs);
        openDayEditor(newDayIdx);
    }

    private void openDayEditor(int di) {
        Intent it = new Intent(this, DayEditorActivity.class);
        it.putExtra("prog_idx", progIdx);
        it.putExtra("day_idx", di);
        startActivity(it);
    }

    private void confirmDeleteDay(int di) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer cette séance ?")
            .setPositiveButton("Supprimer", (d, w) -> {
                List<Models.Program> progs = store.getPrograms();
                progs.get(Math.min(progIdx, progs.size() - 1)).days.remove(di);
                store.savePrograms(progs);
                buildDayList();
                Toast.makeText(this, "Séance supprimée", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Annuler", null)
            .show();
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
            int idx = Math.min(progIdx, progs.size() - 1);
            progs.get(idx).name = n;
            store.savePrograms(progs);
        }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
    }
}
