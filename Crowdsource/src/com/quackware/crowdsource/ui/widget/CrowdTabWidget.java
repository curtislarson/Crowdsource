package com.quackware.crowdsource.ui.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;
import com.quackware.crowdsource.ui.CrowdMap;
import com.quackware.crowdsource.ui.CrowdPreference;
import com.quackware.crowdsource.ui.CrowdTalk;
import com.quackware.crowdsource.ui.PrivateMessageList;

public class CrowdTabWidget extends TabActivity {

	private TabHost _tabHost;
	private static final int NOTIFY_ID = 1;
	private ServerUtil _su;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabs);

		_su = ((MyApplication) getApplication()).getServerUtil();

		setupTabs();

		// Check to see if the user enabled global notifications
		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getBoolean("checkboxStatusBar", true)) {
			if (savedInstanceState == null) {
				setupGlobalNotifications();
			}
		}
	}

	private void setupGlobalNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// CharSequence contentTitle = titleText;
		// CharSequence contentText = Html.fromHtml(descText);
		Intent notificationIntent = new Intent(this, CrowdTabWidget.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		// TODO Change notification icon
		Notification notification = new Notification(R.drawable.icon,
				"CrowdSource", System.currentTimeMillis());
		notification.flags = Notification.FLAG_NO_CLEAR
				| Notification.FLAG_ONGOING_EVENT;

		notification.setLatestEventInfo(getApplicationContext(), "CrowdTalk",
				"Currently connected to a room", contentIntent);

		mNotificationManager.notify(NOTIFY_ID, notification);
	}

	private void setupTabs() {

		_tabHost = getTabHost();
		_tabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
		_tabHost.setCurrentTab(0);
		setupTab("CrowdTalk", new Intent().setClass(this, CrowdTalk.class));
		setupTab("Messages", new Intent().setClass(this,
				PrivateMessageList.class));
		Intent settingsIntent = new Intent();
		settingsIntent.setClass(this, CrowdPreference.class);
		settingsIntent.putExtra("fromCrowdTalk", true);
		setupTab("Settings", settingsIntent);
		Intent mapIntent = new Intent();
		mapIntent.setClass(this, CrowdMap.class);
		mapIntent.putExtra("mapCenter", _su.getCrowd().getCrowdLocation());
		setupTab("Map", mapIntent);
		// tabHost.addTab(tabHost.newTabSpec("CrowdMap").setIndicator("CrowdMap").setContent(new
		// Intent(this,CrowdMap.class)));
		// tabHost.addTab(tabHost.newTabSpec("Settings").setIndicator("Settings").setContent(new
		// Intent(this,CrowdPreference.class)));
		//

	}

	private void setupTab(final String tag, Intent intent) {
		View tabView = createTabView(this, tag);
		TabHost.TabSpec spec = _tabHost.newTabSpec(tag).setIndicator(tabView)
				.setContent(intent);
		_tabHost.addTab(spec);
	}

	private View createTabView(final Context context, final String tag) {
		View view = LayoutInflater.from(context).inflate(R.layout.tab_custom,
				null);
		TextView tv = (TextView) view.findViewById(R.id.tabsText);
		tv.setText(tag);
		return view;
	}

}
