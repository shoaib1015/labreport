package labreport.model;

import java.io.Serializable;
import java.util.List;

public class Test implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String testName;
    private Integer categoryId; // Can be null (no category)
    private Double basePrice;
    private String unit;
    private String description;
    private boolean boldFormat;
    private boolean borderFormat;
    private boolean highlightFormat;
    private Double commissionPercentage; // Commission percentage override for this test
    private String status = "Active";
    private long createdAt;
    private long updatedAt;
    
    // Related data
    private TestCategory category;
    private List<SubTest> subTests;

    // Constructors
    public Test() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isBoldFormat() { return boldFormat; }
    public void setBoldFormat(boolean boldFormat) { this.boldFormat = boldFormat; }

    public boolean isBorderFormat() { return borderFormat; }
    public void setBorderFormat(boolean borderFormat) { this.borderFormat = borderFormat; }

    public boolean isHighlightFormat() { return highlightFormat; }
    public void setHighlightFormat(boolean highlightFormat) { this.highlightFormat = highlightFormat; }

    public Double getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(Double commissionPercentage) { this.commissionPercentage = commissionPercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public TestCategory getCategory() { return category; }
    public void setCategory(TestCategory category) { this.category = category; }

    public List<SubTest> getSubTests() { return subTests; }
    public void setSubTests(List<SubTest> subTests) { this.subTests = subTests; }

    @Override
    public String toString() {
        return "Test{" +
                "id=" + id +
                ", testName='" + testName + '\'' +
                ", categoryId=" + categoryId +
                '}';
    }
}
