package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.CreatorUser;
import de.samply.db.model.Project;
import de.samply.db.model.User;
import de.samply.db.repository.CreatorUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.UserRepository;
import de.samply.utils.DateUtils;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProjectContextFactory {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CreatorUserRepository creatorUserRepository;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final String datePattern;

    public ProjectContextFactory(ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 CreatorUserRepository creatorUserRepository,
                                 BridgeheadConfiguration bridgeheadConfiguration,
                                 @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.creatorUserRepository = creatorUserRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.datePattern = datePattern;
    }

    public ProjectContext createProjectContext(@NotNull String projectCode, String language) {
        Map<ProjectContextKey, String> result = new LinkedHashMap<>();
        fetchProject(projectCode).ifPresent(project -> {
            result.put(ProjectContextKey.PROJECT_CODE, project.getCode());
            result.put(ProjectContextKey.PROJECT_TITLE, project.getQuery().getLabel());
            result.put(ProjectContextKey.PROJECT_DESCRIPTION, project.getQuery().getDescription());
            result.put(ProjectContextKey.PROJECT_CREATION_DATE, DateUtils.fetchDate(project.getCreatedAt(), datePattern, language));
            fetchCreator(project).ifPresent(user -> {
                result.put(ProjectContextKey.CREATOR_NAME, UserUtils.extractFullName(Optional.of(user)));
                result.put(ProjectContextKey.CREATOR_EMAIL, project.getCreatorEmail());
                Set<CreatorUser> users = creatorUserRepository.findByEmail(user.getEmail());
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

        });
        return new ProjectContext(result);
    }

    private Optional<Project> fetchProject(@NotNull String projectCode) {
        Objects.requireNonNull(projectCode);
        return projectRepository.findByCode(projectCode);
    }

    private Optional<User> fetchCreator(@NotNull Project project) {
        return userRepository.findByEmail(project.getCreatorEmail());
    }


}
