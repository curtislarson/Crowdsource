package com.quackware.crowdsource.ui;

import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;

import com.quackware.crowdsource.R;

import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

public class PrivateMessage extends MessageActivity {

	private Chat _c;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// We can just reuse crowdtalk since its the same thing we want.
		setContentView(R.layout.crowdtalk);
		setupChat(savedInstanceState);
		setupButtonClickListeners();
		loadPreferences();

		_c = _su.getCurrentPrivateChat();
		_c.addMessageListener(listener);

	}

	private MessageListener listener = new MessageListener() {

		@Override
		public void processMessage(Chat chat,
				org.jivesoftware.smack.packet.Message message) {
			final Message m = new Message(message.getBody(), chat
					.getParticipant());
			updateMessage(m);
		}
	};

	@Override
	public void sendMessage() {
		if (_su.isConnected()) {
			EditText et = (EditText) findViewById(R.id.edtInput);
			Editable message = et.getText();
			try {
				_c.sendMessage(message.toString());
			} catch (XMPPException e) {
				Toast.makeText(this, getString(R.string.ERROR_SEND_MESSAGE),
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			et.setText("");
		}
	}
	
	@Override
	public void sendServerMessage(String message)
	{
	}
	

}
