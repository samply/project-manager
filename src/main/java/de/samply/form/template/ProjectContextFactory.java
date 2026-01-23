package de.samply.form.template;

import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.CreatorUser;
import de.samply.db.model.Project;
import de.samply.db.model.User;
import de.samply.db.repository.CreatorUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.UserRepository;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProjectContextFactory {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CreatorUserRepository creatorUserRepository;
    private final BridgeheadConfiguration bridgeheadConfiguration;

    public ProjectContextFactory(ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 CreatorUserRepository creatorUserRepository,
                                 BridgeheadConfiguration bridgeheadConfiguration) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.creatorUserRepository = creatorUserRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
    }

    public ProjectContext createProjectContext(@NotNull String projectCode) {
        Map<ProjectContextKey, String> result = new LinkedHashMap<>();
        fetchProject(projectCode).ifPresent(project -> {
            result.put(ProjectContextKey.PROJECT_CODE, project.getCode());
            result.put(ProjectContextKey.PROJECT_TITLE, project.getQuery().getLabel());
            result.put(ProjectContextKey.PROJECT_DESCRIPTION, project.getQuery().getDescription());
            fetchCreator(project).ifPresent(user -> {
                result.put(ProjectContextKey.CREATOR_NAME, UserUtils.extractFullName(Optional.of(user)));
                String bridgeheads = creatorUserRepository.findByEmail(user.getEmail()).stream()
                        .map(CreatorUser::getBridgehead)
                        .map(bridgeheadConfiguration::getHumanReadable)
                        .flatMap(Optional::stream)
                        .collect(Collectors.joining(",")).trim();
                if (!bridgeheads.isEmpty()) {
                    result.put(ProjectContextKey.CREATOR_BRIDGEHEADS, bridgeheads);
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
