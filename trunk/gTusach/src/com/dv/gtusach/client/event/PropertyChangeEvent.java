package com.dv.gtusach.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class PropertyChangeEvent extends GwtEvent<PropertyChangeEventHandler> {	
	public static Type<PropertyChangeEventHandler> TYPE = new Type<PropertyChangeEventHandler>();
	public static enum EventTypeEnum {
		Authentication,
		Script,
		User,
		Book
	}
	private String name;
	private EventTypeEnum eventType;
	private Object value;
	private String errorMessage;

	public PropertyChangeEvent(EventTypeEnum eventType, String name, Object value) {
		this(eventType, name, value, null);
	}
	
	public PropertyChangeEvent(EventTypeEnum eventType, String name, Object value, String errorMessage) {
		this.name = name;
		this.eventType = eventType;
		this.value = value;
		this.errorMessage = errorMessage;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<PropertyChangeEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(PropertyChangeEventHandler handler) {
		handler.onPropertyChanged(this);
	}

	public String getName() {
		return name;
	}

	public EventTypeEnum getEventType() {
		return eventType;
	}
	
	public Object getValue() {
		return value;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
		
}
