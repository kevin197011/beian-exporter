package io.devops.beian.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 备案信息实体类
 */
public class BeianInfo {
    
    @JsonProperty("序号")
    private String serialNumber;
    
    @JsonProperty("主办单位名称")
    private String companyName;
    
    @JsonProperty("主办单位性质")
    private String companyType;
    
    @JsonProperty("网站备案号")
    private String beianNumber;
    
    @JsonProperty("网站名称")
    private String websiteName;
    
    @JsonProperty("网站首页地址")
    private String websiteUrl;
    
    @JsonProperty("审核日期")
    private String approvalDate;

    // 构造函数
    public BeianInfo() {}

    public BeianInfo(String serialNumber, String companyName, String companyType, 
                     String beianNumber, String websiteName, String websiteUrl, String approvalDate) {
        this.serialNumber = serialNumber;
        this.companyName = companyName;
        this.companyType = companyType;
        this.beianNumber = beianNumber;
        this.websiteName = websiteName;
        this.websiteUrl = websiteUrl;
        this.approvalDate = approvalDate;
    }

    // Getters and Setters
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getBeianNumber() {
        return beianNumber;
    }

    public void setBeianNumber(String beianNumber) {
        this.beianNumber = beianNumber;
    }

    public String getWebsiteName() {
        return websiteName;
    }

    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(String approvalDate) {
        this.approvalDate = approvalDate;
    }

    @Override
    public String toString() {
        return "BeianInfo{" +
                "serialNumber='" + serialNumber + '\'' +
                ", companyName='" + companyName + '\'' +
                ", companyType='" + companyType + '\'' +
                ", beianNumber='" + beianNumber + '\'' +
                ", websiteName='" + websiteName + '\'' +
                ", websiteUrl='" + websiteUrl + '\'' +
                ", approvalDate='" + approvalDate + '\'' +
                '}';
    }
}