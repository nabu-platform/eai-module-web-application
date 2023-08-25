package nabu.web.application.types;

import java.util.List;

import be.nabu.eai.module.web.application.api.TranslationKey;

public class TranslationKeyImpl extends PropertyImpl implements TranslationKey {

	private List<String> files;
	
	public TranslationKeyImpl() {
		super();
	}
	public TranslationKeyImpl(String key, String value) {
		super(key, value);
	}
	
	@Override
	public List<String> getFiles() {
		return files;
	}
	public void setFiles(List<String> files) {
		this.files = files;
	}

}
