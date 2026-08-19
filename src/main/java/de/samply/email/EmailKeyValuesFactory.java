package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.document.DocumentService;
import de.samply.frontend.FrontendService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.user.UserService;
import org.springframework.beans.factory.annotation.Value;
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

    private final String researchEnvironmentUrl;

    public EmailKeyValuesFactory(FrontendService frontendService, DocumentService documentService,
                                 EmailContext emailContext,
                                 UserService userService, ProjectBridgeheadService projectBridgeheadService,
                                 BridgeheadsConfiguration bridgeheadsConfiguration,
                                 @Value(ProjectManagerConst.CODER_BASE_URL_SV) String researchEnvironmentUrl) {
        this.frontendService = frontendService;
        this.documentService = documentService;
        this.emailContext = emailContext;
        this.userService = userService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.researchEnvironmentUrl = researchEnvironmentUrl;
    }

    public EmailKeyValues newInstance() {
        return new EmailKeyValues(
                frontendService, emailContext, documentService,
                userService, bridgeheadsConfiguration,
                researchEnvironmentUrl,
                projectBridgeheadService);
    }

}
