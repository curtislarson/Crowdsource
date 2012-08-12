/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource.ui;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PrivateMessageList extends Activity {

	private ServerUtil _su;
	private PrivateMessageAdapter _privateMessageAdapter;
	private ListView _privateMessageView;

	//TODO Make the private message threads stand out more
	//they are way to small.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.privatemessagelist);

		_su = ((MyApplication) getApplication()).getServerUtil();
		setupPrivateMessageList(savedInstanceState);
	}

	private void setupPrivateMessageList(Bundle saved) {
		ArrayList<Chat> privateMessageList = _su.getPrivateMessageList();

		_privateMessageAdapter = new PrivateMessageAdapter(this,
				R.layout.private_message_thread, privateMessageList);
		_privateMessageView = (ListView) findViewById(R.id.privateMessageList);
		_privateMessageView.setAdapter(_privateMessageAdapter);

		_privateMessageView.setOnItemClickListener(privateMessageClickListener);
		
		registerForContextMenu(_privateMessageView);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		// Make sure that the view is the listview.
		if (v.getId() == R.id.privateMessageList) {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle("Action on private message with "
					+ _privateMessageAdapter.getItem(info.position).getParticipant());
			String[] menuItems = getResources().getStringArray(
					R.array.privatemessage_user_options);
			for (int i = 0; i < menuItems.length; i++) {
				menu.add(i, i, i, menuItems[i]);

			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		
		//Open
		case 0:
			_su.setCurrentPrivateChat(_privateMessageAdapter.getItem(info.position));
			startPrivateMessageActivity();
			break;
		//Remove
		case 1:
			_su.removePrivateChat(_privateMessageAdapter.getItem(info.position));
			_privateMessageAdapter.notifyDataSetChanged();
			break;
		}
	return true;
		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		//We need to refresh the data set just in case we might have added a chat.
		_privateMessageAdapter.notifyDataSetChanged();
	}

	private class PrivateMessageAdapter extends ArrayAdapter<Chat> {
		private ArrayList<Chat> items;

		public PrivateMessageAdapter(Context context, int textViewResourceId,
				ArrayList<Chat> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.private_message_thread, null);
			}
			// Fix not updating bug...
			Chat c = items.get(position);
			if (c != null) {
				TextView bt = (TextView) v
						.findViewById(R.id.privateMessageThreadTV);

				if (bt != null) {
					bt.setText("Chat with " + c.getParticipant());
					if (position % 2 == 1) {
						bt.setBackgroundColor(Color.parseColor("#C0D9D9"));
					} else {
						bt.setBackgroundColor(Color.parseColor("#FFFFFF"));

					}
				}
			}
			return v;
		}
	}
	
	private void startPrivateMessageActivity()
	{
		Intent privateMessageIntent = new Intent(PrivateMessageList.this,
				PrivateMessage.class);
		startActivity(privateMessageIntent);
	}

	private OnItemClickListener privateMessageClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			Chat c = (Chat) a.getItemAtPosition(position);
			// This is where we will open up the actual private chat.
			_su.setCurrentPrivateChat(c);
			startPrivateMessageActivity();
		}

	};

}
