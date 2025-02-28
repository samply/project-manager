package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.repository.*;
import de.samply.frontend.FrontendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailKeyValuesFactory {

    private final FrontendService frontendService;
    private final EmailContext emailContext;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;

    private final String researchEnvironmentUrl;

    public EmailKeyValuesFactory(FrontendService frontendService,
                                 EmailContext emailContext,
                                 ProjectBridgeheadRepository projectBridgeheadRepository,
                                 ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 BridgeheadConfiguration bridgeheadConfiguration,
                                 ProjectDocumentRepository projectDocumentRepository,
                                 BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                                 @Value(ProjectManagerConst.CODER_BASE_URL_SV) String researchEnvironmentUrl) {
        this.frontendService = frontendService;
        this.emailContext = emailContext;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.projectDocumentRepository = projectDocumentRepository;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.researchEnvironmentUrl = researchEnvironmentUrl;
    }

    public EmailKeyValues newInstance() {
        return new EmailKeyValues(
                frontendService, emailContext, projectBridgeheadRepository, projectRepository,
                userRepository, bridgeheadConfiguration, projectDocumentRepository,
                bridgeheadAdminUserRepository, researchEnvironmentUrl);
    }

}
