package de.samply.register;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
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


    public Mono<String> register(ProjectCoder projectCoder) {
        if (!appRegisterEnabled) {
            log.error("App register is not enabled. App could not be registered for project {} for bridgehead {} for user {}",
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getId(),
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                    projectCoder.getProjectBridgeheadUser().getEmail());
            return Mono.empty();
        }
        return webClient.post()
                .uri(ProjectManagerConst.REGISTER_PATH)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fetchRegisterBody(projectCoder))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, throwable -> {
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.REGISTER_IN_APP_REGISTER,
                            "User app could not be registered in Beam",
                            ExceptionUtils.getStackTrace(throwable),
                            (HttpStatus) throwable.getStatusCode());
                    log.error(ExceptionUtils.getStackTrace(throwable));
                    return Mono.just("Fallback response");
                })
                .doOnSuccess(response -> {
                    projectCoder.setInAppRegister(true);
                    projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.REGISTER_IN_APP_REGISTER,
                            "User app registered in Beam",
                            null,
                            HttpStatus.OK
                            );
                });
    }

    public Mono<String> unregister(ProjectCoder projectCoder) {
        if (!appRegisterEnabled) {
            log.error("App register is not enabled. App could not be unregistered for project {} for bridgehead {} for user {}",
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getId(),
                    projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                    projectCoder.getProjectBridgeheadUser().getEmail());
            return Mono.empty();
        }
        return webClient.method(HttpMethod.DELETE)
                .uri(ProjectManagerConst.REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .bodyValue(fetchUnRegisterBody(projectCoder))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, throwable -> {
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.UNREGISTER_IN_APP_REGISTER,
                            "User app could not be unregistered in Beam",
                            ExceptionUtils.getStackTrace(throwable),
                            (HttpStatus) throwable.getStatusCode());
                    log.error(ExceptionUtils.getStackTrace(throwable));
                    return Mono.just("Fallback response");
                })
                .doOnSuccess(response -> {
                    projectCoder.setInAppRegister(false);
                    projectCoderRepository.save(projectCoder);
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject().getCode(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.UNREGISTER_IN_APP_REGISTER,
                            "User app unregistered in Beam",
                            null,
                            HttpStatus.OK
                    );
                });
    }

    private AppRegister fetchRegisterBody(ProjectCoder projectCoder) {
        return new AppRegister(projectCoder.getAppId(), projectCoder.getAppSecret());
    }

    private AppRegister fetchUnRegisterBody(ProjectCoder projectCoder) {
        return new AppRegister(){{setBeamSecret(projectCoder.getAppSecret());}};
    }

    private String fetchAuthorizationHeader(String authorizationFormat, String apiKey) {
        return authorizationFormat.replace("{}", apiKey);
    }


}
