package org.uniqush.android;

import org.uniqush.client.CredentialProvider;

public interface UserInfoProvider extends CredentialProvider {
	/**
	 * The app may have several user accounts. This method call only returns
	 * the current user's service name. If a connection to the app server is required to
	 * perform certain operation, then this method will be called to establish such
	 * a connection on behave of the user. (If there's already such a connection, the
	 * connection will be reused. Otherwise a new connection will be established.)
	 * 
	 * @return The current user's service name
	 */
	public String getService();
	
	/**
	 * @return The current user's name
	 */
	public String getUsername();
	
	/**
	 * @return Returns true if the current user wants to receive push notifications
	 * when he is offline. Otherwise, returns false.
	 */
	public boolean subscribe();

	/**
	 * Returns the host name (either domain name or IP address) of the server.
	 * This method will be called only when a new connection needs to be established
	 * between the app and the app server.
	 * 
	 * @return The host name of the server
	 */
	public String getHost();

	/**
	 * Returns the port of the server, on which the uniqush-conn is running.
	 * This method will be called only when a new connection needs to be established
	 * between the app and the app server.
	 * 
	 * @return the port number of the server
	 */
	public int getPort();

	/**
	 * @return The GCM's sender Ids.
	 */
	public String[] getSenderIds();
	
	/**
	 * @param host
	 * @param port
	 * @param service
	 * @param username
	 * @return The corresponding message handler.
	 */
	public MessageHandler getMessageHandler(String host, int port, String service, String username);
}
