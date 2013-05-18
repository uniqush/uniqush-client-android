package org.uniqush.android;

import java.security.interfaces.RSAPublicKey;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageCenter {
	
	private static String TAG = "UniqushMessageCenter";
	
	public MessageCenter(String ...senderIds) {
		for (String s : senderIds) {
			ResourceManager.getResourceManager().setSenderIds(s);
		}
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
		context.startService(intent);
	}
}
