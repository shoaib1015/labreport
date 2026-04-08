package labreport.model;

import java.io.Serializable;

public class LabProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id = 1; // Single lab profile per installation
    private String labName;
    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String website;
    private String operatingHours;
    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    private String franchiseType;
    private String phones;
    private byte[] logoData; // Store logo as BLOB
    private String reportHeader;
    private String reportFooter;
    private String reportDisclaimer;
    private String gstNumber;
    private Double gstRate;
    private boolean enableGst;
    private String currency = "INR";
    private long createdAt;
    private long updatedAt;

    // Constructors
    public LabProfile() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }

    public String getSecondaryPhone() { return secondaryPhone; }
    public void setSecondaryPhone(String secondaryPhone) { this.secondaryPhone = secondaryPhone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getFranchiseType() { return franchiseType; }
    public void setFranchiseType(String franchiseType) { this.franchiseType = franchiseType; }

    public String getPhones() { return phones; }
    public void setPhones(String phones) { this.phones = phones; }

    public byte[] getLogoData() { return logoData; }
    public void setLogoData(byte[] logoData) { this.logoData = logoData; }

    public String getReportHeader() { return reportHeader; }
    public void setReportHeader(String reportHeader) { this.reportHeader = reportHeader; }

    public String getReportFooter() { return reportFooter; }
    public void setReportFooter(String reportFooter) { this.reportFooter = reportFooter; }

    public String getReportDisclaimer() { return reportDisclaimer; }
    public void setReportDisclaimer(String reportDisclaimer) { this.reportDisclaimer = reportDisclaimer; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public Double getGstRate() { return gstRate; }
    public void setGstRate(Double gstRate) { this.gstRate = gstRate; }

    public boolean isEnableGst() { return enableGst; }
    public void setEnableGst(boolean enableGst) { this.enableGst = enableGst; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
