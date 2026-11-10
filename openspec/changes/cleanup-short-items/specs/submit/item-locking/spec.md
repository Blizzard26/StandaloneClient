# Spec Delta

## MODIFIED Requirements

### Requirement: Command Handler Guards All Mutations

The system SHALL reject mutations to submitted items in the `Activities` command handler by calling `requireNotSubmitted()` before any modification. The affected methods are: `addNewActivity`, `endCurrentActivity`, `removeActivity`, `removeActivityAndCloseGap`, `bulkChangeActivity`, and the day cleanup of short items.

#### Scenario: Block editing submitted item
- **WHEN** a user attempts to edit an item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not modified

#### Scenario: Block deleting submitted item
- **WHEN** a user attempts to delete an item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not deleted

#### Scenario: Block stopping submitted ongoing item
- **WHEN** a user attempts to stop an ongoing item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not stopped

#### Scenario: Block gap-close involving submitted item
- **WHEN** a user attempts to remove an item and close the gap, and either the target item or an adjacent item is submitted
- **THEN** the system throws an `IllegalStateException` and no items are modified

#### Scenario: Block bulk rename involving submitted items
- **WHEN** a user attempts to bulk-rename items and any item in the collection is submitted
- **THEN** the system throws an `IllegalStateException` and no items are modified

#### Scenario: Block day cleanup touching submitted items
- **WHEN** a day cleanup is applied whose planned changes would modify or delete a submitted item (for example because the item was submitted after the preview was shown)
- **THEN** the system throws an `IllegalStateException` and no items are modified

#### Scenario: Resume operations are not blocked
- **WHEN** a user resumes an activity from a submitted item
- **THEN** the operation succeeds because a new item is created and the original is unchanged
