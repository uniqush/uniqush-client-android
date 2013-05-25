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

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import javax.security.auth.login.LoginException;

import org.uniqush.client.Message;
import org.uniqush.client.MessageCenter;

import com.google.android.gcm.GCMRegistrar;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class MessageCenterService extends Service {
	
	private MessageCenter center;
	private Thread receiverThread;
	private Semaphore threadLock;
	
	private ConnectionParameter defaultParam;
	private String defaultToken;
	private String regId;
	private String TAG = "UniqushMessageCenterService";
	
	protected final static int CMD_CONNECT = 1;
	protected final static int CMD_SEND_MSG_TO_SERVER = 2;
	protected final static int CMD_SEND_MSG_TO_USER = 3;
	protected final static int CMD_REQUEST_MSG = 4;
	protected final static int CMD_CONFIG = 5;
	protected final static int CMD_VISIBILITY = 6;
	protected final static int CMD_UNSUBSCRIBE = 7;
	protected final static int CMD_MAX_ID_REQUIRES_CONN = 8;
	protected final static int CMD_SUBSCRIBE = 9;
	protected final static int CMD_MAX_CMD_ID = 10;
	

abstract class AsyncTryWithExpBackOff extends AsyncTask<Integer, Void, Void> {
	abstract protected void call() throws InterruptedException, IOException, LoginException ;
	
	@Override
	protected Void doInBackground(Integer... params) {
		int N = 3;
		long time = 1000;
		int id = params[0].intValue();
		if (params.length > 1) {
			N = params[1].intValue();
		}
		if (params.length > 2) {
			time = params[2].intValue();
		}
		Exception error = null;
		for (int i = 0; i < N; i++) {
			Log.i(TAG, "try one more time. wait time: " + time);
			try {
				call();
			} catch (InterruptedException e) {
				return null;
			} catch (IOException e) {
				try {
					Thread.sleep(time);
					time += (long) (Math.random()* time) << 1;
					error = e;
					
					center.connect(defaultParam.address, defaultParam.port, defaultParam.service, defaultParam.username, defaultToken, defaultParam.publicKey, defaultParam.handler);

					threadLock.acquireUninterruptibly();
					receiverThread = new Thread(center);
					receiverThread.start();
					threadLock.release();
					break;
				} catch (IOException e1) {
					error = e1;
					continue;
				} catch (LoginException e1) {
					error = e1;
					break;
				} catch (InterruptedException e1) {
					error = e1;
					break;
				}
			} catch (LoginException e) {
				error = e;
				break;
			}
			break;
		}
		Log.i(TAG, "report result on " + id);
		reportResult(id, error);
		return null;
	}
}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.center = new MessageCenter();
		this.receiverThread = null;
		this.threadLock = new Semaphore(1);
		Log.i(TAG, "onCreate");
	}
	
	private boolean isConnected() {
		boolean ret = false;
		this.threadLock.acquireUninterruptibly();
		if (this.receiverThread != null) {
			ret = this.receiverThread.isAlive();	
		}
		this.threadLock.release();
		return ret;
	}
	
	private boolean reconnect(int id, Intent intent) {
		String cid = intent.getStringExtra("connection");
		if (cid == null) {
			return false;
		}
		ConnectionParameter param = ResourceManager.getResourceManager().getConnectionParameter(cid);
		if (param == null) {
			return false;
		}
		String token = intent.getStringExtra("token");
		if (token == null) {
			return false;
		}
		this.connectToServer(id, param, token);
		if (this.defaultParam == null || this.defaultToken == null) {
			return false;
		}
		return true;
	}
	
	private void reportResult(int id, Exception e) {
		if (this.defaultParam != null) {
			if (this.defaultParam.handler != null) {
				if (id <= 0 && e != null) {
					this.defaultParam.handler.onError(e);
				}
				this.defaultParam.handler.onResult(id, e);
			}
		}
	}
	
	private void sendMessageToUser(final int id,
			final String service,
			final String username,
			final Message msg,
			final int ttl) {
		Log.i(TAG, "sendMessageToUser");
		if (this.defaultParam == null) {
			return;
		}
		
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				center.sendMessageToUser(service, username, msg, ttl);
			}
		};
		task.execute(Integer.valueOf(id));
	}
	
	private void sendMessageToServer(int id, final Message msg) {
		Log.i(TAG, "sendMessageToServer");
		if (this.defaultParam == null) {
			return;
		}
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				center.sendMessageToServer(msg);
			}
		};
		task.execute(Integer.valueOf(id));
	}

	private void connectToServer(final int id, ConnectionParameter param, String token) {
		if (param == null) {
			Log.i(TAG, "null param");
			return;
		}

		threadLock.acquireUninterruptibly();
		if (param.equals(this.defaultParam) &&
				token.equals(this.defaultToken) &&
				this.receiverThread != null) {
			// The thread is still running. We don't need to re-connect.
			threadLock.release();
			return;
		}

		this.defaultParam = null;
		this.defaultToken = null;
		// There's another connection. Close it first.
		if (this.receiverThread != null) {
			threadLock.release();
			this.center.stop();
			try {
				this.receiverThread.join();
				this.receiverThread = null;
			} catch (InterruptedException e) {
				return;
			}
		} else {
			threadLock.release();
		}
		this.defaultParam = param;
		this.defaultToken = token;
		if (this.center == null) {
			this.center = new MessageCenter();
		}
		
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException, LoginException {
				center.connect(defaultParam.address, defaultParam.port, defaultParam.service, defaultParam.username, defaultToken, defaultParam.publicKey, defaultParam.handler);

				threadLock.acquireUninterruptibly();
				receiverThread = new Thread(center);
				receiverThread.start();
				threadLock.release();
				
				if (regId != null) {
					subscribeInSameThread(regId);
				}
			}
		};
		task.execute(Integer.valueOf(id), Integer.valueOf(2));
	}
	
	private synchronized void subscribeInSameThread(String regId) throws InterruptedException, IOException {
		if (this.regId != null && !this.regId.equals(regId)) {
			// We got a new regId, unsubscribe the old one first.
			unsubscribeInSameThread(regId);
		}
		if (regId.equals("")) {
			return;
		}
		final HashMap<String, String> params = new HashMap<String, String>(3);
		params.put("pushservicetype", "gcm");
		params.put("service", this.defaultParam.service);
		params.put("subscriber", this.defaultParam.username);
		params.put("regid", regId);
		center.subscribe(params);
		GCMRegistrar.setRegisteredOnServer(this, true);
	}
	
	private void unsubscribeInSameThread(String regId) throws InterruptedException, IOException {
		final HashMap<String, String> params = new HashMap<String, String>(3);
		params.put("pushservicetype", "gcm");
		params.put("service", this.defaultParam.service);
		params.put("subscriber", this.defaultParam.username);
		params.put("regid", regId);
		center.unsubscribe(params);
		GCMRegistrar.setRegisteredOnServer(this, true);
	}
	
	private void subscribe(final String newRegId) {
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				subscribeInSameThread(newRegId);
				Log.i(TAG, "registerED in subscribe() ");
			}
		};
		task.execute(Integer.valueOf(0));
	}
	
	private synchronized void unsubscribe(final String regId) {
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				unsubscribeInSameThread(regId);
				Log.i(TAG, "registerED in subscribe() ");
			}
		};
		task.execute(Integer.valueOf(0));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent == null) {
			this.stopSelf();
			return START_NOT_STICKY;
		}
		int cmd = intent.getIntExtra("c", -1);
		Log.i(TAG, "onStartCommand");
		
		if (cmd <= 0 || cmd >= MessageCenterService.CMD_MAX_CMD_ID) {
			// Bad call.
			this.stopSelf();
			return START_NOT_STICKY;
		}

		int id = intent.getIntExtra("id", -1);
		if (cmd < MessageCenterService.CMD_MAX_ID_REQUIRES_CONN) {
			if (!this.reconnect(id, intent)) {
				// cannot connect to the server.
				this.stopSelf();
				return START_NOT_STICKY;
			}
		}
		Message msg = null;
		switch (cmd) {
		case MessageCenterService.CMD_CONNECT:
			break;
		case MessageCenterService.CMD_SEND_MSG_TO_SERVER:
			msg = (Message)intent.getSerializableExtra("msg");
			if (msg != null) {
				this.sendMessageToServer(id, msg);
			}
			break;
		case MessageCenterService.CMD_SEND_MSG_TO_USER:
			msg = (Message)intent.getSerializableExtra("msg");
			String service = intent.getStringExtra("service");
			String username = intent.getStringExtra("username");
			int ttl = intent.getIntExtra("ttl", 0);
			if (msg != null && username != null) {
				this.sendMessageToUser(id, service, username, msg, ttl);
			}
			break;
		case MessageCenterService.CMD_SUBSCRIBE:
			String regId = intent.getStringExtra("regId");
			if (regId == null) {
				break;
			} else {
				this.regId = regId;
			}
			this.subscribe(regId);
			break;
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		if (this.center != null) {
			this.center.stop();
			this.threadLock.acquireUninterruptibly();
			if (this.receiverThread != null) {
				try {
					this.receiverThread.join();
				} catch (InterruptedException e) {
				}
			}
			this.threadLock.release();
		}
		this.receiverThread = null;
		if (this.defaultParam != null) {
			if (this.defaultParam.handler != null) {
				this.defaultParam.handler.onServiceDestroyed();
			}
		}
		super.onDestroy();
	}
	
/*	private void printMap(Map<String, String> header) {
		Iterator<Entry<String,String>> iter = header.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			Log.i(TAG, "[" + entry.getKey() + "=" + entry.getValue() + "]");
		}
	}

	private void printMessage(Message msg) {
		Map<String, String> header = msg.getHeader();
		printMap(header);
	}*/
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
