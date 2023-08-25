package be.nabu.eai.module.web.application.api;

import java.util.List;

import be.nabu.libs.types.api.KeyValuePair;

public interface TranslationKey extends KeyValuePair {
	// the files where this key is found (can be multiple)
	public List<String> getFiles();
}
