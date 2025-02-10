package de.samply.register;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.utils.MessageStatus;
import de.samply.utils.WebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AppRegisterService {

    private WebClient webClient = null;
    private final NotificationService notificationService;
    private final ProjectCoderRepository projectCoderRepository;
    private String authorizationHeader = null;
    private final boolean appRegisterEnabled;

    public AppRegisterService(
            @Value(ProjectManagerConst.APP_REGISTER_BASE_URL_SV) String appRegisterBaseUrl,
            @Value(ProjectManagerConst.APP_REGISTER_API_KEY_SV) String appRegisterApiKey,
            @Value(ProjectManagerConst.APP_REGISTER_AUTHORIZATION_FORMAT_SV) String authorizationFormat,
            @Value(ProjectManagerConst.ENABLE_APP_REGISTER_SV) boolean appRegisterEnabled,
            WebClientFactory webClientFactory,
            NotificationService notificationService,
            ProjectCoderRepository projectCoderRepository) {
        this.appRegisterEnabled = appRegisterEnabled;
        this.notificationService = notificationService;
        this.projectCoderRepository = projectCoderRepository;
        if (appRegisterEnabled) {
            this.webClient = webClientFactory.createWebClient(appRegisterBaseUrl);
            this.authorizationHeader = fetchAuthorizationHeader(authorizationFormat, appRegisterApiKey);
        }
    }


    public Mono<Void> register(ProjectCoder projectCoder) {
        if (!appRegisterEnabled) {
            log.error("App register is not enabled. App could not be registered for project {} for bridgehead {} for user {}",
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                    projectCoder.getProjectBridgeheadUser().getEmail());
            return Mono.empty();
        }
        log.info("Registering app for user {}, project {} and bridgehead {}",
                projectCoder.getProjectBridgeheadUser().getEmail(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead()
                );
        return webClient.post()
                .uri(ProjectManagerConst.REGISTER_PATH)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fetchRegisterBody(projectCoder))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    MessageStatus messageStatus = MessageStatus.newInstance(throwable, "User app could not be registered in Beam");
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.REGISTER_IN_APP_REGISTER,
                            messageStatus.message(),
                            ExceptionUtils.getStackTrace(throwable),
                            messageStatus.status());
                    log.error(ExceptionUtils.getStackTrace(throwable));
                })
                .doOnSuccess(response -> {
                    log.info("App registered");
                    projectCoder.setInAppRegister(true);
                    projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.REGISTER_IN_APP_REGISTER,
                            "User app registered in Beam " + (response != null && response.length() > 0 ? " (" + response + ")" : ""),
                            null,
                            HttpStatus.OK
                    );
                }).then();
    }

    public Mono<Void> unregister(ProjectCoder projectCoder) {
        if (!appRegisterEnabled) {
            log.error("App register is not enabled. App could not be unregistered for project {} for bridgehead {} for user {}",
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                    projectCoder.getProjectBridgeheadUser().getEmail());
            return Mono.empty();
        }
        log.info("Unregistering app for user {}, project {} and bridgehead {}",
                projectCoder.getProjectBridgeheadUser().getEmail(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead()
        );
        return webClient.method(HttpMethod.DELETE)
                .uri(ProjectManagerConst.REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(fetchUnRegisterBody(projectCoder))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(throwable -> {
                    MessageStatus messageStatus = MessageStatus.newInstance(throwable, "User app could not be unregistered in Beam");
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.UNREGISTER_IN_APP_REGISTER,
                            messageStatus.message(),
                            ExceptionUtils.getStackTrace(throwable),
                            messageStatus.status());
                    log.error(ExceptionUtils.getStackTrace(throwable));
                })
                .doOnSuccess(response -> {
                    log.info("App unregistered");
                    projectCoder.setInAppRegister(false);
                    projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.UNREGISTER_IN_APP_REGISTER,
                            "User app unregistered in Beam" + (response != null && response.length() > 0 ? response : ""),
                            null,
                            HttpStatus.OK
                    );
                }).then();
    }

    private AppRegister fetchRegisterBody(ProjectCoder projectCoder) {
        return new AppRegister(projectCoder.getAppId(), projectCoder.getAppSecret());
    }

    private AppRegister fetchUnRegisterBody(ProjectCoder projectCoder) {
        return new AppRegister() {{
            setBeamId(projectCoder.getAppId());
        }};
    }

    private String fetchAuthorizationHeader(String authorizationFormat, String apiKey) {
        return authorizationFormat.replace("{}", apiKey);
    }


}
