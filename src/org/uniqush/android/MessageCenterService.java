package org.uniqush.android;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.uniqush.client.Message;
import org.uniqush.client.MessageCenter;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class MessageCenterService extends Service {
	
	private MessageCenter center;
	private Thread receiverThread;
	
	private ConnectionParameter defaultParam;
	private String defaultToken;
	private String TAG = "UniqushMessageCenter";
	
	protected final static int CMD_CONNECT = 1;
	protected final static int CMD_SEND_MSG_TO_SERVER = 2;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.center = new MessageCenter();
		this.receiverThread = null;
		Log.i(TAG, "onCreate");
	}
	
	private void reconnect() {
		this.connectToServer(this.defaultParam, this.defaultToken);
	}
	
	private void reportError(Exception e) {
		if (this.defaultParam != null) {
			if (this.defaultParam.handler != null) {
				this.defaultParam.handler.onError(e);
			}
		}
	}
	
	private void sendMessageToServer(final Message msg, ConnectionParameter param, String token) {
		Log.i(TAG, "sendMessageToServer");
		if (this.defaultParam == null) {
			this.defaultParam = param;
			this.defaultToken = token;
		}
		reconnect();
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				int N = 3;
				long time = 3000;
				for (int i = 0; i < N; i++) {
					try {
						center.sendMessageToServer(msg);
					} catch (InterruptedException e) {
						return null;
					} catch (IOException e) {
						try {
							Thread.sleep(time);
							time = time * time;
							if (i >= N) {
								reportError(e);
							}
							reconnect();
							continue;
						} catch (InterruptedException e1) {
							return null;
						}
					}
					break;
				}
				return null;
			}
		};
		task.execute();
	}

	private void connectToServer(ConnectionParameter param, String token) {
		if (param == null) {
			Log.i(TAG, "null param");
			return;
		}
		if (param.equals(this.defaultParam) && this.receiverThread != null) {
			// The thread is still running. We don't need to re-connect.
			return;
		}
		
		this.defaultParam = null;
		this.defaultToken = null;
		// There's another connection. Close it first.
		if (this.receiverThread != null) {
			this.center.stop();
			try {
				this.receiverThread.join();
			} catch (InterruptedException e) {
				return;
			}
		}
		this.defaultParam = param;
		this.defaultToken = token;
		if (this.center == null) {
			this.center = new MessageCenter();
		}
		receiverThread = new Thread(center);
		
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					center.connect(defaultParam.address, defaultParam.port, defaultParam.service, defaultParam.username, defaultToken, defaultParam.publicKey, defaultParam.handler);
				} catch (InterruptedException e) {
					return null;
				} catch (Exception e) {
					if (defaultParam.handler != null) {
						defaultParam.handler.onError(e);
					}
					return null;
				}
				receiverThread.start();
				return null;
			}
		};
		
		task.execute();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		int cmd = intent.getIntExtra("c", -1);
		Log.i(TAG, "onStartCommand");
		
		switch (cmd) {
		case MessageCenterService.CMD_CONNECT:
			String cid = intent.getStringExtra("connection");
			ConnectionParameter param = ResourceManager.getResourceManager().getConnectionParameter(cid);
			String token = intent.getStringExtra("token");
			this.connectToServer(param, token);
			break;
		case MessageCenterService.CMD_SEND_MSG_TO_SERVER:
			Message msg = (Message)intent.getSerializableExtra("msg");
			cid = intent.getStringExtra("connection");
			param = ResourceManager.getResourceManager().getConnectionParameter(cid);
			token = intent.getStringExtra("token");
			this.printMessage(msg);
			this.sendMessageToServer(msg, param, token);
			break;
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		if (this.center != null) {
			this.center.stop();
			if (this.receiverThread != null) {
				try {
					this.receiverThread.join();
				} catch (InterruptedException e) {
				}
			}
		}
		this.receiverThread = null;
		if (this.defaultParam != null) {
			if (this.defaultParam.handler != null) {
				this.defaultParam.handler.onServiceDestroyed();
			}
		}
		super.onDestroy();
	}
	
	private void printMap(Map<String, String> header) {
		Iterator<Entry<String,String>> iter = header.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			Log.i(TAG, "[" + entry.getKey() + "=" + entry.getValue() + "]");
		}
	}

	private void printMessage(Message msg) {
		Map<String, String> header = msg.getHeader();
		printMap(header);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
