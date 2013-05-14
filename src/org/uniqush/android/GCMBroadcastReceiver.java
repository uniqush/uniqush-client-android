package org.uniqush.android;

import android.content.Context;

public class GCMBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {
	@Override
	protected String getGCMIntentServiceClassName(Context context) {
		return "org.uniqush.android.GCMIntentService";
	}
}
