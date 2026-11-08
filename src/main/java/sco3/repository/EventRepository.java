package sco3.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sco3.domain.Event;

/**
 * Standard Spring Data JPA repository: {@code save}, {@code findById}, HQL
 * queries etc. go through Hibernate and the
 * {@link com.example.clickhouse.dialect.ClickHouseDialect}.
 *
 * <p>
 * Whatever ClickHouse cannot express as plain SQL (row updates, aggregates on
 * ClickHouse-specific functions) is done with native queries instead.
 * </p>
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

	/**
	 * Recent events, newest first (native so we can use ClickHouse's LIMIT syntax).
	 */
	@Query(value = "SELECT * FROM events ORDER BY created_at DESC, id DESC LIMIT :limit", nativeQuery = true)
	List<Event> findRecent(@Param("limit") int limit);

	/** Events per severity, computed by ClickHouse. */
	@Query(value = "SELECT severity, count() AS total FROM events GROUP BY severity ORDER BY total DESC", nativeQuery = true)
	List<SeverityCount> countBySeverity();

	/**
	 * Events bucketed by hour for the last 24 hours (countIf is
	 * ClickHouse-specific).
	 */
	@Query(value = """
			SELECT CAST(toStartOfHour(created_at) AS String) AS bucket,
			       count()                            AS total,
			       countIf(severity = 'ERROR')        AS errors
			FROM events
			WHERE created_at >= now64(6) - INTERVAL 24 HOUR
			GROUP BY bucket
			ORDER BY bucket
			""", nativeQuery = true)
	List<HourlyCount> countByHourLast24h();

	/**
	 * Row update as a ClickHouse mutation. {@code mutations_sync = 1} makes the
	 * call block until the mutation has been applied, so a subsequent read sees the
	 * new value. {@code clearAutomatically} is needed because the bulk update
	 * bypasses the persistence context, which would otherwise keep serving the
	 * stale entity.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			ALTER TABLE events UPDATE message = :message
			WHERE id = :id
			SETTINGS mutations_sync = 1
			""", nativeQuery = true)
	void updateMessage(@Param("id") UUID id, @Param("message") String message);

	/**
	 * ClickHouse lightweight delete (row is hidden immediately, merged away later).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "DELETE FROM events WHERE id = :id", nativeQuery = true)
	void deleteById(@Param("id") UUID id);

	interface SeverityCount {
		String getSeverity();

		Number getTotal();
	}

	interface HourlyCount {
		String getBucket();

		Number getTotal();

		Number getErrors();
	}
}
