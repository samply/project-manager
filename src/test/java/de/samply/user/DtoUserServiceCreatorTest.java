package de.samply.user;

import de.samply.db.model.User;
import de.samply.frontend.dto.DtoFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtoUserServiceCreatorTest {

    @Test
    void convertsAndSortsCreatorsUsingFrontendUserDto() {
        UserService userService = mock(UserService.class);
        when(userService.fetchProjectCreators(Optional.empty()))
                .thenReturn(new LinkedHashSet<>(Set.of("z@example.org", "a@example.org")));
        User namedUser = new User(1L, "z@example.org", "Zoe", "Zimmer", false);
        when(userService.fetchUser("z@example.org")).thenReturn(Optional.of(namedUser));
        when(userService.fetchUser("a@example.org")).thenReturn(Optional.empty());

        Set<de.samply.frontend.dto.User> creators =
                new DtoUserService(userService, mock(DtoFactory.class)).fetchProjectCreators(Optional.empty());

        assertThat(creators).extracting(de.samply.frontend.dto.User::email)
                .containsExactly("a@example.org", "z@example.org");
        assertThat(creators).filteredOn(user -> user.email().equals("z@example.org"))
                .singleElement()
                .extracting(de.samply.frontend.dto.User::firstName)
                .isEqualTo("Zoe");
    }
}
