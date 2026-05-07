package com.qbook.auth;

import com.qbook.business.Business;
import com.qbook.business.BusinessRepository;
import com.qbook.clients.Client;
import com.qbook.clients.ClientRepository;
import com.qbook.common.QBookException;
import com.qbook.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final BusinessRepository businessRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_ATTEMPTS_KEY = "login_attempts:";
    private static final String BLOCKED_KEY = "blocked:";

    // ===== РЕГИСТРАЦИЯ БИЗНЕСА =====
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        // Проверка что email свободен
        if (businessRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw QBookException.conflict(
                "Email уже зарегистрирован", "EMAIL_EXISTS");
        }

        // Валидация типа бизнеса
        Business.BusinessType type;
        try {
            type = Business.BusinessType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            throw QBookException.badRequest(
                "Неверный тип бизнеса. Допустимые: services, booking, food, shop",
                "INVALID_BUSINESS_TYPE");
        }

        // Создаём бизнес
        Business business = Business.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .type(type)
                .city(request.getCity())
                .phone(request.getPhone())
                .status(Business.BusinessStatus.pending)
                .verified(false)
                .build();

        business = businessRepository.save(business);
        log.info("Новый бизнес зарегистрирован: {} ({})", business.getName(), business.getId());

        return generateBusinessTokens(business);
    }

    // ===== ВХОД БИЗНЕСА =====
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, String ip) {
        String email = request.getEmail().toLowerCase().trim();

        // Проверка блокировки по IP
        if (isBlocked(ip)) {
            throw QBookException.unauthorized(
                "Слишком много попыток. Попробуйте через 15 минут");
        }

        // Ищем бизнес
        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> {
                    recordFailedAttempt(ip);
                    return QBookException.unauthorized("Неверный email или пароль");
                });

        // Проверяем пароль
        if (!passwordEncoder.matches(request.getPassword(), business.getPasswordHash())) {
            recordFailedAttempt(ip);
            throw QBookException.unauthorized("Неверный email или пароль");
        }

        // Сбрасываем счётчик
        clearAttempts(ip);

        if (business.isBlocked()) {
            throw QBookException.forbidden("Аккаунт заблокирован");
        }

        if (business.getDeletedAt() != null) {
            throw QBookException.forbidden("Аккаунт удалён");
        }

        log.info("Вход бизнеса: {} ({})", business.getName(), business.getId());
        return generateBusinessTokens(business);
    }

    // ===== АВТОРИЗАЦИЯ КЛИЕНТА ЧЕРЕЗ TELEGRAM =====
    @Transactional
    public AuthDtos.TelegramAuthResponse telegramAuth(AuthDtos.TelegramAuthRequest request) {
        // Верифицируем initData от Telegram
        Map<String, String> data = parseTelegramInitData(request.getInitData());

        if (!verifyTelegramInitData(request.getInitData())) {
            throw QBookException.unauthorized("Недействительная подпись Telegram");
        }

        // Парсим данные пользователя
        String userJson = data.get("user");
        if (userJson == null) {
            throw QBookException.badRequest("Нет данных пользователя", "NO_USER_DATA");
        }

        Long telegramId = extractTelegramId(userJson);
        String firstName = extractField(userJson, "first_name");
        String lastName = extractField(userJson, "last_name");
        String username = extractField(userJson, "username");

        // Создаём или обновляем клиента
        Client client = clientRepository.findByTelegramId(telegramId)
                .orElseGet(() -> Client.builder()
                        .telegramId(telegramId)
                        .build());

        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setUsername(username);
        client = clientRepository.save(client);

        if (client.isBlocked()) {
            throw QBookException.forbidden("Аккаунт заблокирован");
        }

        // Генерируем токен для клиента
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CLIENT");
        claims.put("clientId", client.getId().toString());
        claims.put("telegramId", telegramId);

        org.springframework.security.core.userdetails.User userDetails =
            new org.springframework.security.core.userdetails.User(
                "tg:" + telegramId,
                "",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CLIENT"))
            );

        String accessToken = jwtService.generateAccessToken(userDetails, claims);

        AuthDtos.TelegramAuthResponse response = new AuthDtos.TelegramAuthResponse();
        response.setAccessToken(accessToken);
        response.setExpiresIn(properties.getJwt().getAccessTokenExpiry() / 1000);

        AuthDtos.TelegramAuthResponse.ClientInfo info =
            new AuthDtos.TelegramAuthResponse.ClientInfo();
        info.setId(client.getId().toString());
        info.setTelegramId(telegramId);
        info.setFirstName(firstName);
        info.setLastName(lastName);
        info.setUsername(username);
        response.setClient(info);

        log.info("Telegram авторизация: {} (tg:{})", client.getFullName(), telegramId);
        return response;
    }

    // ===== ВЕРИФИКАЦИЯ TELEGRAM initData =====
    private boolean verifyTelegramInitData(String initData) {
        try {
            Map<String, String> params = parseTelegramInitData(initData);
            String hash = params.remove("hash");
            if (hash == null) return false;

            // Сортируем и формируем строку
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // Вычисляем secret key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                "WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = mac.doFinal(
                properties.getTelegram().getBotToken()
                    .getBytes(StandardCharsets.UTF_8));

            // Вычисляем HMAC
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] expectedHash = mac.doFinal(
                dataCheckString.getBytes(StandardCharsets.UTF_8));

            String expectedHashHex = HexFormat.of().formatHex(expectedHash);
            return MessageDigest.isEqual(
                expectedHashHex.getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("Ошибка верификации Telegram: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, String> parseTelegramInitData(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : initData.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                params.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private Long extractTelegramId(String userJson) {
        try {
            // Простой парсинг без Jackson для избежания циклических зависимостей
            String idStr = userJson.replaceAll(".*\"id\":(\\d+).*", "$1");
            return Long.parseLong(idStr);
        } catch (Exception e) {
            throw QBookException.badRequest("Ошибка парсинга Telegram user", "PARSE_ERROR");
        }
    }

    private String extractField(String json, String field) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + field + "\":\"([^\"]+)\"")
                .matcher(json);
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== ГЕНЕРАЦИЯ ТОКЕНОВ ДЛЯ БИЗНЕСА =====
    private AuthDtos.AuthResponse generateBusinessTokens(Business business) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "BUSINESS");
        claims.put("businessId", business.getId().toString());
        claims.put("businessType", business.getType().name());
        claims.put("businessStatus", business.getStatus().name());

        org.springframework.security.core.userdetails.User userDetails =
            new org.springframework.security.core.userdetails.User(
                business.getEmail(),
                business.getPasswordHash(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_" + business.getType().name().toUpperCase()))
            );

        String accessToken = jwtService.generateAccessToken(userDetails, claims);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        AuthDtos.AuthResponse response = new AuthDtos.AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(properties.getJwt().getAccessTokenExpiry() / 1000);

        AuthDtos.AuthResponse.UserInfo info = new AuthDtos.AuthResponse.UserInfo();
        info.setId(business.getId().toString());
        info.setEmail(business.getEmail());
        info.setName(business.getName());
        info.setType(business.getType().name());
        info.setRole("BUSINESS");
        info.setStatus(business.getStatus().name());
        info.setVerified(business.isVerified());
        response.setUser(info);

        return response;
    }

    // ===== RATE LIMITING =====
    private boolean isBlocked(String ip) {
        Object blocked = redisTemplate.opsForValue().get(BLOCKED_KEY + ip);
        return blocked != null;
    }

    private void recordFailedAttempt(String ip) {
        String key = LOGIN_ATTEMPTS_KEY + ip;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 15, TimeUnit.MINUTES);

        int maxAttempts = properties.getMaxLoginAttempts();
        if (attempts != null && attempts >= maxAttempts) {
            redisTemplate.opsForValue().set(
                BLOCKED_KEY + ip, "1",
                Duration.ofMinutes(properties.getLoginBlockMinutes()));
            redisTemplate.delete(key);
            log.warn("IP заблокирован после {} попыток: {}", maxAttempts, ip);
        }
    }

    private void clearAttempts(String ip) {
        redisTemplate.delete(LOGIN_ATTEMPTS_KEY + ip);
        redisTemplate.delete(BLOCKED_KEY + ip);
    }
}
