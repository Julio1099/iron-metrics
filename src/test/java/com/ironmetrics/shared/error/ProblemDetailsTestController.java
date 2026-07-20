package com.ironmetrics.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class ProblemDetailsTestController {

    @GetMapping("/known-failure")
    void knownFailure() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request.");
    }

    @GetMapping("/unexpected-failure")
    void unexpectedFailure() {
        throw new IllegalStateException("Internal implementation detail.");
    }
}
