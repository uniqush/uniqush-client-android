package org.uniqush.android;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.login.LoginException;

import android.content.Context;
import android.util.Log;

import org.uniqush.client.MessageCenter;
import org.uniqush.client.MessageHandler;

public class ResourceManager {
	private String[] senderIds;
	private MessageCenter center;
	private MessageHandler handler;
	
	protected static ResourceManager manager;
	
	private String TAG = "ResourceManager";
	
	public static ResourceManager getResourceManager() {
		if (manager == null) {
			return new ResourceManager();
		}
		return manager;
	}
	
	protected String[] getSenderIds(Context context) {
		return this.senderIds;
	}
	
	public MessageCenter getMessageCenter() {
		return this.center;
	}
	
	public void connectServer(
			final String address,
			final int port,
			final String service,
			final String username,
			final String token,
			final RSAPublicKey pub,
			final MessageHandler msgHandler) throws UnknownHostException, LoginException, IOException {

		this.handler = msgHandler;
		
		Thread th = new Thread() {
			public void run() {
				try {
					Log.i(TAG, "Running the thread to connect to the server");
					center.connect(address, port, service, username, token, pub, msgHandler);
				} catch (Exception e) {
					if (handler != null) {
						handler.onError(e);
					}
				}
			}
		};
		th.start();
	}
	
	public MessageHandler getMessageHandler() {
		return this.handler;
	}
	
    public void setSenderIds(String ...senderIds) {
    	int n = senderIds.length;
    	this.senderIds = new String[n];
    	
    	int i = 0;
    	for (String s : senderIds) {
    		this.senderIds[i] = s;
    		i++;
    	}
    }

	protected ResourceManager() {
		this.senderIds = null;
		this.center = new MessageCenter();
	}

}
