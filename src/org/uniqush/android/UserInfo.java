package org.uniqush.android;

public class UserInfo {
	private String service;
	private String username;

	public UserInfo(String service, String username) {
		this.service = service;
		this.username = username;
	}
	
	public String getService() {
		return service;
	}

	public String getUsername() {
		return username;
	}
}
