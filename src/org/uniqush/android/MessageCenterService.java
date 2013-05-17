package org.uniqush.android;

import org.uniqush.client.MessageCenter;

import android.app.IntentService;
import android.content.Intent;

public class MessageCenterService extends IntentService {
	
	private MessageCenter center;
	
	public MessageCenterService() {
		super("UniqushMessageCenterService");
		this.center = new MessageCenter();
	}
	
	@Override
	public void onDestroy() {
		this.center.stop();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
	}

}
