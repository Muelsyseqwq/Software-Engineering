package com.nekocafe.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String preferenceType;
    private String preferenceValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPreferenceType() { return preferenceType; }
    public void setPreferenceType(String preferenceType) { this.preferenceType = preferenceType; }
    public String getPreferenceValue() { return preferenceValue; }
    public void setPreferenceValue(String preferenceValue) { this.preferenceValue = preferenceValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
