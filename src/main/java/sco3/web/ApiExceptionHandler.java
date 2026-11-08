package sco3.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import sco3.service.EventNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(EventNotFoundException.class)
	ProblemDetail handleNotFound(EventNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				ex.getMessage());
		problem.setTitle("Not found");
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(LinkedHashMap::new,
						(map, field) -> map
								.put(field.getField(),
										field.getDefaultMessage() == null ? "invalid"
												: field.getDefaultMessage()),
						Map::putAll);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "Request validation failed");
		problem.setTitle("Validation error");
		problem.setProperty("fields", errors);
		ex.getGlobalErrors().stream()
				.map(DefaultMessageSourceResolvable::getDefaultMessage)
				.forEach(message -> problem.setProperty("error", message));
		return problem;
	}
}
