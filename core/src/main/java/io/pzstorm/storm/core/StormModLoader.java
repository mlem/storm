/*
 * Zomboid Storm - Java modding toolchain for Project Zomboid
 * Copyright (C) 2021 Matthew Cain
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.pzstorm.storm.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.pzstorm.storm.event.StormEventDispatcher;
import io.pzstorm.storm.mod.ZomboidMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ObjectArrays;

import io.pzstorm.storm.logging.StormLogger;
import io.pzstorm.storm.mod.ModJar;
import io.pzstorm.storm.mod.ModMetadata;
import io.pzstorm.storm.mod.ModVersion;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * This class is responsible for loading mod components:
 * <ul>
 * <li>Catalog mod jars by mapping them to mod directory name.</li>
 * <li>Catalog mod metadata by mapping them to mod directory name.</li>
 * <li>Catalog mod classes by mapping them to mod directory name.</li>
 * <li>Load mod classes with {@link StormClassLoader}.</li>
 * </ul>
 */
public class StormModLoader extends URLClassLoader {

	/**
	 * This catalog contains {@link ModMetadata} instances mapped to directory names.
	 */
	static final Map<String, ModMetadata> METADATA_CATALOG = new HashMap<>();
	/**
	 * This catalog stores {@link Class} instances mapped to directory names.
	 */
	static final Map<String, ImmutableSet<Class<?>>> CLASS_CATALOG = new HashMap<>();
	/**
	 * This catalog stores {@link ModJar} instances mapped to directory names.
	 */
	private static final Map<String, ImmutableSet<ModJar>> JAR_CATALOG = new HashMap<>();


	private static final Map<String, ZomboidMod> MOD_REGISTRY = new HashMap<>();

	StormModLoader(URL[] urls) {
		super(ObjectArrays.concat(urls, getResourcePaths(), URL.class));
	}

	public StormModLoader() {
		super(getResourcePaths());
	}

	/**
	 * <p>Returns an array of paths pointing to resources loaded by {@code StormModLoader}.</p>
	 * This method will return an empty array if no jars are cataloged.
	 */
	private static URL[] getResourcePaths() {

		List<URL> result = new ArrayList<>();
		for (Set<ModJar> modJars : JAR_CATALOG.values())
		{
			for (ModJar modJar : modJars)
			{
				result.add(modJar.getResourcePath());
				try {
					result.add(modJar.getFilePath().getParent().toUri().toURL());
				}
				catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return result.toArray(new URL[0]);
	}

	/**
	 * Before the mods are loaded, the jar catalog should be reset.
	 */
	@SuppressWarnings("unused")
	public static void resetCatalogs() {
		StormLogger.info(String.format("%s mod jar catalog", JAR_CATALOG.isEmpty() ? "Building" : "Rebuilding"));

		// clear map before entering new data
		JAR_CATALOG.clear();


		StormLogger.info(String.format("%s mod metadata catalog", METADATA_CATALOG.isEmpty() ? "Building" : "Rebuilding"));

		// clear map before entering new data
		METADATA_CATALOG.clear();


		StormLogger.info(String.format("%s mod class catalog", CLASS_CATALOG.isEmpty() ? "Building" : "Rebuilding"));

		// clear map before entering new data
		CLASS_CATALOG.clear();

		StormLogger.info(String.format("%s mod registry", MOD_REGISTRY.isEmpty() ? "Building" : "Rebuilding"));

		MOD_REGISTRY.clear();

		StormLogger.info(String.format("Resetting event dispatcher"));

		StormEventDispatcher.reset();
	}

	/**
	 * called from PZ
	 */
	@SuppressWarnings("unused")
	public static void addJarFiles(File modDir) {

		Set<ModJar> modJars = new HashSet<>();

		for (Path modJar : listJarsInDirectory(modDir.toPath())) {
			try {
				modJars.add(new ModJar(modJar.toFile()));
			} catch (IOException e) {
				StormLogger.error("Couldn't load mod " + modDir.getName(), e);
			}
		}
		JAR_CATALOG.put(modDir.getName(), ImmutableSet.copyOf(modJars));

		StormLogger.debug("Created new jar catalog entry:");
		StormLogger.debug(String.format("Found %d jars in mod directory '%s'", modJars.size(), modDir.getName()));
	}

	/**
	 * Returns a {@code Set} of paths that denote jar files in given directory. This method will
	 * not employ recursion when searching for files. Search depth is one directory which
	 * means that only immediate jar files will be included.
	 *
	 * @param modDir {@code Path} that points to directory to search for jar files.
	 *
	 * @throws RuntimeException if an I/O error occurs when opening the directory.
	 */
	private static Set<Path> listJarsInDirectory(Path modDir) {

		try (Stream<Path> stream = Files.walk(modDir, 1)) {
			return stream.filter(p -> p.getFileName().toString().endsWith(".jar")).collect(Collectors.toSet());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 *
	 * @param modEntry the mod name
	 * @param allModFiles all files found in the mod folder
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public static void registerModInfo(String modEntry, List<String> allModFiles) throws IOException {

		Optional<String> firstModInfo = allModFiles.stream().filter(file -> file.endsWith("mod.info")).findFirst();
		File modInfoFile = firstModInfo.map(File::new).orElse(null);
		if(modInfoFile == null) {
			String message = "Unable to find mod.info file in directory '%s'";
			StormLogger.error(String.format(message, modEntry));
			return;
		}
		Properties modInfo = new Properties();
		modInfo.load(new FileInputStream(modInfoFile));
		StormLogger.debug("Found metadata file for entry '" + modEntry + '\'');

		String modName = modInfo.getProperty("name");
		if (Strings.isNullOrEmpty(modName))
		{
			String message = "Unable to register mod in directory '%s' with missing name, check mod.info file";
			StormLogger.error(String.format(message, modEntry));
			return;
		}
		String modVersion = modInfo.getProperty("modversion");
		if (modVersion == null)
		{
			String message = "Unable to register mod '%s' with missing version, check mod.info file";
			StormLogger.error(String.format(message, modName));
			return;
		}
		else if (modVersion.isEmpty())
		{
			String message = "Mod '%s' has empty version property, using 0.1.0 version instead";
			StormLogger.warn(message, modName);
			modVersion = "0.1.0";
		}
		ModMetadata metadata = new ModMetadata(modName, new ModVersion(modVersion));
		METADATA_CATALOG.put(modName, metadata);

		StormLogger.debug("Created new metadata catalog entry: " + metadata);
	}

	/**
	 * Load classes from cataloged {@link ModJar} instances with {@link StormClassLoader}.
	 * Once loaded the classes will also be cataloged by mapping them to directory name.
	 * Before loading classes it is important to populate the jar catalog with
	 * {@link #addJarFiles(File)} ()} method, otherwise this method will only clear the class catalog.
	 */
	public static void loadModClasses() {
		StormLogger.info(String.format("%s mod class catalog", JAR_CATALOG.isEmpty() ? "Building" : "Rebuilding"));

		// clear map before entering new data
		CLASS_CATALOG.clear();

		for (Map.Entry<String, ImmutableSet<ModJar>> entry : JAR_CATALOG.entrySet())
		{
			Set<Class<?>> modClasses = new HashSet<>();
			for (ModJar modJar : entry.getValue())
			{
				Enumeration<JarEntry> jarEntries = modJar.entries();
				while (jarEntries.hasMoreElements())
				{
					JarEntry jarEntry = jarEntries.nextElement();
					if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
						continue;
					}
					String entryName = jarEntry.getName();
					String className = entryName.substring(0, entryName.length() - 6);
					try {
						URLClassLoader childClassLoader = jarFileClassLoader(modJar);
						modClasses.add(Class.forName(className.replace('/', '.'), true, childClassLoader));
					}
					catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
			CLASS_CATALOG.put(entry.getKey(), ImmutableSet.copyOf(modClasses));

			StormLogger.debug("Created new metadata catalog entry:");
			StormLogger.debug(String.format("Found %d classes in mod directory '%s'", modClasses.size(), entry.getKey()));
		}
	}

	@NotNull
	private static URLClassLoader jarFileClassLoader(ModJar modJar) {

		try
		{
			return new URLClassLoader (new URL[] { modJar.getJarFile().toURI().toURL() },
					StormBootstrap.CLASS_LOADER);
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Is called on PZ mods loading
	 */
	@SuppressWarnings("unused")
	public static void registerAllMods() throws ReflectiveOperationException {

		loadModClasses();

		registerMods();

		// this class should have already been initialized, so just get the reference
		Class<?> zomboidModClass = Class.forName("io.pzstorm.storm.mod.ZomboidMod", false, StormBootstrap.CLASS_LOADER);

		for (ZomboidMod mod : getRegisteredMods()) {
			zomboidModClass.getDeclaredMethod("registerEventHandlers").invoke(mod);
			zomboidModClass.getDeclaredMethod("registerLuaClasses").invoke(mod);
			StormLogger.info(String.format("Successfully loaded mod %s with class %s",
					getModName(mod.getClass()),
					mod.getClass().getCanonicalName()));
		}
	}

	/**
	 * Create and register {@link ZomboidMod} instances from cataloged classes.
	 * This method searches for classes that implement {@code ZomboidMod} and then instantiates
	 * and registers the implementation classes. Remember that the class catalogue has to be populated
	 * before registering mods otherwise no mod instances will be created. Also note that only the first
	 * implementation class for each catalog entry will be registered, the rest will be ignored.
	 *
	 * @throws ReflectiveOperationException if an exception was thrown while
	 * 		instantiating {@code ZomboidMod} implementation class.
	 */
	public static void registerMods() throws ReflectiveOperationException {

		for (Map.Entry<String, ImmutableSet<Class<?>>> entry : StormModLoader.CLASS_CATALOG.entrySet())
		{
			// find the first class that implements ZomboidMod interface
			Optional<Class<?>> modClass = entry.getValue().stream()
					.filter(ZomboidMod.class::isAssignableFrom).findFirst();

			if (!modClass.isPresent())
			{
				ModMetadata meta = StormModLoader.METADATA_CATALOG.get(entry.getKey());
				String format = "Unable to find ZomboidMod class for mod '%s'";

				StormLogger.warn(String.format(format, meta != null ? meta.name : "unknown"));
			}
			else MOD_REGISTRY.put(entry.getKey(), (ZomboidMod) modClass.get().getDeclaredConstructor().newInstance());
		}
	}

	/**
	 * Retrieve a {@code Set} of registered mods. Note that the returned
	 * {@code Set} is <b>unmodifiable</b> and modifying it in any way
	 * will result in an {@code UnsupportedOperationException}.
	 */
	public static @UnmodifiableView Set<ZomboidMod> getRegisteredMods() {
		return Collections.unmodifiableSet(new HashSet<>(MOD_REGISTRY.values()));
	}

	/**
	 * returns for the modType the corresponding mod name
	 * @param modType the registered mod type
	 * @return the actual mod name
	 */
	public static String getModName(Class modType) {

		return MOD_REGISTRY.entrySet().stream()
				.filter(e -> modType.isInstance(e.getValue())).map(Map.Entry::getKey).findFirst().orElse("");
	}
}
