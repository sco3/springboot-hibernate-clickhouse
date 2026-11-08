package sco3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import sco3.web.dto.CreateEventRequest;
import sco3.web.dto.EventResponse;
import sco3.web.dto.StatsResponse;
import sco3.web.dto.UpdateMessageRequest;

/**
 * Full round trip through the REST layer, Hibernate and ClickHouse. Requires
 * the database: {@code docker compose up -d} first. Skips itself (instead of
 * failing) when ClickHouse is not reachable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class EventApiIntegrationTest {

	@Autowired
	private TestRestTemplate rest;

	@BeforeAll
	static void requireClickHouse() {
		Assumptions.assumeTrue(clickHouseIsUp(),
				"ClickHouse is not running on localhost:8123 - start it with 'docker compose up -d'");
	}

	private static boolean clickHouseIsUp() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", 8123), 1500);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static HttpHeaders json() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	@Test
	void createsReadsUpdatesAndDeletesEvents() {
		// create
		ResponseEntity<EventResponse> created = rest.postForEntity("/api/events",
				new HttpEntity<>(
						new CreateEventRequest("test-service",
								sco3.domain.Severity.WARN, "integration test event"),
						json()),
				EventResponse.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		EventResponse event = created.getBody();
		assertThat(event).isNotNull();
		assertThat(event.id()).isNotNull();
		assertThat(event.severity()).isEqualTo("WARN");
		assertThat(event.createdAt()).isNotNull();

		// read it back through Hibernate
		ResponseEntity<EventResponse> fetched = rest
				.getForEntity("/api/events/" + event.id(), EventResponse.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(fetched.getBody().message()).isEqualTo("integration test event");

		// update via ClickHouse mutation
		ResponseEntity<EventResponse> updated = rest
				.exchange("/api/events/" + event.id(), HttpMethod.PUT,
						new HttpEntity<>(new UpdateMessageRequest(
								"message rewritten by mutation"), json()),
						EventResponse.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().message())
				.isEqualTo("message rewritten by mutation");

		// delete via lightweight delete
		ResponseEntity<Void> deleted = rest.exchange("/api/events/" + event.id(),
				HttpMethod.DELETE, null, Void.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		ResponseEntity<String> gone = rest.getForEntity("/api/events/" + event.id(),
				String.class);
		assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void listsRecentEvents() {
		ResponseEntity<List<EventResponse>> response = rest.exchange(
				"/api/events?limit=10", HttpMethod.GET, null,
				new ParameterizedTypeReference<>() {
				});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotEmpty();
		assertThat(response.getBody()).hasSizeLessThanOrEqualTo(10);
	}

	@Test
	void aggregatesStatsInClickHouse() {
		ResponseEntity<StatsResponse> response = rest.getForEntity("/api/stats",
				StatsResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		StatsResponse stats = response.getBody();
		assertThat(stats).isNotNull();
		assertThat(stats.total()).isPositive();
		assertThat(stats.bySeverity()).containsKey("INFO");
	}
}
