package com.ironlog;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import static com.ironlog.Ui.*;

public class SplashActivity extends AppCompatActivity {

    private static final String[] EXERCISES = {
        "Développé couché", "Tractions", "Squat", "Soulevé de terre",
        "Dips", "Rowing barre", "Curl biceps", "Presse à cuisses"
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        LinearLayout root = col(this);
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.CENTER);
        setContentView(root);

        String ex = EXERCISES[(int) (Math.random() * EXERCISES.length)];

        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.HORIZONTAL);
        title.setGravity(Gravity.CENTER);
        title.addView(tv(this, "IRON", 52, TEXT, true));
        title.addView(tv(this, "LOG", 52, ACCENT, true));
        title.setScaleX(0.5f);
        title.setScaleY(0.5f);
        title.setAlpha(0f);
        root.addView(title);

        TextView sub = tv(this, "Aujourd'hui : " + ex, 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(this, 10), 0, 0);
        sub.setAlpha(0f);
        root.addView(sub);

        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(
            ObjectAnimator.ofFloat(title, "scaleX", 0.5f, 1f),
            ObjectAnimator.ofFloat(title, "scaleY", 0.5f, 1f),
            ObjectAnimator.ofFloat(title, "alpha", 0f, 1f)
        );
        anim.setDuration(550);
        anim.setInterpolator(new OvershootInterpolator(1.8f));
        anim.start();

        new Handler(Looper.getMainLooper()).postDelayed(
            () -> ObjectAnimator.ofFloat(sub, "alpha", 0f, 1f).setDuration(350).start(), 350);

        new Handler(Looper.getMainLooper()).postDelayed(
            () -> { startActivity(new Intent(this, MainActivity.class)); finish(); }, 1600);
    }
}
