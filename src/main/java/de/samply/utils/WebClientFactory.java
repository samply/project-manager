package de.samply.utils;

import de.samply.app.ProjectManagerConst;
import de.samply.proxy.HttpProxyConfiguration;
import de.samply.proxy.HttpsProxyConfiguration;
import de.samply.proxy.ProxyConfiguration;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.util.Optional;

@Component
public class WebClientFactory {

    private final int webClientMaxNumberOfRetries;
    private final int webClientTimeInSecondsAfterRetryWithFailure;
    private final int webClientRequestTimeoutInSeconds;
    private final int webClientConnectionTimeoutInSeconds;
    private final int webClientTcpKeepIdleInSeconds;
    private final int webClientTcpKeepIntervalInSeconds;
    private final int webClientTcpKeepConnetionNumberOfTries;
    private final int webClientBufferSizeInBytes;
    private Optional<ProxyConfiguration> httpProxyConfiguration = Optional.empty();
    private Optional<ProxyConfiguration> httpsProxyConfiguration = Optional.empty();

    public WebClientFactory(
            @Value(ProjectManagerConst.WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS_SV) Integer webClientRequestTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS_SV) Integer webClientConnectionTimeoutInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS_SV) Integer webClientTcpKeepIdleInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS_SV) Integer webClientTcpKeepIntervalInSeconds,
            @Value(ProjectManagerConst.WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES_SV) Integer webClientTcpKeepConnetionNumberOfTries,
            @Value(ProjectManagerConst.WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV) Integer webClientMaxNumberOfRetries,
            @Value(ProjectManagerConst.WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV) Integer webClientTimeInSecondsAfterRetryWithFailure,
            @Value(ProjectManagerConst.WEBCLIENT_BUFFER_SIZE_IN_BYTES_SV) Integer webClientBufferSizeInBytes,
            HttpProxyConfiguration httpProxyConfiguration,
            HttpsProxyConfiguration httpsProxyConfiguration
    ) {
        this.webClientMaxNumberOfRetries = webClientMaxNumberOfRetries;
        this.webClientTimeInSecondsAfterRetryWithFailure = webClientTimeInSecondsAfterRetryWithFailure;
        this.webClientRequestTimeoutInSeconds = webClientRequestTimeoutInSeconds;
        this.webClientConnectionTimeoutInSeconds = webClientConnectionTimeoutInSeconds;
        this.webClientTcpKeepIdleInSeconds = webClientTcpKeepIdleInSeconds;
        this.webClientTcpKeepIntervalInSeconds = webClientTcpKeepIntervalInSeconds;
        this.webClientTcpKeepConnetionNumberOfTries = webClientTcpKeepConnetionNumberOfTries;
        this.webClientBufferSizeInBytes = webClientBufferSizeInBytes;

        setHttpProxies(httpProxyConfiguration, httpsProxyConfiguration);
    }

    private void setHttpProxies(HttpProxyConfiguration httpProxyConfiguration, HttpsProxyConfiguration httpsProxyConfiguration) {
        if (httpProxyConfiguration.isConfigured()) {
            this.httpProxyConfiguration = Optional.of(httpProxyConfiguration);
        }
        if (httpsProxyConfiguration.isConfigured()) {
            this.httpsProxyConfiguration = Optional.of(httpsProxyConfiguration);
        }
    }

    public WebClient createWebClient(String baseUrl) {
        WebClient.Builder webClientBuilder = WebClient.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(webClientBufferSizeInBytes))
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(webClientRequestTimeoutInSeconds))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConnectionTimeoutInSeconds * 1000)
                                .option(ChannelOption.SO_KEEPALIVE, true)
                                .option(EpollChannelOption.TCP_KEEPIDLE, webClientTcpKeepIdleInSeconds)
                                .option(EpollChannelOption.TCP_KEEPINTVL, webClientTcpKeepIntervalInSeconds)
                                .option(EpollChannelOption.TCP_KEEPCNT, webClientTcpKeepConnetionNumberOfTries)
                ))
                .baseUrl(baseUrl);
        httpsProxyConfiguration.ifPresent(proxyConfig -> addProxy(webClientBuilder, proxyConfig));
        httpProxyConfiguration.ifPresent(proxyConfig -> addProxy(webClientBuilder, proxyConfig));
        return webClientBuilder.build();
    }

    private void addProxy(WebClient.Builder webClientBuilder, ProxyConfiguration proxyConfiguration) {
        webClientBuilder.clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                        .host(proxyConfiguration.getHost())
                        .port(proxyConfiguration.getPort())
                        .username(proxyConfiguration.getUsername())
                        .password(password -> proxyConfiguration.getPassword()) // Use Function.identity() here if needed
                        .nonProxyHosts(proxyConfiguration.getNoProxyPattern())
                )));
    }

    public int getWebClientMaxNumberOfRetries() {
        return webClientMaxNumberOfRetries;
    }

    public int getWebClientTimeInSecondsAfterRetryWithFailure() {
        return webClientTimeInSecondsAfterRetryWithFailure;
    }

}
