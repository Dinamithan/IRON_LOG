package com.ironlog;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Store {
    private final SharedPreferences sp;

    public Store(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("ironlog", Context.MODE_PRIVATE);
    }

    // --- settings ---
    public int restDefault() { return sp.getInt("rest", 120); }
    public void setRestDefault(int s) { sp.edit().putInt("rest", s).apply(); }

    public double bar() {
        return Double.longBitsToDouble(sp.getLong("bar", Double.doubleToLongBits(20)));
    }
    public void setBar(double b) {
        sp.edit().putLong("bar", Double.doubleToLongBits(b)).apply();
    }

    // --- active program ---
    public int activeProgram() {
        return Math.min(sp.getInt("program", 0), Models.PROGRAMS.size() - 1);
    }
    public void setActiveProgram(int p) { sp.edit().putInt("program", p).apply(); }
    public void resetProgress() { sp.edit().remove("last").apply(); }

    // --- reminders ---
    public boolean remindersEnabled() { return sp.getBoolean("reminders_on", false); }
    public void setRemindersEnabled(boolean v) { sp.edit().putBoolean("reminders_on", v).apply(); }

    public Set<Integer> reminderDays() {
        Set<Integer> out = new HashSet<>();
        String s = sp.getString("reminder_days", "");
        if (s.isEmpty()) return out;
        for (String part : s.split(",")) {
            try { out.add(Integer.parseInt(part.trim())); } catch (Exception ignored) {}
        }
        return out;
    }

    public void setReminderDays(Set<Integer> days) {
        StringBuilder sb = new StringBuilder();
        for (int d : days) { if (sb.length() > 0) sb.append(","); sb.append(d); }
        sp.edit().putString("reminder_days", sb.toString()).apply();
    }

    public int reminderHour() { return sp.getInt("reminder_h", 9); }
    public void setReminderHour(int h) { sp.edit().putInt("reminder_h", h).apply(); }
    public int reminderMinute() { return sp.getInt("reminder_m", 0); }
    public void setReminderMinute(int m) { sp.edit().putInt("reminder_m", m).apply(); }

    // --- last performance per exercise: returns {weight, reps} or null ---
    public double[] last(String name) {
        try {
            JSONObject o = new JSONObject(sp.getString("last", "{}"));
            if (o.has(name)) {
                JSONObject e = o.getJSONObject(name);
                return new double[]{e.getDouble("w"), e.getDouble("reps")};
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void setLast(String name, double w, double reps) {
        try {
            JSONObject o = new JSONObject(sp.getString("last", "{}"));
            JSONObject e = new JSONObject();
            e.put("w", w);
            e.put("reps", reps);
            o.put(name, e);
            sp.edit().putString("last", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    // --- history ---
    public JSONArray getHistory() {
        try { return new JSONArray(sp.getString("history", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    public void addSession(String day, JSONArray entries) {
        try {
            JSONArray h = getHistory();
            JSONObject s = new JSONObject();
            s.put("day", day);
            s.put("date", System.currentTimeMillis());
            s.put("entries", entries);
            h.put(s);
            sp.edit().putString("history", h.toString()).apply();
        } catch (Exception ignored) {}
    }

    public int sessionsThisWeek() {
        int n = 0;
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        JSONArray h = getHistory();
        for (int i = 0; i < h.length(); i++) {
            try { if (h.getJSONObject(i).getLong("date") >= weekAgo) n++; }
            catch (Exception ignored) {}
        }
        return n;
    }

    public Long lastDone(String day) {
        JSONArray h = getHistory();
        Long best = null;
        for (int i = 0; i < h.length(); i++) {
            try {
                JSONObject s = h.getJSONObject(i);
                if (day.equals(s.getString("day"))) {
                    long d = s.getLong("date");
                    if (best == null || d > best) best = d;
                }
            } catch (Exception ignored) {}
        }
        return best;
    }
}
