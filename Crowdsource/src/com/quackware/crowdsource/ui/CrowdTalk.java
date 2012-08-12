/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource.ui;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import com.quackware.crowdsource.Crowd;
import com.quackware.crowdsource.R;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;

public class CrowdTalk extends MessageActivity {

	private String _crowdId;
	private String _crowdName;
	private Location _crowdLocation;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowdtalk);
		setupButtonClickListeners();
		loadPreferences();
		setupChat(savedInstanceState);

		Crowd c = _su.getCrowd();
		_crowdId = c.getCrowdId();
		_crowdName = c.getCrowdName();
		_crowdLocation = c.getCrowdLocation();

		_su.registerCrowdTalkMessageListener(listener);
		_su.registerChatListener(this);
		
		if(savedInstanceState == null)
		{
			if(_su.isAnonymous())
			{
				sendServerMessage("Anonymous has joined the Crowd.");
			}
			else
				sendServerMessage(_su.getUsername() + " has joined the Crowd.");
		}
	}

	PacketListener listener = new PacketListener() {
		@Override
		public void processPacket(Packet packet) {
			if (packet instanceof org.jivesoftware.smack.packet.Message) {
				org.jivesoftware.smack.packet.Message mes = (org.jivesoftware.smack.packet.Message) packet;
				if (!mes.getBody().equals("null")
						&& !mes.getSubject().equals("null")) {
					Message m = new Message(mes.getBody(), mes.getSubject());
					updateMessage(m);
				}
			}

		}

	};

	@Override
	public void onStop() {
		super.onStop();
		try {
			if (isFinishing()) {
				if(_su.isAnonymous())
				{
					sendServerMessage("Anonymous has left the Crowd.");
				}
				else
				{
					sendServerMessage(_su.getUsername() + " has left the Crowd.");
				}
				_su.disconnectMUC();
				_su.removeCrowdTalkMessageListener(listener);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancelAll();
		}

		// _su.disconnectMUC();

	}

	public void getMsg(String message, String username) {
		final Message m = new Message(message, username);
		updateMessage(m);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		default:
			return super.onCreateDialog(id);
		}

	}

	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) {
	 * switch(item.getItemId()) { case R.id.exitCrowdTalk: this.finish(); return
	 * true; case R.id.viewProfile: /*if(_viewProfileTask == null) {
	 * _viewProfileTask = new ViewProfileTask(this);
	 * _viewProfileTask.execute(_su.getUsername()); } else {
	 * _viewProfileTask.attach(this); } return true; case R.id.help: return
	 * true; default: return super.onOptionsItemSelected(item); } }
	 * 
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { MenuInflater
	 * inflater = getMenuInflater();
	 * inflater.inflate(R.menu.crowdtalkoptionsmenu, menu); return true; }
	 */
	@Override
	public void sendMessage() {
		if (_su.isConnected()) {
			// TODO Add something to parse / messages, or we could do that as a
			// plugin in the server.
			EditText et = (EditText) findViewById(R.id.edtInput);
			Editable message = et.getText();
			_su.sendMessage(message.toString(), _crowdName,false);
			et.setText("");
		}

	}
	
	@Override
	public void sendServerMessage(String message)
	{
		if (_su.isConnected()) {
			// TODO Add something to parse / messages, or we could do that as a
			// plugin in the server.
			_su.sendMessage(message, _crowdName,true);
		}
	}
	
}
