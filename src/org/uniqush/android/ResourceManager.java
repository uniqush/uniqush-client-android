package org.uniqush.android;

import java.util.HashMap;

class ResourceManager {
	private String[] senderIds;
	private HashMap<String, ConnectionParameter> connMap;
	
	protected static ResourceManager manager;
	
	//private String TAG = "ResourceManager";
	
	public static ResourceManager getResourceManager() {
		if (manager == null) {
			return new ResourceManager();
		}
		return manager;
	}
	
	public void addConnectionParameter(ConnectionParameter param) {
		connMap.put(param.toString(), param);
	}
	
	public ConnectionParameter getConnectionParameter(String id) {
		return connMap.get(id);
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
	}

}
