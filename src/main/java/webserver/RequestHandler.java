package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    public static final StringBuffer STRING_BUFFER = new StringBuffer();
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

            //요청 전문
            String request = requestReader(bufferedReader);

            //url
            String url = getUrl(request);

            String requestPath = url;

            if (hasQueryParams(url.indexOf("?"))) {
                requestPath = url.substring(0, url.indexOf("?"));
                Map<String, String> queryParamMap = HttpRequestUtils.parseQueryString(
                        url.substring(url.indexOf("?") + 1));

                if (requestPath.equals("/user/create")) {
                    User user = mapToUser(queryParamMap);
                    log.info("/user/create - {}", user);
                }
            }

            byte[] body = getHtmlFileFromUrlToByte(requestPath);

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean hasQueryParams(int index) {
        return index != -1;
    }

    private User mapToUser(Map<String, String> userMap) {
        return new User(userMap.get("userId"), userMap.get("password"), userMap.get("name"),
                userMap.get("email"));
    }

    private byte[] getHtmlFileFromUrlToByte(String url) throws IOException {
        byte[] body;
        try {
            body = Files.readAllBytes(new File("./webapp" + url).toPath());
        } catch (IOException e) {
            body = "hello world".getBytes(StandardCharsets.UTF_8);
        }

        return body;
    }

    private String getUrl(String request) {
        String[] tokens = request.split(" ");
        return tokens[1].equals("") || tokens[1].equals("/") ? "" : tokens[1];
    }

    private String requestReader(BufferedReader bufferedReader) throws IOException {
        STRING_BUFFER.setLength(0);
        try {
            String line = bufferedReader.readLine();

            while (!"".equals(line)) {
                if (line == null) {
                    break;
                }
                STRING_BUFFER.append(line).append("\n");
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return STRING_BUFFER.toString();
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
