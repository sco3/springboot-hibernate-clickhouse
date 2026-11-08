package sco3.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import sco3.domain.Event;

public record EventResponse(UUID id, String source, String severity, String message,
		OffsetDateTime createdAt) {

	public static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getSource(),
				event.getSeverity().name(), event.getMessage(), event.getCreatedAt());
	}
}
