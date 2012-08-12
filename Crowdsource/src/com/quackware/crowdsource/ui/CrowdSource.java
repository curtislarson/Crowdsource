package com.quackware.crowdsource.ui;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.MyDataStore;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;
import com.quackware.crowdsource.Utility;
import com.quackware.crowdsource.test.TestClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

//TODO (General)
/*
 * Profile (Can all be handled with vcards)
 * Settings
 * Exit / Cleaning up
 * Privacy / User moderation stuff. (Give them the ability to manage people?)
 * http://www.igniterealtime.org/builds/smack/docs/latest/documentation/privacy.html
 * We probably want a settings activity to manage this stuff (Can copy all the boiler plate code from HandsFreeMusic)
 * Update location every 30 mins (Will be easy to do now that vcards are good to go)
 * Future option to do it by category
 * Not hardcoding server data. 
 * Security to only ensure that android clients can login. (client control plugin?)
 */

/*
 * Back button from CrowdSource -> Kill app
 * Back button from CrowdChoice -> Logout & go back to CrowdSource
 * Back button from CrowdTalk -> Exit multi chat but DO NOT logout.
 */

public class CrowdSource extends Activity implements OnClickListener {

	private boolean _initializeInOnResume = false;
	private StartupError _errorToBeFixed = null;
	
	
	public class ConnectTask extends AsyncTask<String, Integer, Boolean> {
		private ProgressDialog _connectSpinner;
		@SuppressWarnings("unused")
		private CrowdSource activity = null;
		private boolean _noLocation = false;
		private boolean _lastLocation = false;


		public ConnectTask(CrowdSource cs) {
			attach(cs);
		}

		public void attach(CrowdSource cs) {
			activity = cs;
		}

		public void detatch() {
			activity = null;
		}

		@Override
		protected Boolean doInBackground(String... arg) {
			Log.i(TAG, "Running ConnectTask");
			boolean connect = _su.connect();
			if (connect == false) {
				return false;
			} else {
				// This is a simple way to ensure that the location has been
				// grabbed but
				// is a bad way that should be changed in the future.
				int count = 0;
				while (_su.getLocation() == null) {
					Log
							.i(TAG,
									"Sleeping for 3 seconds to try to wait for location");
					// Sleep three seconds
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
					count++;
					if (count == 18) {
						Log
								.i(TAG,
										"We've waited enought time, try to get last location");
						// Maybe try to get last known location here.
						((MyApplication) CrowdSource.this.getApplication())
								.getLastKnownLocation();
						_lastLocation = true;
					}
					if (count >= 20) {
						Log.i(TAG, "We have given up looking for location");
						break;
					}
				}
				if (_su.getLocation() == null) {
					Log.i(TAG, "getLocation still null after 60 seconds");
					_noLocation = true;
					return false;
				}
				return connect;
			}
		}

		protected void onPostExecute(Boolean result) {
			Log.i(TAG, "Attempting to dismiss connect spinner");
			removeConnectTask();
			if (_connectSpinner != null) {
				_connectSpinner.dismiss();
				_connectSpinner = null;
			}
			if (!result) {
				if (_noLocation) {
					Log.i(TAG, "Unable to retrieve location information");
					Toast
							.makeText(
									getApplicationContext(),
									getString(R.string.ERROR_LOCATION),
									Toast.LENGTH_LONG).show();
					finishApp();
				} else {
					Log.i(TAG, "Unable to connect to server");
					// Message to user saying we were unable to connect to the
					// server, give reason etc.
					Toast.makeText(getApplicationContext(),
							getString(R.string.ERROR_CONNECT_SERVER),
							Toast.LENGTH_LONG).show();
					finishApp();
				}
			} else {
				if (_lastLocation) {
					Log.i(TAG,
							"Showing toast saying we are using last location");
					Toast
							.makeText(
									getApplicationContext(),
									getString(R.string.ERROR_PREVIOUS_LOCATION),
									Toast.LENGTH_LONG).show();
				}
			}

			// Now that we know we are connected to the server with some sort of
			// location we can check
			// to see if we should auto-login
			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			boolean autoLogin = preferences.getBoolean("loginAutoPref", false);
			if (autoLogin) {
				String username = preferences.getString("editTextUsername", "");
				String password = preferences.getString("editTextPassword", "");
				if (!username.equals("") && !password.equals("")) {
					Log.i(TAG, "Starting login task");
					if (_loginTask == null) {
						_loginTask = new LoginTask(CrowdSource.this);
						_loginTask.execute(username, password, "true");
					}
					// Not sure if we should have the attach logic here.
					else {
						_loginTask.attach(CrowdSource.this);
					}
				} else {
					// Unable to automatically login, set autlogin to false.
					Editor edit = preferences.edit();
					edit.putBoolean("loginAutoPref", false);
				}
			}

		}

		@Override
		protected void onPreExecute() {
			_connectSpinner = new ProgressDialog(CrowdSource.this);
			_connectSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_connectSpinner.setMessage(getString(R.string.serverConnect));
			_connectSpinner.setCancelable(false);
			_connectSpinner.show();
		}
	}

	public class LoginTask extends AsyncTask<String, Integer, Boolean> {

		private ProgressDialog _loginSpinner;
		private CrowdSource activity = null;

		private boolean autoLogin = false;

		private String _username;
		private String _password;

		public LoginTask(CrowdSource cs) {
			attach(cs);
		}

		public void attach(CrowdSource cs) {
			activity = cs;
		}

		public void detatch() {
			activity = null;
		}

		@Override
		protected Boolean doInBackground(String... arg) {
			_username = arg[0];
			_password = arg[1];
			if (arg[2].equals("true")) {
				autoLogin = true;
			}
			return _su.login(arg[0], arg[1]);
		}

		protected void onPostExecute(Boolean result) {
			Log.i(TAG, "Attempting to dismiss login spinner");
			if (_loginSpinner != null) {
				_loginSpinner.dismiss();
				_loginSpinner = null;
			}
			if (result) {
				if (autoLogin) {
					// The person clicked the chcekbox or it has previously been
					// checked.
					// We need to set the preferences to the username and the
					// password and autochecked = true
					SharedPreferences pref = getPreferences(MODE_PRIVATE);
					SharedPreferences.Editor edit = pref.edit();
					edit.putString("editTextUsername", _username);
					edit.putString("editTextPassword", _password);
					edit.putBoolean("loginAutoPref", autoLogin);
					edit.commit();
				}
				activity.startCrowdChoiceActivity();
			} else {
				// Alert the user they were unable to login...
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_LOGIN), Toast.LENGTH_SHORT)
						.show();
			}
			activity.removeLoginTask();
		}

		@Override
		protected void onPreExecute() {
			_loginSpinner = new ProgressDialog(CrowdSource.this);
			_loginSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_loginSpinner.setMessage(getString(R.string.loggingIn));
			_loginSpinner.setCancelable(false);
			_loginSpinner.show();
		}

	}

	public class RegisterTask extends AsyncTask<String, Integer, Boolean> {
		private ProgressDialog _registerSpinner;
		private CrowdSource activity = null;

		public RegisterTask(CrowdSource cs) {
			attach(cs);
		}

		public void attach(CrowdSource cs) {
			activity = cs;
		}

		public void detatch() {
			activity = null;
		}

		@Override
		protected Boolean doInBackground(String... arg) {
			Log.i(TAG, "Running RegisterTask");
			return _su.createAccount(arg[0], arg[1]);
		}

		protected void onPostExecute(Boolean result) {
			Log.i(TAG, "Attempting to dismiss register spinner");
			if (_registerSpinner != null) {
				_registerSpinner.dismiss();
				_registerSpinner = null;
			}
			if (result) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.SUCCESSFUL_ACCOUNT_CREATION),
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_ACCOUNT_CREATION),
						Toast.LENGTH_LONG).show();
			}
			activity.removeRegisterTask();
		}

		@Override
		protected void onPreExecute() {
			_registerSpinner = new ProgressDialog(CrowdSource.this);
			_registerSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_registerSpinner.setMessage(getString(R.string.creatingAccount));
			_registerSpinner.setCancelable(false);
		}
	}

	private static final int LOGIN_DIALOG = 2;
	private static final int CREATE_ACCOUNT_DIALOG = 3;
	private static final int PROMPT_NETWORK_DIALOG = 4;
	private static final int PROMPT_GPS_DIALOG = 5;
	private static final int PROMPT_LOCATION_DIALOG = 6;

	private LoginTask _loginTask = null;

	private ConnectTask _connectTask = null;

	private RegisterTask _registerTask = null;

	 //private LocationTask _locationTask = null;

	private ServerUtil _su;

	/*
	 * @Override public void onSaveInstanceState(Bundle savedInstanceState) {
	 * super.onSaveInstanceState(savedInstanceState); }
	 * 
	 * @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
	 * super.onRestoreInstanceState(savedInstanceState);
	 * 
	 * }
	 */

	private MyDataStore _dataStore;

	private static final String TAG = "CrowdSource";

	private void finishApp() {
		((MyApplication)getApplication()).killService();
		this.finish();
	}

	public void removeConnectTask() {
		_connectTask = null;
	}

	public void removeLoginTask() {
		_loginTask = null;
	}

	private void loadPreferences() {
		_dataStore = (MyDataStore) getLastNonConfigurationInstance();
		if (_dataStore != null) {
			_loginTask = _dataStore.getLoginTask();
			_connectTask = _dataStore.getConnectTask();
			_registerTask = _dataStore.getRegisterTask();
			// _locationTask = _dataStore.getLocationTask();
		}
	}

	public void onClick(View v) {
		// Check which view we are getting the click from.
		switch (v.getId()) {
		case R.id.btn_login:
			showDialog(LOGIN_DIALOG);
			break;
		case R.id.btn_create_account:
			showDialog(CREATE_ACCOUNT_DIALOG);
			break;
		case R.id.btn_login_anon:
			boolean success = _su.loginAnon();
			if (success) {
				startCrowdChoiceActivity();
			} else {
				// Alert the user that they were unable to connect anonymously
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_LOGIN_ANON),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.btn_settings:
			startCrowdPreferenceActivity();
			break;
		case R.id.btn_exit:
			this.finish();
		case R.id.btn_help:
			break;
		default:
			break;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.i(TAG, "onCreate called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		StartupError error = verifyEnvironment();
		//Check to see if we have to handle an error.
		if(error != StartupError.NONE)
		{
			handleStartupError(error);
		}
		else
		{
			initialize();
		}

	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(_initializeInOnResume)
		{
			_initializeInOnResume = false;
			StartupError error = _errorToBeFixed;
			_errorToBeFixed = null;
			StartupError newError = verifyEnvironment();
			if(error == newError)
			{
				Toast.makeText(this, "You have failed to change " + error + " settings, CrowdSource will do it's best to continue but may not work...", Toast.LENGTH_LONG);
			}
			initialize();
		}
	}
	
	private void initialize()
	{
		((MyApplication) this.getApplication()).initializeServerUtil();
		_su = ((MyApplication) this.getApplication()).getServerUtil();

		loadPreferences();
		setupButtonClickListeners();
		

		// We might want to move the connecting into only after the user has
		// done something like logging in or creating an account.
		if (!_su.isConnected()) {
			if (_connectTask == null) {
				_connectTask = new ConnectTask(this);
				_connectTask.execute();
			} else {
				_connectTask.attach(this);
			}
		}
	}

	protected Dialog onCreateDialog(int id) {
		Log.i(TAG, "onCreateDialog called with id: " + id);
		switch (id) {

		case LOGIN_DIALOG:
			LayoutInflater factory = LayoutInflater.from(this);
			final View userNamePasswordView = factory.inflate(R.layout.login,
					null);
			CheckBox autoLoginCB = (CheckBox) userNamePasswordView
					.findViewById(R.id.autoLoginCB);
			autoLoginCB.setChecked(PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext())
					.getBoolean("loginAutoPref", false));
			return new AlertDialog.Builder(CrowdSource.this).setTitle("Login")
					.setView(userNamePasswordView).setPositiveButton("Login",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

									EditText te = (EditText) userNamePasswordView
											.findViewById(R.id.passwordET);
									String password = te.getText().toString();
									te.setText("");
									te = (EditText) userNamePasswordView
											.findViewById(R.id.usernameET);
									String username = te.getText().toString();
									te.setText("");
									te.requestFocus();

									CheckBox cb = (CheckBox) userNamePasswordView
											.findViewById(R.id.autoLoginCB);
									String isChecked = "false";
									if (cb.isChecked()) {
										isChecked = "true";
									}

									if (username.equals("")
											|| password.equals("")) {
										// Blank data, prompt them.
										Toast
												.makeText(
														getApplicationContext(),
														getString(R.string.ERROR_BADFIELDS),
														Toast.LENGTH_SHORT)
												.show();
										Log
												.w(TAG,
														"User provided a blank username or password in login");
									} else {
										Log.i(TAG, "Starting login task");
										if (_loginTask == null) {
											_loginTask = new LoginTask(
													CrowdSource.this);
											_loginTask.execute(username,
													password, isChecked);
										} else {
											_loginTask.attach(CrowdSource.this);
										}

									}
								}
							}).setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									((EditText) userNamePasswordView
											.findViewById(R.id.usernameET))
											.setText("");
									((EditText) userNamePasswordView
											.findViewById(R.id.usernameET))
											.requestFocus();
									((EditText) userNamePasswordView
											.findViewById(R.id.passwordET))
											.setText("");
								}
							}).create();
		case CREATE_ACCOUNT_DIALOG:
			LayoutInflater _factory = LayoutInflater.from(this);
			final View createAccountView = _factory.inflate(
					R.layout.createaccount, null);

			return new AlertDialog.Builder(CrowdSource.this).setTitle(
					"Create an Account").setView(createAccountView)
					.setPositiveButton("Create Account",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									EditText te = (EditText) createAccountView
											.findViewById(R.id.CAusernameET);
									te.requestFocus();
									String cuUsername = te.getText().toString();
									te.setText("");
									te = (EditText) createAccountView
											.findViewById(R.id.CApasswordET);
									String cuPassword = te.getText().toString();
									te.setText("");
									te = (EditText) createAccountView
											.findViewById(R.id.CApassword2ET);
									String password2 = te.getText().toString();
									te.setText("");

									if (cuPassword.equals(password2)
											&& (!cuUsername.equals(""))
											&& (!cuPassword.equals(""))
											&& (!password2.equals(""))
											&& (!Utility
													.containsBadCharacters(cuUsername))
											&& (!Utility
													.containsBadStrings(cuUsername))) {
										Log
												.i(TAG,
														"Starting account create spinner");
										if (_registerTask == null) {
											_registerTask = new RegisterTask(
													CrowdSource.this);
											_registerTask.execute(cuUsername,
													cuPassword);
										} else {
											_registerTask
													.attach(CrowdSource.this);
										}

									} else {
										// User did not enter the same password
										// twice, take appropriate action.
										// Or they entered blank passwords.
										Toast
												.makeText(
														getApplicationContext(),
														getString(R.string.ERROR_BADFIELDS),
														Toast.LENGTH_LONG)
												.show();
									}

								}
							}).setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									((EditText) createAccountView
											.findViewById(R.id.CAusernameET))
											.setText("");
									((EditText) createAccountView
											.findViewById(R.id.CAusernameET))
											.requestFocus();
									((EditText) createAccountView
											.findViewById(R.id.CApasswordET))
											.setText("");
									((EditText) createAccountView
											.findViewById(R.id.CApassword2ET))
											.setText("");
								}
							}).create();
		case PROMPT_NETWORK_DIALOG:
			return new AlertDialog.Builder(CrowdSource.this).setTitle(
					"Network support is not enabled.").setMessage(
					getString(R.string.ERROR_NETWORK_DISABLED))
					.setPositiveButton("Go to network settings.",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									_initializeInOnResume = false;
									_errorToBeFixed = StartupError.NETWORK;
									startActivity(new Intent(
											Settings.ACTION_WIRELESS_SETTINGS));

								}
							}).setNegativeButton("Continue without Network support.",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Toast.makeText(getApplicationContext(),
											getString(R.string.WARNING_NETWORK),
											Toast.LENGTH_LONG).show();
								}
							})
					
					.create();
		case PROMPT_GPS_DIALOG:
			return new AlertDialog.Builder(CrowdSource.this).setTitle(
					"GPS is not Enabled.").setMessage(
					getString(R.string.ERROR_GPS_DISABLED)).setPositiveButton(
					"Go to GPS settings.",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Since we know onResume() will be called after this startActivity is returned
							//we can just set a few variables so that we can handle whatever the user
							//does (or does not do) when we return.
							_initializeInOnResume = true;
							_errorToBeFixed = StartupError.GPS;
							startActivity(new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS));

						}
					}).setNegativeButton("Continue without GPS support.",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(getApplicationContext(),
									getString(R.string.WARNING_GPS),
									Toast.LENGTH_LONG).show();
						}
					})

			.create();
		case PROMPT_LOCATION_DIALOG:
			return new AlertDialog.Builder(CrowdSource.this).setTitle(
			"Location features are not enabled.").setMessage(
			getString(R.string.ERROR_GPS_DISABLED)).setPositiveButton(
			"Open Location Settings.",
			new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Since we know onResume() will be called after this startActivity is returned
					//we can just set a few variables so that we can handle whatever the user
					//does (or does not do) when we return.
					_initializeInOnResume = true;
					_errorToBeFixed = StartupError.LOCATION;
					//Lets try just starting two activities.
					startActivity(new Intent(
							Settings.ACTION_WIRELESS_SETTINGS));
					startActivity(new Intent(
							Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					

				}
			}).setNegativeButton("Exit Application.",
			new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(getApplicationContext(), "Please enable location features before starting CrowdSource!", Toast.LENGTH_LONG);
					CrowdSource.this.finish();
				}
			})

	.create();
		default:
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy called");
		super.onDestroy();
		_su.disconnect();
		((MyApplication) getApplication()).killService();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		if (_dataStore == null) {
			_dataStore = new MyDataStore();
		}
		if (_loginTask != null) {
			_loginTask.detatch();
		}
		if (_connectTask != null) {
			_connectTask.detatch();
		}
		if (_registerTask != null) {
			_registerTask.detatch();
		}
		// if(_locationTask != null)
		// {
		// _locationTask.detatch();
		// }
		_dataStore.setLoginTask(_loginTask);
		_dataStore.setConnectTask(_connectTask);
		_dataStore.setRegisterTask(_registerTask);
		// _dataStore.setLocationTask(_locationTask);

		return _dataStore;
	}

	/*
	 * public void removeLocationTask() { _locationTask = null;
	 * 
	 * }
	 */
	public void removeRegisterTask() {
		_registerTask = null;

	}

	private void setupButtonClickListeners() {
		Log.i(TAG, "setupButtonClickListeners called");
		Button b = (Button) findViewById(R.id.btn_login);
		b.setOnClickListener(this);
		b = (Button) findViewById(R.id.btn_login_anon);
		b.setOnClickListener(this);
		b = (Button) findViewById(R.id.btn_create_account);
		b.setOnClickListener(this);
		b = (Button) findViewById(R.id.btn_settings);
		b.setOnClickListener(this);
		b = (Button) findViewById(R.id.btn_exit);
		b.setOnClickListener(this);
		b = (Button) findViewById(R.id.btn_help);
		b.setOnClickListener(this);
	}

	private void startCrowdChoiceActivity() {
		// Make sure we null out logintask in case they go back and maybe want
		// to login
		// again.
		_loginTask = null;
		Log.i(TAG, "startCrowdChoiceActivity called");
		// Make sure we have the most up to date serverutil before we start the
		// activity
		((MyApplication) this.getApplication()).setServerUtil(_su);

		Intent crowdChoiceIntent = new Intent(getApplicationContext(),
				CrowdChoice.class);
		startActivity(crowdChoiceIntent);
	}

	private void startCrowdPreferenceActivity() {
		Log.i(TAG, "startCrowdPreferenceActivity called");
		((MyApplication) this.getApplication()).setServerUtil(_su);
		Intent preferenceIntent = new Intent(this, CrowdPreference.class);
		preferenceIntent.putExtra("fromCrowdTalk", false);
		startActivity(preferenceIntent);
	}

	/**
	 * This method checks the phone to see what features are enabled / disabled.
	 * If an important feature is disabled then the application exits and the
	 * user is notified. Otherwise we pop up a notification that tells the user
	 * that their experience may not be very good because of something missing
	 * (like gps or network).
	 */
	private StartupError verifyEnvironment() {
		Log.i(TAG, "verifyEnvironment called");
		
		
		//Most important thing to check is the internet connection
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		//Check to see if we are not connected
		if(!cm.getActiveNetworkInfo().isConnected())
		{
			return StartupError.INTERNET;
		}
		
		LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		
		//We should initially check if network and gps are enabled.
		boolean network_enabled = false;
		boolean gps_enabled = false;
		boolean no_network = false;
		boolean no_gps = false;
		
		try
		{
			if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				//If we enter into this if statement then we know that GPS support is enabled.
				gps_enabled = true;
			}
			else
			{
				//Ok so they have GPS support but it is disabled....
			}
		}
		catch(Exception ex)
		{
			//We enter into this catch statement if they do not even have gps support on their phone...
			no_gps = true;
		}
		
		//Now we do the same thing for network...
		try
		{
			if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			{
				//If we enter into this if statement then we know that GPS support is enabled.
				network_enabled = true;
			}
			else
			{
				//Ok so they have GPS support but it is disabled....
			}
		}
		catch(Exception ex)
		{
			//We enter into this catch statement if they do not even have gps support on their phone...
			no_network = true;
		}
		
		//We are going to make an assumption here that every Android phone as atleast network or GPS support
		//I am not sure if there are any phones that would not support either of these features.
		
		//We should probably just check for enabled
		if(gps_enabled == false && network_enabled == false)
		{
			return StartupError.LOCATION;
		}
		else if(gps_enabled == false && network_enabled == true)
		{
			return StartupError.GPS;
		}
		else if(gps_enabled == true && network_enabled == false)
		{
			return StartupError.NETWORK;
		}
		//Both true...
		else
		{
			return StartupError.NONE;
		}
	}
	
	private void handleStartupError(StartupError error)
	{
		switch(error)
		{
		case INTERNET:
			//They are not connected to an internet source... (Possibly in Airplane Mode?)
			//We could possibly change this toast to some sort of alert box...
			Toast.makeText(this, getString(R.string.ERROR_INTERNET), Toast.LENGTH_LONG);
			this.finish();
			break;
		case GPS:
			showDialog(PROMPT_GPS_DIALOG);
			break;
		case NETWORK:
			showDialog(PROMPT_NETWORK_DIALOG);
			break;
		case LOCATION:
			showDialog(PROMPT_LOCATION_DIALOG);
			break;
		//We will never have a NONE case.
		case NONE:
		default:
			break;
		}
	}
	

	
}

//Potential values that can be returned from verify environment.
//None = no error
//GPS = No GPS support or not enabled
//NETWORK = No Network support or not enabled
//LOCATION = No GPS OR Network support or not enabled (Might have to change this one around)
//INTERNET = Not currently connected to the internet.
enum StartupError
{
	NONE,
	GPS,
	NETWORK,
	LOCATION,
	INTERNET
}

