package labreport.model;

import java.io.Serializable;

public class DoctorCommission implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int doctorId;
    private Integer testId; // Null means use default commission
    private Integer subTestId; // Null if commission is for entire test
    private Double commissionRate; // Commission percentage (0-100)
    private DoctorCommissionType commissionType = DoctorCommissionType.PERCENTAGE; // Percentage or Fixed Amount
    private Double fixedAmount; // Used if commissionType is FIXED_AMOUNT
    private String status = "Active";
    private long createdAt;
    private long updatedAt;

    private Doctor doctor; // Related doctor object
    private Test test; // Related test object
    private SubTest subTest; // Related sub-test object

    // Enum for commission type
    public enum DoctorCommissionType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    // Constructors
    public DoctorCommission() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public Integer getTestId() { return testId; }
    public void setTestId(Integer testId) { this.testId = testId; }

    public Integer getSubTestId() { return subTestId; }
    public void setSubTestId(Integer subTestId) { this.subTestId = subTestId; }

    public Double getCommissionRate() { return commissionRate; }
    public void setCommissionRate(Double commissionRate) { this.commissionRate = commissionRate; }

    public DoctorCommissionType getCommissionType() { return commissionType; }
    public void setCommissionType(DoctorCommissionType commissionType) { this.commissionType = commissionType; }

    public Double getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(Double fixedAmount) { this.fixedAmount = fixedAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }

    public SubTest getSubTest() { return subTest; }
    public void setSubTest(SubTest subTest) { this.subTest = subTest; }

    @Override
    public String toString() {
        return "DoctorCommission{" +
                "id=" + id +
                ", doctorId=" + doctorId +
                ", testId=" + testId +
                ", commissionRate=" + commissionRate +
                ", commissionType=" + commissionType +
                ", status='" + status + '\'' +
                '}';
    }
}
