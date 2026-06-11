package com.ironlog;

import java.util.ArrayList;
import java.util.List;

public class Models {

    public static class Ex {
        public String name, scheme, planned;
        public Ex(String n, String s, String w) { name = n; scheme = s; planned = w; }

        /** Number of sets parsed from a scheme like "4x6-8" -> 4. */
        public int sets() {
            try {
                String d = scheme.split("[×x]")[0].trim();
                return Math.max(1, Integer.parseInt(d.replaceAll("[^0-9]", "")));
            } catch (Exception e) { return 3; }
        }

        /** Top of the rep range, e.g. "6-8" -> 8, "10-12" -> 12, "Max" -> 0. */
        public int topReps() {
            try {
                String[] p = scheme.split("[×x]");
                String r = p.length > 1 ? p[1] : p[0];
                r = r.replaceAll("[^0-9-]", "");
                if (r.isEmpty()) return 0;
                if (r.contains("-")) return Integer.parseInt(r.split("-")[1]);
                return Integer.parseInt(r);
            } catch (Exception e) { return 0; }
        }
    }

    public static class Day {
        public String name;
        public List<Ex> ex;
        public Day(String n, Ex... e) {
            name = n;
            ex = new ArrayList<>();
            for (Ex x : e) ex.add(x);
        }
    }

    public static final List<Day> PROGRAM = new ArrayList<>();

    static {
        PROGRAM.add(new Day("Lundi — Push",
                new Ex("Développé couché", "4×6-8", "45"),
                new Ex("Développé Militaire (haltères)", "3×8-10", "16"),
                new Ex("Dips", "3×8-12", ""),
                new Ex("Écarté (Pec Deck / poulie)", "3×12-15", ""),
                new Ex("Extensions Triceps Corde", "4×10-12", "17.5"),
                new Ex("Élévations Latérales", "3×12-15", "5")));

        PROGRAM.add(new Day("Mardi — Pull",
                new Ex("Tractions", "4×6-8", ""),
                new Ex("Tirage Horizontal", "4×8-10", "45"),
                new Ex("Tirage Vertical / Lat Pulldown", "3×8-12", "47.5"),
                new Ex("Face Pull", "4×15-20", "27.5"),
                new Ex("Curl Biceps (Barre EZ)", "4×8-12", "12"),
                new Ex("Curl Marteau", "3×10-12", "8")));

        PROGRAM.add(new Day("Mercredi — Legs 1",
                new Ex("Presse à cuisses", "4×6-8", "80"),
                new Ex("Leg Extension", "3×12-15", ""),
                new Ex("Fentes marchées", "3×10-12", ""),
                new Ex("Leg Curl", "3×10-12", "45"),
                new Ex("Mollets à la Presse", "4×12-15", "60"),
                new Ex("Machine à Abdos", "3×15-20", "50")));

        PROGRAM.add(new Day("Jeudi — Push",
                new Ex("Développé Militaire (haltères)", "4×6-8", "17"),
                new Ex("Chest Press", "3×8-12", "60"),
                new Ex("Développé incliné / Pec Deck", "3×10-12", ""),
                new Ex("Élévations Latérales", "4×12-15", "5"),
                new Ex("Extensions Triceps overhead", "3×12-15", "")));

        PROGRAM.add(new Day("Vendredi — Pull",
                new Ex("Rowing / Tirage Horizontal lourd", "4×6-8", "45"),
                new Ex("Tirage Vertical / Lat Pulldown", "4×8-12", "47.5"),
                new Ex("Tractions assistées", "3×8-12", ""),
                new Ex("Face Pull", "4×15-20", "27.5"),
                new Ex("Curl Biceps (poulie)", "3×10-12", ""),
                new Ex("Curl Marteau", "3×12-15", "8")));

        PROGRAM.add(new Day("Samedi — Legs 2",
                new Ex("Soulevé de Terre Roumain (RDL)", "4×8-10", ""),
                new Ex("Presse à cuisses (pieds hauts)", "3×12-15", "70"),
                new Ex("Leg Curl allongé", "4×12-15", "45"),
                new Ex("Mollets à la Presse", "4×15-20", "60"),
                new Ex("Machine à Abdos", "3×15-20", "50"),
                new Ex("Pompes (Finisher)", "Max", "")));
    }
}
