package be.nabu.eai.module.web.application;

import be.nabu.libs.types.api.ComplexType;

public interface WebFragmentConfiguration {
	public ComplexType getType();
	public String getPath();
}
