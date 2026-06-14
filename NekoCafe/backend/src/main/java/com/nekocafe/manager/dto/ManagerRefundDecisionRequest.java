package com.nekocafe.manager.dto;

public record ManagerRefundDecisionRequest(
    String decision,
    String remark
) {
}
