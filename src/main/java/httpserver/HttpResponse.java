package httpserver;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

    private String httpVersion;

    private int statusCode;

    private String reasonPhase;

    private Map<String, Object> messageHeader = new LinkedHashMap<>();;

    private InputStream messageBody;

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getReasonPhase() {
        return reasonPhase;
    }

    public void setReasonPhase(String reasonPhase) {
        this.reasonPhase = reasonPhase;
    }

    public Map<String, Object> getMessageHeader() {
        return messageHeader;
    }

    public void setMessageHeader(Map<String, Object> messageHeader) {
        this.messageHeader = messageHeader;
    }

    public InputStream getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(InputStream messageBody) {
        this.messageBody = messageBody;
    }

}
