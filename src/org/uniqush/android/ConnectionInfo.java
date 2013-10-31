/*
 * Copyright 2013 Nan Deng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.uniqush.android;

import java.util.ArrayList;
import java.util.List;

public class ConnectionInfo {
	private String host;
	private int port;
	private String service;
	private String username;
	private boolean subscribe;

	private boolean visible = true;
	private int compressThreshold = 1024;
	private int digestThreshold = 4096;
	private List<String> digestFields;

	public boolean isVisible() {
		return visible;
	}

	public int getCompressThreshold() {
		return this.compressThreshold;
	}

	public int getDigestThreshold() {
		return this.digestThreshold;
	}

	public List<String> getDigestFields() {
		return this.digestFields;
	}

	protected boolean shouldReconfig(ConnectionInfo i) {
		if (i == null) {
			return true;
		}
		if (!(i.compressThreshold < 0 && this.compressThreshold < 0)) {
			if (i.compressThreshold != this.compressThreshold) {
				return true;
			}
		}

		if (!(i.digestThreshold < 0 && this.digestThreshold < 0)) {
			if (i.digestThreshold != this.digestThreshold) {
				return true;
			}
		}

		if (i.digestFields == null && this.digestFields == null) {
			return false;
		}

		if ((i.digestFields == null && this.digestFields != null)
				|| (i.digestFields != null && this.digestFields == null)) {
			return true;
		}

		if (i.digestFields.size() != this.digestFields.size()) {
			return true;
		}

		// XXX We do consider order though, which is not necessary.
		if (!i.digestFields.equals(this.digestFields)) {
			return true;
		}

		return false;
	}

	/**
	 * 
	 * 
	 * @param host
	 *            The host name of the server. Could be domain name, IP address.
	 * @param port
	 *            The port number of the server. port <= 0 means the default
	 *            port: 8964.
	 * @param service
	 *            The service name. Service name should be less than 10
	 *            characters, and should not contain the following characters:
	 *            '\n', '\t', ',', ';', ':'.
	 * @param username
	 *            The username. User name should be less than 30 characters, and
	 *            should not contain the following characters: '\n', '\t', ',',
	 *            ';', ':'.
	 * @param subscribe
	 *            If this value is true, then it means the user wants to receive
	 *            push notifications if he is not online.
	 * @param visible
	 *            If this value is false, then the client will be considered as
	 *            offline even if there's a connection with the server. The
	 *            client can still receive messages/digests. However, the server
	 *            will send a push notification to all clients if there is no
	 *            online client for the receiver.
	 * @param compressThreshold
	 *            > 0: If the message size is larger than the threshold, then
	 *            the message will be compressed.
	 * 
	 *            =< 0: Never compress the message.
	 * @param digestThreshold
	 *            >= 0: If a message sent to this client is larger than the
	 *            threshold, then a message digest will be sent rather than the
	 *            message itself.
	 * 
	 *            < 0: Ask the server to always send the message itself.
	 * @param digestFields
	 *            If a digest of a message needs to be sent, then headers whose
	 *            field name is in the digestFields will be sent along the
	 *            digest as parameters.
	 * @throws IllegalArgumentException
	 *             when any argument is invalid.
	 */
	public ConnectionInfo(String host, int port, String service,
			String username, boolean subscribe, boolean visible,
			int compressThreshold, int digestThreshold, String... digestFields) {
		validateServiceName(service);
		validateUsername(username);
		if (port <= 0) {
			port = DEFAULT_PORT;
		}

		// TODO validate the host name
		this.host = host;
		this.port = port;
		this.service = service;
		this.username = username;
		this.subscribe = subscribe;

		this.visible = visible;
		this.compressThreshold = compressThreshold;
		this.digestThreshold = digestThreshold;
		if (digestFields.length > 0) {
			this.digestFields = new ArrayList<String>(digestFields.length);
			for (String f : digestFields) {
				this.digestFields.add(f);
			}
		}
	}

	private static final int DEFAULT_PORT = 8964;
	private static final int MAX_SERVICE_NAME_LENGTH = 10;
	private static final int MAX_USER_NAME_LENGTH = 30;

	private void validateString(String name, String str, int maxLen,
			String stopChars) {
		if (str.length() > maxLen) {
			throw new IllegalArgumentException(name + " is too long");
		}
		for (int i = 0; i < stopChars.length(); i++) {
			int ch = stopChars.charAt(i);
			if (str.indexOf(ch) >= 0) {
				throw new IllegalArgumentException(name
						+ " should not contain " + (char) ch);
			}
		}
	}

	private void validateServiceName(String service) {
		validateString("Service name", service, MAX_SERVICE_NAME_LENGTH,
				"\n\t,;:");
	}

	private void validateUsername(String username) {
		validateString("User name", username, MAX_USER_NAME_LENGTH, "\n\t,;:");
	}

	/**
	 * 
	 * @param host
	 *            The host name of the server. Could be domain name, IP address.
	 * @param port
	 *            The port number of the server. port <= 0 means the default
	 *            port: 8964.
	 * @param service
	 *            The service name. Service name should be less than 10
	 *            characters, and should not contain the following characters:
	 *            '\n', '\t', ',', ';', ':'.
	 * @param username
	 *            The username. User name should be less than 30 characters, and
	 *            should not contain the following characters: '\n', '\t', ',',
	 *            ';', ':'.
	 * @throws IllegalArgumentException
	 *             when any argument is invalid.
	 */
	public ConnectionInfo(String host, int port, String service, String username) {
		this(host, port, service, username, true, true, 1024, -1);
	}

	/**
	 * @return the host
	 */
	public String getHostName() {
		return host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return service name
	 */
	public String getServiceName() {
		return service;
	}

	/**
	 * @return the username
	 */
	public String getUserName() {
		return username;
	}

	/**
	 * @return returns true if the user wants to receive push notifications when
	 *         he is offline.
	 */
	public boolean shouldSubscribe() {
		return subscribe;
	}

	protected void unsubscribe() {
		this.subscribe = false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof ConnectionInfo) {
			ConnectionInfo c = (ConnectionInfo) obj;
			if (service.equals(c.service) && username.equals(c.username)
					&& host.equals(c.host) && port == c.port) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return host + ":" + port + ";" + service + "," + username;
	}
}
