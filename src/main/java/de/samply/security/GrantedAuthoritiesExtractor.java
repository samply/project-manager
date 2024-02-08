package de.samply.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class GrantedAuthoritiesExtractor {


    private final GroupToRoleMapper groupToRoleMapper;

    public GrantedAuthoritiesExtractor(GroupToRoleMapper groupToRoleMapper) {
        this.groupToRoleMapper = groupToRoleMapper;
    }


    public Collection<GrantedAuthority> extractAuthoritiesFromGroups(Collection<String> groups) throws OAuth2AuthenticationException {
        return groups.stream()
                .map(groupToRoleMapper::getRoleFromGroup)
                .filter(role -> role != null)
                .map(role -> new SimpleGrantedAuthority(role.name()))// change me
                .collect(Collectors.toList());
    }

}
