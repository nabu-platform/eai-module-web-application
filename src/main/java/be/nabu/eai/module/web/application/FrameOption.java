package be.nabu.eai.module.web.application;

public enum FrameOption {
	NONE(null), DENY("DENY"), SAME_ORIGIN("SAMEORIGIN");
	private String option;

	private FrameOption(String option) {
		this.option = option;
	}

	public String getOption() {
		return option;
	}
}
