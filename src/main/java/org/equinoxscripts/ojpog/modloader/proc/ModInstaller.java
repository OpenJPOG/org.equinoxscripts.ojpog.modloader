package org.equinoxscripts.ojpog.modloader.proc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import javax.imageio.ImageIO;

import org.equinoxscripts.ojpog.io.IOUtils;
import org.equinoxscripts.ojpog.io.dds.DDS_File;
import org.equinoxscripts.ojpog.io.tml.TML_File;
import org.equinoxscripts.ojpog.io.tml.TML_File.TML_Material;
import org.equinoxscripts.ojpog.io.tml.TML_Texture;
import org.equinoxscripts.ojpog.io.tml.TML_Texture_Format;
import org.equinoxscripts.ojpog.modloader.mod.ModContainer;

public class ModInstaller {
	private final ModContainer mod;
	private final File dest;

	public ModInstaller(File dest, ModContainer mod) {
		this.dest = dest;
		this.mod = mod;
	}

	public void install() throws UnsupportedEncodingException, IOException {
		List<String> files = mod.files();
		unprocessedMaterials.clear();
		for (String s : files)
			handle(s);
		handleUnprocessedMaterials();
	}

	private void handle(String key) throws IOException {
		String extension = FileUtils.extension(key);
		if (extension != null)
			extension = extension.toLowerCase();

		String parent = FileUtils.parentFile(key);
		String name = FileUtils.fileName(key);

		boolean deferred = false;
		if (parent != null && parent.toLowerCase().endsWith("_tml")
				&& FileUtils.isOneOf(extension, FileUtils.ACCEPTED_IMAGES)) {
			String materialLibrary = parent.substring(0, parent.length() - 4) + "."
					+ parent.substring(parent.length() - 3);
			unprocessedMaterials.add(new String[] { materialLibrary, key });
			deferred = true;
		}

		if (name.toLowerCase().endsWith(".tml") && name.equals("Vehicles.tml")) {
			unprocessedMaterials.add(new String[] { key, key });
			deferred = true;
		}

//		if (!deferred) {
//			File fd = new File(dest, key);
//			if (!fd.getParentFile().exists())
//				fd.getParentFile().mkdirs();
//			org.apache.commons.io.FileUtils.copyInputStreamToFile(mod.handle(key), fd);
//		}
	}

	private final Queue<String[]> unprocessedMaterials = new LinkedList<String[]>();

	private void handleUnprocessedMaterials() throws UnsupportedEncodingException, IOException {
		Map<String, TML_File> matlibs = new HashMap<String, TML_File>();
		while (!unprocessedMaterials.isEmpty()) {
			String[] ent = unprocessedMaterials.poll();
			String matlib = ent[0];
			String imag = ent[1];

			if (!matlibs.containsKey(matlib)) {
				// Load the material library.
				File matfile = new File(dest, matlib);
				if (matfile.exists())
					matlibs.put(matlib, new TML_File(IOUtils.read(matfile)));
				else
					matlibs.put(matlib, new TML_File());
			}

			TML_File output = matlibs.get(matlib);

			if (imag.toLowerCase().endsWith(".tml")) {
				TML_File input;
				try {
					input = new TML_File(IOUtils.read(mod.handle(imag)));
				} catch (Exception e) {
					throw new IOException("Failed to load " + imag);
				}
				TML_Texture[] textureRemap = new TML_Texture[input.textures.length];
				for (int i = 0; i < textureRemap.length; i++) {
					TML_Texture t = output.getFreeTexture();
					textureRemap[input.textures[i].textureID] = t;
					t.set(input.textures[i]);
				}
				for (TML_Material in : input.stringMapping.values()) {
					TML_Material out = output.createOrGetMaterial(in.name);
					out.unknown = in.unknown;
					if (in.textures.length != out.textures.length)
						out.textures = Arrays.copyOf(out.textures, in.textures.length);
					for (int i = 0; i < in.textures.length; i++) 
						out.textures[i] = textureRemap[in.textures[i].textureID];
				}
			} else {
				String materialName = FileUtils.fileName(imag);
				int dot = materialName.lastIndexOf('.');
				if (dot >= 0)
					materialName = materialName.substring(0, dot);
				int und = materialName.lastIndexOf('_');
				int piece = 0;
				if (und >= 0) {
					piece = Integer.parseInt(materialName.substring(und + 1));
					materialName = materialName.substring(0, und);
				}
				TML_Material out = output.createOrGetMaterial(materialName);
				if (out.textures.length <= piece)
					out.textures = Arrays.copyOf(out.textures, piece + 1);
				for (int i = 0; i < out.textures.length; i++)
					if (out.textures[i] == null)
						out.textures[i] = output.getFreeTexture();

				BufferedImage image;
				if (imag.toLowerCase().endsWith(".dds"))
					image = new DDS_File(IOUtils.read(mod.handle(imag))).readImage();
				else
					image = ImageIO.read(mod.handle(imag));
				if (matlib.toLowerCase().endsWith("16.tml"))
					out.textures[piece].writeImage(TML_Texture_Format.ARGB_4444, image);
				else
					out.textures[piece].writeDDS(image, "DXT35AUTO");
			}
		}

		for (Entry<String, TML_File> matlib : matlibs.entrySet()) {
			try {
				ByteBuffer buff = ByteBuffer.allocate(matlib.getValue().length());
				buff.order(ByteOrder.LITTLE_ENDIAN);
				matlib.getValue().write(buff);
				buff.flip();
				File f = new File(dest, matlib.getKey());
				if (!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				IOUtils.write(f, buff);
			} catch (Exception e) {
				throw new IOException("Failed to write " + matlib.getKey());
			}
		}
	}
}
