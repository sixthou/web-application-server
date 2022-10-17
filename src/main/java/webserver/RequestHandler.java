package webserver;

import static util.HttpRequestUtils.parseQueryString;

import db.DataBase;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.HttpRequestUtils.Pair;
import util.IOUtils;

public class RequestHandler extends Thread {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            int responseCode = 200;
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

            //요청 전문
            String requestHeader = getRequestHeader(bufferedReader);
            String customHeader = "";
            String url = getUrl(requestHeader);
            String requestPath = hasQueryParams(url) ? url.substring(0, url.indexOf("?")) : url;
            String requestMethod = getRequestMethod(requestHeader);
            List<Pair> requestHeaderValue = getRequestHeaderValue(requestHeader);
            Map<String, String> queryParamMap = requestMethod.equals("GET") && hasQueryParams(url) ?
                    parseQueryString(url.substring(url.indexOf("?") + 1)) : null;
            String requestBody = requestMethod.equals("POST") ?
                    IOUtils.readData(bufferedReader, getContentLength(requestHeaderValue)) : "";
            Map<String, String> cookie = HttpRequestUtils.parseCookies(requestHeaderValue.stream()
                    .filter(p -> p.getKey().equals("Cookie"))
                    .map(Pair::getValue)
                    .findFirst()
                    .orElse(""));

            Map<String, String> bodyParamMap = parseQueryString(requestBody);

            log.debug("requestBody -{}", requestBody);

            if (requestMethod.equals("GET") && requestPath.equals("/user/list")) {

                boolean logined = "true".equals(cookie.get("logined"));

                if (logined) {
                    Collection<User> allUsers = DataBase.findAll();

                } else {
                    customHeader = "Location: /index.html\n\r\n";
                    responseCode = 302;
                }
            }

            if (requestMethod.equals("POST") && requestPath.equals("/user/create")) {
                DataBase.addUser(mapToUser(bodyParamMap));
                customHeader = "Location: /index.html\n\r\n";
                responseCode = 302;
            }

            if (requestMethod.equals("POST") && requestPath.equals("/user/login")) {
                String userId = bodyParamMap.get("userId");
                String password = bodyParamMap.get("password");
                Optional<User> user = Optional.ofNullable(DataBase.findUserById(userId));

                if (user.isPresent() && user.get().getPassword().equals(password)) {
                    customHeader = "Set-Cookie: logined=true;\r\n" + "Location: /index.html\n\r\n";
                    responseCode = 302;
                } else {
                    customHeader = "Set-Cookie: logined=false;\r\n" + "Location: /user/login_failed.html\n\r\n";
                    responseCode = 302;
                }

            }

            byte[] body = getHtmlFileFromUrlToByte(requestPath);

            responseCodeHeader(dos, body.length, responseCode, customHeader, requestPath);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private int getContentLength(List<Pair> requestHeaderValue) {
        return Integer.parseInt(requestHeaderValue.stream()
                .filter(p -> p.getKey().equals("Content-Length"))
                .findFirst()
                .map(Pair::getValue)
                .orElse(""));
    }

    private boolean hasQueryParams(String url) {
        return url.contains("?");
    }

    private User mapToUser(Map<String, String> userMap) {
        User user = new User(userMap.get("userId"), userMap.get("password"), userMap.get("name"),
                userMap.get("email"));
        log.debug(user.toString());
        return user;
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

    private String getRequestMethod(String requestHeader) {
        String[] tokens = requestHeader.split("\n")[0].split(" ");
        return tokens[0];
    }

    private String getUrl(String request) {
        String[] tokens = request.split("\n")[0].split(" ");
        return tokens[1].equals("") || tokens[1].equals("/") ? "" : tokens[1];
    }


    private List<Pair> getRequestHeaderValue(String requestHeader) {
        String[] tokens = requestHeader.split("\n");
        List<Pair> pairs = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            pairs.add(HttpRequestUtils.parseHeader(tokens[i]));
        }
        return pairs;
    }

    private String getRequestHeader(BufferedReader bufferedReader) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.setLength(0);
        try {
            String line = bufferedReader.readLine();

            while (!"".equals(line)) {
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        log.debug("getRequestHeader - {}", sb);
        return sb.toString();
    }

    private void responseCodeHeader(DataOutputStream dos, int lengthOfBodyContent, int responseCode,
                                    String customHeader, String requestUrl) {
        try {
            String type;
            if (requestUrl.contains("/css")) {
                type = "css";
            } else {
                type = "html";
            }

            if (responseCode == 200) {
                dos.writeBytes("HTTP/1.1 200 OK \r\n");
                dos.writeBytes("Content-Type: text/" + type + ";charset=utf-8\r\n");
                dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
                dos.writeBytes(customHeader);
                dos.writeBytes("\r\n");
            }
            if (responseCode == 302) {
                dos.writeBytes("HTTP/1.1 302 Found\r\n");
                dos.writeBytes(customHeader);
                dos.writeBytes("\r\n");
            }
            if (responseCode == 201) {
                dos.writeBytes("HTTP/1.1 201 Created\r\n");
                dos.writeBytes(customHeader);
                dos.writeBytes("\r\n");
            }
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
