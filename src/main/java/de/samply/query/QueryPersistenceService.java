package de.samply.query;

import de.samply.db.model.Query;
import de.samply.db.model.QueryOutput;
import de.samply.db.repository.QueryRepository;
import de.samply.db.utils.QueryMapper;
import de.samply.db.utils.QueryOutputMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QueryPersistenceService {

    // Query Repository only used for saving queries within a transaction
    private final QueryRepository queryRepository;

    private final QueryMapper queryMapper;
    private final QueryOutputMapper queryOutputMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    public QueryPersistenceService(
            QueryRepository queryRepository,
            QueryMapper queryMapper,
            QueryOutputMapper queryOutputMapper,
            ApplicationEventPublisher applicationEventPublisher) {
        this.queryRepository = queryRepository;
        this.queryMapper = queryMapper;
        this.queryOutputMapper = queryOutputMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Saves or updates a Query entity, including its associated QueryOutputs.
     *
     * <p><b>Important:</b> For updates, the QueryOutputs collection is NOT replaced via
     * clear()+addAll(). Instead, it is merged (diff-based) to comply with JPA/Hibernate
     * lifecycle rules and database constraints.</p>
     *
     * <p>Why this is necessary:
     * <ul>
     *   <li>The QueryOutput table has a unique constraint on (query_id, project_type).</li>
     *   <li>A naive replacement (clear and re-insert) may cause INSERTs to be executed
     *       before DELETEs during a flush, leading to constraint violations.</li>
     *   <li>Hibernate tracks changes on managed entities; replacing collections breaks
     *       this mechanism and can lead to detached entity or duplicate key issues.</li>
     * </ul>
     *
     * <p>What this method does instead:
     * <ul>
     *   <li>Updates scalar fields using MapStruct.</li>
     *   <li>Synchronizes QueryOutputs:
     *     <ul>
     *       <li>Adds new outputs</li>
     *       <li>Updates existing ones (by projectType)</li>
     *       <li>Removes obsolete ones (triggering orphanRemoval)</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>All changes are applied within a transaction. Hibernate dirty checking ensures
     * that only necessary SQL operations are executed on commit.</p>
     *
     * <p>After persistence, a QueryChangedEvent is published to trigger downstream updates.</p>
     */
    @Transactional
    public Query saveQuery(Query query) {

        Query result = (query.getId() == null)
                ? queryRepository.save(query)
                : queryRepository.findByCode(query.getCode())
                  .map(managed -> {

                      // update scalar fields
                      queryMapper.updateQuery(query, managed);

                      // --- merge outputs (no clear!) ---
                      var existing = managed.getOutputs().stream()
                              .collect(Collectors.toMap(QueryOutput::getProjectType, Function.identity()));

                      var incoming = Optional.ofNullable(query.getOutputs())
                              .orElseGet(Set::of);

                      var incomingTypes = incoming.stream()
                              .map(QueryOutput::getProjectType)
                              .collect(Collectors.toSet());

                      // add or update
                      incoming.forEach(in -> {
                          var current = existing.get(in.getProjectType());

                          if (current == null) {
                              managed.addOutput(queryOutputMapper.toEntity(in));
                          } else {
                              current.setOutputFormat(in.getOutputFormat());
                              current.setTemplateId(in.getTemplateId());
                          }
                      });

                      // remove obsolete
                      managed.getOutputs().removeIf(o -> !incomingTypes.contains(o.getProjectType()));

                      return managed;
                  })
                  .orElseThrow(() ->
                               new RuntimeException("Query with code " + query.getCode() + " does not exist")
                  );

        applicationEventPublisher.publishEvent(new QueryChangedEvent(result.getId()));

        return result;
    }

}
