package de.samply.batch;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The request of one entry of an actions batch, as its endpoint sees it: a GET without a body whose parameters are
 * the entry's parameters. Headers and session are those of the batch request.
 * <p>
 * Attributes are kept per entry: Spring stores request-scoped beans there, and every entry needs its own. Only the
 * lookups of the batch (projects and bridgeheads, {@link ActionsBatchLookups}) are the same for all its entries.
 */
public class ActionsBatchEntryRequest extends HttpServletRequestWrapper {

    private final HttpSession session;
    private final Map<String, String[]> parameters;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public ActionsBatchEntryRequest(HttpServletRequest batchRequest, HttpSession session, Map<String, String> parameters,
                                    ActionsBatchLookups lookups) {
        super(batchRequest);
        this.session = session;
        attributes.put(ActionsBatchLookups.REQUEST_ATTRIBUTE, lookups);
        Map<String, String[]> parameterValues = new java.util.LinkedHashMap<>();
        parameters.forEach((name, value) -> parameterValues.put(name, new String[]{value}));
        this.parameters = Collections.unmodifiableMap(parameterValues);
    }

    @Override
    public String getMethod() {
        return HttpMethod.GET.name();
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    // The session is fetched once by the batch, on the thread of the batch request
    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return session;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }

    // The body of the batch request belongs to the batch, not to its entries
    @Override
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return -1;
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new StringReader(""));
    }

}
