package org.uniqush.android;

import org.uniqush.client.CredentialProvider;

public interface UserInfoProvider extends CredentialProvider {
	/**
	 * The app may have several user accounts. This method call only returns the
	 * current user's infomation. The uniqush library will use the returned
	 * information to connect the server.
	 * 
	 * The library will call this method every time when it needs to send some
	 * information to the server. If there is no existing connection with the
	 * server using the returned information, the library will establish a new
	 * connection with the server using the returned information. Otherwise, it
	 * will reuse the existing connection.
	 * 
	 * The app developer should make sure that the implementation of this method
	 * is fast enough on average because it will be called *frequently*.
	 * 
	 * @return The current user's
	 */
	public ConnectionInfo getConnectionInfo();

	/**
	 * @return The GCM's sender Ids.
	 */
	public String[] getSenderIds();

	/**
	 * This should be a pure method, or its side effect should not affect its
	 * return value.
	 * 
	 * More precisely, with same arguments, this method should return same (or
	 * equivalent) handler
	 * 
	 * @param host
	 * @param port
	 * @param service
	 * @param username
	 * @return The corresponding message handler.
	 */
	public MessageHandler getMessageHandler(String host, int port,
			String service, String username);
}
