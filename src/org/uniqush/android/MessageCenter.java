package org.uniqush.android;

import java.security.interfaces.RSAPublicKey;

import org.uniqush.client.Message;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageCenter {
	
	private static String TAG = "UniqushMessageCenter";
	private ConnectionParameter defaultParam;
	private String defaultToken;
	
	public MessageCenter(String ...senderIds) {
		for (String s : senderIds) {
			ResourceManager.getResourceManager().setSenderIds(s);
		}
	}
	
	public void stop(Context context) {
		Log.i(TAG, "stoping the service...");
		Intent intent = new Intent(context, MessageCenterService.class);
		context.stopService(intent);
	}
	
	public void sendMessageToServer(Context context, Message msg) {
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_SEND_MSG_TO_SERVER);
		intent.putExtra("connection", this.defaultParam.toString());
		intent.putExtra("token", this.defaultToken);
		intent.putExtra("msg", msg);
		context.startService(intent);
	}
	
	public void connectServer(Context context,
			String address,
			int port, 
			RSAPublicKey publicKey, 
			String service, 
			String username, 
			String token,
			MessageHandler handler) {
		
		Log.i(TAG, "connect in message center");
		ConnectionParameter param = new ConnectionParameter(address,
				port,
				publicKey,
				service,
				username,
				handler);
		ResourceManager.getResourceManager().addConnectionParameter(param);
		Intent intent = new Intent(context, MessageCenterService.class);
		intent.putExtra("c", MessageCenterService.CMD_CONNECT);
		intent.putExtra("connection", param.toString());
		intent.putExtra("token", token);
		this.defaultParam = param;
		this.defaultToken = token;
		context.startService(intent);
	}
}
