package com.nekocafe.manager.dto;

public record ActivityDecisionRequest(
    String acceptStatus,
    String remark
) {
}
