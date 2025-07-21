package com.example.securenotes.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

public class FileUtils {

    public static String detectFileType(Context context, Uri uri) {
        String fileName = getFileName(context, uri);
        if (fileName != null) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot != -1 && lastDot < fileName.length() - 1) {
                return fileName.substring(lastDot + 1).toLowerCase();
            }
        }

        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) return extension.toLowerCase();
        }

        return "bin";
    }

    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        fileName = cursor.getString(index);
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public static String getMimeTypeFromExtension(String filename) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
        if (extension != null && !extension.isEmpty()) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime != null) return mime;
        }
        return "*/*";
    }
}
