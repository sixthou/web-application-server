package http;

import java.util.HashMap;
import java.util.Map;
import util.HttpRequestUtils;

public class HttpParam {
    Map<String, String> param = new HashMap<>();

    public HttpParam() {
    }

    public void addQueryParam(String queryParam) {
        param.putAll(HttpRequestUtils.parseQueryString(queryParam));
    }

    public void addBodyParam(String bodyParam) {
        param.putAll(HttpRequestUtils.parseQueryString(bodyParam));
    }

    public String getParameter(String param) {
        return this.param.get(param);
    }
}
