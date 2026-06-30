package de.samply.utils;

import de.samply.db.model.User;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserUtils {

    public static String extractFullName(Optional<User> optionalUser){
        return optionalUser.map(user -> Stream.of(user.getFirstName(), user.getLastName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(" ")))
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

}
