package com.qbook.auth;

import com.qbook.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {

        AuthDtos.AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Регистрация прошла успешно", response));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        AuthDtos.AuthResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.ok("Вход выполнен", response));
    }

    // POST /api/auth/telegram
    @PostMapping("/telegram")
    public ResponseEntity<ApiResponse<AuthDtos.TelegramAuthResponse>> telegramAuth(
            @Valid @RequestBody AuthDtos.TelegramAuthRequest request) {

        AuthDtos.TelegramAuthResponse response = authService.telegramAuth(request);
        return ResponseEntity.ok(ApiResponse.ok("Авторизация через Telegram успешна", response));
    }

    // GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> me(
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(auth.getName()));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // Берём первый IP если несколько
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
