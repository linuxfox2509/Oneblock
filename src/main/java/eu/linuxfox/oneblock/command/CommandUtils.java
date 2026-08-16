package eu.linuxfox.oneblock.command;

public final class CommandUtils {

    private CommandUtils() {}

    public static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}