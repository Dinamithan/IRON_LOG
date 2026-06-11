package com.ironlog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import static com.ironlog.Ui.*;

public class ProgramsActivity extends AppCompatActivity {

    private Store store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = col(this);
        int p = dp(this, 16);
        root.setPadding(p, dp(this, 20), p, p);
        sv.addView(root);
        setContentView(sv);

        LinearLayout head = rowh(this);
        Button back = button(this, "‹", false);
        back.setBackgroundColor(0);
        back.setTextColor(ACCENT);
        back.setPadding(0, dp(this, 4), dp(this, 8), dp(this, 4));
        back.setOnClickListener(v -> finish());
        head.addView(back);
        TextView title = tv(this, "Programmes", 20, TEXT, true);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button addBtn = button(this, "+ Nouveau", true);
        addBtn.setOnClickListener(v -> newProgram());
        head.addView(addBtn);
        root.addView(head);

        list = col(this);
        list.setPadding(0, dp(this, 16), 0, 0);
        root.addView(list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildList();
    }

    private void buildList() {
        list.removeAllViews();
        List<Models.Program> progs = store.getPrograms();
        int active = Math.min(store.activeProgram(), progs.size() - 1);

        for (int i = 0; i < progs.size(); i++) {
            final int idx = i;
            Models.Program prog = progs.get(i);
            boolean isActive = (idx == active);

            LinearLayout row = rowh(this);
            row.setBackground(round(CARD, 12, isActive ? ACCENT : BORDER, isActive ? 2 : 1, this));
            int rp = dp(this, 14);
            row.setPadding(rp, rp, rp, rp);

            LinearLayout info = col(this);
            info.addView(tv(this, prog.name, 15, TEXT, true));
            String sub = prog.days.size() + " séance" + (prog.days.size() > 1 ? "s" : "");
            info.addView(tv(this, sub, 12, MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, WRAP, 1f));

            if (isActive) {
                TextView check = tv(this, "✓", 16, ACCENT, true);
                check.setPadding(0, 0, dp(this, 10), 0);
                row.addView(check);
            }

            Button edit = iconBtn("✏");
            edit.setOnClickListener(v -> openEditor(idx));
            row.addView(edit);

            if (progs.size() > 1) {
                Button del = iconBtn("🗑");
                del.setTextColor(0xFFFF6B6B);
                del.setOnClickListener(v -> confirmDelete(idx));
                row.addView(del);
            }

            row.setOnClickListener(v -> {
                store.setActiveProgram(idx);
                finish();
            });

            LinearLayout.LayoutParams lp = lp(MATCH, WRAP);
            lp.bottomMargin = dp(this, 10);
            list.addView(row, lp);
        }
    }

    private void newProgram() {
        List<Models.Program> progs = store.getPrograms();
        int newIdx = progs.size();
        progs.add(new Models.Program("Nouveau programme"));
        store.savePrograms(progs);
        openEditor(newIdx);
    }

    private void openEditor(int idx) {
        Intent it = new Intent(this, ProgramEditorActivity.class);
        it.putExtra("prog_idx", idx);
        startActivity(it);
    }

    private void confirmDelete(int idx) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer ce programme ?")
            .setMessage("Cette action est irréversible.")
            .setPositiveButton("Supprimer", (d, w) -> {
                List<Models.Program> progs = store.getPrograms();
                progs.remove(idx);
                store.savePrograms(progs);
                int active = store.activeProgram();
                if (active >= progs.size()) store.setActiveProgram(Math.max(0, progs.size() - 1));
                buildList();
                Toast.makeText(this, "Programme supprimé", Toast.LENGTH_SHORT).show();
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
}
