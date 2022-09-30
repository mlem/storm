package io.pzstorm.storm.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.pzstorm.storm.IntegrationTest;
import io.pzstorm.storm.mod.ModJar;
import io.pzstorm.storm.mod.ModMetadata;
import io.pzstorm.storm.mod.ModVersion;

/**
 * Run test methods in a certain order to ensure the tests pass on CI.
 * This is only needed when running on Windows platform because resources like
 * metadata and jar files cannot be removed once they have been loaded by JVM.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StormModLoaderIntegrationTest extends ModLoaderTestFixture {

	/* do not use TempDir annotation to create temporary directory
	 * because Windows cannot delete these temporary directories due
	 * to jar files in those directories being in use by JVM
	 */
	private static final File TEMP_DIR = IntegrationTest.getTemporaryBuildDir(StormModLoaderIntegrationTest.class);
	private static final File ZOMBOID_MODS_DIR = new File(TEMP_DIR, "Zomboid/mods");

	@BeforeAll
	static void prepareStormModLoaderTest() throws IOException {
		prepareTestClass(TEMP_DIR);
	}




	@Test
	@Order(4)
	void shouldIncludeModDirectoryPathsInResourcePaths() {

		StormModLoader modLoader = new StormModLoader();
		HashSet<Path> classLoaderURLs = new HashSet<>();
		for (URL resourcePath : modLoader.getURLs()) {
			classLoaderURLs.add(Paths.get(toPath(resourcePath)).toAbsolutePath());
		}
		for (String modDirName : new String[] { "A", "B", "C" })
		{
			File modDir = new File(ZOMBOID_MODS_DIR, modDirName).getAbsoluteFile();
			Assertions.assertTrue(classLoaderURLs.contains(modDir.toPath()));
		}
	}

	private String toPath(URL resourcePath) {

		return resourcePath.getPath().substring(
				resourcePath.getPath().indexOf(":") + 1,
				resourcePath.getPath().indexOf("!") != -1 ? resourcePath.getPath().indexOf("!") :
						resourcePath.getPath().length());
	}


}
