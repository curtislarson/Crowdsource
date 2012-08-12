/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource;

import static com.quackware.crowdsource.util.C.D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PrivacyItem;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.IBBProviders;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

public class ServerUtil {

	private XMPPConnection _xmpp;
	private Location _location;
	private MultiUserChat _muc;
	private String _username;
	private String _password;
	private Crowd _c;
	private AccountManager _am;
	
	private ArrayList<Chat> _privateMessageList;
	private ArrayList<String> _privateMessageOccupants;
	private Chat _currentPrivateChat;
	
	private Context _chatContext;

	/**
	 * Creates a new instance of the ServerUtil class that handles all
	 * interaction with the openfire XMPP server.
	 * 
	 * @param ip
	 *            The ip of the server to connect to.
	 */
	public ServerUtil(String ip) {
		configure(ProviderManager.getInstance());
		XMPPConnection
				.addConnectionCreationListener(new ConnectionCreationListener() {
					public void connectionCreated(Connection connection) {
						new ServiceDiscoveryManager(connection);
					}
				});
		ConnectionConfiguration cc = new ConnectionConfiguration(ip, 5222);

		cc.setSASLAuthenticationEnabled(false);
		SmackConfiguration.setKeepAliveInterval(30000);

		_xmpp = new XMPPConnection(cc);
		_am = new AccountManager(_xmpp);

		_privateMessageList = new ArrayList<Chat>();
		_privateMessageOccupants = new ArrayList<String>();
		
		if(D)
		{
			XMPPConnection.DEBUG_ENABLED = true;
		}
		
		/*_location = new Location("");
		_location.setLatitude(38.898016033333334);
		_location.setLongitude(-76.18922625);*/
	}

	/**
	 * Blocks a user from any communication.
	 * @param username The user to block communication to/from
	 * @return a boolean indicating success. Returns false if there is an exception in blocking
	 * the user.
	 */
	public boolean blockUser(String username) {

		ArrayList<PrivacyItem> privacyItems = new ArrayList<PrivacyItem>();
		String realUser = username + "@" + getServiceName();
		String listName = "block";
		// This returns null for android
		// PrivacyListManager manager =
		// PrivacyListManager.getInstanceFor(_xmpp);
		PrivacyListManager manager = new PrivacyListManager(_xmpp);
		PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid.toString(),
				false, 1);
		item.setValue(realUser);
		item.setFilterMessage(true);
		privacyItems.add(item);

		try {
			List<PrivacyItem> oldList = manager.getPrivacyList(listName)
					.getItems();
			oldList.add(item);
			manager.updatePrivacyList(listName, oldList);
		} catch (Exception e) {

			// This will catch if they have not created the block list yet.
			try {
				manager.createPrivacyList("block", privacyItems);
			} catch (XMPPException e1) {
				
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets the current private chat that we are dealing with in PrivateMessage
	 * @param c The chat object to set as the current private chat.
	 */
	public void setCurrentPrivateChat(Chat c) {
		_currentPrivateChat = c;
	}

	/**
	 * Retrieves the current private chat object that we are dealing with in PrivateMessage
	 * @return the current private chat object that we are dealing with in PrivateMessage.
	 */
	public Chat getCurrentPrivateChat() {
		return _currentPrivateChat;
	}

	/**
	 * Changes the password of the currently logged in user.
	 * @param oldPassword The old password of the user.
	 * @param newPassword The new password of the user.
	 * @return a boolean indicating success, can be false if either the old password is incorrect,
	 * we are not logged in, or there is an exception with changing the password.
	 */
	public boolean changePassword(String oldPassword, String newPassword) {
		try {
			if (_xmpp.isAuthenticated()) {
				if (oldPassword.equals(_password)) {
					_xmpp.getAccountManager().changePassword(newPassword);
					_password = newPassword;
					return true;
				} else {
					return false;
				}

			} else {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
	}

	public boolean changePassword(String username, String oldPassword,
			String newPassword) {
		try {
			AccountManager am = new AccountManager(_xmpp);
			if (_xmpp.isAuthenticated()) {
				am.changePassword(newPassword);
			} else {
				_xmpp.login(username, oldPassword);
				am.changePassword(newPassword);
				logout();
			}
			return true;
		} catch (Exception ex) {
			return false;
		}

	}

	// Used to fix the bug detailed below.
	// http://code.google.com/p/asmack/issues/detail?id=43
	public void configure(ProviderManager pm) {

		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time", Class
					.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
			if (D)
				Log
						.w("TestClient",
								"Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());

		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version", Class
					.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}

		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm
				.addIQProvider("query", "jabber:iq:last",
						new LastActivity.Provider());

		// User Search
		pm
				.addIQProvider("query", "jabber:iq:search",
						new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		pm.addIQProvider("open", "http://jabber.org/protocol/ibb",
				new IBBProviders.Open());

		pm.addIQProvider("close", "http://jabber.org/protocol/ibb",
				new IBBProviders.Close());

		pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb",
				new IBBProviders.Data());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.SessionExpiredError());
	}

	/**
	 * Connects to the XMPP server.
	 * @return a boolean indicating whether we have connected to the server successfully.
	 */
	public boolean connect() {
		try {
			_xmpp.connect();

			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Connects to the provided room.
	 * @param room The crowd we want to connect to.
	 * @param create Indicating whether to create the crowd if it does not exist.
	 * @return a boolean indicating success. Is false if we failed to either create or connect
	 * to the given crowd.
	 */
	public boolean connectToRoom(Crowd room, boolean create) {
		try {
			_muc = new MultiUserChat(_xmpp, room.getCrowdName());
			if (create) {
				
					// Form f = _muc.getConfigurationForm();
					boolean success = createNewRoom(room);
					if (!success) {
						return false;
					}
				
			} else {
				if (_xmpp.isAnonymous()) {
					_muc.join("Anonymous" + _xmpp.getConnectionID());
				} else {
					_muc.join(_username);
				}
			}
			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Creates an account with the given username and password.
	 * @param username The username for the account.
	 * @param password THe password for the account.
	 * @return a boolean indicating success. Will be false if there is an error in creating
	 * the account or the username already exists.
	 */
	public boolean createAccount(String username, String password) {
		// We want to scrub the username for bad charactersc (ones we dont
		// want).
		try {

			// _am.supportsAccountCreation();

			// For some reason you cannot create an account using the
			// standard username,password createAccount so you need to pass in a
			// blank hashmap!
			// hooray for stupid bugs.
			_am.createAccount(username, password,
							new HashMap<String, String>());
			_username = username;

			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		}
	}

	
	/**
	 * Creates a crowd object based off the current user's location. Attempts to also find a local
	 * city name to name the crowd after.
	 * @return The created crowd, or null if an error occured.
	 */
	public Crowd createCrowd() {
		String roomName;
		int counter = 0;

		// Kind of a bad way to see if a room exists
		// but its the only way I could find after some googling.
		while (true)
			try {
				if (counter == 0) {
					MultiUserChat.getRoomInfo(_xmpp, Utility
							.getCityNameFromGPS(_location, MyApplication
									.getContext()));
				} else {
					MultiUserChat.getRoomInfo(_xmpp, Utility
							.getCityNameFromGPS(_location, MyApplication
									.getContext())
							+ "_" + counter);
				}
				// Since no exception was thrown we know that the room exists,
				// so increment counter.
				counter++;
				// We should probably have a failsafe in case for some reason
				// this does not work.
				if (counter > 50) {
					roomName = null;
					break;
				}
			} catch (Exception ex) {
				// So the room with the current counter (or the default one)
				// does not exist, set the roomName!
				if (counter == 0) {
					roomName = Utility.getCityNameFromGPS(_location,
							MyApplication.getContext());
				} else {
					roomName = Utility.getCityNameFromGPS(_location,
							MyApplication.getContext())
							+ "_" + counter;

				}
				break;
			}
		if (roomName == null) {
			return null;
		}
		
		//Adding in connection id because the auto increment stuff above was not working.
		return new Crowd(roomName + "`" + _xmpp.getConnectionID() + "@conference." + getServiceName(),
				_location, _xmpp, locationToRoomId(roomName, _location));
	}

	/**
	 * Creates a chat room from a crowd object.
	 * @param room The crowd to create the room after.
	 * @return a boolean indicating success.
	 */
	private boolean createNewRoom(Crowd room) {
		try {
			_muc.create(_username);
			// Get the the room's configuration form
			Form form = _muc.getConfigurationForm();
			// Create a new form to submit based on the original form
			Form submitForm = form.createAnswerForm();
			// Add default answers to the form to submit
			for (Iterator fields = form.getFields(); fields.hasNext();) {
				FormField field = (FormField) fields.next();
				if (!FormField.TYPE_HIDDEN.equals(field.getType())
						&& field.getVariable() != null) {
					if (D)
						Log.i("info", field.getLabel());
					if (field.getVariable().equals("muc#roomconfig_roomdesc")) {
						submitForm.setAnswer(field.getVariable(), room
								.getCrowdId());
					} else if (field.getVariable().equals(
							"muc#roomconfig_moderatedroom")) {
						submitForm.setAnswer(field.getVariable(), false);
					} else {
						// Sets the default value as the answer
						submitForm.setDefaultAnswer(field.getVariable());
					}
				}
			}

			_muc.sendConfigurationForm(submitForm);
			return true;
		}

		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Registers a listener to listen for other users sending private messages to the current
	 * logged in user. 
	 */
	public void registerChatListener(Context con) {
		_chatContext = con;
		_xmpp.getChatManager().addChatListener(new ChatManagerListener() {

			@Override
			public void chatCreated(Chat chat, boolean createdLocally) {
				if (!createdLocally) {
					_privateMessageList.add(chat);
					_privateMessageOccupants.add(chat.getParticipant());
					//Show a little message when someone creates a chat with us.
					if(_chatContext != null)
					{
						Toast.makeText(_chatContext,"A user is trying to private message you!", Toast.LENGTH_LONG).show();
					}
				}
			}
		});
	}
	
	/**
	 * Creates a private chat with the given occupant of the MUC. If one already exists
	 * then it returns that private chat.
	 * @param occupant The occupant to begin a private chat with.
	 * @return The chat object used to communicate between the two users.
	 */
	public Chat createPrivateChat(String occupant) {
		
		//We need to convert occupant to room's JID
		occupant = _muc.getRoom() + "@" + getServiceName() + "/" + occupant;
		if (_muc != null) {
			if(!_privateMessageOccupants.contains(occupant))
			{
				Chat c = _muc.createPrivateChat(occupant, null);
				_privateMessageList.add(c);
				_privateMessageOccupants.add(occupant);
				return c;
			}
			else
			{
				int index = _privateMessageOccupants.indexOf(occupant);
				return _privateMessageList.get(index);
			}
		} else {
			return null;
		}
	}

	/**
	 * Disconnects from the XMPP server.
	 */
	public void disconnect() {
		_xmpp.disconnect();
	}

	/**
	 * Disconnects from the currently logged in MUC.
	 */
	public void disconnectMUC() {
		// Not sure if getRoom is the right one we want.
		try {
			_muc.leave();
			_privateMessageList.clear();
			_privateMessageOccupants.clear();
		} catch (Exception ex) {
		}
	}

	/**
	 * Retrieves the current crowd object.
	 * @return The current crowd object for the MUC.
	 */
	public Crowd getCrowd() {
		return _c;
	}

	/**
	 * Retrieves all the hosted rooms for the given serviceName.
	 * @param serviceName The service to check rooms for.
	 * @return A collection of all the MUC rooms for the service.
	 */
	private Collection<HostedRoom> getHostedRooms(String serviceName) {
		try {
			return MultiUserChat.getHostedRooms(_xmpp, "conference."
					+ serviceName);
		} catch (XMPPException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Retrieves the current location object for the user.
	 * @return The current location object for the user.
	 */
	public Location getLocation() {
		return _location;
	}

	/**
	 * Returns a list of all the current private message threads that are occuring between this user
	 * and other users.
	 * @return An ArrayList of chat objects.
	 */
	public ArrayList<Chat> getPrivateMessageList() {
		return _privateMessageList;
	}

	/**
	 * Retrieves the profile object for the given username, or null if an error occurrs.
	 * @param username The username to retrieve the profile of.
	 * @return The profile object of the username or null if the user is anonymous or an
	 * error occurs.
	 */
	public Profile getProfile(String username) {

		boolean isMyProfile = username.equals(_username);
		// Since it is our own profile we can just grab the info from the
		// current serverutil
		if (isMyProfile) {
			if (isAnonymous()) {
				return null;
			} else {
				return new Profile(username, _location, isMyProfile);
			}
		}
		// It is someone elses profile so we have to grab the information from
		// vcard stuff.
		else {
			VCard card = getVCard(username + "@" + getServiceName());
			if (card == null || card.getField("latitude") == null
					|| card.getField("longitude") == null) {
				// Something went wrong or they are anonymous, return null.
				return null;
			}
			Location l = new Location("");
			l.setLatitude(Double.parseDouble(card.getField("latitude")));
			l.setLongitude(Double.parseDouble(card.getField("longitude")));
			return new Profile(username, l, false);
		}
	}

	public ArrayList<Crowd> getRoomList(float maxDistance) {
		ArrayList<Crowd> roomList = new ArrayList<Crowd>();
		String serviceName = getServiceName();
		if (serviceName == null) {
			return null;
		}
		Collection<HostedRoom> rooms = getHostedRooms(serviceName);
		if (rooms == null) {
			return null;
		}
		for (HostedRoom room : rooms) {
			// Lets assume the decription contains long/lat information in the
			// form roomname-lat:----lon:----
			// This is where we would put login to check the name of the room
			// based on the
			// location object we have, we would then add it to the list of
			// crowds.
			String roomName = null;
			try {
				RoomInfo info = MultiUserChat.getRoomInfo(_xmpp, room.getJid());
				roomName = info.getDescription();
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			double roomLon = parseLon(roomName);
			double roomLat = parseLat(roomName);
			if (roomLon == -1 || roomLat == -1) {
				return null;
				// Something went wrong retrieving the location of the room.
				// We should probably record this somehow so it can be fixed
				// later.
				// But we definitely do not want to continue on..
			} else {
				Location roomLocation = new Location("roomgeneration");
				roomLocation.setLongitude(roomLon);
				roomLocation.setLatitude(roomLat);
				Crowd newCrowd = new Crowd(room.getJid(), roomLocation, _xmpp,
						roomName);
				// @ 1609 meters in a mile.
				float distance = newCrowd.getDistanceFrom(_location);

				if (distance <= maxDistance) {
					roomList.add(newCrowd);
				}

			}
		}
		return roomList;
	}

	private String getServiceName() {
		try {
			return _xmpp.getServiceName();
		} catch (Exception e) {

			e.printStackTrace();
			return null;
		}
	}

	public ArrayList<ParcelableGeoPoint> getUserLocations() {
		try {
			ArrayList<ParcelableGeoPoint> locations = new ArrayList<ParcelableGeoPoint>();
			Iterator<String> occupants = _muc.getOccupants();

			while (occupants.hasNext()) {

				String name = occupants.next();
				VCard card = getVCard(StringUtils.parseResource(name) + "@"
						+ getServiceName());
				String lon = card.getField("longitude");
				String lat = card.getField("latitude");
				if (lon != null && lat != null) {
					locations.add(new ParcelableGeoPoint(new GeoPoint(
							(int) (Double.parseDouble(lat) * 1000000),
							(int) (Double.parseDouble(lon) * 1000000))));
				}
			}
			return locations;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public String getUsername() {
		if(_xmpp.isAnonymous())
		{
			return "Anonymous-" + _xmpp.getConnectionID();
		}
		else
			return _username;
	}

	public VCard getVCard(String user) {
		VCard card = new VCard();
		try {
			card.load(_xmpp, user);
			return card;
		} catch (XMPPException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isAnonymous() {
		return _xmpp.isAnonymous();
	}

	public boolean isAuthenticated() {
		return _xmpp.isAuthenticated();
	}

	public boolean isConnected() {
		return _xmpp.isConnected();
	}

	// roomname-lat:----lon:----
	private String locationToRoomId(String name, Location location) {
		double lon = location.getLongitude();
		double lat = location.getLatitude();
		return name + "-lat:" + Double.toString(lat) + "lon:"
				+ Double.toString(lon);
	}

	public boolean login(String username, String password) {
		try {
			_xmpp.login(username, password);
			_username = username;
			_password = password;
			boolean didUpdate = updateAccountLocation();

			if (!didUpdate) {
				// Unable to update location, notify user?
			}

			VCard card = new VCard();
			try {
				card.load(_xmpp);
			} catch (Exception ex) {
			}

			card.setJabberId(_xmpp.getUser().split("/")[0]);
			card.setField("latitude", Double.toString(_location.getLatitude()));
			card.setField("longitude", Double
					.toString(_location.getLongitude()));
			card.save(_xmpp);

			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean loginAnon() {
		try {
			_xmpp.loginAnonymously();
			_username = "Anonymous";
			return true;
		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	public void logout() {

		try {
			// Not sure about the best way to logout.
			Presence logoutPresense = new Presence(Presence.Type.unavailable,
					"", 1, Presence.Mode.away);
			_xmpp.disconnect();
			_xmpp.connect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private double parseLat(String roomName) {
		// roomname-lat:----lon:----
		try {
			double d = Double.parseDouble(roomName.substring(roomName
					.indexOf("lat:") + 4, roomName.indexOf("lon:")));
			return d;
		} catch (Exception ex) {
			return -1;
		}
	}

	private double parseLon(String roomName) {
		// roomname-lat:----lon:----
		try {
			double d = Double.parseDouble(roomName.substring(roomName
					.indexOf("lon:") + 4));
			return d;
		} catch (Exception ex) {
			return -1;
		}

	}
	
	public void removePrivateChat(Chat c)
	{
		_privateMessageOccupants.remove(c.getParticipant());
		_privateMessageList.remove(c);
	}

	public boolean reportComment(String username, String message) {
		return false;
	}

	/**
	 * Sends a message from the client to the server.
	 * 
	 * @param message
	 *            The message to be sent.
	 * @param crowdId
	 *            The id of the crowd to send the message to.
	 * @param channelMessage
	 *            Boolean indicating whether this is a message sent by the
	 *            channel or not.
	 * @return a boolean indicating success.
	 */
	public boolean sendMessage(String message, String crowdId,
			boolean channelMessage) {
		if (_muc.isJoined()) {
			try {
				Message m = new Message();
				m.addBody("en", message);
				// Need these two fields or else the connection will insta-dc.
				m.setType(Message.Type.groupchat);
				m.setTo(crowdId);
				// For now we are storing the username as the subject,
				// I don't see another way to do this well.
				if (channelMessage)
					m.addSubject("en", "Channel");
				else
					m.addSubject("en", _username);
				_muc.sendMessage(m);
				return true;
			} catch (XMPPException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}

	}

	public void setCrowd(Crowd c) {
		_c = c;
	}

	/**
	 * Sets the location tied to the current user logged in
	 * 
	 * @param il
	 *            The location object to be tied to the user.
	 */
	public void setLocation(Location il) {
		_location = il;
		if (_xmpp.isAuthenticated()) {
			boolean success = updateAccountLocation();
		}
	}

	public void registerCrowdTalkMessageListener(PacketListener listener) {
		_muc.addMessageListener(listener);
	}

	public void removeCrowdTalkMessageListener(PacketListener listener) {
		_muc.removeMessageListener(listener);
	}

	private boolean updateAccountLocation() {

		// We probably want to use a vcard for that.
		return false;
	}

}
