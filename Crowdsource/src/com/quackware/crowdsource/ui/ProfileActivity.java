/*******************************************************************************
 * Copyright (c) 2012 Curtis Larson (QuackWare).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.quackware.crowdsource.ui;

import org.jivesoftware.smackx.packet.VCard;

import com.quackware.crowdsource.MyApplication;
import com.quackware.crowdsource.Profile;
import com.quackware.crowdsource.R;
import com.quackware.crowdsource.ServerUtil;
import com.quackware.crowdsource.R.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

//Give them an option to view on map (Just user and person thats looking at profile)
//Allow edit of profile from own profile screen.

public class ProfileActivity extends Activity implements OnClickListener {

	private static final int EDIT_PROFILE_DIALOG = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);

		Intent i = getIntent();
		Profile p = (Profile) i.getParcelableExtra("profile");

		setupProfile(p);
		revealLinearLayouts();
	}

	private void revealLinearLayouts() {
		LinearLayout ll = (LinearLayout) findViewById(R.id.about_me_layout);
		ll.setVisibility(View.VISIBLE);
		ll = (LinearLayout) findViewById(R.id.age_layout);
		ll.setVisibility(View.VISIBLE);
		ll = (LinearLayout) findViewById(R.id.sex_layout);
		ll.setVisibility(View.VISIBLE);
		ll = (LinearLayout) findViewById(R.id.name_layout);
		ll.setVisibility(View.VISIBLE);
		ll = (LinearLayout) findViewById(R.id.location_layout);
		ll.setVisibility(View.VISIBLE);
		ll = (LinearLayout) findViewById(R.id.username_layout);
		ll.setVisibility(View.VISIBLE);
	}

	private void setupProfile(Profile p) {

		// All the different things that the profile is going to have.
		// Some of it is provided by the profile object (The necessary stuff)
		// while the rest is found via VCards
		int distance;
		String username = null;
		String realName = null;
		String gender = null;
		String about_me = null;
		String age = null;

		ServerUtil su = ((MyApplication) this.getApplication()).getServerUtil();
		if (p.isMyProfile()) {
			distance = 0;
			username = "Your Profile";

			// We may also want to make it so that we can edit values if it
			// is our own profile.
			((LinearLayout) findViewById(R.id.edit_profile_layout))
					.setVisibility(View.VISIBLE);
			Button editButton = (Button) findViewById(R.id.edit_profile_button);
			editButton.setOnClickListener(this);
		} else {
			distance = (int) (su.getLocation().distanceTo(p.getLocation()) / 1609);
			username = p.getUsername();

			VCard v = ((MyApplication) this.getApplication()).getServerUtil()
					.getVCard(username);

			// These .getField methods are either going to return a value or
			// null if the person
			// never edited their profile and added this information.
			if(v != null)
			{
				realName = v.getField("realName");
				gender = v.getField("gender");
				about_me = v.getField("about_me");
				age = v.getField("age");
			}
		}

		((TextView) findViewById(R.id.username)).setText(username);
		((TextView) findViewById(R.id.location)).setText(distance
				+ " miles away!");

		// Set the textviews (as long as the info is not null)
		if (realName != null) {
			((TextView) findViewById(R.id.name)).setText(realName);
		}
		if (gender != null) {
			((TextView) findViewById(R.id.sex)).setText(gender);
		}
		if (about_me != null) {
			((TextView) findViewById(R.id.about_me)).setText(about_me);
		}
		if (age != null) {
			((TextView) findViewById(R.id.age)).setText(age);
		}
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case EDIT_PROFILE_DIALOG:
			LayoutInflater factory = LayoutInflater.from(this);
			final View editProfileView = factory.inflate(R.layout.editprofile,
					null);

			return new AlertDialog.Builder(ProfileActivity.this).setTitle(
					getString(R.string.edit_profile)).setView(editProfileView)
					.setPositiveButton(getString(R.string.save),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									// Grab all the texts from the new textview
									// maybe perform some checks like a
									// numberical age (should we tell them or
									// just ignore)
									// then get a vcard and use _su to send it.
								}
							}).setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									// We don't need to do anything here.
								}
							}).create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.edit_profile_button:
			showDialog(EDIT_PROFILE_DIALOG);
			break;
		default:
			return;
		}
	}

}
