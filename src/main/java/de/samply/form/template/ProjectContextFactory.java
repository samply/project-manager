package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.CreatorUser;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectDocument;
import de.samply.db.model.QueryOutput;
import de.samply.db.model.User;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.document.DocumentService;
import de.samply.document.DocumentType;
import de.samply.query.QueryFormat;
import de.samply.user.UserService;
import de.samply.utils.DateUtils;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProjectContextFactory {

    private final UserService userService;
    private final BridgeheadsConfiguration bridgeheadsConfiguration;
    private final String datePattern;
    private final DocumentService documentService;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    public ProjectContextFactory(
            UserService userService,
            BridgeheadsConfiguration bridgeheadsConfiguration,
            @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern,
            DocumentService documentService,
            ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.userService = userService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.datePattern = datePattern;
        this.documentService = documentService;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public ProjectContext createProjectContext(@NotNull Project project, String language) {
        Map<ProjectContextKey, String> result = new LinkedHashMap<>();
        result.put(ProjectContextKey.PROJECT_CODE, project.getCode());
        result.put(ProjectContextKey.PROJECT_TITLE, project.getQuery().getLabel());
        result.put(ProjectContextKey.PROJECT_DESCRIPTION, project.getQuery().getDescription());
        result.put(ProjectContextKey.PROJECT_CREATION_DATE, DateUtils.fetchDate(project.getCreatedAt(), datePattern, language));
        result.put(ProjectContextKey.ETHICAL_APPROVAL, existsVotum(project).toString());

        // F10/Phase 7: native/FIXED fields resolved directly from the Project/
        // Query entities and DocumentService, the same mechanism used above
        // for PROJECT_TITLE/PROJECT_DESCRIPTION. Unlike CREATOR_BRIDGEHEADS/
        // CREATOR_AFFILIATIONS below (which are embedded inside a larger
        // combined value, so the surrounding text still reads fine if
        // omitted), each of these keys is used as a standalone project_fields
        // value in configuration - an unresolved "${...}" placeholder would
        // otherwise leak into the PDF verbatim for any project missing that
        // particular native field. So these are always put, blank when unset,
        // matching how an empty DYNAMIC field renders.
        putOrBlank(result, ProjectContextKey.ETHICS_VOTE_FOR_ALL_SITES_FILENAME,
                fetchLastDocumentFilename(project, Optional.empty(), DocumentType.VOTUM).orElse(null));
        putOrBlank(result, ProjectContextKey.DESCRIPTION_UPLOAD_FILENAME,
                fetchLastDocumentFilename(project, Optional.empty(), DocumentType.DESCRIPTION).orElse(null));
        putOrBlank(result, ProjectContextKey.ENVIRONMENT_VARIABLES, project.getQuery().getContext());
        putOrBlank(result, ProjectContextKey.QUERY_FORMAT,
                Optional.ofNullable(project.getQuery().getQueryFormat()).map(QueryFormat::name).orElse(null));
        putOrBlank(result, ProjectContextKey.ADDITIONAL_FILTER_CRITERIA, project.getQuery().getCohortDefinition());
        putOrBlank(result, ProjectContextKey.SELECTED_COHORT, project.getQuery().getHumanReadable());

        // Phase 8: structured native fields - each joined into one string,
        // since project_fields configuration substitutes one value per key,
        // not a dynamic number of rows (see §5.6/§5.8 of the plan for the
        // "one row per site/output" ideal this simplifies).
        Set<ProjectBridgehead> queriedBridgeheads = projectBridgeheadRepository.findByProject(project);
        putOrBlank(result, ProjectContextKey.QUERIED_SITES, joinHumanReadableBridgeheads(queriedBridgeheads, ","));
        putOrBlank(result, ProjectContextKey.ETHICS_VOTES_PER_SITE, fetchEthicsVotesPerSite(project, queriedBridgeheads));

        Set<QueryOutput> outputs = project.getQuery().getOutputs();
        putOrBlank(result, ProjectContextKey.PROJECT_TYPES, joinOutputs(outputs, o -> o.getProjectType().name()));
        putOrBlank(result, ProjectContextKey.OUTPUT_FORMATS,
                joinOutputs(outputs, o -> o.getOutputFormat() != null ? o.getOutputFormat().name() : ""));
        putOrBlank(result, ProjectContextKey.TEMPLATE_IDS, joinOutputs(outputs, QueryOutput::getTemplateId));

        putOrBlank(result, ProjectContextKey.PROJECT_CONFIGURATION,
                Boolean.TRUE.equals(project.getIsCustomConfigSelected()) ? "Custom" : null);

        fetchCreator(project).ifPresent(user -> {
            String creatorName = UserUtils.extractFullName(Optional.of(user));
            result.put(ProjectContextKey.CREATOR_NAME, creatorName);
            result.put(ProjectContextKey.CREATOR_EMAIL, project.getCreatorEmail());
            Set<CreatorUser> users = userService.fetchCreatorUser(user.getEmail());
            String bridgeheads = users.stream()
                    .map(CreatorUser::getBridgehead)
                    .map(bridgeheadsConfiguration::getHumanReadable)
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(",")).trim();
            if (!bridgeheads.isEmpty()) {
                result.put(ProjectContextKey.CREATOR_BRIDGEHEADS, bridgeheads);
            }
            String affiliations = users.stream()
                    .map(CreatorUser::getBridgehead)
                    .map(bridgeheadsConfiguration::getAffiliation)
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(",")).trim();
            if (!affiliations.isEmpty()) {
                result.put(ProjectContextKey.CREATOR_AFFILIATIONS, affiliations);
            }
            // Point 1 (2026-09-09): only append " (affiliations)" when there
            // actually are any, so a creator with none doesn't show a
            // dangling empty "()".
            result.put(ProjectContextKey.CREATOR_NAME_WITH_AFFILIATIONS,
                    affiliations.isEmpty() ? creatorName : creatorName + " (" + affiliations + ")");
        });

        return new ProjectContext(result);
    }

    private Boolean existsVotum(Project project){
        return documentService
                .fetchLastDocumentOfThisType(project, Optional.empty(), DocumentType.VOTUM)
                .isPresent();
    }

    private Optional<String> fetchLastDocumentFilename(
            Project project, Optional<ProjectBridgehead> bridgehead, DocumentType type) {
        return documentService
                .fetchLastDocumentOfThisType(project, bridgehead, type)
                .map(ProjectDocument::getOriginalFilename);
    }

    private void putOrBlank(Map<ProjectContextKey, String> result, ProjectContextKey key, String value) {
        result.put(key, value != null ? value : "");
    }

    private String joinHumanReadableBridgeheads(Set<ProjectBridgehead> bridgeheads, String separator) {
        return bridgeheads.stream()
                .map(ProjectBridgehead::getBridgehead)
                .map(bridgeheadsConfiguration::getHumanReadable)
                .flatMap(Optional::stream)
                .sorted()
                .collect(Collectors.joining(separator));
    }

    // ETHICS_VOTE (plain, per-bridgehead): one line per queried site, listing
    // whether that site has its own uploaded ethics vote (as opposed to
    // ETHICS_VOTE_FOR_ALL_SITES_FILENAME above, which is a single vote
    // covering every site at once).
    private String fetchEthicsVotesPerSite(Project project, Set<ProjectBridgehead> bridgeheads) {
        return bridgeheads.stream()
                .sorted(Comparator.comparing(ProjectBridgehead::getBridgehead))
                .map(bridgehead -> {
                    String siteName = bridgeheadsConfiguration.getHumanReadable(bridgehead.getBridgehead())
                            .orElse(bridgehead.getBridgehead());
                    String filename = fetchLastDocumentFilename(project, Optional.of(bridgehead), DocumentType.VOTUM)
                            .orElse("not uploaded");
                    return siteName + ": " + filename;
                })
                .collect(Collectors.joining("\n"));
    }

    private String joinOutputs(Set<QueryOutput> outputs, Function<QueryOutput, String> fetchField) {
        return outputs.stream()
                .sorted(Comparator.comparing(o -> o.getProjectType().name()))
                .map(fetchField)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    private Optional<User> fetchCreator(@NotNull Project project) {
        return userService.fetchUser(project.getCreatorEmail());
    }


}
