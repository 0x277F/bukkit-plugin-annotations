package com.torchmind.minecraft.annotation.test;

import com.torchmind.minecraft.annotation.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

/**
 * Showcases command annotations outside of main class
 */
@com.torchmind.minecraft.annotation.command.Command(name = "testext", aliases = "testext2", permission = "test.testext", permissionMessage = "Oopsy!", usage = "/testext test test")
@Permission(name = "test.testext", description = "Provides access to /textext command", defaultValue = PermissionDefault.TRUE)
public class CommandTest implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return true;
    }
}
