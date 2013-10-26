/*
 * Copyright 2013 Nan Deng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.uniqush.android;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.uniqush.client.CredentialProvider;

class ResourceManager {
	protected static ResourceManager manager;
	final private static String TAG = "ResourceManager";
	private static String PREF_NAME = "uniqush";
	private static String USER_INFO_PROVIDER = "user-info-provider";
	private static String USER_SUBSCRIPTION_PREFIX = "subscribed:";

	/**
	 * TODO put it somewhere else. Dear java: don't you think this method is
	 * useful and deserves a chance to get into the standard library?
	 * 
	 * @param col
	 * @param delim
	 * @return
	 */
	public static String join(Collection<?> col, String delim) {
		StringBuilder sb = new StringBuilder();
		Iterator<?> iter = col.iterator();
		if (iter.hasNext())
			sb.append(iter.next().toString());
		while (iter.hasNext()) {
			sb.append(delim);
			sb.append(iter.next().toString());
		}
		return sb.toString();
	}

	/**
	 * TODO put it somewhere else. Dear java: don't you think this method is
	 * useful and deserves a chance to get into the standard library?
	 */
	public static String join(String[] col, String delim) {
		StringBuilder sb = new StringBuilder();

		if (col == null) {
			return "";
		}
		if (col.length == 0) {
			return "";
		}
		sb.append(col[0]);
		for (int i = 1; i < col.length; i++) {
			sb.append(delim);
			sb.append(col[i]);
		}
		return sb.toString();
	}

	public static void setUserInfoProvider(Context context, String className)
			throws ClassNotFoundException, SecurityException,
			NoSuchMethodException {
		Log.i(TAG, "user info provider class name: " + className);
		Class<?> userInfoProvider = Class.forName(className);
		if (!CredentialProvider.class.isAssignableFrom(userInfoProvider)) {
			throw new ClassNotFoundException(className
					+ " is not an implementation of "
					+ UserInfoProvider.class.getName());
		}
		userInfoProvider.getConstructor(Context.class);

		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(USER_INFO_PROVIDER, className);
		editor.commit();
		Log.i(TAG, "committed");
	}

	public static UserInfoProvider getUserInfoProvider(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		String className = pref.getString(USER_INFO_PROVIDER, null);
		if (className == null) {
			Log.i(TAG, "user info provider has not been set");
			return null;
		}
		Log.i(TAG, "user info provider class name: " + className);

		try {
			Class<?> userInfoProvider = Class.forName(className);
			Constructor<?> constructor = userInfoProvider.getConstructor(Context.class);
			Object obj = constructor.newInstance(context);
			if (obj instanceof UserInfoProvider) {
				return (UserInfoProvider) obj;
			}
			throw new InstantiationException(
					"should implement org.uniqush.client.UserInfoProvider");
		} catch (Exception e) {
			Log.e(TAG, e.getClass().getName() + ": " + e.getMessage());
		}
		return null;
	}
	
	public static void setSubscribed(Context context, String service, String username, boolean sub) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putBoolean(USER_SUBSCRIPTION_PREFIX + service + "," + username, sub);
		editor.commit();
	}
	
	public static boolean subscribed(Context context, String service, String username) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		return pref.getBoolean(USER_SUBSCRIPTION_PREFIX + service + "," + username, false);
	}
}
