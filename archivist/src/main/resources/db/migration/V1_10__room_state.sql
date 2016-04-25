
/*
 * An object representing the shared state of given room.
 */
ALTER TABLE room ADD json_search TEXT NOT NULL;

/*
 * An object representing the shared state of given room.
 */
ALTER TABLE room ADD json_selection TEXT NOT NULL;

/*
 * Every time the state of a room changes (selection, query) the version is incremented.
 */
ALTER TABLE room ADD int_version INTEGER NOT NULL DEFAULT 0;
