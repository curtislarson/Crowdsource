package com.quackware.crowdsource;

import static com.quackware.crowdsource.util.C.D;
import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.quackware.crowdsource.service.LocationService;
import com.quackware.crowdsource.ui.CrowdTalk;

public class MyApplication extends Application {

	private ServerUtil _su;
	private CrowdTalk _cu;

	private static MyApplication instance;

	private LocationService _locationService;
	private boolean _isBound;
	private Messenger _messengerService;

	private static final String TAG = "MyApplication";

	@Override
	public void onCreate() {
		super.onCreate();

		// Start the service that will periodically retrieve location.
		doBindService();
	}
	
	public void killService()
	{
		Log.i(TAG,"Calling killService()");
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		super.onTerminate();
		try {
			Message mes = new Message();
			mes.what = LocationService.TERMINATE_SERVICE;
			_messengerService.send(mes);
			doUnbindService();
		} catch (Exception ex) {
		}
	}

	public void getLastKnownLocation() {
		Message mes = new Message();
		mes.what = LocationService.MSG_LAST_KNOWN_LOCATION;
		mes.replyTo = _messenger;
		try {
			_messengerService.send(mes);
		} catch (RemoteException e) {
			if (D)
				Log.e(TAG, "Unable to send getLastKnownLocation message");
			e.printStackTrace();
		}
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LocationService.MSG_LAST_KNOWN_LOCATION:
			case LocationService.MSG_UPDATE_LOCATION:
				Bundle data = msg.getData();
				Location location = data.getParcelable("location");
				if (location != null) {
					_su.setLocation(location);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	final Messenger _messenger = new Messenger(new IncomingHandler());

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			if (D)
				Log.i(TAG, "onServiceConnected called");
			// _locationService =
			// ((LocationService.LocalBinder)service).getService();
			_messengerService = new Messenger(service);

			Message mes = Message.obtain(null,
					LocationService.MSG_UPDATE_LOCATION);
			mes.replyTo = _messenger;
			try {
				_messengerService.send(mes);
			} catch (RemoteException e) {
				if (D)
					Log.e(TAG, "_messengerService.send() threw an exception.");
				e.printStackTrace();
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			_locationService = null;
			if (D)
				Log.e(TAG, "onServiceDisconnected called");
		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		if (D)
			Log.i(TAG, "doBindService called with _isBound = " + _isBound);
		bindService(new Intent(getApplicationContext(), LocationService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		_isBound = true;
	}

	void doUnbindService() {
		if (D)
			Log.i(TAG, "doUnbindService called with _isBound = " + _isBound);
		if (_isBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			
			_isBound = false;
		}
	}

	public MyApplication() {
		instance = this;
	}

	public static Context getContext() {
		return instance;
	}

	public ServerUtil getServerUtil() {
		return _su;
	}

	public void setServerUtil(ServerUtil is) {
		_su = is;
	}

	public void initializeServerUtil() {
		if (_su == null) {
			_su = new ServerUtil(getString(R.string.ip));
		}
	}

	public CrowdTalk getCrowdTalk() {
		return _cu;
	}

}
