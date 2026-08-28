package de.samply.user;

import de.samply.db.model.User;
import de.samply.db.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailingBlacklistFileImporterTest {

    @Test
    void createsAndBlacklistsConfiguredUsers(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("mailing-black-list.txt");
        Files.writeString(file, "# managers\n manager@example.org # no notifications\nmanager@example.org\ninvalid\n");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail("manager@example.org")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new MailingBlacklistFileImporter(repository, file)
                .run(new DefaultApplicationArguments());

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("manager@example.org");
        assertThat(captor.getValue().isInMailingBlackList()).isTrue();
    }

    @Test
    void doesNotWriteAlreadyBlacklistedUsers(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("mailing-black-list.txt");
        Files.writeString(file, "manager@example.org\n");
        User existing = new User(1L, "manager@example.org", "Manager", "Example", true);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));

        new MailingBlacklistFileImporter(repository, file)
                .run(new DefaultApplicationArguments());

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void failsWhenConfiguredFileCannotBeRead(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> new MailingBlacklistFileImporter(mock(UserRepository.class), missing)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to read mailing blacklist file");
    }
}
