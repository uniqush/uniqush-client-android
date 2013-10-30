package org.uniqush.android;

import java.util.Map;

import org.uniqush.client.Message;

import android.content.Context;

class ErrorHandler implements MessageHandler {

	private Context context;
	private MessageHandler handler;

	public ErrorHandler(Context context, MessageHandler handler) {
		this.context = context;
		this.handler = handler;
	}

	@Override
	public void onMessageFromServer(String dstService, String dstUser,
			String id, Message msg) {
		handler.onMessageFromServer(dstService, dstUser, id, msg);
	}

	@Override
	public void onMessageFromUser(String dstService, String dstUser,
			String srcService, String srcUser, String id, Message msg) {
		handler.onMessageFromUser(dstService, dstUser, srcService, srcUser, id,
				msg);
	}

	@Override
	public void onMessageDigestFromServer(String dstService, String dstUser,
			int size, String id, Map<String, String> parameters) {
		handler.onMessageDigestFromServer(dstService, dstUser, size, id,
				parameters);
	}

	@Override
	public void onMessageDigestFromUser(String dstService, String dstUser,
			String srcService, String srcUser, int size, String id,
			Map<String, String> parameters) {
		handler.onMessageDigestFromUser(dstService, dstUser, srcService,
				srcUser, size, id, parameters);
	}

	@Override
	public void onCloseStart() {
		handler.onCloseStart();
	}

	@Override
	public void onClosed() {
		MessageCenter.stop(context, 0);
		handler.onClosed();
	}

	@Override
	public void onError(Exception e) {
		MessageCenter.stop(context, 0);
		handler.onError(e);
	}

	@Override
	public void onMissingAccount() {
		handler.onMissingAccount();
	}

	@Override
	public void onServiceDestroyed() {
		handler.onServiceDestroyed();
	}

	@Override
	public void onResult(int id, Exception e) {
		if (e != null) {
			MessageCenter.stop(context, 0);
		}
		handler.onResult(id, e);
	}

}
