package be.nabu.eai.module.web.application;

import be.nabu.libs.types.api.ComplexType;

public interface WebFragmentConfiguration {
	/**
	 * The type of the configuration
	 */
	public ComplexType getType();
	/**
	 * The paths interested in this configuration
	 */
	public String getPath();
}
