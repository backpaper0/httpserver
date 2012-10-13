package httpserver;

import java.io.InputStream;
import java.util.Map;

public class HttpRequest {

    private String[] requestLine;

    private Map<String, String> requestHeader;

    private InputStream requestEntity;

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

    public InputStream getRequestEntity() {
        return requestEntity;
    }

    public void setRequestEntity(InputStream requestEntity) {
        this.requestEntity = requestEntity;
    }

}
