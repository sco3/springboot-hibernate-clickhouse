package sco3.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import sco3.service.EventService;
import sco3.web.dto.StatsResponse;

@RestController
public class StatsController {

	private final EventService service;

	public StatsController(EventService service) {
		this.service = service;
	}

	@GetMapping("/api/stats")
	public StatsResponse stats() {
		return service.stats();
	}
}
