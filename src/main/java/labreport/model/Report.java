package labreport.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int patientId;
    private String reportDate;
    private Integer referredByDoctorId; // Doctor who referred
    private String clinicalNotes;
    private String sampleCollectionDate;
    private String reportSubmittedDate;
    private String testingBy; // Lab technician name
    private String approvedBy; // Doctor's name who approved
    private String status = "Draft"; // Draft, Submitted, Approved, Printed
    private String filePath; // Path or filename for report PDF/HTML
    private long createdAt;
    private long updatedAt;

    private List<ReportTestResult> testResults = new ArrayList<>();
    private Patient patient; // Related patient object
    private Doctor referredDoctor; // Related doctor object

    // Constructors
    public Report() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public Integer getReferredByDoctorId() { return referredByDoctorId; }
    public void setReferredByDoctorId(Integer referredByDoctorId) { this.referredByDoctorId = referredByDoctorId; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public String getSampleCollectionDate() { return sampleCollectionDate; }
    public void setSampleCollectionDate(String sampleCollectionDate) { this.sampleCollectionDate = sampleCollectionDate; }

    public String getReportSubmittedDate() { return reportSubmittedDate; }
    public void setReportSubmittedDate(String reportSubmittedDate) { this.reportSubmittedDate = reportSubmittedDate; }

    public String getTestingBy() { return testingBy; }
    public void setTestingBy(String testingBy) { this.testingBy = testingBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<ReportTestResult> getTestResults() { return testResults; }
    public void setTestResults(List<ReportTestResult> testResults) { this.testResults = testResults; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getReferredDoctor() { return referredDoctor; }
    public void setReferredDoctor(Doctor referredDoctor) { this.referredDoctor = referredDoctor; }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", reportDate='" + reportDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
