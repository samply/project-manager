package de.samply.token;

import java.util.Arrays;
import java.util.List;

import de.samply.app.ProjectManagerConst;
import de.samply.security.SessionUser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TokenManagerService {

    private final SessionUser sessionUser;

    public TokenManagerService(SessionUser sessionUser) {
        this.sessionUser = sessionUser;
    }

    public String fetchAuthenticationScript(String projectCode, String bridgehead) {

        try {

            List<String> bridgeheadIds = Arrays.asList(bridgehead.split(","));
            TokenParams tokenParams = new TokenParams();
            tokenParams.setEmail(sessionUser.getEmail());
            tokenParams.setProjectId(projectCode);
            tokenParams.setBridgeheadIds(bridgeheadIds);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(ProjectManagerConst.TOKENS_ENDPOINT, tokenParams, String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error creating token: " + e.getMessage();
        }
    }

    public String checkProjectStatus(String projectId) {
        String uri = ProjectManagerConst.PROJECT_STATUS_ENDPOINT + projectId + "/status";
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error checking project status: " + e.getMessage();
        }
    }

    public String generateUserScript(String projectId, String userEmail) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ProjectManagerConst.SCRIPTS_ENDPOINT)
                .queryParam("project", projectId)
                .queryParam("user", userEmail);
        String uriWithParams = builder.toUriString();

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(uriWithParams, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating script: " + e.getMessage();
        }
    }

}
