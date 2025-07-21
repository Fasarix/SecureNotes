package com.example.securenotes.utils;

import static com.example.securenotes.utils.Constants.ZIP_BUFFER_SIZE;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ZipCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void zipDirectoryAsync(File sourceDir, File zipFile, ZipCallback callback) {
        executor.execute(() -> {
            try {
                zipDirectory(sourceDir, zipFile);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    public static void unzipAsync(File zipFile, File destDir, ZipCallback callback) {
        executor.execute(() -> {
            try {
                unzip(zipFile, destDir);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }

    private static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            zipFileHelper(sourceDir, sourceDir, zos);
        }
    }

    private static void zipFileHelper(File rootDir, File fileToZip, ZipOutputStream zos) throws IOException {
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipFileHelper(rootDir, child, zos);
                }
            }
            return;
        }

        String zipEntryName = rootDir.toURI().relativize(fileToZip.toURI()).getPath();
        try (
                FileInputStream fis = new FileInputStream(fileToZip)
        ) {
            zos.putNextEntry(new ZipEntry(zipEntryName));
            byte[] buffer = new byte[ZIP_BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new IOException("Impossibile creare la directory: " + destDir.getAbsolutePath());
        }

        try (
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))
        ) {
            ZipEntry entry;
            byte[] buffer = new byte[ZIP_BUFFER_SIZE];

            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());

                String canonicalDestDir = destDir.getCanonicalPath();
                String canonicalOutFile = outFile.getCanonicalPath();
                if (!canonicalOutFile.startsWith(canonicalDestDir + File.separator)) {
                    throw new IOException("Errore di sicurezza: estrazione non consentita");
                }

                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new IOException("Impossibile creare directory: " + outFile.getAbsolutePath());
                    }
                } else {
                    File parentDir = outFile.getParentFile();
                    assert parentDir != null;
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Impossibile creare directory padre: " + parentDir.getAbsolutePath());
                    }

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
