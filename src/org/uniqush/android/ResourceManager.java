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
import java.util.Hashtable;
import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.uniqush.client.CredentialProvider;

class ResourceManager {
	private Hashtable<String, ConnectionParameter> connMap;

	protected static ResourceManager manager;
	final private static String TAG = "ResourceManager";
	private static String PREF_NAME = "uniqush";
	private static String USER_INFO_PROVIDER = "user-info-provider";
	private static String MSG_HANDLER = "message-handler-class";
	private static String CRED_PROVIDER = "credential-provider-class";
	private static String SENDER_IDS = "sender-ids";

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

	public static void setSenderIds(Context context, String[] senderIds) {
		String sids = join(senderIds, "\t");
		if (sids.length() <= 0) {
			return;
		}
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SENDER_IDS, sids);
		editor.commit();
	}

	public static String[] getSenderIds(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		String sids = pref.getString(SENDER_IDS, "");
		String[] ret = sids.split("\t");
		return ret;
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

	public static void setCredentialProvider(Context context, String className)
			throws ClassNotFoundException, SecurityException,
			NoSuchMethodException {
		Log.i(TAG, "credential provider class name: " + className);
		Class<?> credentialProvider = Class.forName(className);
		if (!CredentialProvider.class.isAssignableFrom(credentialProvider)) {
			throw new ClassNotFoundException(className
					+ " is not an implementation of "
					+ CredentialProvider.class.getName());
		}
		credentialProvider.getConstructor(Context.class);

		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(CRED_PROVIDER, className);
		editor.commit();
		Log.i(TAG, "committed");
	}

	public static void setMessageHandler(Context context, String className)
			throws ClassNotFoundException, SecurityException,
			NoSuchMethodException {
		Log.i(TAG, "message handler class name: " + className);
		Class<?> messageHandlerClass = Class.forName(className);
		if (!MessageHandler.class.isAssignableFrom(messageHandlerClass)) {
			throw new ClassNotFoundException(className
					+ " is not an implementation of "
					+ MessageHandler.class.getName());
		}
		messageHandlerClass.getConstructor(Context.class);

		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(MSG_HANDLER, className);
		editor.commit();
		Log.i(TAG, "committed");
	}

	public static CredentialProvider getUserInfoProvider(Context context) {
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

	public static CredentialProvider getCredentialProvider(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		String className = pref.getString(CRED_PROVIDER, null);
		if (className == null) {
			Log.i(TAG, "credential provider has not been set");
			return null;
		}
		Log.i(TAG, "credential provider class name: " + className);

		try {
			Class<?> credentialProviderClass = Class.forName(className);
			Constructor<?> constructor = credentialProviderClass
					.getConstructor(Context.class);
			Object obj = constructor.newInstance(context);
			if (obj instanceof CredentialProvider) {
				return (CredentialProvider) obj;
			}
			throw new InstantiationException(
					"should implement org.uniqush.client.CredentialProvider");
		} catch (Exception e) {
			Log.e(TAG, e.getClass().getName() + ": " + e.getMessage());
		}
		return null;
	}

	public static MessageHandler getMessageHandler(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		String className = pref.getString(MSG_HANDLER, null);
		if (className == null) {
			Log.i(TAG, "message handler has not been set");
			return null;
		}
		Log.i(TAG, "message handler class name: " + className);

		try {
			Class<?> messageHandlerClass = Class.forName(className);
			Constructor<?> constructor = messageHandlerClass
					.getConstructor(Context.class);
			Object obj = constructor.newInstance(context);
			if (obj instanceof MessageHandler) {
				return (MessageHandler) obj;
			}
			throw new InstantiationException(
					"should implement org.uniqush.android.MessageHandler");
		} catch (Exception e) {
			Log.e(TAG, e.getClass().getName() + ": " + e.getMessage());
		}
		return null;
	}

	public static ResourceManager getResourceManager() {
		if (manager == null) {
			manager = new ResourceManager();
		}
		return manager;
	}

	public void addConnectionParameter(ConnectionParameter param) {
		connMap.put(param.toString(), param);
	}

	/**
	 * Connection parameters should not be stored by uniqush because it contains
	 * user's token which is sensible data.
	 * 
	 * @param id
	 * @return
	 */
	public ConnectionParameter getConnectionParameter(String id) {
		ConnectionParameter ret = connMap.get(id);
		return ret;
	}

	protected ResourceManager() {
		this.connMap = new Hashtable<String, ConnectionParameter>();
	}

}
