/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource.ui;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quackware.crowdsource.Crowd;
import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.MyDataStore;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;
import com.quackware.crowdsource.ui.widget.CrowdTabWidget;

public class CrowdChoice extends Activity implements OnClickListener {

	// TODO
	/*
	 * Add divider between different rooms. Make "room" text stick out so people
	 * are more likely to think its clickable. Enable refresh room?
	 */

	private static final int PROMPT_NEW_CROWD = 1;
	private ServerUtil _su;

	private CrowdListTask _crowdListTask;
	private CreateCrowdTask _createCrowdTask;

	private ArrayList<Crowd> _crowdCache;

	private MyDataStore _dataStore;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowdchoice);

		loadPreferences();

		if (_crowdCache == null) {
			if (_crowdListTask == null) {
				_crowdListTask = new CrowdListTask(this);
				_crowdListTask.execute();
			} else {
				_crowdListTask.attach(this);
			}
		} else {

		}
		((Button) findViewById(R.id.refreshButton)).setOnClickListener(this);
	}

	public Object onRetainNonConfigurationInstance() {
		if (_dataStore == null) {
			_dataStore = new MyDataStore();
		}
		if (_crowdListTask != null) {
			_crowdListTask.detatch();
		}
		if (_createCrowdTask != null) {
			_createCrowdTask.detatch();
		}

		_dataStore.setCrowdListTask(_crowdListTask);
		_dataStore.setCrowdCache(_crowdCache);
		_dataStore.setCreateCrowdTask(_createCrowdTask);

		return _dataStore;
	}

	private void loadPreferences() {
		_su = ((MyApplication) this.getApplication()).getServerUtil();

		_dataStore = (MyDataStore) getLastNonConfigurationInstance();
		if (_dataStore != null) {
			_crowdListTask = _dataStore.getCrowdListTask();
			_crowdCache = _dataStore.getCrowdCache();
			_createCrowdTask = _dataStore.getCreateCrowdTask();
		}
		if (_crowdCache != null) {
			buildRoomChoices(_crowdCache);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (isFinishing()) {
					if (_su.isAuthenticated()) {
						_su.logout();
					}
				}
			}

		}).start();
	}

	private void buildRoomChoices(ArrayList<Crowd> crowdList) {

		// Set the cache
		_crowdCache = crowdList;
		LinearLayout layout = (LinearLayout) findViewById(R.id.roomLinearLayout);
		// Make sure to remove any views that might be there when we refresh.
		layout.removeAllViews();
		for (Crowd c : crowdList) {
			TextView tv = new TextView(this);
			int numPeople = c.getNumPeople();
			if (numPeople == -1) {
				// Some sort of error retrieving the number of people in the
				// room, do something.
			}
			tv.setOnClickListener(this);
			tv.setText("Room Name: "
					+ c.getCrowdName().substring(0,
							c.getCrowdName().indexOf('`'))
					+ "\nNumber of People: " + numPeople
					+ "\nDistance Away: Approx "
					+ (int) c.getDistanceFrom(_su.getLocation()) + " miles.");
			tv.setClickable(true);
			tv.setTag(c);

			layout.addView(tv);

			// layout.addView(divider);
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(_crowdCache != null)
		{
			buildRoomChoices(_crowdCache);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refreshButton:
			if (_crowdListTask == null) {
				_crowdListTask = new CrowdListTask(this);
				_crowdListTask.execute();
			} else {
				_crowdListTask.attach(this);
			}
			break;
		default:
			Crowd c = (Crowd) v.getTag();
			boolean success = _su.connectToRoom(c, false);

			if (success) {
				startCrowdTalkIntent(c);
			} else {
				// Some sort of warning if we are unable to join the room.
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_CONNECT_ROOM),
						Toast.LENGTH_SHORT).show();
			}
			break;

		}

	}

	private void startCrowdTalkIntent(Crowd c) {
		Intent crowdTalkIntent = new Intent(getApplicationContext(),
				CrowdTabWidget.class);
		_su.setCrowd(c);
		startActivity(crowdTalkIntent);
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROMPT_NEW_CROWD:
			return new AlertDialog.Builder(CrowdChoice.this).setTitle(
					"No Crowd Found.").setMessage(
					"No crowd was found, would you like to make a new one?")
					.setPositiveButton("Create a crowd",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									if (_createCrowdTask == null) {
										_createCrowdTask = new CreateCrowdTask(
												CrowdChoice.this);
										_createCrowdTask.execute();
									} else {
										_createCrowdTask
												.attach(CrowdChoice.this);
									}

								}
							}).setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// We don't need to do anything here.

								}
							}).create();
		default:
			return null;
		}

	}

	public class CreateCrowdTask extends AsyncTask<Void, Void, Boolean> {
		private CrowdChoice activity = null;
		private ProgressDialog _createCrowdSpinner;
		private Crowd _c;

		public CreateCrowdTask(CrowdChoice cc) {
			attach(cc);
		}

		@Override
		protected void onPreExecute() {
			_createCrowdSpinner = new ProgressDialog(CrowdChoice.this);
			_createCrowdSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_createCrowdSpinner.setMessage(getString(R.string.buildCrowdList));
			_createCrowdSpinner.setCancelable(false);
			_createCrowdSpinner.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			_c = _su.createCrowd();
			return _su.connectToRoom(_c, true);
		}

		protected void onPostExecute(Boolean success) {
			activity.removeCreateCrowdTask();
			if (_createCrowdSpinner != null) {
				_createCrowdSpinner.dismiss();
				_createCrowdSpinner = null;
			}
			if (_c == null) {
				success = false;
			}

			if (success) {
				startCrowdTalkIntent(_c);
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_CREATE_CROWD),
						Toast.LENGTH_SHORT).show();
				// Just close CrowdChoice which will force them back to
				// CrowdSource.
				CrowdChoice.this.finish();
			}

		}

		public void attach(CrowdChoice cc) {
			activity = cc;
		}

		public void detatch() {
			activity = null;
		}

	}

	public class CrowdListTask extends AsyncTask<Void, Void, ArrayList<Crowd>> {

		private CrowdChoice activity = null;
		private ProgressDialog _crowdListSpinner;

		public CrowdListTask(CrowdChoice cc) {
			attach(cc);
		}

		@Override
		protected void onPreExecute() {
			_crowdListSpinner = new ProgressDialog(CrowdChoice.this);
			_crowdListSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_crowdListSpinner.setMessage(getString(R.string.buildCrowdList));
			_crowdListSpinner.setCancelable(false);
			_crowdListSpinner.show();
		}

		@Override
		protected ArrayList<Crowd> doInBackground(Void... params) {
			float maxDistance = Float.parseFloat(PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext())
					.getString("crowdRadius", "50").replace(" miles", ""));
			return _su.getRoomList(maxDistance);
		}

		protected void onPostExecute(ArrayList<Crowd> crowdList) {

			try {
				if (_crowdListSpinner != null) {
					_crowdListSpinner.dismiss();
					_crowdListSpinner = null;
				}
			} catch (Exception ex) {
				_crowdListSpinner = null;
			}
			if (crowdList == null || crowdList == Collections.EMPTY_LIST
					|| crowdList.size() == 0) {
				// This is where we prompt them to make a new room since there
				// are no rooms available.
				showDialog(PROMPT_NEW_CROWD);

			} else {
				buildRoomChoices(crowdList);
			}
			activity.removeCrowdListTask();
		}

		public void attach(CrowdChoice cc) {
			activity = cc;
		}

		public void detatch() {
			activity = null;
		}

	}

	public void removeCrowdListTask() {
		_crowdListTask = null;
	}

	public void removeCreateCrowdTask() {
		_createCrowdTask = null;
	}
}
