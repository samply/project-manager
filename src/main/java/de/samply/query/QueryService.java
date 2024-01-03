package de.samply.query;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.QueryRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class QueryService {

    private final QueryRepository queryRepository;
    private final ProjectRepository projectRepository;

    public QueryService(QueryRepository queryRepository,
                        ProjectRepository projectRepository) {
        this.queryRepository = queryRepository;
        this.projectRepository = projectRepository;
    }

    public String createQuery(
            String query, QueryFormat queryFormat, String label, String description,
            OutputFormat outputFormat, String templateId, String humanReadable, String explorerUrl) {
        Query tempQuery = new Query();
        tempQuery.setCode(generateQueryCode());
        tempQuery.setQuery(query);
        tempQuery.setQueryFormat(queryFormat);
        tempQuery.setCreatedAt(Instant.now());
        tempQuery.setLabel(label);
        tempQuery.setDescription(description);
        tempQuery.setOutputFormat(outputFormat);
        tempQuery.setTemplateId(templateId);
        tempQuery.setHumanReadable(humanReadable);
        tempQuery.setExplorerUrl(explorerUrl);
        tempQuery = this.queryRepository.save(tempQuery);
        return tempQuery.getCode();
    }

    private String generateQueryCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, ProjectManagerConst.QUERY_CODE_SIZE);
    }

    public void editQuery(@NotNull String projectCode,
                          String query, QueryFormat queryFormat, String label, String description,
                          OutputFormat outputFormat, String templateId, String humanReadable, String explorerUrl) {
        Optional<Project> projectOptional = projectRepository.findByCode(projectCode);
        if (projectOptional.isPresent()) {
            Query projectQuery = projectOptional.get().getQuery();
            if (projectQuery != null) {
                boolean hasChanged = false;
                if (query != null) {
                    projectQuery.setQuery(query);
                    hasChanged = true;
                }
                if (queryFormat != null) {
                    projectQuery.setQueryFormat(queryFormat);
                    hasChanged = true;
                }
                if (label != null) {
                    projectQuery.setLabel(label);
                    hasChanged = true;
                }
                if (description != null) {
                    projectQuery.setDescription(description);
                    hasChanged = true;
                }
                if (outputFormat != null) {
                    projectQuery.setOutputFormat(outputFormat);
                    hasChanged = true;
                }
                if (templateId != null) {
                    projectQuery.setTemplateId(templateId);
                    hasChanged = true;
                }
                if (humanReadable != null) {
                    projectQuery.setHumanReadable(humanReadable);
                    hasChanged = true;
                }
                if (explorerUrl != null) {
                    projectQuery.setExplorerUrl(explorerUrl);
                    hasChanged = true;
                }
                if (hasChanged) {
                    queryRepository.save(projectQuery);
                }
            }
        }
    }

}
