package org.equinoxscripts.ojpog.modloader.config;

import java.io.File;

import org.equinoxscripts.ojpog.modloader.Program;

public class ConfigManager {
	private static File configLocation;

	public static File configLocation() {
		if (configLocation == null) {
			String userHome = System.getProperty("user.home");
			boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
			File rootConfigDir = null;
			if (userHome != null) {
				if (isWindows)
					userHome += File.separator + "AppData" + File.separator + "Roaming";
				rootConfigDir = new File(userHome);
			}
			if (rootConfigDir == null || !rootConfigDir.canWrite())
				rootConfigDir = new File("./");
			String pathName = "OpenJPOG Modloader";
			if (!isWindows)
				pathName = "." + pathName.replace(' ', '.').toLowerCase();
			configLocation = new File(rootConfigDir, pathName);
			if (!configLocation.exists())
				if (!configLocation.mkdirs())
					Program.reportError("Failed to create config directory", configLocation.getAbsolutePath());
		}
		return configLocation;
	}

	private static File create(String name) {
		File f = new File(configLocation(), name);
		if (!f.exists())
			if (!f.mkdirs())
				Program.reportError("Failed to create " + name + " directory", f.getAbsolutePath());
		return f;
	}

	public static File modsFolder() {
		return create("mods");
	}

	public static File installFolder() {
		return create("install");
	}
}