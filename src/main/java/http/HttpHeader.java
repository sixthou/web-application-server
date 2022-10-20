package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpHeader {
    private static final Logger log = LoggerFactory.getLogger(HttpHeader.class);
    private Map<String, String> header = new HashMap<>();

    public HttpHeader(BufferedReader br) throws IOException {
        String line;
        while (!(line = br.readLine()).equals("")) {
            log.debug("HttpHeader - {}", line);
            this.add(line);
        }
    }

    private void add(String headerLine) {
        String[] token = headerLine.split(":");
        header.put(token[0].trim(), token[1].trim());
    }

    public String getHeaderValue(String connection) {
        return header.get(connection);
    }

    public int getContentLength() {
        return header.get("Content-Length") != null ? Integer.parseInt(header.get("Content-Length")) : 0;
    }
}
