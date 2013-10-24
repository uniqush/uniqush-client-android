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

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

import org.uniqush.client.Message;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageCenter {

	private static String TAG = "UniqushMessageCenter";
	private ConnectionParameter defaultParam;
	
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

	public MessageCenter(Context context) {
	}

	/**
	 * Close the connection and stop the service. To save energy, the app should
	 * call stop() if there is no message for a while.
	 * 
	 * @param context
	 */
	public void stop(Context context) {
		Log.i(TAG, "stoping the service...");
		Intent intent = new Intent(context, MessageCenterService.class);
		context.stopService(intent);
	}

	/**
	 * Asynchronously send the message to server. The result will be reported
	 * through the handler's onResult() method if id > 0.
	 * 
	 * NOTE: the method will return silently if connect() has never been called.
	 * 
	 * Multiple calls to connect() method with same parameter will not lead to
	 * multiple connection nor reconnection.
	 * 
	 * As long as connect() has been called once, users do not need to worry
	 * about re-connection and other related issues. MessageCenter will trigger
	 * another connection if the connection dropped or the background service
	 * killed by the OS.
	 * 
	 * @param context
	 *            The context
	 * @param id
	 *            If id > 0, then any result (error or success) will be reported
	 *            through the handler's onResult() method. Otherwise (i.e. id <=
	 *            0), only error will be reported through the handler's
	 *            onError() method.
	 * @param msg
	 */
	public void sendMessageToServer(Context context, int id, Message msg) {
		if (this.defaultParam == null || this.defaultToken == null) {
			return;
		}
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_SEND_MSG_TO_SERVER);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		intent.putExtra("msg", msg);
		context.startService(intent);
	}

	/**
	 * Asynchronously send the message to another user. The result will be
	 * reported through the handler's onResult() method if id > 0.
	 * 
	 * The message is actually forwarded by the server. The server may decline
	 * the forwarding request depending on the implementation of the server.
	 * 
	 * NOTE: the method will return silently if connect() has never been called.
	 * 
	 * As long as connect() has been called once, users do not need to worry
	 * about re-connection and other related issues. MessageCenter will trigger
	 * another connection if the connection dropped or the background service
	 * killed by the OS.
	 * 
	 * @param context
	 * @param id
	 *            If id > 0, then any result (error or success) will be reported
	 *            through the handler's onResult() method. Otherwise (i.e. id <=
	 *            0), only error will be reported through the handler's
	 *            onError() method.
	 * @param service
	 * @param username
	 * @param msg
	 * @param ttl
	 */
	public void sendMessageToUser(Context context, int id, String service,
			String username, Message msg, int ttl) {
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_SEND_MSG_TO_USER);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		intent.putExtra("service", service);
		intent.putExtra("username", username);
		intent.putExtra("ttl", ttl);
		intent.putExtra("msg", msg);
		context.startService(intent);
	}

	/**
	 * 
	 * @param context
	 * @param id
	 *            If id > 0, then any result (error or success) will be reported
	 *            through the handler's onResult() method with the id value of
	 *            id. Otherwise (i.e. id <= 0), only error will be reported
	 *            through the handler's onError() method.
	 * @param address
	 * @param port
	 * @param publicKey
	 *            The public key of the server.
	 * @param service
	 *            The name of the service
	 * @param username
	 * @param token
	 *            This could be used for password-based user authentication. The
	 *            token will be sent to the server securely. i.e. encrypted with
	 *            the session key. The value of the token has nothing to do with
	 *            the value of the session key. In another word, it is not used
	 *            to derive the session key.
	 * @param subscribe
	 *            true if it wants to receive push notification from GCM. NOTE:
	 *            It is org.uniqush.android.MessageHandler, not
	 *            org.uniqush.client.MessageHanlder.
	 */
	public void connectServer(Context context, int id, String address,
			int port, RSAPublicKey publicKey, String service, String username,
			String token, boolean subscribe) {
		Log.i(TAG, "connect in message center");
		ConnectionParameter param = new ConnectionParameter(address, port,
				publicKey, service, username);
		ResourceManager.getResourceManager().addConnectionParameter(param);
		this.defaultParam = param;
		this.defaultToken = token;
		reconnect(context, id);

		if (subscribe) {
			// Make sure the device has the proper dependencies.
			GCMRegistrar.checkDevice(context);
			Intent intent = new Intent(context, MessageCenterService.class);
			intent.putExtra("c", MessageCenterService.CMD_SUBSCRIBE);
			context.startService(intent);
		}
	}

	/**
	 * Reconnect the server with the parameters provided on the last call to
	 * connect().
	 * 
	 * @param context
	 * @param id
	 *            If id > 0, then any result (error or success) will be reported
	 *            through the handler's onResult() method with the id value of
	 *            id. Otherwise (i.e. id <= 0), only error will be reported
	 *            through the handler's onError() method.
	 */
	public void reconnect(Context context, int id) {
		Log.i(TAG, "reconnect: " + id);
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_CONNECT);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		context.startService(intent);
	}

	/**
	 * Change the token. This method will not re-connect the server with the new
	 * token. To re-connect the server, use reconnect().
	 * 
	 * @param newToken
	 */
	public void changeToken(String newToken) {
		this.defaultToken = newToken;
	}

	/**
	 * Request a message by its message Id from the server.
	 * 
	 * When a message is too large or the client is not currently connecting
	 * with the server, the handler's
	 * onMessageDigestFromServer()/onMessageDigestFromUser() will be called.
	 * Rather than the message itself, a digest (short version of a message)
	 * will be passed to the method along with an Id of the message.
	 * 
	 * The client could then use the Id of the message to retrieve the message
	 * later.
	 * 
	 * This is very useful for mobile environment since total data transmission
	 * may be limited within a certain period (e.g. using a 3/4G network.) The
	 * client may want to retrieve the data after connecting to a network with
	 * unlimited data plan (e.g. a home WiFi.)
	 * 
	 * @param context
	 * @param id
	 *            The id used to identify this call when reporting the status of
	 *            this call.
	 * @param msgId
	 *            The message ID.
	 */
	public void requestMessage(Context context, int id, String msgId) {
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_REQUEST_MSG);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		intent.putExtra("msgId", msgId);
		context.startService(intent);
	}
	public void requestAllCachedMessages(Context context, int id, String... excludes) {
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_REQUEST_ALL_CACHED_MSG);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		if (excludes.length > 0) {
			ArrayList<String> ex = new ArrayList<String>(excludes.length);
			for (String e : excludes) {
				ex.add(e);
			}
			intent.putExtra("excludes", ex);
		}
		context.startService(intent);
	}
	/**
	 * Let's first define some terms:
	 * 
	 * online - a client is online means the client is connecting with the
	 * server.
	 * 
	 * off-line - a client is off line when it is not online.
	 * 
	 * visible - a client is visible if the client is online and has not
	 * explicitly told the server that it is invisible.
	 * 
	 * invisible - a client is invisible if: a) it is off line or b) it is
	 * online but explicitly told the server it is invisible.
	 * 
	 * A client will receive a message or its digest through a direct connection
	 * with the server if it is *online*.
	 * 
	 * When a message needs to be sent to a user, the server will send a
	 * notification to all devices under the server, iff there is no visible
	 * client under the user.
	 * 
	 * @param context
	 * @param id
	 *            The id used to identify this call when reporting the status of
	 *            this call.
	 * @param visible
	 *            true for visible. false for invisible (but can still receive
	 *            the message/digest.)
	 */
	public void setVisibility(Context context, int id, boolean visible) {
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_SET_VISIBILITY);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);
		intent.putExtra("visible", visible);
		context.startService(intent);
	}

	/**
	 * Set the parameter for the connection.
	 * 
	 * @param context
	 *            The context
	 * @param id
	 *            If id > 0, then any result (error or success) will be reported
	 *            through the handler's onResult() method with the id value of
	 *            id. Otherwise (i.e. id <= 0), only error will be reported
	 *            through the handler's onError() method.
	 * @param digestThreshold
	 *            If the message length is greater than the digestThreshold, the
	 *            server will, instead of sending the message itself, send a
	 *            message digest to the client. The client has to retrieve the
	 *            message manually using requestMessage() method.
	 * @param compressThreshold
	 *            If the message length is greater than the compressThreshold,
	 *            the message will be compressed before sending to the client or
	 *            the server.
	 * @param digestFields
	 *            Tells the server which fields in the message header should be
	 *            included in the message digest.
	 */
	public void config(Context context, int id, int digestThreshold,
			int compressThreshold, String... digestFields) {

		Log.i(TAG, "config: " + id);
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_CONFIG);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("id", id);

		intent.putExtra("digestThreshold", digestThreshold);
		intent.putExtra("compressThreshold", compressThreshold);

		ArrayList<String> dfs = new ArrayList<String>(digestFields.length);
		for (String df : digestFields) {
			dfs.add(df);
		}
		intent.putStringArrayListExtra("digestFields", dfs);
		context.startService(intent);
	}
}
