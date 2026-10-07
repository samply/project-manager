package de.samply.register;

import de.samply.annotations.ModuleComponent;
import de.samply.modules.OptionalModule;
import de.samply.researchenvironment.ResearchEnvironmentService;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectCoder;
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

import java.util.Optional;

/**
 * Registers each research environment workspace as a Beam app (beam-register), so that files can be sent to it. Part
 * of the optional module RESEARCH_ENVIRONMENT.
 */
@Slf4j
@Service
@ModuleComponent(OptionalModule.RESEARCH_ENVIRONMENT)
public class AppRegisterService {

    private final WebClient webClient;
    private final NotificationService notificationService;
    private final ResearchEnvironmentService researchEnvironmentService;
    private final String authorizationHeader;

    public AppRegisterService(
            @Value(ProjectManagerConst.APP_REGISTER_BASE_URL_SV) String appRegisterBaseUrl,
            @Value(ProjectManagerConst.APP_REGISTER_API_KEY_SV) String appRegisterApiKey,
            @Value(ProjectManagerConst.APP_REGISTER_AUTHORIZATION_FORMAT_SV) String authorizationFormat,
            WebClientFactory webClientFactory,
            ResearchEnvironmentService researchEnvironmentService,
            NotificationService notificationService) {
        this.researchEnvironmentService = researchEnvironmentService;
        this.notificationService = notificationService;
        this.webClient = webClientFactory.createWebClient(appRegisterBaseUrl);
        this.authorizationHeader = fetchAuthorizationHeader(authorizationFormat, appRegisterApiKey);
    }


    public Mono<Void> register(ProjectCoder projectCoder) {
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
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
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
                    researchEnvironmentService.saveCoder(projectCoder);
                    String message = Optional.ofNullable(response)
                            .filter(r -> !r.isEmpty())
                            .map(r -> " (" + r + ")")
                            .orElse("");
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.REGISTER_IN_APP_REGISTER,
                            "User app registered in Beam" + message,
                            null,
                            HttpStatus.OK
                    );

                }).then();
    }

    public Mono<Void> unregister(ProjectCoder projectCoder) {
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
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
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
                    researchEnvironmentService.saveCoder(projectCoder);
                    String message = Optional.ofNullable(response)
                            .filter(r -> !r.isEmpty())
                            .map(r -> " (" + r + ")")
                            .orElse("");
                    notificationService.createNotification(
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getProject(),
                            projectCoder.getProjectBridgeheadUser().getProjectBridgehead().getBridgehead(),
                            projectCoder.getProjectBridgeheadUser().getEmail(),
                            OperationType.UNREGISTER_IN_APP_REGISTER,
                            "User app unregistered in Beam" + message,
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
