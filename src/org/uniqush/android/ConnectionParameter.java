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

import java.security.interfaces.RSAPublicKey;

class ConnectionParameter {
	public String address;
	public int port;
	public RSAPublicKey publicKey;
	public String username;
	public String service;
	
	public ConnectionParameter(String address,
			int port, 
			RSAPublicKey publicKey,
			String service,
			String username) {
		this.address = address;
		this.port = port;
		this.publicKey = publicKey;
		this.service = service;
		this.username = username;
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
