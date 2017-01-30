package org.equinoxscripts.ojpog.modloader.mod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.equinoxscripts.ojpog.io.mod.ModManifest;
import org.equinoxscripts.ojpog.io.mod.ModVersion;
import org.equinoxscripts.ojpog.modloader.Program;

import com.google.gson.stream.JsonReader;

public class ModContainer {
	public final ModManifest manifest;
	public final File source;
	private final ZipFile zipped;

	public ModContainer(File f) throws IOException {
		this.source = f;
		if (this.source.isFile())
			try {
				zipped = new ZipFile(this.source);
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			}
		else
			zipped = null;

		Reader r = handleReader(ModManifest.MANIFEST_FILE_NAME);
		if (r == null) {
			// Guess the manifest, since none exists.
			String name = this.source.getName();
			if (this.source.isFile()) {
				int offset = name.lastIndexOf('.');
				if (offset >= 0)
					name = name.substring(0, offset);
			}
			manifest = new ModManifest(name, "developer", "", new ModVersion(0, 0, 1));
		} else
			manifest = Program.gson().fromJson(new JsonReader(r), ModManifest.class);
	}

	private static void enumerate(File base, String prefix, List<String> out) {
		for (String s : base.list()) {
			File handle = new File(base, s);
			String path = prefix + File.separator + s;
			if (handle.isDirectory())
				enumerate(handle, path, out);
			else
				out.add(path);
		}
	}

	public List<String> files() {
		if (source.isDirectory()) {
			List<String> out = new ArrayList<String>();
			enumerate(source, ".", out);
			return out;
		} else
			return zipped.stream().map(new Function<ZipEntry, String>() {
				@Override
				public String apply(ZipEntry t) {
					return t.getName();
				}
			}).collect(Collectors.toCollection(new Supplier<List<String>>() {
				@Override
				public List<String> get() {
					return new ArrayList<String>();
				}
			}));
	}

	public InputStream handle(String name) {
		if (source.isDirectory()) {
			File f = new File(source, name);
			if (!f.exists())
				return null;
			try {
				return new FileInputStream(f);
			} catch (IOException e) {
				Program.reportWarning("Failed to open file stream", e);
				return null;
			}
		} else {
			ZipEntry ent = zipped.getEntry(name);
			if (ent != null)
				return null;
			try {
				return zipped.getInputStream(ent);
			} catch (IOException e) {
				Program.reportWarning("Failed to open ZIP stream", e);
				return null;
			}
		}
	}

	public Reader handleReader(String name) {
		InputStream str = handle(name);
		if (str == null)
			return null;
		return new InputStreamReader(str);
	}

	@Override
	public String toString() {
		return manifest.name + " by " + manifest.author + " (v" + manifest.version.major + "." + manifest.version.minor
				+ "." + manifest.version.patch + ")";
	}
}
