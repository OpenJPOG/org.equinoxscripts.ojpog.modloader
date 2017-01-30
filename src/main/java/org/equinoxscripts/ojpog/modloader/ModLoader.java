package org.equinoxscripts.ojpog.modloader;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.equinoxscripts.ojpog.io.mod.ModManifest;
import org.equinoxscripts.ojpog.io.mod.ModVersion;
import org.equinoxscripts.ojpog.modloader.config.ConfigManager;
import org.equinoxscripts.ojpog.modloader.mod.ModContainer;
import org.equinoxscripts.ojpog.modloader.proc.ModInstaller;

import com.google.gson.stream.JsonWriter;

public class ModLoader {
	public static final String SIM_JP_EXE = "SimJP.exe";

	private JFrame frame;
	private JList<ModContainer> mods;
	private DefaultListModel<ModContainer> modsModel;
	private JButton btnInstall;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ModLoader window = new ModLoader();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ModLoader() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 800, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel header = new JPanel();
		frame.getContentPane().add(header, BorderLayout.NORTH);

		JButton btnLaunch = new JButton("Launch");
		header.add(btnLaunch);
		btnLaunch.addActionListener((a) -> {
			File install = ConfigManager.installFolder();
			File exe = new File(install, SIM_JP_EXE);
			if (!exe.exists()) {
				JOptionPane.showMessageDialog(ModLoader.this.frame,
						"Failed to locate SimJP.exe.  Did you install the core mod?");
			} else
				try {
					Process p = Runtime.getRuntime().exec(new String[] { exe.getAbsolutePath() }, new String[] {},
							install);
					JDialog diag = new JDialog();
					diag.setTitle("JP Running...");
					diag.setSize(100, 100);
					diag.setVisible(true);
					diag.setModal(false);
					while (p.isAlive()) {
						try {
							p.waitFor();
						} catch (InterruptedException e1) {
						}
					}
					diag.setVisible(false);
					diag.dispose();
				} catch (IOException e) {
				}
		});

		JButton btnRefresh = new JButton("Refresh");
		header.add(btnRefresh);
		btnRefresh.addActionListener((a) -> {
			refreshModList();
		});

		JButton btnModFolder = new JButton("Open Mod Folder");
		header.add(btnModFolder);
		btnModFolder.addActionListener((a) -> {
			try {
				Runtime.getRuntime().exec("explorer.exe " + ConfigManager.modsFolder().getAbsolutePath());
			} catch (IOException e) {
			}
		});

		JButton btnImport = new JButton("Import Mod");
		header.add(btnImport);

		this.btnInstall = new JButton("Install");
		this.btnInstall.setEnabled(false);
		header.add(btnInstall);
		btnImport.addActionListener((a) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(false);
			if (chooser.showOpenDialog(ModLoader.this.frame) == JFileChooser.APPROVE_OPTION) {
				File selected = chooser.getSelectedFile();
				try {
					JDialog diag = new JDialog();
					diag.setModal(false);
					diag.setTitle("Copying Mod...");
					diag.setSize(100, 100);
					diag.setVisible(true);
					if (selected.isDirectory()) {
						File simJP = new File(selected, SIM_JP_EXE);
						File manifest = new File(selected, ModManifest.MANIFEST_FILE_NAME);
						if (!simJP.exists() && !manifest.exists())
							JOptionPane.showMessageDialog(ModLoader.this.frame,
									"The directory, " + selected.getAbsolutePath()
											+ " doesn't have a manifest and isn't the JP install folder");
						else {
							File outDir = new File(ConfigManager.modsFolder(), selected.getName());
							org.apache.commons.io.FileUtils.copyDirectory(selected, outDir);
							if (simJP.exists()) {
								ModManifest mfs = new ModManifest("Jurassic Park: Operation Genesis",
										"Blue Tongue Entertainment", "", new ModVersion(1, 0, 0));
								JsonWriter jsw = new JsonWriter(
										new FileWriter(new File(outDir, ModManifest.MANIFEST_FILE_NAME)));
								Program.gson().toJson(mfs, ModManifest.class, jsw);
								jsw.close();
							}
						}
					} else {
						org.apache.commons.io.FileUtils.copyFile(selected,
								new File(ConfigManager.modsFolder(), selected.getName()));
					}
					diag.setVisible(false);
					diag.dispose();
					refreshModList();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(ModLoader.this.frame,
							"Failed to copy " + selected.getAbsolutePath() + " into the mod folder");
				}
			}
		});

		this.modsModel = new DefaultListModel<ModContainer>();
		this.mods = new JList<ModContainer>(this.modsModel);
		this.mods.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		frame.getContentPane().add(new JScrollPane(mods), BorderLayout.CENTER);

		this.mods.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				btnInstall.setEnabled(!mods.isSelectionEmpty());
			}
		});

		this.btnInstall.addActionListener((a) -> {
			ModContainer mce = this.mods.getSelectedValue();
			if (mce != null) {
				install(mce);
			}
		});

		refreshModList();
	}

	private void refreshModList() {
		File[] mods = ConfigManager.modsFolder().listFiles();
		this.modsModel.clear();
		for (File f : mods) {
			try {
				this.modsModel.addElement(new ModContainer(f));
			} catch (IOException e) {
				Program.reportWarning("Failed to import " + f.getName(), e);
			}
		}
	}

	private void install(ModContainer m) {
		JDialog diag = new JDialog();
		diag.setModal(false);
		diag.setTitle("Installing Mod: " + m);
		diag.setSize(100, 100);
		diag.setVisible(true);
		ModInstaller install = new ModInstaller(ConfigManager.installFolder(), m);
		try {
			install.install();
		} catch (Exception e) {
			Program.reportWarning("Failed to install " + m.toString(), e);
		}
		diag.setVisible(false);
		diag.dispose();
	}
}
