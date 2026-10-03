package SRC.model;

/**
 * Encapsulates the details of an undoable administrative operation.
 * Used by the UndoService stack to revert the most recent changes.
 */
public class UndoOperation {

    /**
     * Types of administrative actions that can be undone.
     */
    public enum Type {
        ADD_BOOK,
        UPDATE_BOOK,
        DELETE_BOOK,
        REGISTER_STUDENT
    }

    private final Type type;
    private final Object entityId;       // The unique ID of the affected book or student
    private final Object previousState;   // A snapshot of the entity state before the action (null for ADD)
    private final Object currentState;    // A snapshot of the entity state after the action (null for DELETE)

    public UndoOperation(Type type, Object entityId, Object previousState, Object currentState) {
        this.type = type;
        this.entityId = entityId;
        this.previousState = previousState;
        this.currentState = currentState;
    }

    public Type getType() {
        return type;
    }

    public Object getEntityId() {
        return entityId;
    }

    public Object getPreviousState() {
        return previousState;
    }

    public Object getCurrentState() {
        return currentState;
    }

    @Override
    public String toString() {
        return "UndoOperation{" +
                "type=" + type +
                ", entityId=" + entityId +
                '}';
    }
}
