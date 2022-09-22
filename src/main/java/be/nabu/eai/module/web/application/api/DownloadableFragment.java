package be.nabu.eai.module.web.application.api;

public interface DownloadableFragment {
	public boolean isDownloadable();
	public String getDownloadAuthenticationIdParameter();
	public String getDownloadSecretParameter();
	public String getDownloadCorrelationIdParameter();
}
