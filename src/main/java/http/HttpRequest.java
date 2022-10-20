package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IOUtils;

public class HttpRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
    private RequestLine requestLine;
    private HttpHeader header;
    private HttpParam param = new HttpParam();

    //HTTP Method, Url, 헤더, 본문
    public HttpRequest(InputStream is) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            this.requestLine = new RequestLine(br.readLine());
            this.header = new HttpHeader(br);
            param.addQueryParam(requestLine.getQueryString());
            int contentLength = header.getContentLength();
            String s = IOUtils.readData(br, contentLength);
            param.addBodyParam(s);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public HttpMethod getMethod() {
        return requestLine.getHttpMethod();
    }

    public String getPath() {
        return requestLine.getPath();
    }

    public String getHeader(String connection) {
        return header.getHeaderValue(connection);
    }

    public String getParameter(String paramKey) {
        return param.getParameter(paramKey);
    }
}
