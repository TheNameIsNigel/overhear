package com.afollestad.overhear.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Convenience methods for commonly accessed local preferences.
 *
 * @author Aidan Follestad
 */
public class Store {

    public static int i(Context context, String key, int def) {
        SharedPreferences prefs = context.getSharedPreferences("store", 0);
        return prefs.getInt(key, def);
    }

    public static void put(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences("store", 0);
        prefs.edit().putInt(key, value).commit();
    }

    public static boolean b(Context context, String key, boolean def) {
        SharedPreferences prefs = context.getSharedPreferences("store", 0);
        return prefs.getBoolean(key, def);
    }

    public static void put(Context context, String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences("store", 0);
        prefs.edit().putBoolean(key, value).commit();
    }
}
