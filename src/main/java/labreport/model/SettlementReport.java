package labreport.model;

import java.io.Serializable;

public class SettlementReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Integer doctorId; // Null for all doctors
    private String startDate;
    private String endDate;
    private int totalReportsCount = 0;
    private Double subtotal = 0.0; // Before tax
    private Double gstAmount = 0.0;
    private Double totalAmount = 0.0; // After tax
    private String paymentStatus = "Pending"; // Pending, Partial, Completed, Cancelled
    private String paymentDate;
    private String paymentMethod; // Cash, Check, Bank Transfer, etc.
    private String bankDetails; // Reference number or cheque details
    private String notes;
    private long createdAt;
    private long updatedAt;

    private Doctor doctor; // Related doctor object (null if for all doctors)

    // Constructors
    public SettlementReport() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getDoctorId() { return doctorId; }
    public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getTotalReportsCount() { return totalReportsCount; }
    public void setTotalReportsCount(int totalReportsCount) { this.totalReportsCount = totalReportsCount; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getGstAmount() { return gstAmount; }
    public void setGstAmount(Double gstAmount) { this.gstAmount = gstAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBankDetails() { return bankDetails; }
    public void setBankDetails(String bankDetails) { this.bankDetails = bankDetails; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    @Override
    public String toString() {
        return "SettlementReport{" +
                "id=" + id +
                ", doctorId=" + doctorId +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", totalAmount=" + totalAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
