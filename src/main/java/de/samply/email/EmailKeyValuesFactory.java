package de.samply.email;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.document.DocumentService;
import de.samply.frontend.FrontendService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.researchenvironment.ResearchEnvironmentService;
import de.samply.user.UserService;
import org.springframework.stereotype.Component;

@Component
public class EmailKeyValuesFactory {

    // Services
    private final FrontendService frontendService;
    private final DocumentService documentService;
    private final UserService userService;
    private final ProjectBridgeheadService projectBridgeheadService;

    private final EmailContext emailContext;
    private final BridgeheadsConfiguration bridgeheadsConfiguration;

    private final ResearchEnvironmentService researchEnvironmentService;

    public EmailKeyValuesFactory(FrontendService frontendService, DocumentService documentService,
                                 EmailContext emailContext,
                                 UserService userService, ProjectBridgeheadService projectBridgeheadService,
                                 BridgeheadsConfiguration bridgeheadsConfiguration,
                                 ResearchEnvironmentService researchEnvironmentService) {
        this.frontendService = frontendService;
        this.documentService = documentService;
        this.emailContext = emailContext;
        this.userService = userService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.researchEnvironmentService = researchEnvironmentService;
    }

    public EmailKeyValues newInstance() {
        return new EmailKeyValues(
                frontendService, emailContext, documentService,
                userService, bridgeheadsConfiguration,
                researchEnvironmentService.fetchResearchEnvironmentUrl().orElse(null),
                projectBridgeheadService);
    }

}
