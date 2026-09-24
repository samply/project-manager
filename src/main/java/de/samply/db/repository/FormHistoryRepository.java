package de.samply.db.repository;

import de.samply.db.model.FormHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormHistoryRepository extends JpaRepository<FormHistory, Long> {

    Optional<FormHistory> findFirstByFormTitleOrderByVersionDesc(String formTitle);

    @Query("SELECT DISTINCT f.formTitle FROM FormHistory f")
    List<String> findAllFormTitles();

}
