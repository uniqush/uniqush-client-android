package org.uniqush.android;

import java.security.interfaces.RSAPublicKey;

class ConnectionParameter {
	public String address;
	public int port;
	public RSAPublicKey publicKey;
	public MessageHandler handler;
	public String username;
	public String service;
	
	public ConnectionParameter(String address,
			int port, 
			RSAPublicKey publicKey,
			String service,
			String username,
			MessageHandler handler) {
		this.address = address;
		this.port = port;
		this.publicKey = publicKey;
		this.service = service;
		this.username = username;
		this.handler = handler;
	}
	
	
	public String toString() {
		// address:port:service:username
		String idfmt = "%s:%d:%s:%s";
		return String.format(idfmt, address, port, service, username);
	}
}
