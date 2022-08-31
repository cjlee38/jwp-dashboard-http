package org.apache.coyote.http11.request;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class RequestHeaders {

    private static final Pattern HEADER_PATTERN = Pattern.compile("(?<field>[a-zA-Z\\- ]+): ?(?<value>.+)");

    private final Map<String, String> headers;

    public RequestHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public static RequestHeaders parse(List<String> lines) {
        return new RequestHeaders(lines.stream()
                .map(HEADER_PATTERN::matcher)
                .filter(Matcher::find)
                .collect(Collectors.toMap(
                        matcher -> matcher.group("field"),
                        matcher -> matcher.group("value")
                )));
    }

    public String getPairByKey(String key) {
        return key + ": " + headers.get(key);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "RequestHeaders{" +
                "headers=" + headers +
                '}';
    }
}