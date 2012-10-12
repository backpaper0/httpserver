package httpserver;

import java.io.IOException;

public interface HttpResponseWriter {

    void write(HttpResponse response) throws IOException;

}