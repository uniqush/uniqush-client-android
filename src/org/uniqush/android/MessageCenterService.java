package org.uniqush.android;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.LoginException;

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
	protected final static int CMD_SEND_MSG_TO_USER = 3;
	protected final static int CMD_REQUEST_MSG = 4;
	protected final static int CMD_CONFIG = 5;
	protected final static int CMD_VISIBILITY = 6;
	protected final static int CMD_MAX_CMD_ID = 7;
	

abstract class AsyncTryWithExpBackOff extends AsyncTask<Integer, Void, Void> {
	abstract protected void call() throws InterruptedException, IOException, LoginException ;
	
	@Override
	protected Void doInBackground(Integer... params) {
		int N = 3;
		long time = 6000;
		int id = params[0].intValue();
		Exception error = null;
		for (int i = 0; i < N; i++) {
			try {
				call();
			} catch (InterruptedException e) {
				return null;
			} catch (Exception e) {
				try {
					Thread.sleep(time);
					time += (long) (Math.random()* time) << 1;
					error = e;
					reconnect(id);
					continue;
				} catch (InterruptedException e1) {
					error = e;
					break;
				}
			}
			break;
		}
		reportResult(id, error);
		return null;
	}
}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.center = new MessageCenter();
		this.receiverThread = null;
		Log.i(TAG, "onCreate");
	}
	
	private void reconnect(int id) {
		this.connectToServer(id, this.defaultParam, this.defaultToken);
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
				if (id <= 0) {
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
		AsyncTryWithExpBackOff task = new AsyncTryWithExpBackOff() {
			@Override
			protected void call() throws InterruptedException, IOException, LoginException {
				center.connect(defaultParam.address, defaultParam.port, defaultParam.service, defaultParam.username, defaultToken, defaultParam.publicKey, defaultParam.handler);
				receiverThread.start();
			}
		};
		task.execute(Integer.valueOf(id));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		int cmd = intent.getIntExtra("c", -1);
		Log.i(TAG, "onStartCommand");
		
		if (cmd <= 0 || cmd >= MessageCenterService.CMD_MAX_CMD_ID) {
			// Bad call.
			this.stopSelf();
			return START_NOT_STICKY;
		}

		int id = intent.getIntExtra("id", -1);
		if (!this.reconnect(id, intent)) {
			// cannot connect to the server.
			this.stopSelf();
			return START_NOT_STICKY;
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
		case MessageCenterService.CMD_SEND_MSG_TO_USER:
			msg = (Message)intent.getSerializableExtra("msg");
			String service = intent.getStringExtra("service");
			String username = intent.getStringExtra("username");
			int ttl = intent.getIntExtra("ttl", 0);
			if (msg != null && username != null) {
				this.sendMessageToUser(id, service, username, msg, ttl);
			}
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
