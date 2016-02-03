package nabu.web.application.types;

import be.nabu.libs.types.api.KeyValuePair;

public class PropertyImpl implements KeyValuePair {
	private String key, value;
	public PropertyImpl() {
		// auto construct
	}
	public PropertyImpl(String key, String value) {
		this.key = key;
		this.value = value;
	}
	@Override
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	@Override
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}