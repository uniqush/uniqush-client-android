package org.uniqush.android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageCenter {

	private static String TAG = "UniqushMessageCenter";
	
	/**
	 * This method has to be called in the very first.
	 * 
	 * The user should provide a class name of an implementation of UserInfoProvider.
	 * The implementation should follows the following requirements:
	 * 1. It has to implement org.uniqush.android.UserInfoProvider
	 * 2. It must have a constructor which takes one and only one parameter, a Context
	 * 
	 * Exceptions may be thrown if the class does not satisfy the requirement
	 * mentioned above.
	 * 
	 * @param context
	 * @param userInfoProviderClassName
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 */
	public static void init(Context context, String userInfoProviderClassName)
			throws SecurityException,
			ClassNotFoundException, NoSuchMethodException {
		ResourceManager.setUserInfoProvider(context, userInfoProviderClassName);
	}
	
	public static void connect(Context context, int id) {
		Log.i(TAG, "reconnect: " + id);
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_CONNECT);
		intent.putExtra("id", id);
		context.startService(intent);
	}
}
