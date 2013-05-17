package org.uniqush.android;

import java.security.interfaces.RSAPublicKey;

import android.content.Intent;

public class MessageCenter {
	public MessageCenter(String ...senderIds) {
		for (String s : senderIds) {
			ResourceManager.getResourceManager().setSenderIds(s);
		}
	}
	
	public void connectServer(String address,
			int port, 
			RSAPublicKey publicKey, 
			String service, 
			String username, 
			String token,
			MessageHandler handler) {
		ConnectionParameter param = new ConnectionParameter(address,
				port,
				publicKey,
				service,
				username,
				handler);
		ResourceManager.getResourceManager().addConnectionParameter(param);
		Intent intent = new Intent();
	}
}
