package io.myforevermusic.api.modules.system.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnBootstrapInfo() throws Exception {
        mockMvc.perform(get("/api/v1/system/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("my-forever-music-api"))
            .andExpect(jsonPath("$.status").value("BOOTSTRAP_READY"));
    }
}
