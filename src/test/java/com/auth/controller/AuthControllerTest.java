package com.auth.controller;

import com.auth.config.JwtProperties;
import com.auth.dto.request.LoginRequest;
import com.auth.service.AuthService;
import com.auth.service.TwoFactorService;
import com.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TwoFactorService twoFactorService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private com.auth.repository.ApiKeyRepository apiKeyRepository;

    @MockBean
    private com.auth.security.JwtUtils jwtUtils;

    @MockBean
    private com.auth.config.SecurityProperties securityProperties;

    @MockBean
    private com.auth.repository.IpRuleRepository ipRuleRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        com.auth.config.SecurityProperties.Cors cors = new com.auth.config.SecurityProperties.Cors();
        com.auth.config.SecurityProperties.RateLimit rateLimit = new com.auth.config.SecurityProperties.RateLimit();
        when(securityProperties.getCors()).thenReturn(cors);
        when(securityProperties.getRateLimit()).thenReturn(rateLimit);
    }

    @Test
    void login_withEmptyTotpCode_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTenantSlug("default");
        request.setUsername("admin");
        request.setPassword("Admin@123");
        request.setTotpCode(""); // Empty or missing should trigger validation error

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void login_withMissingFields_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        // Missing username, password, etc.

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setTenantSlug("default");
        request.setUsername("admin");
        request.setPassword("Admin@123");
        request.setTotpCode("000000");

        com.auth.domain.User mockUser = new com.auth.domain.User();
        mockUser.setId(java.util.UUID.randomUUID());
        mockUser.setUsername("admin");
        mockUser.setEmail("admin@example.com");
        mockUser.setRoles(new java.util.HashSet<>());

        when(authService.login(any(), any(), any(), any(), any()))
                .thenReturn(new AuthService.LoginResult("access", "refresh", mockUser));
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(900000L);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }
}
