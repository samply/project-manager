package de.samply.query;

import de.samply.db.repository.QueryRepository;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final QueryRepository queryRepository;

    public QueryService(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }


}
