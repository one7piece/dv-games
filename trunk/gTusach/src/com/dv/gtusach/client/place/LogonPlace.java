package com.dv.gtusach.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class LogonPlace extends Place {
	private String name;

	public LogonPlace(String token) {
		this.name = token;
	}

	public String getName() {
		return name;
	}

	public static class Tokenizer implements PlaceTokenizer<LogonPlace> {
		@Override
		public String getToken(LogonPlace place) {
			return place.getName();
		}

		@Override
		public LogonPlace getPlace(String token) {
			return new LogonPlace(token);
		}

	}
}
