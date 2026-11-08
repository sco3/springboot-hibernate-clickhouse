package sco3.bootstrap;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import sco3.domain.Event;
import sco3.domain.Severity;
import sco3.repository.EventRepository;

/**
 * Inserts ~200 demo events on the very first start, so the API has something to show.
 * ClickHouse inserts are the cheap path, so this only takes a moment.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] SOURCES = {
            "checkout-service", "payment-service", "auth-service", "inventory-service", "api-gateway"
    };
    private static final String[] INFO_MESSAGES = {
            "request completed", "cache warmed", "background job finished", "configuration reloaded",
            "health check passed", "deployment finished"
    };
    private static final String[] WARN_MESSAGES = {
            "slow query detected", "retrying upstream call", "cache miss rate high",
            "connection pool nearly exhausted", "deprecated endpoint called"
    };
    private static final String[] ERROR_MESSAGES = {
            "upstream timeout", "payment declined", "unhandled exception",
            "dead-letter queue full", "circuit breaker opened"
    };

    private final EventRepository repository;

    public DemoDataSeeder(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        RandomGenerator random = RandomGenerator.getDefault();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Severity severity = pickSeverity(random.nextDouble());
            String source = SOURCES[random.nextInt(SOURCES.length)];
            String message = pickMessage(severity, random);
            OffsetDateTime createdAt = now.minusSeconds(random.nextLong(24 * 3600));
            events.add(new Event(source, severity, message, createdAt));
        }
        repository.saveAll(events);
        log.info("Seeded {} demo events into ClickHouse", events.size());
    }

    private Severity pickSeverity(double chance) {
        if (chance < 0.70) {
            return Severity.INFO;
        }
        if (chance < 0.90) {
            return Severity.WARN;
        }
        return Severity.ERROR;
    }

    private String pickMessage(Severity severity, RandomGenerator random) {
        return switch (severity) {
            case INFO -> INFO_MESSAGES[random.nextInt(INFO_MESSAGES.length)];
            case WARN -> WARN_MESSAGES[random.nextInt(WARN_MESSAGES.length)];
            case ERROR -> ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        };
    }
}
