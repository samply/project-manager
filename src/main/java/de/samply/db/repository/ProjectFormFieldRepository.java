package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFormFieldRepository extends JpaRepository<ProjectFormField, Long> {

    List<ProjectFormField> findByProject(Project project);

    List<ProjectFormField> findByProjectAndFormTitle(Project project, String formTitle);

    void deleteProjectFormFieldByProjectAndFormTitleAndLabelAndBlockInstance(Project project, String formTitle, String label, Integer blockInstance);

    void deleteProjectFormFieldByProjectAndFormTitleAndLabelAndBlockInstanceAndFieldInstance(Project project, String formTitle, String label, Integer blockInstance, Integer fieldInstance);

    // Whether any project has a non-blank value for this field.
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM ProjectFormField f "
            + "WHERE f.formTitle = :formTitle AND f.label = :label AND f.value IS NOT NULL AND TRIM(f.value) <> ''")
    boolean existsValue(@Param("formTitle") String formTitle, @Param("label") String label);

    // Whether any project has a non-blank value for any field of this form.
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM ProjectFormField f "
            + "WHERE f.formTitle = :formTitle AND f.value IS NOT NULL AND TRIM(f.value) <> ''")
    boolean existsValueInForm(@Param("formTitle") String formTitle);

}
