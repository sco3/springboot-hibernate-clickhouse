package sco3.web.dto;

import sco3.domain.Severity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(

		@NotBlank @Size(max = 64) String source,

		@NotNull Severity severity,

		@NotBlank @Size(max = 4096) String message) {
}
