package de.samply.email.attachment;

import java.util.Optional;

public record ParsedAttachment(
        AttachmentFile type,
        Optional<AttachmentExtra> extraInfo
) {
}