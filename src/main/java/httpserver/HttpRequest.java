package httpserver;

import java.util.Map;

public class HttpRequest {

    private String[] requestLine;

    private Map<String, String> requestHeader;

    private byte[] requestEntity;

    public String[] getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(String[] requestLine) {
        this.requestLine = requestLine;
    }

    public Map<String, String> getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(Map<String, String> requestHeader) {
        this.requestHeader = requestHeader;
    }

    public byte[] getRequestEntity() {
        return requestEntity;
    }

    public void setRequestEntity(byte[] requestEntity) {
        this.requestEntity = requestEntity;
    }

}
