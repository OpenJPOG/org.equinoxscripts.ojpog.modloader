package org.equinoxscripts.ojpog.modloader.proc;

public class FileUtils {
	public static String extension(String path) {
		int v1 = path.lastIndexOf('\\');
		int v2 = path.lastIndexOf('/');
		int v3 = path.lastIndexOf('.');
		if (v3 > v2 && v3 > v1)
			return path.substring(v3 + 1);
		return null;
	}

	public static String parentFile(String path) {
		int v1 = path.lastIndexOf('\\');
		int v2 = path.lastIndexOf('/');
		int idx = 0;
		if (v1 > idx)
			idx = v1;
		if (v2 > idx)
			idx = v2;
		if (idx == 0)
			return null;
		return path.substring(0, idx);
	}

	public static String fileName(String path) {
		int v1 = path.lastIndexOf('\\');
		int v2 = path.lastIndexOf('/');
		int idx = 0;
		if (v1 > idx)
			idx = v1 + 1;
		if (v2 > idx)
			idx = v2 + 1;
		return path.substring(idx);
	}

	public static boolean isOneOf(String needle, String... hay) {
		for (String s : hay) {
			if (needle == null && s == null)
				return true;
			if (s != null && s.equalsIgnoreCase(needle))
				return true;
		}
		return false;
	}

	public static final String[] ACCEPTED_IMAGES = { "png", "bmp", "jpg", "jpeg", "dds" };
}
