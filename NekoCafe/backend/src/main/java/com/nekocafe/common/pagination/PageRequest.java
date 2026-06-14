package com.nekocafe.common.pagination;

public record PageRequest(long page, long size) {

    public PageRequest {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }
    }
}
