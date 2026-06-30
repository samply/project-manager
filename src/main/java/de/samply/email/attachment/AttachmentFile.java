package de.samply.email.attachment;

import java.util.Optional;

public enum AttachmentFile {

    FORM {
        @Override
        public Optional<AttachmentExtra> parseExtra(String extra) {
            return Optional.of(new FormExtra(extra));
        }
    };

    public Optional<AttachmentExtra> parseExtra(String extra) {
        return Optional.empty();
    }

    public static Optional<ParsedAttachment> parse(String value) {

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String[] parts = value.split("-", 2);

        return fromString(parts[0])
                .map(type -> {
                    Optional<AttachmentExtra> extra =
                            parts.length > 1
                                    ? type.parseExtra(parts[1])
                                    : Optional.empty();

                    return new ParsedAttachment(type, extra);
                });
    }

    private static Optional<AttachmentFile> fromString(String value) {
        try {
            return Optional.of(
                    AttachmentFile.valueOf(value)
            );
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}