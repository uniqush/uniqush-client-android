package org.uniqush.android;

import java.util.Hashtable;
import org.uniqush.client.MessageCenter;
import android.util.Log;

class ResourceManager {
	private String[] senderIds;
	private Hashtable<String, ConnectionParameter> connMap;
	private MessageCenter center;
	
	private static String TAG = "UniqushResourceManager";
	protected static ResourceManager manager;
	
	//private String TAG = "ResourceManager";
	
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
	
	public void setSenderIds(String ...senderIds) {
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
