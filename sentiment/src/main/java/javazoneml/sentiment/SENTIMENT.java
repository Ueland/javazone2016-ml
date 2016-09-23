package javazoneml.sentiment;

public enum SENTIMENT {
	VERY_NEGATIVE("Very negative"), NEGATIVE("Negative"), NEUTRAL("Neutral"), POSITIVE("Positive"), VERY_POSITIVE(
			"Very positive");

	private String name;

	private SENTIMENT(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static SENTIMENT value(String value) {
		for (SENTIMENT v : values()) {
			if (value.equalsIgnoreCase(v.toString()))
				return v;
		}

		throw new IllegalArgumentException("No sentiment for " + value);
	}
}
