package com.nekocafe.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_store_role")
public class UserStoreRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long storeId;
    private String roleCode;
    private String status;
    private Long createdBy;
    private Long dismissedBy;
    private LocalDateTime dismissedAt;
    private String dismissReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getDismissedBy() { return dismissedBy; }
    public void setDismissedBy(Long dismissedBy) { this.dismissedBy = dismissedBy; }
    public LocalDateTime getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(LocalDateTime dismissedAt) { this.dismissedAt = dismissedAt; }
    public String getDismissReason() { return dismissReason; }
    public void setDismissReason(String dismissReason) { this.dismissReason = dismissReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
