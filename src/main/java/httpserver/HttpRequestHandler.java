package httpserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class HttpRequestHandler {

    private final Path webappDir;

    private final Map<String, String> contentTypes;

    public HttpRequestHandler(Path webappDir) {
        this.webappDir = webappDir;
        this.contentTypes = new HashMap<>();

        this.contentTypes.put(".txt", "text/plain");
        this.contentTypes.put(".json", "application/json");
    }

    public HttpResponse handleRequest(OutputStream responseStream,
            String[] requestLine, Map<String, String> requestHeader,
            byte[] requestEntity) throws IOException,
            UnsupportedEncodingException {

        //ちょくちょく使うのでインスタンス化しとく
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        if (requestLine[0].equals("GET") == false
            && requestLine[0].equals("POST") == false) {
            //今回はGETリクエスト、POSTリクエスト以外は扱わない
            System.out.println(requestLine[0] + "は扱えないメソッド");

            HttpResponse response = new HttpResponse();
            response.setHttpVersion("HTTP/1.0");
            response.setStatusCode(501);
            response.setReasonPhase("Not Implemented");
            response.getMessageHeader().put(
                "Content-Type",
                "text/plain; charset=UTF-8");
            response.setMessageBody(new ByteArrayInputStream(
                "501 Not Implemented".getBytes("UTF-8")));
            return response;
        }

        if (requestLine[0].equals("POST")) {
            //POSTリクエストは取りあえずリクエストエンティティを
            //そのまま返す実装にしておく
            String contentType = requestHeader.get("content-type");

            /* 
             * レスポンスを書く 
             */
            HttpResponse response = new HttpResponse();
            response.setHttpVersion("HTTP/1.0");
            response.setStatusCode(200);
            response.setReasonPhase("OK");
            response.getMessageHeader().put("Content-Type", contentType);
            response.setMessageBody(new ByteArrayInputStream(requestEntity));
            return response;
        }

        /*
         * リクエストを解析（というほどのことはしていないが）
         */
        //Request-URI は abs_path であることを前提にしておく
        Path requestPath = webappDir.resolve(requestLine[1].substring(1));

        if (Files.notExists(requestPath)) {
            //ファイルがなければ404
            System.out.println(requestPath + "が見つからない");

            HttpResponse response = new HttpResponse();
            response.setHttpVersion("HTTP/1.0");
            response.setStatusCode(404);
            response.setReasonPhase("Not Found");
            response.getMessageHeader().put(
                "Content-Type",
                "text/plain; charset=UTF-8");
            response.setMessageBody(new ByteArrayInputStream("404 Not Found"
                .getBytes("UTF-8")));
            return response;
        }

        /*
         * レスポンスの準備
         */
        String lastModified =
            df.format(new Date(Files
                .getLastModifiedTime(requestPath)
                .toMillis()));

        byte[] fileContent = Files.readAllBytes(requestPath);

        String contentType = null;
        String fileName = requestPath.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index > -1) {
            String extension = fileName.substring(index);
            contentType = contentTypes.get(extension);
        }
        //Content-Typeが不明なファイルはoctet-streamにしちゃう
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        /* 
         * レスポンスを書く 
         */
        HttpResponse response = new HttpResponse();
        response.setHttpVersion("HTTP/1.0");
        response.setStatusCode(200);
        response.setReasonPhase("OK");
        response.getMessageHeader().put(
            "Content-Type",
            contentType + "; charset=UTF-8");
        response.getMessageHeader().put("Last-Modified", lastModified);
        response.setMessageBody(new ByteArrayInputStream(fileContent));
        return response;
    }

}
