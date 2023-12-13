package de.samply.token;


import de.samply.security.SessionUser;
import org.springframework.stereotype.Service;

@Service
public class TokenManagerService {

    private final SessionUser sessionUser;

    public TokenManagerService(SessionUser sessionUser) {
        this.sessionUser = sessionUser;
    }

    public String fetchAuthenticationScript(String projectCode, String bridgehead) {
        //TODO
        return "TODO: Authentication for DataSHIELD";
    }

}
