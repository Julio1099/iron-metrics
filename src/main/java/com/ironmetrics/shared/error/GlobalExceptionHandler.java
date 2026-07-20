package com.ironmetrics.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                exception.getStatusCode(),
                exception.getReason(),
                request
        );

        return problemResponse(exception.getStatusCode(), problem);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponseException(
            ErrorResponseException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = exception.getBody();
        problem.setInstance(URI.create(request.getRequestURI()));

        return problemResponse(exception.getStatusCode(), problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request
        );
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Invalid value."
                                : fieldError.getDefaultMessage(),
                        (first, ignored) -> first
                ));
        problem.setProperty("fieldErrors", fieldErrors);

        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );

        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    private ProblemDetail createProblem(
            HttpStatusCode statusCode,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusCode, detail);
        problem.setTitle(resolveTitle(statusCode));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private String resolveTitle(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }

        return "HTTP " + statusCode.value();
    }

    private ResponseEntity<ProblemDetail> problemResponse(
            HttpStatusCode statusCode,
            ProblemDetail problem
    ) {
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
