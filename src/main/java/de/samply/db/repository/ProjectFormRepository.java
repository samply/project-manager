package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectFormRepository extends JpaRepository<ProjectForm, Long> {

    List<ProjectForm> findByProject(Project project);

    Optional<ProjectForm> findByProjectAndFormTitle(Project project, String formTitle);

}
