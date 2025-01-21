package de.samply.email;

import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.repository.*;
import de.samply.frontend.FrontendService;
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

    public EmailKeyValuesFactory(FrontendService frontendService,
                                 EmailContext emailContext,
                                 ProjectBridgeheadRepository projectBridgeheadRepository,
                                 ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 BridgeheadConfiguration bridgeheadConfiguration,
                                 ProjectDocumentRepository projectDocumentRepository,
                                 BridgeheadAdminUserRepository bridgeheadAdminUserRepository) {
        this.frontendService = frontendService;
        this.emailContext = emailContext;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.projectDocumentRepository = projectDocumentRepository;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
    }

    public EmailKeyValues newInstance() {
        return new EmailKeyValues(frontendService, emailContext, projectBridgeheadRepository, projectRepository,
                userRepository, bridgeheadConfiguration, projectDocumentRepository, bridgeheadAdminUserRepository);
    }

}
