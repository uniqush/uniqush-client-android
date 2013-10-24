package org.uniqush.android;

import org.uniqush.client.CredentialProvider;

public interface UserInfoProvider extends CredentialProvider {
	String getUsername();
	String getService();
	String getHost();
	int getPort();
	String[] getSenderIds();
	MessageHandler getMessageHandler(String host, int port, String service, String username);
}
