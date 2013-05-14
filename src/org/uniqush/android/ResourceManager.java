package org.uniqush.android;

public class ResourceManager {
	private String[] senderIds;
	
	protected static ResourceManager manager;
	
	public static ResourceManager getMessageCenter() {
		if (manager == null) {
			return new ResourceManager();
		}
		return manager;
	}
	
	public String[] getSenderIds() {
		return senderIds;
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
