package com.quackware.crowdsource.test;
import java.util.ArrayList;

import android.content.Context;
import android.location.Location;

import com.quackware.crowdsource.Crowd;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;

public class TestClient {
	
	private static final double LAT = 38.898016033333334;
	private static final double LONG = -76.18922625;
	
	public TestClient(Context context)
	{
		
		Location l = new Location("");
		l.setLatitude(LAT);
		l.setLongitude(LONG);
		
		ServerUtil testSU = new ServerUtil(context.getString(R.string.ip));
		testSU.setLocation(l);
		testSU.connect();
		testSU.login("admin", "admin");

		ServerUtil testSU2 = new ServerUtil(context.getString(R.string.ip));
		testSU2.setLocation(l);
		testSU2.connect();
		testSU2.login("ttt", "tt");
		
		
		ArrayList<Crowd> crowds = testSU.getRoomList(50);
		Crowd crowd = crowds.get(0);
		testSU.connectToRoom(crowd, false);
		testSU2.connectToRoom(crowd, false);
		
		boolean success = testSU.sendMessage("Test message from admin", crowd.getCrowdId(), false);
		success = testSU2.sendMessage("Test message from ttt", crowd.getCrowdId(), false);
		
		
		testSU.disconnect();
		testSU2.disconnect();

		
	}

}
