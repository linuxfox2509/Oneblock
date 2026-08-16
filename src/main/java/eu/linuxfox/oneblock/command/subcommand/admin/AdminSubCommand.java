package eu.linuxfox.oneblock.command.subcommand.admin;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface AdminSubCommand {

    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    String getPermission();
}
