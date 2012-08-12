/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

//Some things we might want to store in (local) profile object.
//Name, JID, Location (distance away?), Photos?, other personal information.
//This class will be used with ProfileActivity to display information to the user.
//It will be built from information grabbed from the server.

public class Profile implements Parcelable {

	private boolean _isMyProfile;
	private Location _location;
	private String _username;

	public Profile(String username, Location location, boolean isMyProfile) {
		_isMyProfile = isMyProfile;
		_location = location;
		_username = username;
	}

	public boolean isMyProfile() {
		return _isMyProfile;
	}

	public Location getLocation() {
		return _location;
	}

	public String getUsername() {
		return _username;
	}

	public Profile(Parcel source) {
		readFromParcel(source);
	}

	private void readFromParcel(Parcel in) {
		if (in.readInt() == 0) {
			_isMyProfile = false;
		} else {
			_isMyProfile = true;
		}
		_location = in.readParcelable(Location.class.getClassLoader());
		_username = in.readString();
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		if (_isMyProfile) {
			out.writeInt(1);
		} else {
			out.writeInt(0);
		}
		out.writeParcelable(_location, 0);
		out.writeString(_username);
	}

	public static final Parcelable.Creator<Profile> CREATOR = new Parcelable.Creator<Profile>() {

		@Override
		public Profile createFromParcel(Parcel source) {
			return new Profile(source);
		}

		@Override
		public Profile[] newArray(int size) {
			return new Profile[size];
		}

	};

}
