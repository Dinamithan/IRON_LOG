package com.ironlog;

import java.util.ArrayList;
import java.util.List;

public class Plates {
    static final double[] AV = {25, 20, 15, 10, 5, 2.5, 1.25};

    /** Plates to load PER SIDE to reach target with a given bar. */
    public static String perSide(double target, double bar) {
        if (target <= 0) return "Entre un poids cible.";
        double side = (target - bar) / 2.0;
        if (side < 0) return "Cible inférieure à la barre (" + trim(bar) + " kg).";
        StringBuilder sb = new StringBuilder();
        double rem = side;
        for (double p : AV) {
            int n = 0;
            while (rem >= p - 1e-6) { rem -= p; n++; }
            if (n > 0) {
                if (sb.length() > 0) sb.append("  +  ");
                sb.append(n).append("×").append(trim(p));
            }
        }
        String res = sb.length() == 0 ? "barre seule" : sb.toString();
        if (rem > 1e-6) res += "\n(reste " + trim(rem) + " kg/côté — pas faisable pile)";
        return res + "\n\nde chaque côté de la barre";
    }

    public static String trim(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-6) return String.valueOf((int) Math.rint(d));
        return String.valueOf(d);
    }
}
