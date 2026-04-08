package labreport.model;

import java.io.Serializable;

public class ReportTestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int reportId;
    private int testId;
    private Integer subTestId; // Null if test has no sub-tests
    private String testName;
    private String subTestName;
    private String resultValue;
    private String unit;
    private Double normalRangeMin;
    private Double normalRangeMax;
    private Boolean isAbnormal = false;
    private String notes;
    private long createdAt;
    private long updatedAt;

    private Test test; // Related test object
    private SubTest subTest; // Related sub-test object

    // Constructors
    public ReportTestResult() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public Integer getSubTestId() { return subTestId; }
    public void setSubTestId(Integer subTestId) { this.subTestId = subTestId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getSubTestName() { return subTestName; }
    public void setSubTestName(String subTestName) { this.subTestName = subTestName; }

    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getNormalRangeMin() { return normalRangeMin; }
    public void setNormalRangeMin(Double normalRangeMin) { this.normalRangeMin = normalRangeMin; }

    public Double getNormalRangeMax() { return normalRangeMax; }
    public void setNormalRangeMax(Double normalRangeMax) { this.normalRangeMax = normalRangeMax; }

    public Boolean getIsAbnormal() { return isAbnormal; }
    public void setIsAbnormal(Boolean isAbnormal) { this.isAbnormal = isAbnormal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }

    public SubTest getSubTest() { return subTest; }
    public void setSubTest(SubTest subTest) { this.subTest = subTest; }

    @Override
    public String toString() {
        return "ReportTestResult{" +
                "id=" + id +
                ", reportId=" + reportId +
                ", testName='" + testName + '\'' +
                ", resultValue='" + resultValue + '\'' +
                ", isAbnormal=" + isAbnormal +
                '}';
    }
}
