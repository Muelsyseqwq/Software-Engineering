package com.nekocafe.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("waiting_queue_counter")
public class WaitingQueueCounter {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private LocalDate queueDate;
    private Integer currentNumber;
    private Integer nextNumber;
    private Integer resetVersion;
    private Long resetBy;
    private LocalDateTime resetAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public LocalDate getQueueDate() { return queueDate; }
    public void setQueueDate(LocalDate queueDate) { this.queueDate = queueDate; }
    public Integer getCurrentNumber() { return currentNumber; }
    public void setCurrentNumber(Integer currentNumber) { this.currentNumber = currentNumber; }
    public Integer getNextNumber() { return nextNumber; }
    public void setNextNumber(Integer nextNumber) { this.nextNumber = nextNumber; }
    public Integer getResetVersion() { return resetVersion; }
    public void setResetVersion(Integer resetVersion) { this.resetVersion = resetVersion; }
    public Long getResetBy() { return resetBy; }
    public void setResetBy(Long resetBy) { this.resetBy = resetBy; }
    public LocalDateTime getResetAt() { return resetAt; }
    public void setResetAt(LocalDateTime resetAt) { this.resetAt = resetAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
