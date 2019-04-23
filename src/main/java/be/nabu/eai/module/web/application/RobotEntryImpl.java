package be.nabu.eai.module.web.application;

import be.nabu.eai.module.web.application.api.RobotEntry;

public class RobotEntryImpl implements RobotEntry {
	private String key, value;

	public RobotEntryImpl() {
		// auto
	}
	public RobotEntryImpl(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
