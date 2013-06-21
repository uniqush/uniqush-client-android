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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private MessageHandler handler;
	private Semaphore handlerLock;
	private AtomicBoolean shouldSubscribe;
	private String TAG = "UniqushMessageCenterService";

	protected final static int CMD_CONNECT = 1;
	protected final static int CMD_SEND_MSG_TO_SERVER = 2;
	protected final static int CMD_SEND_MSG_TO_USER = 3;
	protected final static int CMD_REQUEST_MSG = 4;
	protected final static int CMD_CONFIG = 5;
	protected final static int CMD_SET_VISIBILITY = 6;
	protected final static int CMD_MAX_ID_REQUIRES_CONN = 7;
	protected final static int CMD_SUBSCRIBE = 8;
	protected final static int CMD_UNSUBSCRIBE = 9;
	protected final static int CMD_MESSAGE_DIGEST = 10;
	protected final static int CMD_HANDLER_READY = 11;
	protected final static int CMD_MAX_CMD_ID = 12;

	abstract class AsyncTryWithExpBackOff extends
			AsyncTask<Integer, Void, Void> {
		abstract protected void call() throws InterruptedException,
				IOException, LoginException;

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
				try {
					call();
				} catch (InterruptedException e) {
					return null;
				} catch (IOException e) {
					try {
						Log.i(TAG, "try one more time. wait time: " + time);
						Thread.sleep(time);
						time += (long) (Math.random() * time) << 1;
						error = e;

						threadLock.acquireUninterruptibly();
						if (receiverThread != null && center != null) {
							center.stop();
							receiverThread.join();
						}
						center.connect(defaultParam.address, defaultParam.port,
								defaultParam.service, defaultParam.username,
								defaultToken, defaultParam.publicKey,
								getMessageHandler());
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
		this.handler = null;
		this.threadLock = new Semaphore(1);
		this.handlerLock = new Semaphore(1);
		this.shouldSubscribe = new AtomicBoolean(true);
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
		ConnectionParameter param = null;
		String token = null;
		if (cid.equals("default")) {
			param = this.defaultParam;
			token = this.defaultToken;
		} else {
			param = ResourceManager.getResourceManager()
					.getConnectionParameter(cid);
			token = intent.getStringExtra("token");
		}
		if (param == null || token == null) {
			return false;
		}
		this.connectToServer(id, param, token);
		if (this.defaultParam == null || this.defaultToken == null) {
			return false;
		}
		return true;
	}

	private MessageHandler getMessageHandler() {
		this.handlerLock.acquireUninterruptibly();
		if (this.handler == null) {
			this.handler = ResourceManager.getMessageHandler(this);
		}
		MessageHandler handler = this.handler;
		this.handlerLock.release();
		return handler;
	}

	private void reportResult(int id, Exception e) {
		MessageHandler handler = this.getMessageHandler();
		if (handler != null) {
			if (id <= 0 && e != null) {
				handler.onError(e);
			}
			handler.onResult(id, e);
		}
	}

	private void requestMessage(final int id, final String msgId) {
		Log.i(TAG, "retrieveMessage");
		if (this.defaultParam == null) {
			return;
		}

		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				center.requestMessage(msgId);
			}
		};
		task.execute(Integer.valueOf(id));
	}

	private void sendMessageToUser(final int id, final String service,
			final String username, final Message msg, final int ttl) {
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

	private void connectToServer(final int id, ConnectionParameter param,
			String token) {
		if (param == null) {
			Log.i(TAG, "null param");
			return;
		}

		threadLock.acquireUninterruptibly();
		if (param.equals(this.defaultParam) && token.equals(this.defaultToken)
				&& this.receiverThread != null) {
			// The thread is still running. We don't need to re-connect.
			Log.i(TAG, "still running, no need to re-connect");
			threadLock.release();
			return;
		}

		Log.i(TAG, "have to re-connect. old connection: " + this.defaultParam
				+ "; new connection: " + param);
		this.defaultParam = null;
		this.defaultToken = null;
		// There's another connection. Close it first.
		if (this.receiverThread != null) {
			threadLock.release();
			this.center.stop();
			try {
				threadLock.acquireUninterruptibly();
				this.receiverThread.join();
				this.receiverThread = null;
			} catch (InterruptedException e) {
				return;
			}
		}
		this.receiverThread = new Thread(center);
		threadLock.release();
		this.defaultParam = param;
		this.defaultToken = token;
		if (this.center == null) {
			this.center = new MessageCenter();
		}

		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException,
					LoginException {
				center.connect(defaultParam.address, defaultParam.port,
						defaultParam.service, defaultParam.username,
						defaultToken, defaultParam.publicKey,
						getMessageHandler());

				threadLock.acquireUninterruptibly();
				receiverThread.start();
				threadLock.release();
			}
		};
		task.execute(Integer.valueOf(id), Integer.valueOf(2));
	}

	private synchronized void subscribeInSameThread(String regId)
			throws InterruptedException, IOException {
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

	private void unsubscribeInSameThread(String regId)
			throws InterruptedException, IOException {
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
				if (!isConnected()) {
					throw new IOException("not connected");
				}
				subscribeInSameThread(newRegId);
				Log.i(TAG, "registerED in subscribe() ");
			}
		};
		task.execute(Integer.valueOf(0), Integer.valueOf(4),
				Integer.valueOf(5000));
	}

	private synchronized void unsubscribe(final String regId) {
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				if (!isConnected()) {
					throw new IOException("not connected");
				}
				unsubscribeInSameThread(regId);
				Log.i(TAG, "registerED in subscribe() ");
			}
		};
		task.execute(Integer.valueOf(0), Integer.valueOf(4),
				Integer.valueOf(5000));
	}

	private void config(int id, final int digestThreshold,
			final int compressThreshold, final List<String> digestFields) {

		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				if (!isConnected()) {
					throw new IOException("Not connected for config");
				}
				center.config(digestThreshold, compressThreshold, digestFields);
			}
		};

		task.execute(Integer.valueOf(id));
	}
	
	private void setVisibility(int id, final boolean visible) {

		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException {
				if (!isConnected()) {
					throw new IOException("Not connected for config");
				}
				center.setVisibility(visible);
			}
		};
		task.execute(Integer.valueOf(id));
	}

	@SuppressWarnings("unchecked")
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
			Log.i(TAG, "wrong command: " + cmd);
			this.stopSelf();
			return START_NOT_STICKY;
		}

		int id = intent.getIntExtra("id", -1);
		if (cmd < MessageCenterService.CMD_MAX_ID_REQUIRES_CONN) {
			if (!this.reconnect(id, intent)) {
				MessageHandler handler = this.getMessageHandler();
				if (handler != null) {
					handler.onError(new IOException(
							"cannot connect to server: bad argument"));
				}
				this.stopSelf();
				return START_NOT_STICKY;
			}
		}
		Message msg = null;
		switch (cmd) {
		case MessageCenterService.CMD_HANDLER_READY:
			Log.i(TAG, "handler is ready");
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					getMessageHandler();
					Log.i(TAG, "have set the handler");
					return null;
				}
			};
			task.execute();
			break;
		case MessageCenterService.CMD_CONNECT:
			Log.i(TAG, "processing connect command");
			break;
		case MessageCenterService.CMD_SEND_MSG_TO_SERVER:
			msg = (Message) intent.getSerializableExtra("msg");
			if (msg != null) {
				this.sendMessageToServer(id, msg);
			}
			break;
		case MessageCenterService.CMD_SEND_MSG_TO_USER:
			msg = (Message) intent.getSerializableExtra("msg");
			String service = intent.getStringExtra("service");
			String username = intent.getStringExtra("username");
			int ttl = intent.getIntExtra("ttl", 0);
			if (msg != null && username != null) {
				this.sendMessageToUser(id, service, username, msg, ttl);
			}
			break;
		case MessageCenterService.CMD_SUBSCRIBE:
			// We should not subscribe this device for some reason.
			// e.g.: It is invisible
			if (!this.shouldSubscribe.get()) {
				break;
			}
			String regId = GCMRegistrar.getRegistrationId(this);
			if (regId.equals("")) {
				Log.i(TAG, "not registered");
				GCMRegistrar.register(this, ResourceManager.getSenderIds(this));
				break;
			} else {
				if (GCMRegistrar.isRegisteredOnServer(this)) {
					break;
				}
			}
			this.subscribe(regId);
			break;
		case MessageCenterService.CMD_UNSUBSCRIBE:
			regId = intent.getStringExtra("regId");
			if (regId != null && !regId.equals("")) {
				this.unsubscribe(regId);
			}
			break;
		case MessageCenterService.CMD_MESSAGE_DIGEST:
			MessageHandler handler = getMessageHandler();
			if (handler != null) {
				String msgId = intent.getStringExtra("msgId");
				int size = intent.getIntExtra("size", 0);
				HashMap<String, String> params = null;
				if (intent.hasExtra("params")) {
					params = (HashMap<String, String>) intent
							.getSerializableExtra("params");
				}
				if (intent.hasExtra("sender")) {
					String sender = intent.getStringExtra("sender");
					String senderService = intent.getStringExtra("service");
					handler.onMessageDigestFromUser(senderService, sender,
							size, msgId, params);
				} else {
					handler.onMessageDigestFromServer(size, msgId, params);
				}
			}
			break;
		case MessageCenterService.CMD_CONFIG:
			int compressThreshold = intent
					.getIntExtra("compressThreshold", 512);
			int digestThreshold = intent.getIntExtra("digestThreshold", 1024);
			ArrayList<String> digestFields = intent
					.getStringArrayListExtra("digestFields");
			this.config(id, digestThreshold, compressThreshold, digestFields);
			break;
		case MessageCenterService.CMD_REQUEST_MSG:
			String msgId = intent.getStringExtra("msgId");
			if (msgId != null) {
				this.requestMessage(id, msgId);
			}
			break;
		case MessageCenterService.CMD_SET_VISIBILITY:
			boolean visible = intent.getBooleanExtra("visible", true);
			this.setVisibility(id, visible);
			break;
		}
		Log.i(TAG, "successfully processed one command");
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
		MessageHandler handler = this.getMessageHandler();
		handler.onServiceDestroyed();
		super.onDestroy();
	}

	/*
	 * private void printMap(Map<String, String> header) {
	 * Iterator<Entry<String,String>> iter = header.entrySet().iterator(); while
	 * (iter.hasNext()) { Entry<String, String> entry = iter.next(); Log.i(TAG,
	 * "[" + entry.getKey() + "=" + entry.getValue() + "]"); } }
	 * 
	 * private void printMessage(Message msg) { Map<String, String> header =
	 * msg.getHeader(); printMap(header); }
	 */

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
