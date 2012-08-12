package com.quackware.crowdsource.ui;

import static com.quackware.crowdsource.util.C.D;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.MyDataStore;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;

public class CrowdPreference extends PreferenceActivity implements
		OnPreferenceChangeListener, OnPreferenceClickListener {

	private static final int CHANGE_PASSWORD_DIALOG = 1;
	private static final int LOGIN_DIALOG = 2;

	private static final String TAG = "CrowdPreference";

	private static final String SETTINGS_FILE = "SettingsFile";

	private static final String ET_USERNAME = "editTextUsername";
	private static final String ET_PASSWORD = "editTextPassword";
	private static final String ET_PASSWORD2 = "editTextPassword2";

	private static final String BUTTON_PREF = "savePrefs";

	private MyDataStore _dataStore;

	private ChangePasswordTask _changePasswordTask;
	private LoginTask _loginTask;

	private ServerUtil _su;

	private boolean _fromCrowdTalk;

	// TODO When they try to change any account information make them login
	// first?

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		setEditTextListeners();
		setClickListeners();

		loadPreferences();
	}

	private void loadPreferences() {
		_su = ((MyApplication) CrowdPreference.this.getApplication())
				.getServerUtil();
		_dataStore = (MyDataStore) getLastNonConfigurationInstance();
		if (_dataStore != null) {
			_changePasswordTask = _dataStore.getChangePasswordTask();
			_loginTask = _dataStore.getCPLoginTask();
		}
		try {
			_fromCrowdTalk = getIntent().getExtras()
					.getBoolean("fromCrowdTalk");
		} catch (Exception ex) {
			_fromCrowdTalk = false;
		}
	}

	private void setClickListeners() {
		Preference changePasswordPref = (Preference) findPreference("changePassword");
		changePasswordPref.setOnPreferenceClickListener(this);
		PreferenceScreen accountPrefScreen = (PreferenceScreen) findPreference("userAccountPreferenceScreen");
		accountPrefScreen.setOnPreferenceClickListener(this);
	}

	private void setEditTextListeners() {
		((EditTextPreference) findPreference("editTextLocationInterval"))
				.setOnPreferenceChangeListener(this);
		((EditTextPreference) findPreference("editTextLocationInterval"))
				.setText("10");
	}

	@Override
	public void onStop() {
		super.onStop();
		// Thread because this actually lags the switch from crowdpreference
		// back to another activity.
		// And we don't need to really display a dialog or anything.

		// We don't want to log out if we are simply editting the settings from
		// the tab view.
		if (!_fromCrowdTalk) {
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

	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (_dataStore == null) {
			_dataStore = new MyDataStore();
		}

		if (_changePasswordTask != null) {
			_changePasswordTask.detatch();
		}
		if (_loginTask != null) {
			_loginTask.detatch();
		}
		_dataStore.setChangePasswordTask(_changePasswordTask);
		_dataStore.setCPLoginTask(_loginTask);
		return _dataStore;

	}

	protected Dialog onCreateDialog(int id) {
		if (D)
			Log.i(TAG, "onCreateDialog called with id: " + id);
		switch (id) {
		case LOGIN_DIALOG:
			LayoutInflater factory = LayoutInflater.from(this);
			final View userNamePasswordView = factory.inflate(R.layout.login,
					null);
			// We need to disable the checkbox this time since they are just
			// logging in to view preferences.
			CheckBox cb = (CheckBox) userNamePasswordView
					.findViewById(R.id.autoLoginCB);
			cb.setVisibility(View.GONE);

			return new AlertDialog.Builder(CrowdPreference.this).setTitle(
					"Login").setView(userNamePasswordView).setCancelable(false)
					.setPositiveButton("Login",
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

									if (username.equals("")
											|| password.equals("")) {
										// Blank data, prompt them.
										Toast
												.makeText(
														getApplicationContext(),
														getString(R.string.ERROR_BADFIELDS),
														Toast.LENGTH_SHORT)
												.show();
										if (D)
											Log
													.w(TAG,
															"User provided a blank username or password in login");
										PreferenceScreen ps = (PreferenceScreen) CrowdPreference.this
												.findPreference("userAccountPreferenceScreen");
										ps.getDialog().dismiss();

									} else {
										if (D)
											Log.i(TAG, "Starting login task");
										if (_loginTask == null) {
											_loginTask = new LoginTask(
													CrowdPreference.this);
											_loginTask.execute(username,
													password);
										} else {
											_loginTask
													.attach(CrowdPreference.this);
										}

									}
								}
							}).setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// We have to go back!
									PreferenceScreen ps = (PreferenceScreen) CrowdPreference.this
											.findPreference("userAccountPreferenceScreen");
									ps.getDialog().dismiss();

									// Text stuff.
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
		case CHANGE_PASSWORD_DIALOG:
			LayoutInflater _factory = LayoutInflater.from(this);
			final View createAccountView = _factory.inflate(
					R.layout.changepassword, null);

			return new AlertDialog.Builder(CrowdPreference.this).setTitle(
					"Change Password").setView(createAccountView)
					.setPositiveButton("Save",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									EditText te = (EditText) createAccountView
											.findViewById(R.id.CFoldpasswordET);
									String oldPassword = te.getText()
											.toString();
									te.requestFocus();
									te.setText("");
									te = (EditText) createAccountView
											.findViewById(R.id.CFnewpasswordET);
									String newPassword = te.getText()
											.toString();
									te.setText("");

									if ((!oldPassword.equals(""))
											&& (!newPassword.equals(""))) {
										if (D)
											Log
													.i(TAG,
															"Starting change password spinner");
										if (_changePasswordTask == null) {
											_changePasswordTask = new ChangePasswordTask(
													CrowdPreference.this);
											_changePasswordTask.execute(
													oldPassword, newPassword);
										} else {
											_changePasswordTask
													.attach(CrowdPreference.this);
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
											.findViewById(R.id.CFoldpasswordET))
											.requestFocus();
									((EditText) createAccountView
											.findViewById(R.id.CFoldpasswordET))
											.setText("");
									((EditText) createAccountView
											.findViewById(R.id.CFnewpasswordET))
											.setText("");
								}
							}).create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("changePassword")) {
			showDialog(CHANGE_PASSWORD_DIALOG);
		} else if (preference.getKey().equals("userAccountPreferenceScreen")) {
			if (!_su.isAuthenticated()) {
				showDialog(LOGIN_DIALOG);
			} else if (_su.isAnonymous()) {
				Toast
						.makeText(
								getApplicationContext(),
								getString(R.string.ERROR_ANON_EDIT),
								Toast.LENGTH_LONG).show();

			}
		}
		return true;
	}

	public class ChangePasswordTask extends AsyncTask<String, Void, Boolean> {
		private CrowdPreference activity;
		private ProgressDialog _changePasswordSpinner;

		public ChangePasswordTask(CrowdPreference cp) {
			attach(cp);
		}

		@Override
		protected void onPreExecute() {
			_changePasswordSpinner = new ProgressDialog(CrowdPreference.this);
			_changePasswordSpinner
					.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_changePasswordSpinner
					.setMessage(getString(R.string.serverConnect));
			_changePasswordSpinner.setCancelable(false);
			_changePasswordSpinner.show();
		}

		@Override
		protected Boolean doInBackground(String... args) {
			// [0] = username
			// [1] = oldPassword
			// [2] = newPassword
			return ((MyApplication) CrowdPreference.this.getApplication())
					.getServerUtil().changePassword(args[0], args[1]);
		}

		protected void onPostExecute(Boolean result) {
			if (D)
				Log.i(TAG, "Attempting to dismiss change password spinner");

			if (_changePasswordSpinner != null) {
				_changePasswordSpinner.dismiss();
				_changePasswordSpinner = null;
			}
			if (!result) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_CHANGE_PASSWORD), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.SUCCESSFULL_CHANGE_PASSWORD), Toast.LENGTH_SHORT)
						.show();
			}
		}

		public void attach(CrowdPreference cp) {
			activity = cp;
		}

		public void detatch() {
			activity = null;
		}

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		String key = preference.getKey();
		if (key.equals("editTextLocationInterval")) {
			try {
				Integer.parseInt(newValue.toString());
			} catch (Exception ex) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_INT), Toast.LENGTH_SHORT)
						.show();
				return false;
			}
		}
		return true;
		/*
		 * Preference buttonPref = (Preference)findPreference(BUTTON_PREF);
		 * String key = preference.getKey(); String usernameText =
		 * ((EditTextPreference)findPreference(ET_USERNAME)).getText(); String
		 * passwordText =
		 * ((EditTextPreference)findPreference(ET_PASSWORD)).getText(); String
		 * password2Text =
		 * ((EditTextPreference)findPreference(ET_PASSWORD2)).getText(); //Check
		 * to see if they were editing either of the passwords.
		 * if(key.equals(ET_PASSWORD) || key.equals(ET_PASSWORD2)) {
		 * 
		 * }
		 * 
		 * //Enable the button only after everything has been filled in.
		 * if(key.equals(ET_USERNAME) && !newValue.equals("")) {
		 * if(!passwordText.equals("") && !password2Text.equals("")) {
		 * buttonPref.setEnabled(true); } } else if(key.equals(ET_PASSWORD) &&
		 * !newValue.equals("")) { if(!usernameText.equals("") &&
		 * !password2Text.equals("")) { buttonPref.setEnabled(true); } } else
		 * if(key.equals(ET_PASSWORD2) && !newValue.equals("")) {
		 * if(!passwordText.equals("") && !usernameText.equals("")) {
		 * buttonPref.setEnabled(true); } }
		 */
	}

	public class LoginTask extends AsyncTask<String, Integer, Boolean> {

		private ProgressDialog _loginSpinner;
		private CrowdPreference activity = null;

		private String _username;
		private String _password;

		public LoginTask(CrowdPreference crowdPreference) {
			attach(crowdPreference);
		}

		public void attach(CrowdPreference cs) {
			activity = cs;
		}

		public void detatch() {
			activity = null;
		}

		@Override
		protected Boolean doInBackground(String... arg) {
			_username = arg[0];
			_password = arg[1];
			return _su.login(arg[0], arg[1]);
		}

		protected void onPostExecute(Boolean result) {
			if (D)
				Log.i(TAG, "Attempting to dismiss login spinner");
			if (_loginSpinner != null) {
				_loginSpinner.dismiss();
				_loginSpinner = null;
			}
			if (result) {
				// We need to save the username and password for later.
				Editor edit = PreferenceManager.getDefaultSharedPreferences(
						getApplicationContext()).edit();
				edit.putString("editTextUsername", _username);
				edit.putString("editTextPassword", _password);
				edit.commit();

			} else {
				PreferenceScreen ps = (PreferenceScreen) CrowdPreference.this
						.findPreference("userAccountPreferenceScreen");
				ps.getDialog().dismiss();
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_LOGIN), Toast.LENGTH_SHORT)
						.show();
			}
			activity.removeLoginTask();
		}

		@Override
		protected void onPreExecute() {
			_loginSpinner = new ProgressDialog(CrowdPreference.this);
			_loginSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_loginSpinner.setMessage(getString(R.string.loggingIn));
			_loginSpinner.setCancelable(false);
			_loginSpinner.show();
		}

	}

	private void removeLoginTask() {
		_loginTask = null;
	}

}
