package sco3.web.dto;

import java.util.List;
import java.util.Map;

public record StatsResponse(long total, Map<String, Long> bySeverity,
		List<HourlyCountResponse> last24h) {
}
