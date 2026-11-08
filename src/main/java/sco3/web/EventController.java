package sco3.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import sco3.service.EventService;
import sco3.web.dto.CreateEventRequest;
import sco3.web.dto.EventResponse;
import sco3.web.dto.UpdateMessageRequest;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService service;

	public EventController(EventService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<EventResponse> create(
			@Valid @RequestBody CreateEventRequest request) {
		EventResponse created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	public List<EventResponse> recent(@RequestParam(defaultValue = "50") int limit) {
		return service.recent(limit);
	}

	@GetMapping("/{id}")
	public EventResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PutMapping("/{id}")
	public EventResponse updateMessage(@PathVariable UUID id,
			@Valid @RequestBody UpdateMessageRequest request) {
		return service.updateMessage(id, request.message());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
