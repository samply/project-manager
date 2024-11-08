package de.samply.proxy;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
public class ProxyConfiguration {

    private String host;
    private Integer port;
    private String schema;
    private String username;
    private String password;
    private String url;

    // Comma-separated list of hosts or IPs to bypass the proxy
    private String noProxy;
    private String noProxyPattern;

    // Convenience method to get noProxy as a List
    public List<String> getNoProxyList() {
        return noProxy != null ? Arrays.asList(noProxy.split(",")) : List.of();
    }

    @PostConstruct
    public void init() throws MalformedURLException {
        if (StringUtils.hasText(this.url)) {
            URL url = new URL(this.url);
            this.schema = url.getProtocol();
            this.host = url.getHost();
            this.port = url.getPort();
            setProxyUserAndPassword(url.getUserInfo());
            if (this.noProxy != null) {
                this.noProxyPattern = createNonProxyPattern(this.noProxy);
            }
        }
        if (isConfigured()) {
            String schema = (this.schema != null) ? this.schema : "";
            log.info(schema + " Proxy configured:");
            log.info("\t-Host: " + this.host);
            log.info("\t-Port: " + this.port);
            if (username != null && password != null) {
                log.info("\t-Username: " + username);
            }
            if (noProxy != null) {
                log.info("\t-NoProxy: " + noProxy);
            }
        }
    }

    private void setProxyUserAndPassword(String userInfo) {
        if (userInfo != null) {
            String[] credentials = userInfo.split(":");
            if (credentials.length == 2) {
                this.username = credentials[0];
                this.password = credentials[1];
            }
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(this.host) && this.port != null;
    }

    // Method to create a regex pattern for non-proxy hosts
    private static String createNonProxyPattern(String nonProxyHosts) {
        // Split the comma-separated list into individual host patterns and build the regex
        return Arrays.stream(nonProxyHosts.split(","))
                .map(String::trim)                           // Trim whitespace
                .map(host -> host.replace(".", "\\.")        // Escape dots to literal
                        .replace("*", ".*"))      // Convert '*' to '.*' for wildcard matching
                .collect(Collectors.joining("|"));           // Join with '|' to match any pattern
    }


}
