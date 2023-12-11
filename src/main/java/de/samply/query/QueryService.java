package de.samply.query;

import de.samply.db.model.Query;
import de.samply.db.repository.QueryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class QueryService {

    private final QueryRepository queryRepository;

    public QueryService(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public void createQuery(String query, QueryFormat queryFormat) {
        Query tempQuery = new Query();
        tempQuery.setQuery(query);
        tempQuery.setQueryFormat(queryFormat);
        tempQuery.setCreatedAt(LocalDate.now());
        this.queryRepository.save(tempQuery);
    }

}
