package org.example;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class UIHelper {
    public static void showError(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static File chooseMibFile(File initialDirectory) {
        JFileChooser fileChooser = new JFileChooser();
        if (initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setCurrentDirectory(initialDirectory);
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("MIB Files", "json"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static File chooseMibFile() {
        return chooseMibFile((File) null);
    }

    public static String chooseDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (dirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return dirChooser.getSelectedFile().getPath();
        }
        return null;
    }

    public static String chooseSaveLocation(String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " Files", extension));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getPath();
            if (!path.endsWith("." + extension)) {
                path += "." + extension;
            }
            return path;
        }
        return null;
    }
}