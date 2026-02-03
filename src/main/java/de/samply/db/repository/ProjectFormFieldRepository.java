package de.samply.db.repository;

import de.samply.db.model.ProjectFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFormFieldRepository extends JpaRepository<ProjectFormField, Long> {

    List<ProjectFormField> findByProject_Code(String projectCode);

    List<ProjectFormField> findByProject_CodeAndFormTitle(String projectCode, String formTitle);

}
