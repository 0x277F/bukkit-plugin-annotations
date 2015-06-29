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
package com.torchmind.minecraft.annotation.processor;

import com.torchmind.minecraft.annotation.Plugin;
import com.torchmind.minecraft.annotation.command.Command;
import com.torchmind.minecraft.annotation.command.Commands;
import com.torchmind.minecraft.annotation.dependency.Dependencies;
import com.torchmind.minecraft.annotation.dependency.LoadBeforePlugins;
import com.torchmind.minecraft.annotation.dependency.SoftDependencies;
import com.torchmind.minecraft.annotation.permission.ChildPermission;
import com.torchmind.minecraft.annotation.permission.Permission;
import com.torchmind.minecraft.annotation.permission.Permissions;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Processes annotations provided by the {@link com.torchmind.minecraft.annotation} package.
 * @author Johannes Donath
 */
@SupportedSourceVersion (SourceVersion.RELEASE_8)
@SupportedAnnotationTypes ({
                                   "com.torchmind.minecraft.annotation.Plugin",
                                   "com.torchmind.minecraft.annotation.command.*",
                                   "com.torchmind.minecraft.annotation.dependency.*",
                                   "com.torchmind.minecraft.annotation.permission.*"
                           })
public class PluginAnnotationProcessor extends AbstractProcessor {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat ("MM/DD/yyyy HH:mm:ss");
        private boolean pluginMainElementLocated = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                // for sanity reasons we will need to verify whether there is more than one class annotated with our
                // @Plugin annotation. Luckily we can combine this check with our search for the main plugin class
                // as Java does not seem to sanely provide us with those.
                Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith (Plugin.class);

                if (annotatedElements.size () > 1) {
                        this.raiseError ("Found more than one plugin class.");
                        return false;
                }

                // make sure we found at least one instance of @Plugin before actually generating the plugin metadata
                // Note: Stopping silently might not be the sanest choice here however this might be a saner solution
                // than raising a warning as plugins may choose to depend on another plugin that utilizes this processor.
                if (annotatedElements.size () == 0) return false;

                if (this.pluginMainElementLocated) {
                        this.raiseError ("The plugin class has already been located.");
                        return false;
                }

                Element mainPluginElement = annotatedElements.iterator ().next ();
                this.pluginMainElementLocated = true;

                if (!(mainPluginElement instanceof TypeElement)) {
                        this.raiseError ("Element annotated with @Plugin is not a type!");
                        return false;
                }

                TypeElement mainPluginType = ((TypeElement) mainPluginElement);

                // due to the fact that we cannot instantiate non-static inner classes we will have to check whether the
                // annotated class is encapsulated in a package (top level class) or marked as static (inner class).
                if (!(mainPluginType.getEnclosingElement () instanceof PackageElement) && !mainPluginType.getModifiers ().contains (Modifier.STATIC)) {
                        this.raiseError ("Plugin class needs to be a top level class or a static inner class.");
                        return false;
                }

                // to protect developers from doing even more stupid things than trying to instantiate a non-static inner
                // class, we will also verify whether the class extends Bukkit's JavaPlugin (which is required here anyways
                // as it is the only plugin loader implementation that uses the plugin.yml metadata file which we will
                // use here until Bukkit gets it's own annotation based system).
                if (!this.processingEnv.getTypeUtils ().isSubtype (mainPluginType.asType (), this.processingEnv.getElementUtils ().getTypeElement (JavaPlugin.class.getName ()).asType ())) {
                        this.raiseError ("Plugin class does not extend org.bukkit.plugin.java.JavaPlugin.");
                        return false;
                }

                // retrieve the plugin manifest (if present) to allow automatic insertion of values like the plugin's
                // name and version via the MANIFEST_VALUE magical value.
                Manifest manifest = this.retrieveManifest ();

                // as of now we'll set up a small little map containing the chosen nodes that will be inserted into
                // the plugin.yml (given that their value differs from Bukkit's default values). This might not be the
                // best way of generating the plugin metadata however this system is not as messy as an object based
                // method would be.
                Map<String, Object> plugin = new HashMap<> ();

                Plugin pluginAnnotation = mainPluginType.getAnnotation (Plugin.class);
                plugin.put ("main", mainPluginType.getQualifiedName ().toString ());

                // make sure the manifest is present when using the magical value (see retrieveManifest () for a couple
                // of reasons why this could technically fail - I'm too lazy to explain this again).
                if (Plugin.MANIFEST_VALUE.equals (pluginAnnotation.name ()) || Plugin.MANIFEST_VALUE.equals (pluginAnnotation.version ())) {
                        if (manifest == null) {
                                this.raiseError ("Cannot locate or read manifest! Either provide a manifest or use hardcoded values.");
                                return false;
                        }

                        Attributes attributes = manifest.getMainAttributes ();

                        if (Plugin.MANIFEST_VALUE.equals (pluginAnnotation.name ())) {
                                if (!attributes.containsKey ("Implementation-Title")) {
                                        this.raiseError ("Implementation-Title is not specified in plugin manifest.");
                                        return false;
                                }

                                plugin.put ("name", attributes.getValue ("Implementation-Title"));
                        }

                        if (Plugin.MANIFEST_VALUE.equals (pluginAnnotation.version ())) {
                                if (!attributes.containsKey ("Implementation-Version")) {
                                        this.raiseError ("Implementation-Version is not specified in plugin manifest");
                                        return false;
                                }

                                plugin.put ("version", attributes.getValue ("Implementation-Version"));
                        }
                }

                if (!plugin.containsKey ("name")) plugin.put ("name", pluginAnnotation.name ());
                if (!plugin.containsKey ("version")) plugin.put ("version", pluginAnnotation.version ());
                if (!"".equals (pluginAnnotation.description ())) plugin.put ("description", pluginAnnotation.description ());
                if (PluginLoadOrder.POSTWORLD != pluginAnnotation.load ()) plugin.put ("load", pluginAnnotation.load ().toString ());

                if (pluginAnnotation.author ().length == 1)
                        plugin.put ("author", pluginAnnotation.author ()[0]);
                else if (pluginAnnotation.author ().length > 1)
                        plugin.put ("authors", pluginAnnotation.author ());

                if (!"".equals (pluginAnnotation.website ())) plugin.put ("website", pluginAnnotation.website ());
                if (pluginAnnotation.database ()) plugin.put ("database", pluginAnnotation.database ());
                if (!"".equals (pluginAnnotation.prefix ())) plugin.put ("prefix", pluginAnnotation.prefix ());

                Dependencies dependencies = mainPluginType.getAnnotation (Dependencies.class);

                if (dependencies != null) {
                        String[] pluginDependencies = new String[dependencies.value ().length];
                        for (int i = 0; i < pluginDependencies.length; i++) pluginDependencies[i] = dependencies.value ()[i].value ();

                        plugin.put ("depend", pluginDependencies);
                }

                LoadBeforePlugins loadBeforePlugins = mainPluginType.getAnnotation (LoadBeforePlugins.class);

                if (loadBeforePlugins != null) {
                        String[] loadBefore = new String[loadBeforePlugins.value ().length];
                        for (int i = 0; i < loadBefore.length; i++) loadBefore[i] = loadBeforePlugins.value ()[i].value ();

                        plugin.put ("loadbefore", loadBefore);
                }

                SoftDependencies softDependencies = mainPluginType.getAnnotation (SoftDependencies.class);

                if (softDependencies != null) {
                        String[] pluginDependencies = new String[softDependencies.value ().length];
                        for (int i = 0; i < pluginDependencies.length; i++) pluginDependencies[i] = softDependencies.value ()[i].value ();

                        plugin.put ("softdepend", pluginDependencies);
                }

                Commands commands = mainPluginType.getAnnotation (Commands.class);
                if (commands != null) plugin.put ("commands", this.processCommands (commands));

                Permissions permissions = mainPluginType.getAnnotation (Permissions.class);
                if (permissions != null) plugin.put ("permissions", this.processPermissions (permissions));

                Yaml yaml = new Yaml ();

                try {
                        FileObject descriptorFile = this.processingEnv.getFiler ().createResource (StandardLocation.CLASS_OUTPUT, "", "plugin.yml");

                        try (Writer writer = descriptorFile.openWriter ()) {
                                writer.append ("# Plugin descriptor automatically generated at " + DATE_FORMAT.format (new Date ()) + ".\n");
                                yaml.dump (plugin, writer);
                        }

                        return true;
                } catch (IOException ex) {
                        throw new RuntimeException ("Cannot serialize plugin descriptor: " + ex.getMessage (), ex);
                }
        }

        /**
         * Processes a single command.
         * @param commandAnnotation The annotation.
         * @return The generated command metadata.
         */
        protected Map<String, Object> processCommand (Command commandAnnotation) {
                Map<String, Object> command = new HashMap<> ();

                if (commandAnnotation.aliases ().length == 1)
                        command.put ("aliases", commandAnnotation.aliases ()[0]);
                else if (commandAnnotation.aliases ().length > 1)
                        command.put ("aliases", commandAnnotation.aliases ());

                if (!"".equals (commandAnnotation.description ())) command.put ("description", commandAnnotation.description ());
                if (!"".equals (commandAnnotation.permission ())) command.put ("permission", commandAnnotation.permission ());
                if (!"".equals (commandAnnotation.permissionMessage ())) command.put ("permission-message", commandAnnotation.permissionMessage ());
                if (!"".equals (commandAnnotation.usage ())) command.put ("usage", commandAnnotation.usage ());

                return command;
        }

        /**
         * Processes a set of commands.
         * @param commands The annotation.
         * @return The generated command metadata.
         */
        protected Map<String, Map<String, Object>> processCommands (Commands commands) {
                Map<String, Map<String, Object>> commandList = new HashMap<> ();
                for (Command command : commands.value ()) commandList.put (command.name (), this.processCommand (command));
                return commandList;
        }

        /**
         * Processes a command.
         * @param permissionAnnotation The annotation.
         * @return The generated permission metadata.
         */
        protected Map<String, Object> processPermission (Permission permissionAnnotation) {
                Map<String, Object> permission = new HashMap<> ();

                if (!"".equals (permissionAnnotation.description ())) permission.put ("description", permissionAnnotation.description ());
                if (PermissionDefault.OP != permissionAnnotation.defaultValue ()) permission.put ("default", permissionAnnotation.defaultValue ().toString ().toLowerCase ());

                if (permissionAnnotation.children ().length > 0) {
                        Map<String, Boolean> childrenList = new HashMap<> ();
                        for (ChildPermission childPermission : permissionAnnotation.children ()) childrenList.put (childPermission.value (), childPermission.inherit ());
                        permission.put ("children", childrenList);
                }

                return permission;
        }

        /**
         * Processes a set of permissions.
         * @param permissions The annotation.
         * @return The generated permission metadata.
         */
        protected Map<String, Map<String, Object>> processPermissions (Permissions permissions) {
                Map<String, Map<String, Object>> permissionList = new HashMap<> ();
                for (Permission permission : permissions.value ()) permissionList.put (permission.name (), this.processPermission (permission));
                return permissionList;
        }

        /**
         * Raises a processor error.
         * @param message The error message.
         */
        protected void raiseError (String message) {
                this.processingEnv.getMessager ().printMessage (Diagnostic.Kind.ERROR, message);
        }

        /**
         * Retrieves the plugin manifest (if present).
         * @return The manifest.
         */
        protected Manifest retrieveManifest () {
                try {
                        FileObject object = this.processingEnv.getFiler ().getResource (StandardLocation.SOURCE_PATH, "", "META-INF/MANIFEST.MF");

                        try (InputStream manifestStream = object.openInputStream ()) {
                                return new Manifest (manifestStream);
                        }
                } catch (FileNotFoundException ex) {
                        // we will just entirely ignore the error if locating the class was not possible. The upcoming
                        // checks within the process method will take care of the problem if (and only if) the user tries
                        // to use the MANIFEST_VALUE magical value without having a manifest present.
                        // Keep in mind though that this can cause issues on different compiler implementations as they
                        // may not support StandardLocation.SOURCE_PATH as one of their locations and thus this check
                        // will always fail.
                        return null;
                } catch (IOException ex) {
                        this.raiseError ("Cannot access plugin manifest: " + ex.getMessage ());
                        ex.printStackTrace ();
                        return null;
                }
        }
}
