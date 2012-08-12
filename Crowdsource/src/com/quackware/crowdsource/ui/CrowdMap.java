/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.MyDataStore;
import com.quackware.crowdsource.ParcelableGeoPoint;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;

public class CrowdMap extends MapActivity {

	// http://code.google.com/android/add-ons/google-apis/maps-overview.html#useslibrary
	// All the shenanigans with setting up maps.

	// The google mapview stuff relies on an api key which uses an md5 of the
	// keystore
	// right now it is set up to use the debug keystore, make sure to change it
	// to
	// my own keystore when deploying.

	private MapView _mv;

	private List<Overlay> _mapOverlays;
	private Drawable _drawable;
	private ItemizedOverlay _itemizedOverlay;

	private LoadPointsTask _loadPointsTask;

	private ServerUtil _su;

	private MyDataStore _dataStore;

	// In the future probably want to change the icon, adjust the zoom level
	// Another cool feature would be able to chat through the map and messages
	// from
	// certain people would pop up above their icon.

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		loadPreferences();
		_su = ((MyApplication) getApplication()).getServerUtil();
		if (_loadPointsTask == null) {
			_loadPointsTask = new LoadPointsTask(this);
			_loadPointsTask.execute();
		} else {
			_loadPointsTask.attach(this);
		}
	}

	private void setupMapView(ArrayList<ParcelableGeoPoint> points,
			Location crowdCenter) {
		_mv = (MapView) findViewById(R.id.map);
		_mv.setBuiltInZoomControls(true);

		_mapOverlays = _mv.getOverlays();
		_drawable = this.getResources().getDrawable(R.drawable.marker);
		_itemizedOverlay = new ItemizedOverlay(_drawable);

		GeoPoint centerPoint = new GeoPoint(
				(int) (crowdCenter.getLatitude() * 1000000), (int) (crowdCenter
						.getLongitude() * 1000000));
		_mv.getController().setCenter(centerPoint);

		// Inbetween 1 and 21, probably should find the best number.
		_mv.getController().setZoom(10);

		for (ParcelableGeoPoint point : points) {
			OverlayItem item = new OverlayItem(point.getGeoPoint(), "", "");
			_itemizedOverlay.addOverlay(item);
		}
		// Only add if we actually have points.
		if (points.size() > 0) {
			_mapOverlays.add(_itemizedOverlay);
		}

		// We also need to set the center point with no drawable.

	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadPreferences() {
		_dataStore = (MyDataStore) getLastNonConfigurationInstance();
		if (_dataStore != null) {
			_loadPointsTask = _dataStore.getLoadPointsTask();
		}
	}

	public Object onRetainNonConfigurationInstance() {
		if (_dataStore == null) {
			_dataStore = new MyDataStore();
		}
		if (_loadPointsTask != null) {
			_loadPointsTask.detatch();
		}
		_dataStore.setLoadPointsTask(_loadPointsTask);
		return _dataStore;
	}

	public class LoadPointsTask extends
			AsyncTask<Void, Void, ArrayList<ParcelableGeoPoint>> {
		private CrowdMap activity;
		private ProgressDialog _showMapSpinner;

		public LoadPointsTask(CrowdMap ct) {
			attach(ct);
		}

		@Override
		protected void onPreExecute() {
			_showMapSpinner = new ProgressDialog(CrowdMap.this);
			_showMapSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_showMapSpinner.setMessage(getString(R.string.buildMap));
			_showMapSpinner.setCancelable(false);
			_showMapSpinner.show();
		}

		@Override
		protected ArrayList<ParcelableGeoPoint> doInBackground(Void... arg0) {
			return _su.getUserLocations();
		}

		protected void onPostExecute(ArrayList<ParcelableGeoPoint> points) {
			activity.removeLoadPointsTask();
			if (_showMapSpinner != null) {
				_showMapSpinner.dismiss();
				_showMapSpinner = null;
			}

			if (points != null && _su.getCrowd().getCrowdLocation() != null) {
				activity
						.setupMapView(points, _su.getCrowd().getCrowdLocation());
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_DISPLAY_MAP), Toast.LENGTH_SHORT).show();
				CrowdMap.this.finish();
			}

		}

		public void attach(CrowdMap ct) {
			activity = ct;
		}

		public void detatch() {
			activity = null;
		}

	}

	private void removeLoadPointsTask() {
		_loadPointsTask = null;
	}
}
