package com.github.dghng36.eauction.core.base;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse <T> {
    private int statusHttpCode;
    private String message;
    private T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> of(String message, int statusHttpCode, T data) {
        return ApiResponse.<T>builder()
            .statusHttpCode(statusHttpCode)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return of(message, HttpStatus.OK.value(), null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return of(message, HttpStatus.OK.value(), data);
    }

    public static <T> ApiResponse<T> error(String message, int statusHttpCode) {
        return of(message, statusHttpCode, null);
    }

    public static <T> ApiResponse<T> error(String message, int statusHttpCode, T data) {
        return of(message, statusHttpCode, data);
    }
}
