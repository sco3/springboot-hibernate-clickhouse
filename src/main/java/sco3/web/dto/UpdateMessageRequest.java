package sco3.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMessageRequest(

		@NotBlank @Size(max = 4096) String message) {
}
