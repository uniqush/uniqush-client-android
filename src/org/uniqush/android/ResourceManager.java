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
import java.util.Hashtable;
import org.uniqush.client.MessageCenter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

class ResourceManager {
	private String[] senderIds;
	private Hashtable<String, ConnectionParameter> connMap;
	private MessageCenter center;

	protected static ResourceManager manager;
	final private static String TAG = "ResourceManager";
	private static String PREF_NAME = "uniqush";
	private static String MSG_HANDLER = "message-handler-class";

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
		pref.edit().putString(MSG_HANDLER, className);
		pref.edit().commit();
	}

	public static MessageHandler getMessageHandler(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NAME,
				Context.MODE_PRIVATE);
		String className = pref.getString(MSG_HANDLER, null);
		if (className == null) {
			return null;
		}
		Log.i(TAG, "message handler class name: " + className);

		try {
			Class<?> messageHandlerClass = Class.forName(className);
			Constructor<?> constructor = messageHandlerClass
					.getConstructor(context.getClass());
			Object obj = constructor.newInstance(context);
			if (obj instanceof MessageHandler) {
				return (MessageHandler) obj;
			}
			throw new InstantiationException(
					"should implement org.uniqush.android.MessageHandler");
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
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

	public ConnectionParameter getConnectionParameter(String id) {
		ConnectionParameter ret = connMap.get(id);
		return ret;
	}

	public String[] getSenderIds() {
		return this.senderIds;
	}

	public void setSenderIds(String... senderIds) {
		int n = senderIds.length;
		this.senderIds = new String[n];

		int i = 0;
		for (String s : senderIds) {
			this.senderIds[i] = s;
			i++;
		}
	}

	public MessageCenter getMessageCenter() {
		if (this.center == null) {
			this.center = new MessageCenter();
		}
		return this.center;
	}

	protected ResourceManager() {
		this.senderIds = null;
		this.connMap = new Hashtable<String, ConnectionParameter>();
		this.center = new MessageCenter();
	}

}
