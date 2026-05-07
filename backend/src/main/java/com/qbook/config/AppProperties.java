package com.qbook.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "qbook")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Supabase supabase = new Supabase();
    private Telegram telegram = new Telegram();
    private Fraud fraud = new Fraud();
    private Media media = new Media();
    private Frontend frontend = new Frontend();

    private double commissionRate = 0.10;
    private double lowBalanceThreshold = 500;
    private String bookingRefPrefix = "QBK";
    private int cartSessionTtlHours = 3;
    private int bindingCodeTtlHours = 24;
    private int topupAlertHours = 2;
    private int topupCriticalHours = 6;
    private int maxLoginAttempts = 5;
    private int loginBlockMinutes = 15;

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpiry = 900000L;
        private long refreshTokenExpiry = 2592000000L;
    }

    @Data
    public static class Supabase {
        private String url;
        private String anonKey;
        private String serviceKey;
        private String storageBucket = "qbook-media";
    }

    @Data
    public static class Telegram {
        private String botToken;
        private String botUsername;
        private String webhookSecret;
        private String miniAppUrl;
    }

    @Data
    public static class Fraud {
        private int allowThreshold = 30;
        private int confirmThreshold = 60;
        private int reviewThreshold = 80;
        private int blockThreshold = 80;
    }

    @Data
    public static class Media {
        private long logoMaxSize = 5_242_880L;
        private long coverMaxSize = 10_485_760L;
        private long videoMaxSize = 104_857_600L;
        private List<String> allowedImageTypes = List.of("image/jpeg", "image/png", "image/webp");
        private List<String> allowedVideoTypes = List.of("video/mp4");
        private int thumbnailSize = 200;
        private float webpQuality = 0.85f;
    }

    @Data
    public static class Frontend {
        private String siteUrl;
        private String miniappUrl;
        private String verifyUrl;
    }
}
