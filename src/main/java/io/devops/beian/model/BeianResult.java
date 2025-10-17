package io.devops.beian.model;

import java.util.List;

/**
 * 备案查询结果
 */
public class BeianResult {
    
    public enum Status {
        SUCCESS("success"),
        NOT_FOUND("not_found"),
        PARSE_ERROR("parse_error"),
        ERROR("error");
        
        private final String value;
        
        Status(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    private Status status;
    private String message;
    private List<BeianInfo> data;
    private String error;

    // 构造函数
    public BeianResult() {}

    public BeianResult(Status status) {
        this.status = status;
    }

    public BeianResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public BeianResult(Status status, List<BeianInfo> data) {
        this.status = status;
        this.data = data;
    }

    // 静态工厂方法
    public static BeianResult success(List<BeianInfo> data) {
        return new BeianResult(Status.SUCCESS, data);
    }

    public static BeianResult notFound(String message) {
        return new BeianResult(Status.NOT_FOUND, message);
    }

    public static BeianResult parseError(String message) {
        return new BeianResult(Status.PARSE_ERROR, message);
    }

    public static BeianResult error(String error) {
        BeianResult result = new BeianResult(Status.ERROR);
        result.setError(error);
        return result;
    }

    // Getters and Setters
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<BeianInfo> getData() {
        return data;
    }

    public void setData(List<BeianInfo> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS && data != null && !data.isEmpty();
    }

    @Override
    public String toString() {
        return "BeianResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", error='" + error + '\'' +
                '}';
    }
}