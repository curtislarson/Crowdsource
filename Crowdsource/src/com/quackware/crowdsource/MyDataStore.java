package com.quackware.crowdsource;

import java.util.ArrayList;

import com.quackware.crowdsource.ui.CrowdPreference;
import com.quackware.crowdsource.ui.CrowdChoice.CreateCrowdTask;
import com.quackware.crowdsource.ui.CrowdChoice.CrowdListTask;
import com.quackware.crowdsource.ui.CrowdMap.LoadPointsTask;
import com.quackware.crowdsource.ui.CrowdPreference.ChangePasswordTask;
import com.quackware.crowdsource.ui.CrowdSource.ConnectTask;
import com.quackware.crowdsource.ui.CrowdSource.LoginTask;
import com.quackware.crowdsource.ui.CrowdSource.RegisterTask;
import com.quackware.crowdsource.ui.MessageActivity.ViewProfileTask;

public class MyDataStore {
	// CrowdSource tasks.
	private LoginTask _loginTask;
	private RegisterTask _registerTask;
	private ConnectTask _connectTask;
	// private LocationTask _locationTask;

	// CrowdChoice tasks.
	private CrowdListTask _crowdListTask;
	private CreateCrowdTask _createCrowdTask;
	// CrowdChoice cache list.
	private ArrayList<Crowd> _crowdCache;

	// CrowdPreference tasks.
	private ChangePasswordTask _changePasswordTask;
	private CrowdPreference.LoginTask _CPLoginTask;

	// MessageActivity tasks.
	private ViewProfileTask _viewProfileTask;

	// CrowdMap tasks.
	private LoadPointsTask _loadPointsTask;

	public MyDataStore() {
	}

	public void setLoginTask(LoginTask _loginTask) {
		this._loginTask = _loginTask;
	}

	public LoginTask getLoginTask() {
		return _loginTask;
	}

	public void setRegisterTask(RegisterTask _registerTask) {
		this._registerTask = _registerTask;
	}

	public RegisterTask getRegisterTask() {
		return _registerTask;
	}

	public void setConnectTask(ConnectTask _connectTask) {
		this._connectTask = _connectTask;
	}

	public ConnectTask getConnectTask() {
		return _connectTask;
	}

	/*
	 * public void setLocationTask(LocationTask _locationTask) {
	 * this._locationTask = _locationTask; }
	 * 
	 * public LocationTask getLocationTask() { return _locationTask; }
	 */

	public void setCrowdListTask(CrowdListTask _crowdListTask) {
		this._crowdListTask = _crowdListTask;
	}

	public CrowdListTask getCrowdListTask() {
		return _crowdListTask;
	}

	public void setCrowdCache(ArrayList<Crowd> _crowdCache) {
		this._crowdCache = _crowdCache;
	}

	public ArrayList<Crowd> getCrowdCache() {
		return _crowdCache;
	}

	public void setCreateCrowdTask(CreateCrowdTask _createCrowdTask) {
		this._createCrowdTask = _createCrowdTask;
	}

	public CreateCrowdTask getCreateCrowdTask() {
		return _createCrowdTask;
	}

	public void setChangePasswordTask(ChangePasswordTask _changePasswordTask) {
		this._changePasswordTask = _changePasswordTask;
	}

	public ChangePasswordTask getChangePasswordTask() {
		return _changePasswordTask;
	}

	public void setCPLoginTask(CrowdPreference.LoginTask _CPLoginTask) {
		this._CPLoginTask = _CPLoginTask;
	}

	public CrowdPreference.LoginTask getCPLoginTask() {
		return _CPLoginTask;
	}

	public void setViewProfileTask(ViewProfileTask _viewProfileTask) {
		this._viewProfileTask = _viewProfileTask;
	}

	public ViewProfileTask getViewProfileTask() {
		return _viewProfileTask;
	}

	public void setLoadPointsTask(LoadPointsTask _loadPointsTask) {
		this._loadPointsTask = _loadPointsTask;
	}

	public LoadPointsTask getLoadPointsTask() {
		return _loadPointsTask;
	}

}