package com.quackware.crowdsource.ui;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.MyDataStore;
import com.quackware.crowdsource.Profile;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public abstract class MessageActivity extends Activity implements OnClickListener {

	private ArrayList<Message> messages;

	private MessageAdapter _conversationArrayAdapter;
	private ListView _conversationView;

	private ViewProfileTask _viewProfileTask;

	protected ServerUtil _su;

	private MyDataStore _dataStore;

	protected void loadPreferences() {

		_dataStore = (MyDataStore) getLastNonConfigurationInstance();
		if (_dataStore != null) {
			_viewProfileTask = _dataStore.getViewProfileTask();
		}
	}

	protected void updateMessage(final Message m) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				_conversationArrayAdapter.add(m);
				_conversationArrayAdapter.notifyDataSetChanged();

			}
		});
	}

	protected void setupChat(Bundle saved) {
		_su = ((MyApplication) this.getApplication()).getServerUtil();

		ArrayList<Message> savedMessages = null;
		if (saved != null) {
			savedMessages = saved.getParcelableArrayList("messages");
		}
		if (savedMessages != null) {
			messages = savedMessages;
		}

		// Make sure we are not wiping messages we retained on orientation
		// change.
		if (messages == null) {
			messages = new ArrayList<Message>();
			messages.add(new Message(
					"Welcome to the chat, type /help to view useful commands!",
					"Channel"));
		}
		_conversationArrayAdapter = new MessageAdapter(this, R.layout.message,
				messages);
		_conversationView = (ListView) findViewById(R.id.in);
		_conversationView.setAdapter(_conversationArrayAdapter);

		registerForContextMenu(_conversationView);

		EditText et = (EditText) findViewById(R.id.edtInput);
		if (saved != null) {
			String sendText = saved.getString("sendText");
			if (sendText != null) {
				et.setText(sendText);
			}
		}

		// Handles the enter key being pressed while in the edittext.
		et.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL) {
					sendMessage();
					return true;
				}
				return false;
			}
		});
		et.requestFocus();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		// Make sure that the view is the listview.
		if (v.getId() == R.id.in) {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			menu.setHeaderTitle("Action on "
					+ _conversationArrayAdapter.getItem(info.position)
							.getUsername());
			String[] menuItems = getResources().getStringArray(
					R.array.crowdtalk_user_options);
			for (int i = 0; i < menuItems.length; i++) {
				menu.add(i, i, i, menuItems[i]);

			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putParcelableArrayList("messages", messages);
		savedInstanceState.putString("sendText",
				((EditText) findViewById(R.id.edtInput)).getText().toString());
	}

	protected void setupButtonClickListeners() {
		Button b = (Button) findViewById(R.id.btnSend);
		b.setOnClickListener(this);
	}

	public class ViewProfileTask extends AsyncTask<String, Void, Profile> {

		private MessageActivity activity;
		private ProgressDialog _viewProfileSpinner;

		public ViewProfileTask(MessageActivity ct) {
			attach(ct);
		}

		@Override
		protected void onPreExecute() {
			_viewProfileSpinner = new ProgressDialog(MessageActivity.this);
			_viewProfileSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			_viewProfileSpinner.setMessage(getString(R.string.loadProfile));
			_viewProfileSpinner.setCancelable(false);
			_viewProfileSpinner.show();
		}

		@Override
		protected Profile doInBackground(String... params) {
			return (_su.getProfile(params[0]));
		}

		protected void onPostExecute(Profile result) {
			activity.removeViewProfileTask();
			if (_viewProfileSpinner != null) {
				_viewProfileSpinner.dismiss();
				_viewProfileSpinner = null;
			}

			if (result != null) {
				startProfileActivity(result);
			} else {
				// Something wrong retrieving profile, so we wont be able to
				// start the activity.
				Toast.makeText(getApplicationContext(),
						getString(R.string.ERROR_SHOW_PROFILE),
						Toast.LENGTH_SHORT).show();

			}
		}

		public void attach(MessageActivity ct) {
			activity = ct;
		}

		public void detatch() {
			activity = null;
		}

	}

	private void removeViewProfileTask() {
		_viewProfileTask = null;
	}

	private void startProfileActivity(Profile p) {
		Intent profileIntent = new Intent(getApplicationContext(),
				ProfileActivity.class);
		profileIntent.putExtra("profile", p);
		startActivity(profileIntent);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		// Use _contextMesage.
		switch (item.getItemId()) {
		// Profile
		case 0:
			if (_viewProfileTask == null) {
				_viewProfileTask = new ViewProfileTask(this);
				_viewProfileTask.execute(_conversationArrayAdapter.getItem(
						info.position).getUsername());
			} else {
				_viewProfileTask.attach(this);
			}
			break;
		// Private Message
		case 1:
			// We want to start a private message with the person we selected...
			String username = _conversationArrayAdapter.getItem(info.position)
					.getUsername();
			if (username.equals(_su.getUsername())) {
				Toast.makeText(this, getString(R.string.ERROR_MESSAGE_YOURSELF),
						Toast.LENGTH_SHORT).show();
			} else {
				Chat c = _su.createPrivateChat(username);
				_su.setCurrentPrivateChat(c);
				Intent intent = new Intent(this, PrivateMessage.class);
				startActivity(intent);
			}

			break;
		// Block User
		case 2:
			_su.blockUser(_conversationArrayAdapter.getItem(info.position)
					.getUsername());
			break;
		// Report Comment
		case 3:
			_su.reportComment(_conversationArrayAdapter.getItem(info.position)
					.getUsername(), _conversationArrayAdapter.getItem(
					info.position).getMessage());
			break;
		default:
			return false;

		}
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnSend:
			sendMessage();
			break;
		}

	}

	public Object onRetainNonConfigurationInstance() {
		if (_dataStore == null) {
			_dataStore = new MyDataStore();
		}
		if (_viewProfileTask != null) {
			_viewProfileTask.detatch();
		}
		_dataStore.setViewProfileTask(_viewProfileTask);
		return _dataStore;
	}

	public abstract void sendMessage();
	public abstract void sendServerMessage(String message);

	protected class Message implements Parcelable {
		private String _message;
		private String _username;

		public Message(String message, String username) {
			_message = message;
			_username = username;
		}

		public Message(Parcel parc) {
			readFromParcelable(parc);
		}

		private void readFromParcelable(Parcel in) {
			_message = in.readString();
			_username = in.readString();
		}

		public String getMessage() {
			return _message;
		}

		public String getUsername() {
			return _username;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(_message);
			dest.writeString(_username);
		}
	}

	private class MessageAdapter extends ArrayAdapter<Message> {

		private ArrayList<Message> items;

		public MessageAdapter(Context context, int textViewResourceId,
				ArrayList<Message> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.message, null);
			}
			// Fix not updating bug...
			Message m = items.get(position);
			if (m != null) {
				TextView bt = (TextView) v.findViewById(R.id.tvmessage);

				if (bt != null) {
					bt.setText(m.getUsername() + ": " + m.getMessage());
					if (position % 2 == 1) {
						bt.setBackgroundColor(Color.parseColor("#C0D9D9"));
					} else {
						bt.setBackgroundColor(Color.parseColor("#FFFFFF"));

					}
				}
			}
			return v;
		}
	}

}
