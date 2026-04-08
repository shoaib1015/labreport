package labreport.model;

import java.io.Serializable;

public class SubTest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int testId;
    private String subTestName;
    private String unit;
    private Double normalRangeMin;
    private Double normalRangeMax;
    private String ageRanges; // Age/gender specific ranges as JSON or delimited string
    private Double price; // Individual price for sub-test
    private String instructions; // Instructions for this sub-test (optional)
    private boolean printInstructions; // Whether to print instructions in report
    private int displayOrder;
    private String status = "Active";
    private long createdAt;
    private long updatedAt;

    // Constructors
    public SubTest() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public String getSubTestName() { return subTestName; }
    public void setSubTestName(String subTestName) { this.subTestName = subTestName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getNormalRangeMin() { return normalRangeMin; }
    public void setNormalRangeMin(Double normalRangeMin) { this.normalRangeMin = normalRangeMin; }

    public Double getNormalRangeMax() { return normalRangeMax; }
    public void setNormalRangeMax(Double normalRangeMax) { this.normalRangeMax = normalRangeMax; }

    public String getAgeRanges() { return ageRanges; }
    public void setAgeRanges(String ageRanges) { this.ageRanges = ageRanges; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isPrintInstructions() { return printInstructions; }
    public void setPrintInstructions(boolean printInstructions) { this.printInstructions = printInstructions; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "SubTest{" +
                "id=" + id +
                ", testId=" + testId +
                ", subTestName='" + subTestName + '\'' +
                ", price=" + price +
                '}';
    }
}
