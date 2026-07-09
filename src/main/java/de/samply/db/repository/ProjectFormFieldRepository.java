package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFormFieldRepository extends JpaRepository<ProjectFormField, Long> {

    List<ProjectFormField> findByProject(Project project);

    List<ProjectFormField> findByProjectAndFormTitle(Project project, String formTitle);

    void deleteProjectFormFieldByProjectAndFormTitleAndLabelAndBlockInstance(Project project, String formTitle, String label, Integer blockInstance);

}
