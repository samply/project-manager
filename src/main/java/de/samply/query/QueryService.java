package de.samply.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.db.model.QueryOutput;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.QueryRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectServiceException;
import de.samply.project.ProjectType;
import de.samply.security.SessionUser;
import de.samply.utils.Base64Utils;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class QueryService {

    private final NotificationService notificationService;
    private final SessionUser sessionUser;
    private final QueryRepository queryRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryService(NotificationService notificationService,
                        SessionUser sessionUser,
                        QueryRepository queryRepository,
                        ProjectRepository projectRepository) {
        this.notificationService = notificationService;
        this.sessionUser = sessionUser;
        this.queryRepository = queryRepository;
        this.projectRepository = projectRepository;
    }

    public String createQuery(
            String query, QueryFormat queryFormat, String label, String description,
            OutputFormat outputFormat, String templateId, ProjectType projectType,
            String humanReadable, String explorerUrl, String queryContext) {
        Query tempQuery = new Query();
        tempQuery.setCode(generateQueryCode());
        tempQuery.setQuery(query);
        tempQuery.setQueryFormat(queryFormat);
        tempQuery.setCreatedAt(Instant.now());
        tempQuery.setLabel(label);
        tempQuery.setDescription(description);
        Base64Utils.decodeIfNecessary(humanReadable).ifPresent(tempQuery::setHumanReadable);
        tempQuery.setExplorerUrl(decodeUrlIfNecessary(explorerUrl));
        tempQuery.setContext(queryContext);
        tempQuery = this.queryRepository.save(tempQuery);
        // Every Query should have at least one output:
        addOutputToQuery(tempQuery, Optional.ofNullable(outputFormat), Optional.ofNullable(templateId), projectType);
        return tempQuery.getCode();
    }

    private void addOutputToQuery(Query query,
                                  Optional<OutputFormat> outputFormat,
                                  Optional<String> templateId,
                                  @NotNull ProjectType projectType) {
        if (projectType != null) {
            QueryOutput output = query.fetchOutput(projectType)
                    .orElseGet(() -> {
                        QueryOutput newOutput = new QueryOutput();
                        newOutput.setProjectType(projectType);
                        query.addOutput(newOutput);
                        return newOutput;
                    });

            outputFormat.ifPresent(output::setOutputFormat);
            templateId.ifPresent(output::setTemplateId);
            queryRepository.save(query);
        }
    }

    public void removeOutput(@NotNull String projectCode, @NotNull ProjectType projectType) {
        this.projectRepository.findByCode(projectCode).ifPresentOrElse(project -> {
                    project.getQuery().removeOutput(projectType);
                    this.queryRepository.save(project.getQuery());
                    this.notificationService.createNotification(project.getCode(), null, sessionUser.getEmail(),
                            OperationType.EDIT_PROJECT, "Removed output of type " + projectType.name(), null, null);
                },
                () -> {
                    throw new ProjectServiceException("Project " + projectCode + " not found");
                }
        );
    }


    public void addProjectCodeToExporterUrl(@NotNull String queryCode, @NotNull String projectCode) {
        queryRepository.findByCode(queryCode).ifPresent(query -> {
            if (query.getExplorerUrl() != null) {
                query.setExplorerUrl(addProjectCodeToUrl(query.getExplorerUrl(), projectCode));
                this.queryRepository.save(query);
            }
        });
    }

    private String addProjectCodeToUrl(@NotNull String url, @NotNull String projectCode) {
        return (url.contains(ProjectManagerConst.PROJECT_CODE)) ? url :
                UriComponentsBuilder.fromUriString(url).queryParam(ProjectManagerConst.PROJECT_CODE, projectCode).toUriString();
    }

    private String generateQueryCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, ProjectManagerConst.QUERY_CODE_SIZE);
    }

    public void editQuery(@NotNull String projectCode,
                          String query, QueryFormat queryFormat, String label, String description,
                          OutputFormat outputFormat, String templateId, ProjectType projectType,
                          String humanReadable, String explorerUrl, String queryContext) {
        Optional<Project> projectOptional = projectRepository.findByCode(projectCode);
        if (projectOptional.isPresent()) {
            Query projectQuery = projectOptional.get().getQuery();
            if (projectQuery != null) {
                Map<String, String> changedKeyValueMap = new HashMap<>();
                if (query != null) {
                    projectQuery.setQuery(query);
                    changedKeyValueMap.put("query", query);
                }
                if (queryFormat != null) {
                    projectQuery.setQueryFormat(queryFormat);
                    changedKeyValueMap.put("query format", queryFormat.toString());
                }
                if (label != null) {
                    projectQuery.setLabel(label);
                    changedKeyValueMap.put("label", label);
                }
                if (description != null) {
                    projectQuery.setDescription(description);
                    changedKeyValueMap.put("description", description);
                }
                if (humanReadable != null) {
                    Base64Utils.decodeIfNecessary(humanReadable).ifPresent(projectQuery::setHumanReadable);
                    changedKeyValueMap.put("human readable", humanReadable);
                }
                if (explorerUrl != null) {
                    projectQuery.setExplorerUrl(addProjectCodeToUrl(decodeUrlIfNecessary(explorerUrl), projectCode));
                    changedKeyValueMap.put("explorer url", explorerUrl);
                }
                if (queryContext != null) {
                    projectQuery.setContext(queryContext);
                    changedKeyValueMap.put("query context", queryContext);
                }
                if (projectType != null && (outputFormat != null || templateId != null)) {
                    Optional<OutputFormat> outputFormatOptional = Optional.ofNullable(outputFormat);
                    Optional<String> templateIdOptional = Optional.ofNullable(templateId);
                    addOutputToQuery(projectQuery, outputFormatOptional, templateIdOptional, projectType);
                    outputFormatOptional.ifPresent(of -> changedKeyValueMap.put("output format for project type " + projectType, of.toString()));
                    templateIdOptional.ifPresent(tid -> changedKeyValueMap.put("template id for project type " + projectType, tid));
                }
                if (!changedKeyValueMap.isEmpty()) {
                    queryRepository.save(projectQuery);
                    this.notificationService.createNotification(projectCode, null, sessionUser.getEmail(),
                            OperationType.EDIT_QUERY, printInOneLine(changedKeyValueMap), null, null);
                }
            }
        }
    }

    private String decodeUrlIfNecessary(String encodedUrl) {
        try {
            return URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encodedUrl;
        }
    }

    private String printInOneLine(Map<String, String> changedKeyValuesMaps) {
        try {
            return "Query edited: " + objectMapper.writeValueAsString(changedKeyValuesMaps);
        } catch (JsonProcessingException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

}
