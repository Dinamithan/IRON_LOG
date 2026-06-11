package com.ironlog;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Store {
    private final SharedPreferences sp;

    public Store(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("ironlog", Context.MODE_PRIVATE);
    }

    // --- programs ---

    public List<Models.Program> getPrograms() {
        String raw = sp.getString("programs", null);
        if (raw == null) {
            List<Models.Program> defs = Models.defaultPrograms();
            savePrograms(defs);
            return defs;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            java.util.List<Models.Program> out = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) out.add(parseProgram(arr.getJSONObject(i)));
            return out.isEmpty() ? Models.defaultPrograms() : out;
        } catch (Exception e) { return Models.defaultPrograms(); }
    }

    public void savePrograms(List<Models.Program> programs) {
        try {
            JSONArray arr = new JSONArray();
            for (Models.Program p : programs) arr.put(toJson(p));
            sp.edit().putString("programs", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private Models.Program parseProgram(JSONObject o) throws Exception {
        Models.Program p = new Models.Program(o.getString("name"));
        JSONArray days = o.getJSONArray("days");
        for (int i = 0; i < days.length(); i++) p.days.add(parseDay(days.getJSONObject(i)));
        return p;
    }

    private Models.Day parseDay(JSONObject o) throws Exception {
        Models.Day d = new Models.Day(o.getString("name"));
        JSONArray exes = o.getJSONArray("exercises");
        for (int i = 0; i < exes.length(); i++) {
            JSONObject e = exes.getJSONObject(i);
            d.ex.add(new Models.Ex(e.getString("name"), e.getString("scheme"), e.optString("planned", "")));
        }
        return d;
    }

    private JSONObject toJson(Models.Program p) throws Exception {
        JSONObject o = new JSONObject();
        o.put("name", p.name);
        JSONArray days = new JSONArray();
        for (Models.Day d : p.days) days.put(toJson(d));
        o.put("days", days);
        return o;
    }

    private JSONObject toJson(Models.Day d) throws Exception {
        JSONObject o = new JSONObject();
        o.put("name", d.name);
        JSONArray exes = new JSONArray();
        for (Models.Ex e : d.ex) {
            JSONObject eo = new JSONObject();
            eo.put("name", e.name);
            eo.put("scheme", e.scheme);
            eo.put("planned", e.planned);
            exes.put(eo);
        }
        o.put("exercises", exes);
        return o;
    }

    // --- active program ---

    public int activeProgram() { return sp.getInt("program", 0); }
    public void setActiveProgram(int p) { sp.edit().putInt("program", p).apply(); }
    public void resetProgress() { sp.edit().remove("last").apply(); }

    // --- settings ---

    public int restDefault() { return sp.getInt("rest", 120); }
    public void setRestDefault(int s) { sp.edit().putInt("rest", s).apply(); }

    public double bar() {
        return Double.longBitsToDouble(sp.getLong("bar", Double.doubleToLongBits(20)));
    }
    public void setBar(double b) {
        sp.edit().putLong("bar", Double.doubleToLongBits(b)).apply();
    }

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
