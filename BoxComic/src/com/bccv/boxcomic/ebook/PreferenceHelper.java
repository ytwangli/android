package com.bccv.boxcomic.ebook;

import com.bccv.boxcomic.tool.MyApplication;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

public class PreferenceHelper {

	public static boolean getBoolean(String key, boolean defValue) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MyApplication
						.getGlobalContext());
		return settings.getBoolean(key, defValue);
	}

	public static void putBoolean(String key, boolean value) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(
						MyApplication.getGlobalContext()).edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public static int getInt(String key, int defValue) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MyApplication
						.getGlobalContext());
		return settings.getInt(key, defValue);
	}

	public static void putInt(String key, int value) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(
						MyApplication.getGlobalContext()).edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public static long getLong(String key, long defValue) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MyApplication
						.getGlobalContext());
		return settings.getLong(key, defValue);
	}

	public static void putLong(String key, long value) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(
						MyApplication.getGlobalContext()).edit();
		editor.putLong(key, value);
		editor.commit();
	}

	public static String getString(String key, String defValue) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MyApplication
						.getGlobalContext());
		return settings.getString(key, defValue);
	}

	public static void putString(String key, String value) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(
						MyApplication.getGlobalContext()).edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static void remove(String key) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(
						MyApplication.getGlobalContext()).edit();
		editor.remove(key);
		editor.commit();
	}

	public static void registerOnPrefChangeListener(
			OnSharedPreferenceChangeListener listener) {
		try {
			PreferenceManager.getDefaultSharedPreferences(
					MyApplication.getGlobalContext())
					.registerOnSharedPreferenceChangeListener(listener);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static void unregisterOnPrefChangeListener(
			OnSharedPreferenceChangeListener listener) {
		try {
			PreferenceManager.getDefaultSharedPreferences(
					MyApplication.getGlobalContext())
					.unregisterOnSharedPreferenceChangeListener(listener);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
