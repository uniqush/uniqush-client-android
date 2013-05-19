package org.uniqush.android;

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
	
	private void connectToServer(ConnectionParameter param, String token) {
		if (param == null) {
			Log.i(TAG, "null param");
			return;
		}
		if (param.equals(this.defaultParam)) {
			// The thread is still running. We don't need to re-connect.
			if (this.receiverThread != null) {
				return;
			}
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
				receiverThread = new Thread(center);
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
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
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
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
