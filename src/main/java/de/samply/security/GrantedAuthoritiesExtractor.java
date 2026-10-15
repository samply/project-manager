package de.samply.security;

import de.samply.user.roles.UserOrganisationRoles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class GrantedAuthoritiesExtractor {


    private final GroupToRoleMapper groupToRoleMapper;

    public GrantedAuthoritiesExtractor(GroupToRoleMapper groupToRoleMapper) {
        this.groupToRoleMapper = groupToRoleMapper;
    }


    // The organisation roles are collected in userOrganisationRoles, which the caller assigns to the session user
    // at once: parallel requests of the same session must never see a half-built set of roles.
    public Collection<GrantedAuthority> extractAuthoritiesFromGroups(Collection<String> groups, UserOrganisationRoles userOrganisationRoles) throws OAuth2AuthenticationException {
        return groups.stream()
                .map(group -> groupToRoleMapper.getRoleFromGroup(group, userOrganisationRoles))
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(role.name()))// change me
                .collect(Collectors.toList());
    }

}
