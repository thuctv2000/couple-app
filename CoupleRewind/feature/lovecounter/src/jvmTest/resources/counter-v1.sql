CREATE TABLE IF NOT EXISTS `local_spaces` (`slot` INTEGER NOT NULL, `localOwnerId` TEXT NOT NULL, `firebaseUid` TEXT, PRIMARY KEY(`slot`))
CREATE TABLE IF NOT EXISTS `counter_settings` (`localOwnerId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `partnerName` TEXT NOT NULL, `startDate` TEXT NOT NULL, `timeZoneId` TEXT NOT NULL, `localRevision` INTEGER NOT NULL, PRIMARY KEY(`localOwnerId`))
CREATE TABLE IF NOT EXISTS `counter_outbox` (`operationId` TEXT NOT NULL, `localOwnerId` TEXT NOT NULL, `firebaseUid` TEXT, `baseServerVersion` INTEGER, `localRevision` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `partnerName` TEXT NOT NULL, `startDate` TEXT NOT NULL, `timeZoneId` TEXT NOT NULL, `payloadVersion` INTEGER NOT NULL, PRIMARY KEY(`operationId`))
CREATE INDEX IF NOT EXISTS `index_counter_outbox_localOwnerId` ON `counter_outbox` (`localOwnerId`)
CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)
INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4a61c798d7cf9070c5bede7326d0d57d')
PRAGMA user_version = 1
