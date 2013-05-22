package org.uniqush.android;

public interface MessageHandler extends org.uniqush.client.MessageHandler {
	public void onMissingAccount();
	public void onServiceDestroyed();
	public void onResult(int id, Exception e);
}
