package com.quackware.crowdsource;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.location.Location;

public class Crowd {

	private String _crowdName;
	private String _crowdId;
	private Location _crowdLocation;
	private XMPPConnection _xmpp;

	public Crowd(String crowdName, Location crowdLocation,
			XMPPConnection ixmpp, String crowdId) {
		_crowdId = crowdId;
		_crowdName = crowdName;
		_crowdLocation = crowdLocation;
		_xmpp = ixmpp;
	}

	public String getCrowdName() {
		return _crowdName;
	}

	public Location getCrowdLocation() {
		return _crowdLocation;
	}

	public float getDistanceFrom(Location newLoc) {
		// Approximate conversion to miles.
		return _crowdLocation.distanceTo(newLoc) / 1609;
	}

	public String getCrowdId() {
		return _crowdId;
	}

	public int getNumPeople() {
		try {
			return MultiUserChat.getRoomInfo(_xmpp, getCrowdName())
					.getOccupantsCount();
		} catch (XMPPException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
