/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class ParcelableGeoPoint implements Parcelable {

	private GeoPoint _geoPoint;

	public ParcelableGeoPoint(GeoPoint point) {
		_geoPoint = point;
	}

	public GeoPoint getGeoPoint() {
		return _geoPoint;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeInt(_geoPoint.getLatitudeE6());
		out.writeInt(_geoPoint.getLongitudeE6());
	}

	public static final Parcelable.Creator<ParcelableGeoPoint> CREATOR = new Parcelable.Creator<ParcelableGeoPoint>() {
		public ParcelableGeoPoint createFromParcel(Parcel in) {
			return new ParcelableGeoPoint(in);
		}

		public ParcelableGeoPoint[] newArray(int size) {
			return new ParcelableGeoPoint[size];
		}
	};

	private ParcelableGeoPoint(Parcel in) {
		int lat = in.readInt();
		int lon = in.readInt();
		_geoPoint = new GeoPoint(lat, lon);
	}

}
