package com.qbook.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        @Pattern(regexp = ".*[A-Z].*", message = "Пароль должен содержать заглавную букву")
        @Pattern(regexp = ".*[0-9].*", message = "Пароль должен содержать цифру")
        private String password;

        @NotBlank(message = "Название бизнеса обязательно")
        @Size(min = 2, max = 100, message = "Название от 2 до 100 символов")
        private String name;

        @NotNull(message = "Тип бизнеса обязателен")
        private String type; // services/booking/food/shop

        private String city;
        private String phone;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email обязателен")
        @Email
        private String email;

        @NotBlank(message = "Пароль обязателен")
        private String password;
    }

    @Data
    public static class TelegramAuthRequest {
        @NotBlank
        private String initData; // raw initData от Telegram
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
        private UserInfo user;

        @Data
        public static class UserInfo {
            private String id;
            private String email;
            private String name;
            private String type;
            private String role;
            private String status;
            private boolean verified;
        }
    }

    @Data
    public static class TelegramAuthResponse {
        private String accessToken;
        private long expiresIn;
        private ClientInfo client;

        @Data
        public static class ClientInfo {
            private String id;
            private Long telegramId;
            private String firstName;
            private String lastName;
            private String username;
        }
    }
}
