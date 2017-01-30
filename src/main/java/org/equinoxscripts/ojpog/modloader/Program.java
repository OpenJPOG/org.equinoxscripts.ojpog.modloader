package org.equinoxscripts.ojpog.modloader;

import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Program {
	public static void reportError(String error, String details) {
		System.err.println("Program failed: " + error);
		if (details != null)
			System.err.println(details);
		if (!GraphicsEnvironment.isHeadless())
			JOptionPane.showOptionDialog(null, "Failed: " + error, "Critical Error", JOptionPane.OK_OPTION,
					JOptionPane.ERROR_MESSAGE, null, null, null);
		System.exit(0);
	}

	private static Gson gson;

	public static Gson gson() {
		if (gson == null) {
			GsonBuilder gsb = new GsonBuilder();
			gson = gsb.create();
		}
		return gson;
	}

	public static void reportWarning(String message, Throwable e) {
		System.err.println(message);
		e.printStackTrace(System.err);
		if (!GraphicsEnvironment.isHeadless()) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			JOptionPane.showOptionDialog(null, sw.toString(), "Warning", JOptionPane.OK_OPTION,
					JOptionPane.WARNING_MESSAGE, null, null, null);
		}
	}
}
