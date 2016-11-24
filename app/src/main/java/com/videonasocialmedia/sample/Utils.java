package com.videonasocialmedia.sample;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alvaro on 20/09/16.
 */

public class Utils {


    public static File copyResourceToTemp(Context ctx, String pathDirectory, String name, int resourceId,
                                          String fileTypeExtensionConstant) throws IOException {
        String nameFile = String.valueOf(name);
        File file = new File(pathDirectory + File.separator + nameFile +
                fileTypeExtensionConstant);

        if (!file.exists() || !file.isFile()) {
            if (!file.isFile())
                file.delete();
            InputStream in = ctx.getResources().openRawResource(resourceId);
            try {
                FileOutputStream out = new FileOutputStream(pathDirectory + File.separator +
                        nameFile + fileTypeExtensionConstant);
                byte[] buff = new byte[1024];
                int read = 0;
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
                out.close();
            } catch (FileNotFoundException e) {
                //TODO show error message
            } finally {
                in.close();
            }
        }

        return file;
    }

    public static void cleanDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) { //some JVMs return null for empty dirs
                for (File f : files) {
                    if (f.isDirectory()) {
                        cleanDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
    }
}
