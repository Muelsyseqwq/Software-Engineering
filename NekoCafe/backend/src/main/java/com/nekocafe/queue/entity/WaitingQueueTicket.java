package com.nekocafe.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("waiting_queue_ticket")
public class WaitingQueueTicket {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long userId;
    private LocalDate queueDate;
    private Integer queueNumber;
    private Integer resetVersion;
    private Integer partySize;
    private Long tableId;
    private String status;
    private String contactName;
    private String contactPhone;
    private LocalDateTime calledAt;
    private LocalDateTime seatedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getQueueDate() { return queueDate; }
    public void setQueueDate(LocalDate queueDate) { this.queueDate = queueDate; }
    public Integer getQueueNumber() { return queueNumber; }
    public void setQueueNumber(Integer queueNumber) { this.queueNumber = queueNumber; }
    public Integer getResetVersion() { return resetVersion; }
    public void setResetVersion(Integer resetVersion) { this.resetVersion = resetVersion; }
    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }
    public LocalDateTime getSeatedAt() { return seatedAt; }
    public void setSeatedAt(LocalDateTime seatedAt) { this.seatedAt = seatedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
