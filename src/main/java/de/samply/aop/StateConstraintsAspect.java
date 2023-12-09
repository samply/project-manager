package de.samply.aop;

import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.utils.AspectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Aspect
public class StateConstraintsAspect {

    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    public StateConstraintsAspect(ProjectRepository projectRepository,
                                  ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    @Pointcut("@annotation(de.samply.annotations.StateConstraints)")
    public void stateConstraintsPointcut() {
    }

    @Around("stateConstraintsPointcut()")
    public Object aroundStateConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        StateConstraints stateConstraints = fetchStateConstrains(joinPoint);

        Optional<String> projectName = AspectUtils.fetchProjectName(joinPoint);
        if (projectName.isEmpty() || projectName.get().length() == 0) {
            return ResponseEntity.badRequest().body("Project name not provided");
        }
        Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectName);
        if (project.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (stateConstraints.projectStates().length > 0) {
            boolean hasAnyProjectStateConstraint = false;
            for (ProjectState projectState : stateConstraints.projectStates()) {
                if (project.get().getState() == projectState) {
                    hasAnyProjectStateConstraint = true;
                    break;
                }
            }
            if (!hasAnyProjectStateConstraint) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }
        }

        if (stateConstraints.projectBridgeheadStates().length > 0) {
            boolean hasAnyProjectBridgeheadState = false;
            Optional<ProjectBridgehead> projectBridgehead = fetchProjectBridgehead(project.get(), AspectUtils.fetchBridghead(joinPoint));
            if (projectBridgehead.isEmpty()) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }
            boolean hasAnyProjectBridgeheadStateConstraint = false;
            for (ProjectBridgeheadState projectBridgeheadState : stateConstraints.projectBridgeheadStates()) {
                if (projectBridgehead.get().getState() == projectBridgeheadState) {
                    hasAnyProjectBridgeheadStateConstraint = true;
                    break;
                }
            }
            if (!hasAnyProjectBridgeheadStateConstraint) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }
        }
        return joinPoint.proceed();
    }

    private StateConstraints fetchStateConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchMethod(joinPoint).getAnnotation(StateConstraints.class);
    }

    private Optional<ProjectBridgehead> fetchProjectBridgehead(Project project, Optional<String> bridgehead) {
        return (bridgehead.isEmpty()) ? Optional.empty() : this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead.get(), project);
    }


}
