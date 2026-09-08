package de.samply.aop;

import de.samply.annotations.ProjectConstraints;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.document.DocumentService;
import de.samply.utils.AspectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Aspect
public class ProjectConstraintsAspect {

    private final ConstraintsService constraintsService;
    private final DocumentService documentService;

    public ProjectConstraintsAspect(ConstraintsService constraintsService, DocumentService documentService) {
        this.constraintsService = constraintsService;
        this.documentService = documentService;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.ProjectConstraints)")
    public void projectConstraintsPointcut() {
    }

    @Around("projectConstraintsPointcut()")
    public Object aroundProjectConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<ProjectConstraints> projectConstraints = fetchProjectConstrains(joinPoint);
        Optional<Project> project = AspectUtils.fetchProject(joinPoint);
        if (projectConstraints.map(ProjectConstraints::documentCreatorOrProjectManagerAdmin).orElse(false)) {
            Optional<Long> documentId = AspectUtils.fetchRequestParameter(
                    joinPoint, ProjectManagerConst.DOCUMENT_ID, Long.class);
            if (documentId.isPresent() && (project.isEmpty() ||
                    !documentService.isDocumentCreatorOrProjectManagerAdmin(project.get(), documentId.get()))) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED).build();
            }
        }
        @SuppressWarnings("rawtypes") Optional<ResponseEntity> result = this.constraintsService.checkProjectConstraints(projectConstraints, project);
        return (result.isEmpty()) ? joinPoint.proceed() : result.get();
    }

    private Optional<ProjectConstraints> fetchProjectConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, ProjectConstraints.class);
    }

}
