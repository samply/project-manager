package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.CreatorUser;
import de.samply.db.model.Project;
import de.samply.db.model.User;
import de.samply.document.DocumentService;
import de.samply.document.DocumentType;
import de.samply.user.UserService;
import de.samply.utils.DateUtils;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProjectContextFactory {

    private final UserService userService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final String datePattern;
    private final DocumentService documentService;

    public ProjectContextFactory(
            UserService userService,
            BridgeheadConfiguration bridgeheadConfiguration,
            @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern,
            DocumentService documentService) {
        this.userService = userService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.datePattern = datePattern;
        this.documentService = documentService;
    }

    public ProjectContext createProjectContext(@NotNull Project project, String language) {
        Map<ProjectContextKey, String> result = new LinkedHashMap<>();
        result.put(ProjectContextKey.PROJECT_CODE, project.getCode());
        result.put(ProjectContextKey.PROJECT_TITLE, project.getQuery().getLabel());
        result.put(ProjectContextKey.PROJECT_DESCRIPTION, project.getQuery().getDescription());
        result.put(ProjectContextKey.PROJECT_CREATION_DATE, DateUtils.fetchDate(project.getCreatedAt(), datePattern, language));
        result.put(ProjectContextKey.ETHICAL_APPROVAL, existsVotum(project).toString());
        fetchCreator(project).ifPresent(user -> {
            result.put(ProjectContextKey.CREATOR_NAME, UserUtils.extractFullName(Optional.of(user)));
            result.put(ProjectContextKey.CREATOR_EMAIL, project.getCreatorEmail());
            Set<CreatorUser> users = userService.fetchCreatorUser(user.getEmail());
            String bridgeheads = users.stream()
                    .map(CreatorUser::getBridgehead)
                    .map(bridgeheadConfiguration::getHumanReadable)
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(",")).trim();
            if (!bridgeheads.isEmpty()) {
                result.put(ProjectContextKey.CREATOR_BRIDGEHEADS, bridgeheads);
            }
            String affiliations = users.stream()
                    .map(CreatorUser::getBridgehead)
                    .map(bridgeheadConfiguration::getAffiliation)
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(",")).trim();
            if (!affiliations.isEmpty()) {
                result.put(ProjectContextKey.CREATOR_AFFILIATIONS, affiliations);
            }
        });

        return new ProjectContext(result);
    }

    private Boolean existsVotum(Project project){
        return documentService
                .fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM)
                .isPresent();
    }

    private Optional<User> fetchCreator(@NotNull Project project) {
        return userService.fetchUser(project.getCreatorEmail());
    }


}
