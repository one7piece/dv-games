package com.dv.gtusach.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class MainPlace extends Place {
	private String name;

	public MainPlace(String token) {
		this.name = token;
	}

	public String getName() {
		return name;
	}

	public static class Tokenizer implements PlaceTokenizer<MainPlace> {
		@Override
		public String getToken(MainPlace place) {
			return place.getName();
		}

		@Override
		public MainPlace getPlace(String token) {
			return new MainPlace(token);
		}
	}
}
