/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

public class Utility {

	public static boolean containsBadCharacters(String username) {
		// Only allow a-Z and 0-9
		Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
		Matcher m = p.matcher(username);
		return !m.matches();
	}

	public static boolean containsBadStrings(String username) {
		String l = username.toLowerCase();
		// I don't really care about curse words, just people trying to be a
		// jackass and imitate me.
		return l.contains("admin") || l.contains("quackware")
				|| l.contains("moderator") || l.contains("crowdsource");
	}

	public static String getCityNameFromGPS(Location location, Context context) {
		try {
			Geocoder gc = new Geocoder(context);
			List<Address> addresses = gc.getFromLocation(
					location.getLatitude(), location.getLongitude(), 10);
			Address add = addresses.get(0);
			return add.getLocality();
		} catch (Exception ex) {
			ex.printStackTrace();
			// Rather then returning null we might as well return something
			// usefull.
			// return null;
			return "chatroom";
		}
	}

}
