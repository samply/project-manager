package de.samply.email;

import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.UserRepository;
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

    public EmailKeyValuesFactory(FrontendService frontendService,
                                 EmailContext emailContext,
                                 ProjectBridgeheadRepository projectBridgeheadRepository,
                                 ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 BridgeheadConfiguration bridgeheadConfiguration) {
        this.frontendService = frontendService;
        this.emailContext = emailContext;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
    }

    public EmailKeyValues newInstance() {
        return new EmailKeyValues(frontendService, emailContext, projectBridgeheadRepository, projectRepository,
                userRepository, bridgeheadConfiguration);
    }

}
