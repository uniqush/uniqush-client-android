package org.uniqush.android;

import java.util.HashMap;

import android.util.Log;

class ResourceManager {
	private String[] senderIds;
	private HashMap<String, ConnectionParameter> connMap;
	
	private static String TAG = "UniqushResourceManager";
	protected static ResourceManager manager;
	
	//private String TAG = "ResourceManager";
	
	public static ResourceManager getResourceManager() {
		Log.i(TAG, "get reouce manager");
		if (manager == null) {
			manager = new ResourceManager();
		}
		return manager;
	}
	
	public void addConnectionParameter(ConnectionParameter param) {
		Log.i(TAG, "add connection: " + param.toString());
		connMap.put(param.toString(), param);
	}
	
	public ConnectionParameter getConnectionParameter(String id) {
		ConnectionParameter ret = connMap.get(id);
		Log.i(TAG, "get connection: " + id + "; " + ret.toString());
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

	protected ResourceManager() {
		this.senderIds = null;
		this.connMap = new HashMap<String, ConnectionParameter>();
	}

}
