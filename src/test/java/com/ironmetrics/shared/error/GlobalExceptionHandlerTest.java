package com.ironmetrics.shared.error;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProblemDetailsTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc;

    @Autowired
    GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldReturnRfc7807ProblemDetailForKnownHttpException() throws Exception {
        mockMvc.perform(get("/known-failure"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Invalid request.")))
                .andExpect(jsonPath("$.instance", is("/known-failure")));
    }

    @Test
    void shouldReturnRfc7807ProblemDetailForUnexpectedException() throws Exception {
        mockMvc.perform(get("/unexpected-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred.")))
                .andExpect(jsonPath("$.instance", is("/unexpected-failure")));
    }
}
