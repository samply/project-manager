package de.samply.query;

import de.samply.db.model.Query;

public record QueryChangedEvent(Query query) {
}
