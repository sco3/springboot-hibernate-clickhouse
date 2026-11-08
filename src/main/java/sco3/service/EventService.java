package sco3.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sco3.domain.Event;
import sco3.repository.EventRepository;
import sco3.web.dto.CreateEventRequest;
import sco3.web.dto.EventResponse;
import sco3.web.dto.HourlyCountResponse;
import sco3.web.dto.StatsResponse;

@Service
public class EventService {

	private static final int MAX_LIMIT = 1000;

	private final EventRepository repository;

	public EventService(EventRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<EventResponse> recent(int limit) {
		int bounded = Math.min(Math.max(limit, 1), MAX_LIMIT);
		return repository.findRecent(bounded).stream().map(EventResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public EventResponse get(UUID id) {
		return repository.findById(id).map(EventResponse::from)
				.orElseThrow(() -> new EventNotFoundException(id));
	}

	@Transactional
	public EventResponse create(CreateEventRequest request) {
		Event saved = repository.save(new Event(request.source().trim(),
				request.severity(), request.message().trim(), null));
		return EventResponse.from(saved);
	}

	/**
	 * Updates one message via a ClickHouse mutation. Unlike a JPA {@code merge}
	 * this rewrites the row in place and is the idiomatic way to change data in
	 * ClickHouse.
	 */
	@Transactional
	public EventResponse updateMessage(UUID id, String message) {
		Event event = repository.findById(id)
				.orElseThrow(() -> new EventNotFoundException(id));
		repository.updateMessage(id, message.trim());
		// re-read so the response reflects the mutated row
		return EventResponse.from(repository.findById(event.getId()).orElse(event));
	}

	@Transactional
	public void delete(UUID id) {
		if (!repository.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		repository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public StatsResponse stats() {
		long total = repository.count();
		Map<String, Long> bySeverity = repository.countBySeverity().stream()
				.collect(Collectors.toMap(EventRepository.SeverityCount::getSeverity,
						row -> row.getTotal().longValue()));
		List<HourlyCountResponse> last24h = repository.countByHourLast24h().stream()
				.map(row -> new HourlyCountResponse(row.getBucket(),
						row.getTotal().longValue(), row.getErrors().longValue()))
				.toList();
		return new StatsResponse(total, bySeverity, last24h);
	}
}
