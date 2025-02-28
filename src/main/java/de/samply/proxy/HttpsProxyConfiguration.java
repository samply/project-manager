package de.samply.proxy;

import de.samply.app.ProjectManagerConst;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = ProjectManagerConst.HTTPS_PROXY_PREFIX)
public class HttpsProxyConfiguration extends ProxyConfiguration {

    public HttpsProxyConfiguration() {
        this.setSchema(ProjectManagerConst.HTTPS_PROTOCOL_SCHEMA);
    }

}
