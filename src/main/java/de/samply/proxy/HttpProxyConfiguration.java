package de.samply.proxy;

import de.samply.app.ProjectManagerConst;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = ProjectManagerConst.HTTP_PROXY_PREFIX)
public class HttpProxyConfiguration extends ProxyConfiguration {

    public HttpProxyConfiguration() {
        this.setSchema(ProjectManagerConst.HTTP_PROTOCOL_SCHEMA);
    }

}
