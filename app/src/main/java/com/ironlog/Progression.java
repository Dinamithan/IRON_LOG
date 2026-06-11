package com.ironlog;

import java.util.ArrayList;
import java.util.List;

public class Progression {

    /** Suggest next working weight: bump +2.5 kg when the top of the rep range was hit. */
    public static double suggest(double lastW, double lastReps, int topReps) {
        if (topReps > 0 && lastReps >= topReps) return lastW + 2.5;
        return lastW;
    }

    /** Estimated 1-rep max (Epley formula). */
    public static int oneRM(double w, double reps) {
        if (w <= 0 || reps <= 0) return 0;
        return (int) Math.round(w * (1.0 + reps / 30.0));
    }

    /** Ramping warm-up sets toward a working weight. */
    public static List<String> warmups(double workW, double bar) {
        List<String> out = new ArrayList<>();
        if (workW <= 0) return out;
        out.add("Barre " + Plates.trim(bar) + " × 8");
        double[][] r = {{0.5, 5}, {0.7, 3}, {0.85, 1}};
        for (double[] x : r) {
            double w = Math.max(bar, Math.round(workW * x[0] / 2.5) * 2.5);
            if (w < workW) out.add(Plates.trim(w) + " kg × " + (int) x[1]);
        }
        return out;
    }
}
