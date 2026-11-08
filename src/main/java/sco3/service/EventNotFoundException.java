package sco3.service;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final UUID id;

	public EventNotFoundException(UUID id) {
		super("Event %s not found".formatted(id));
		this.id = id;
	}

	public UUID getId() {
		return id;
	}
}
