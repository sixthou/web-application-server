package http;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLine {

    private static final Logger log = LoggerFactory.getLogger(RequestLine.class);

    private final HttpMethod httpMethod;
    private final String path;
    private final String queryString;

    public RequestLine(String requestLine) throws IOException {
        log.debug("RequestLine - {}", requestLine);
        String[] tokens = requestLine.split(" ");
        this.httpMethod = HttpMethod.valueOf(tokens[0]);
        int index = tokens[1].indexOf("?");
        if (index == -1) {
            path = tokens[1];
            queryString = "";
        } else {
            path = tokens[1].substring(0, index);
            queryString = tokens[1].substring(index + 1);
        }
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }
}
