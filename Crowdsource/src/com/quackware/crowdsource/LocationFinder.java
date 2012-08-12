/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource;

import java.util.Timer;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationFinder {

	private static final String TAG = "LocationFinder";
	Timer timer;
	LocationManager lm;
	LocationResult lr;
	boolean gps_enabled = false;
	boolean network_enabled = false;
	
	public void stopGettingLocation()
	{
		Log.i(TAG,"stopGettingLocation() called");
		if(lm != null)
		{
			lm.removeUpdates(locationListenerGps);
			lm.removeUpdates(locationListenerNetwork);
		}
		lr = null;
		lm = null;
		locationListenerGps = null;
		locationListenerNetwork = null;
	}

	public boolean getLocation(Context context, LocationResult result,
			boolean newUpdate) {
		lr = result;
		if (lm == null)
			lm = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

		// exceptions will be thrown if provider is not permitted.
		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		// don't start listeners if no provider is enabled
		if (!gps_enabled && !network_enabled)
			return false;

		// if(network_enabled)
		// lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
		// locationListenerNetwork);

		// If they are asking for a new update just go straight to that
		// otherwise
		// get the last location since that will be much faster.
		if (newUpdate) {
			if (gps_enabled) {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						locationListenerGps);
			} if (network_enabled) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
						0, locationListenerNetwork);
			}
		} else {
			_getLastLocation();
		}
		return true;
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (timer != null) {
				timer.cancel();
			}
			lr.gotLocation(location);
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerNetwork);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (timer != null) {
				timer.cancel();
			}
			lr.gotLocation(location);
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerGps);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	private void _getLastLocation() {
		lm.removeUpdates(locationListenerGps);
		lm.removeUpdates(locationListenerNetwork);

		Location net_loc = null, gps_loc = null;
		if (gps_enabled)
			gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (network_enabled)
			net_loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (gps_loc != null) {
			lr.gotLocation(gps_loc);
		} else if (net_loc != null) {
			lr.gotLocation(net_loc);
		} else {
			lr.gotLocation(null);
		}
		try
		{
			lm.removeUpdates(locationListenerNetwork);
			lm.removeUpdates(locationListenerGps);
		}
		catch(Exception ex)
		{
			
		}

		/*
		 * //if there are both values use the latest one if(gps_loc!=null &&
		 * net_loc!=null){ if(gps_loc.getTime()>net_loc.getTime())
		 * lr.gotLocation(gps_loc); else lr.gotLocation(net_loc); return; }
		 * 
		 * if(gps_loc!=null){ lr.gotLocation(gps_loc); return; }
		 * if(net_loc!=null){ lr.gotLocation(net_loc); return; }
		 * lr.gotLocation(null);
		 */
	}

	public static abstract class LocationResult {
		public abstract void gotLocation(Location location);
	}

}
