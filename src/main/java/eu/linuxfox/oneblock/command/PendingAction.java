package eu.linuxfox.oneblock.command;

public record PendingAction(
        ActionType type,
        int gridX,
        int gridZ,
        long expiresAt
) {

    public enum ActionType {
        DELETE_OWN_ISLAND,
        ADMIN_DELETE_ISLAND
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}