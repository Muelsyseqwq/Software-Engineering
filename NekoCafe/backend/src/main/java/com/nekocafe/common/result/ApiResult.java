package com.nekocafe.common.result;

import java.time.OffsetDateTime;

public record ApiResult<T>(int code, String message, T data, String traceId, OffsetDateTime timestamp) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data, null, OffsetDateTime.now());
    }

    public static ApiResult<Void> ok() {
        return ok(null);
    }

    public static ApiResult<Void> fail(int code, String message) {
        return new ApiResult<>(code, message, null, null, OffsetDateTime.now());
    }
}
