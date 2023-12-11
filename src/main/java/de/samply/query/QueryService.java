package de.samply.query;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Query;
import de.samply.db.repository.QueryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class QueryService {

    private final QueryRepository queryRepository;

    public QueryService(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public String createQuery(String query, QueryFormat queryFormat) {
        Query tempQuery = new Query();
        tempQuery.setCode(generateQueryCode());
        tempQuery.setQuery(query);
        tempQuery.setQueryFormat(queryFormat);
        tempQuery.setCreatedAt(LocalDate.now());
        tempQuery = this.queryRepository.save(tempQuery);
        return tempQuery.getCode();
    }

    private String generateQueryCode(){
        return UUID.randomUUID().toString().substring(0, ProjectManagerConst.QUERY_CODE_SIZE);
    }

}
