package de.samply.rstudio.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.WebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@ConditionalOnProperty(name = ProjectManagerConst.RSTUDIO_GROUP_IMPLEMENTATION, havingValue = ProjectManagerConst.RSTUDIO_GROUP_KEYCLOAK_IMPLEMENTATION)
public class RstudioKeycloakGroupManager implements RstudioGroupManager {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tokenRequestBody;
    private final String rstudioGroup;
    private final String realm;
    private final WebClient webClient;
    private final boolean enabled;

    public RstudioKeycloakGroupManager(
            @Value(ProjectManagerConst.OIDC_URL_SV) String keycloakUrl,
            @Value(ProjectManagerConst.KEYCLOAK_RSTUDIO_GROUP_CLIENT_ID_SV) String clientId,
            @Value(ProjectManagerConst.KEYCLOAK_RSTUDIO_GROUP_CLIENT_SECRET_SV) String clientSecret,
            @Value(ProjectManagerConst.KEYCLOAK_RSTUDIO_GROUP_SV) String rstudioGroup,
            @Value(ProjectManagerConst.OIDC_REALM_SV) String realm,
            @Value(ProjectManagerConst.ENABLE_RSTUDIO_GROUP_MANAGER_SV) boolean enabled,
            WebClientFactory webClientFactory) {
        this.tokenRequestBody = createTokenRequestBody(clientId, clientSecret);
        this.rstudioGroup = rstudioGroup;
        this.realm = realm;
        this.enabled = enabled;
        this.webClient = webClientFactory.createWebClient(keycloakUrl);
    }

    private String createTokenRequestBody(String clientId, String clientSecret) {
        StringBuilder stringBuilder = new StringBuilder();
        append(stringBuilder, ProjectManagerConst.GRANT_TYPE_KEYCLOAK_PARAM, ProjectManagerConst.CLIENT_CREDENTIALS_KEYCLOAK_CONST);
        append(stringBuilder, ProjectManagerConst.CLIENT_ID_KEYCLOAK_PARAM, clientId);
        append(stringBuilder, ProjectManagerConst.CLIENT_SECRET_KEYCLOAK_PARAM, clientSecret);
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private void append(StringBuilder stringBuilder, String key, String value) {
        stringBuilder.append(key + "=" + value + "&");
    }


    @Override
    public void addUserToRstudioGroup(String email) throws RstudioGroupManagerException {
        if (enabled) {
            log.info("Adding user " + email + " to Rstudio-Group");
            addUserToGroup(email);
        }
    }

    @Override
    public void removeUserFromRstudioGroup(String email) throws RstudioGroupManagerException {
        if (enabled) {
            log.info("Removing user " + email + " from Rstudio-Group");
            removeUserFromGroup(email);
        }
    }

    private void addUserToGroup(String email) {
        modifyUserInGroup(email, HttpMethod.PUT);
    }

    private void removeUserFromGroup(String email) {
        modifyUserInGroup(email, HttpMethod.DELETE);
    }

    private void modifyUserInGroup(String email, HttpMethod method) {
        String authToken = fetchToken();
        Optional<String> userId = fetchUserId(email, authToken);
        if (userId.isPresent()) {
            Optional<String> groupId = fetchGroupId(rstudioGroup, authToken);
            if (groupId.isPresent()) {
                this.webClient.method(method)
                        .uri(ProjectManagerConst.CHANGE_USER_GROUP_KEYCLOAK_PATH, realm, userId.get(), groupId.get())
                        .headers(httpHeaders -> addAuthenticationHeaders(httpHeaders, authToken))
                        .retrieve().bodyToMono(String.class).block();
            }
        }
    }

    private Optional<String> fetchUserId(String email, String authToken) {
        try {
            return Optional.ofNullable(fetchUserIdWithoutHandlingExeception(email, authToken));
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return Optional.empty();
        }
    }

    private String fetchUserIdWithoutHandlingExeception(String email, String authToken) {
        return this.webClient.get()
                .uri(ProjectManagerConst.FETCH_USER_ID_KEYCLOAK_PATH, realm, email)
                .headers(httpHeaders -> addAuthenticationHeaders(httpHeaders, authToken))
                .retrieve().bodyToMono(String.class)
                .map(response -> extractAttribute(response, jsonNode -> jsonNode.get(0).get(ProjectManagerConst.ID_KEYCLOAK_CONST)))
                .block();
    }

    private Optional<String> fetchGroupId(String group, String authToken) {
        try {
            return Optional.ofNullable(fetchGroupIdWithoutHandlingException(group, authToken));
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return Optional.empty();
        }
    }

    private String fetchGroupIdWithoutHandlingException(String group, String authToken) {
        return this.webClient.get()
                .uri(ProjectManagerConst.FETCH_GROUP_ID_KEYCLOAK_PATH, realm, group)
                .headers(httpHeaders -> addAuthenticationHeaders(httpHeaders, authToken))
                .retrieve().bodyToMono(String.class)
                .map(response -> extractAttribute(response, jsonNode -> jsonNode.get(0).get(ProjectManagerConst.ID_KEYCLOAK_CONST)))
                .block();
    }

    private void addAuthenticationHeaders(HttpHeaders httpHeaders, String authToken) {
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        httpHeaders.setBearerAuth(authToken);
    }

    private String fetchToken() {
        return this.webClient.post()
                .uri(ProjectManagerConst.FETCH_TOKEN_KEYCLOAK_PATH, realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(tokenRequestBody)
                .retrieve().bodyToMono(String.class)
                .map(response -> extractAttribute(response, jsonNode -> jsonNode.get(ProjectManagerConst.ACCES_TOKEN_KEYCLOAK_CONST)))
                .block();
    }

    private String extractAttribute(String jsonObject, Function<JsonNode, JsonNode> attributeExtractor) {
        try {
            return attributeExtractor.apply(objectMapper.readTree(jsonObject)).asText();
        } catch (Exception e) {
            throw new RstudioGroupManagerException(e);
        }
    }

}
