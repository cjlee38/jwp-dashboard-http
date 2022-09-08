package org.apache.coyote.http11.request;

import static org.apache.coyote.http11.context.HttpCookie.SESSION_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.coyote.http11.context.Context;
import org.apache.coyote.http11.context.HttpCookie;
import org.apache.coyote.http11.request.headers.RequestHeader;
import org.apache.coyote.http11.context.Session;
import org.apache.coyote.http11.context.SessionManager;
import org.apache.exception.TempException;
import org.apache.util.NumberUtil;
import org.apache.util.StringUtil;

public class HttpRequest {

    private static final SessionManager MANAGER = new SessionManager();

    private final RequestGeneral requestGeneral;
    private final RequestHeaders requestHeaders;
    private final RequestBody requestBody;
    private final Context context;

    private HttpRequest(RequestGeneral requestGeneral, RequestHeaders requestHeaders, RequestBody requestBody,
                       Context context) {
        this.requestGeneral = requestGeneral;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.context = context;
    }

    public static HttpRequest parse(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        RequestGeneral general = readGeneralLine(reader);
        RequestHeaders headers = readHeaderLines(reader);
        RequestBody body = readBodyLines(reader, headers);
        Context context = parseOrCreateContext(headers);

        return new HttpRequest(general, headers, body, context);
    }

    private static RequestGeneral readGeneralLine(BufferedReader reader) {
        return RequestGeneral.parse(StringUtil.readOneLine(reader));
    }

    private static RequestHeaders readHeaderLines(BufferedReader reader) {
        List<String> lines = new ArrayList<>();
        String line;
        while (!"".equals(line = StringUtil.readOneLine(reader))) {
            lines.add(line);
        }
        return RequestHeaders.parse(lines);
    }

    private static RequestBody readBodyLines(BufferedReader reader, RequestHeaders headers) {
        int length = findContentLength(headers);
        if (length == 0) {
            return RequestBody.empty();
        }
        return readActualBody(reader, length);
    }

    private static int findContentLength(RequestHeaders headers) {
        try {
            RequestHeader header = headers.findHeader("Content-Length");
            return NumberUtil.parseIntSafe(header.getValue());
        } catch (IllegalArgumentException | NullPointerException e) {
            return 0;
        }
    }

    private static RequestBody readActualBody(BufferedReader reader, int length) {
        char[] buffer = new char[length];
        try {
            reader.read(buffer, 0, length);
            return new RequestBody(new String(buffer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Context parseOrCreateContext(RequestHeaders headers) {
        RequestHeader cookieHeader = headers.findHeader("Cookie");
        if (cookieHeader == null) {
            return Context.createNew();
        }
        HttpCookie cookie = HttpCookie.parse(cookieHeader.getValue());
        Session session = findOrCreateSession(cookie.find(SESSION_NAME));

        return new Context(session, cookie);
    }

    private static Session findOrCreateSession(String uuid) {
        Session findSession = MANAGER.findSession(uuid);
        if (findSession != null) {
            return findSession;
        }
        Session createSession = new Session(UUID.randomUUID().toString());
        MANAGER.add(createSession);
        return createSession;
    }

    public Session getSession(boolean isCreate) {
        if (!isCreate && MANAGER.findSession(context.getSession().getId()) == null) {
            return null;
        }
        return findOrCreateSession(context.getSession().getId());
    }

    public String getPath() {
        return requestGeneral.getPath().getPath();
    }

    public RequestMethod getMethod() {
        return requestGeneral.getMethod();
    }

    public String getRequestBody() {
        return requestBody.getBody();
    }

    public Context getContext() {
        return context;
    }

    public HttpCookie getCookie() {
        return context.getCookie();
    }

    @Override
    public String toString() {
        return "HttpRequest{\n" +
                "requestGeneral=" + requestGeneral +
                ", requestHeaders=" + requestHeaders +
                ", requestBody=" + requestBody +
                "\n}";
    }
}
