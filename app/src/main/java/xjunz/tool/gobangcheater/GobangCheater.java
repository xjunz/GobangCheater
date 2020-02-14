package xjunz.tool.gobangcheater;

import android.app.Application;
import android.content.SharedPreferences;

public class GobangCheater extends Application {
    private static SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences("config", MODE_PRIVATE);
    }

    public static void notifyChessboardConfigSaved() {
        preferences.edit().putBoolean("has", true).apply();
    }


    public static boolean hasChessboardConfig() {
        return preferences.getBoolean("has", false);
    }


    public static void putChessboardGridSpec(float spec) {
        preferences.edit().putFloat("spec", spec).apply();
    }

    public static float getChessboardGridSpec() {
        return preferences.getFloat("spec", -1);

    }

    public static void putChessboardTop(float top) {
        preferences.edit().putFloat("t", top).apply();
    }

    public static float getChessboardTop() {
        return preferences.getFloat("t", -1);

    }

    public static void putChessboardLeft(float left) {
        preferences.edit().putFloat("l", left).apply();
    }

    public static float getChessboardLeft() {
         return preferences.getFloat("l", -1);

    }

    public static void putChessboardOffsetX(int x) {
        preferences.edit().putInt("x", x).apply();
    }

    public static void putChessboardOffsetY(int y) {
        preferences.edit().putInt("y", y).apply();
    }

    public static int getChessboardOffsetX() {
        return preferences.getInt("x", -1);
    }

    public static void putSeekBarProgress(int p) {
        preferences.edit().putInt("p", p).apply();
    }

    public static int getSeekBarProgress() {
        return preferences.getInt("p", -1);
    }

    public static int getChessboardOffsetY() {
        return preferences.getInt("y", -1);
    }


}
