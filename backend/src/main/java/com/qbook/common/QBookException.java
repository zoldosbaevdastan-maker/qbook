package com.qbook.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QBookException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public QBookException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    // 400
    public static QBookException badRequest(String message, String code) {
        return new QBookException(message, code, HttpStatus.BAD_REQUEST);
    }

    // 401
    public static QBookException unauthorized(String message) {
        return new QBookException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    // 403
    public static QBookException forbidden(String message) {
        return new QBookException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    // 404
    public static QBookException notFound(String entity, Object id) {
        return new QBookException(
                entity + " не найден: " + id,
                entity.toUpperCase() + "_NOT_FOUND",
                HttpStatus.NOT_FOUND);
    }

    // 409
    public static QBookException conflict(String message, String code) {
        return new QBookException(message, code, HttpStatus.CONFLICT);
    }

    // 422
    public static QBookException unprocessable(String message, String code) {
        return new QBookException(message, code, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // Business-specific
    public static QBookException insufficientBalance(double required, double current) {
        return new QBookException(
                String.format("Недостаточно средств. Требуется: %.2f, доступно: %.2f", required, current),
                "INSUFFICIENT_BALANCE",
                HttpStatus.PAYMENT_REQUIRED);
    }

    public static QBookException businessBlocked(String businessName) {
        return new QBookException(
                "Бизнес " + businessName + " заблокирован",
                "BUSINESS_BLOCKED",
                HttpStatus.FORBIDDEN);
    }

    public static QBookException slotUnavailable() {
        return new QBookException(
                "Выбранное время уже занято",
                "SLOT_UNAVAILABLE",
                HttpStatus.CONFLICT);
    }
}
