package com.nekocafe.staff.dto;

public class StaffReviewRow {
    private Long id;
    private String customerName;
    private String orderNo;
    private Integer rating;
    private String content;
    private String status;
    private String createdAt;

    public StaffReviewRow(Long id, String customerName, String orderNo,
                          Integer rating, String content, String status, String createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.orderNo = orderNo;
        this.rating = rating;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
