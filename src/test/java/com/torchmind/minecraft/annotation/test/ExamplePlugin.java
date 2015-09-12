/*
 * Copyright 2015 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.torchmind.minecraft.annotation.test;

import com.torchmind.minecraft.annotation.Plugin;
import com.torchmind.minecraft.annotation.command.Command;
import com.torchmind.minecraft.annotation.dependency.Dependency;
import com.torchmind.minecraft.annotation.dependency.LoadBefore;
import com.torchmind.minecraft.annotation.dependency.SoftDependency;
import com.torchmind.minecraft.annotation.permission.ChildPermission;
import com.torchmind.minecraft.annotation.permission.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Showcases the annotations.
 *
 * @author Johannes Donath
 */
@Plugin (name = "Test Plugin", version = "0.1.0", description = "This is a test plugin", load = PluginLoadOrder.STARTUP, author = "Akkarin", website = "http://www.example.org", database = true, prefix = "ExamplePlugin")
@Command (name = "test", aliases = "test2", permission = "test.test", permissionMessage = "Oopsy!", usage = "/test test test")
@Command (name = "test3", aliases = "test4", permission = "test.test", permissionMessage = "Oopsy!", usage = "/test test test")
@Dependency ("TestPlugin2")
@Dependency ("TestPlugin3")
@LoadBefore ("TestPlugin4")
@LoadBefore ("TestPlugin5")
@SoftDependency ("TestPlugin6")
@SoftDependency ("TestPlugin7")
@Permission (name = "test", description = "Provides access to all commands.", defaultValue = PermissionDefault.TRUE, children = { @ChildPermission ("test.test"), @ChildPermission (value = "test.notTest", inherit = false) })
@Permission (name = "test.test", description = "Provides access to the test command.", defaultValue = PermissionDefault.TRUE)
public class ExamplePlugin extends JavaPlugin {
}
