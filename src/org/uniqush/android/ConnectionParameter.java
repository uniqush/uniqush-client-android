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
	
	@Override
	public boolean equals(Object p) {
		if (p == null) {
			return false;
		}
		if (!(p instanceof ConnectionParameter)) {
			return false;
		}
		ConnectionParameter param = (ConnectionParameter) p;
		if (param.port != this.port) 
			return false;
		if (!param.service.endsWith(this.service))
			return false;
		if (!param.username.endsWith(this.username))
			return false;
		if (!param.address.endsWith(this.address))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		// address:port:service:username
		String idfmt = "%s:%d:%s:%s";
		return String.format(idfmt, address, port, service, username);
	}
}
