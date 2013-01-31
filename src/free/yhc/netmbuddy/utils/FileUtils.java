/*****************************************************************************
 *    Copyright (C) 2012, 2013 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(FileUtils.class);

    // Characters that is not allowed as filename in Android.
    private static final char[] sNoPathNameChars = new char[] {
        '/', '?', '"', '\'', '`', ':', ';', '*', '|', '\\', '<', '>'
    };

    public static String
    pathNameEscapeString(String str) {
        // Most Unix (including Linux) allows all 8bit-character as file name
        //   except for ('/' and 'null').
        // But android shell doens't allows some characters.
        // So, we need to handle those...
        for (char c : sNoPathNameChars)
            str = str.replace(c, '~');
        return str;
    }

    public static String
    getFileBaseName(String path) {
        int i = path.lastIndexOf("/");
        if (i < 0)
            return path; // return path itself.
        return path.substring(i + 1);
    }

    private static void
    zipDir(ZipOutputStream zos, String directory, String path)
            throws IOException, FileNotFoundException {
        final int BUFSZ = 4096;
        File zipDir = new File(directory);

        // get a listing of the directory content
        String[] dirList = zipDir.list();
        byte[] buf = new byte[BUFSZ];
        // loop through dirList, and zip the files
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = f.getPath();
                zipDir(zos, filePath, path + f.getName() + "/");
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            try {
                ZipEntry anEntry = new ZipEntry(path + f.getName());
                zos.putNextEntry(anEntry);
                int br; // bytes read
                while (0 < (br = fis.read(buf)))
                    zos.write(buf, 0, br);
            } finally {
                fis.close();
            }
        }
    }

    private static void
    zipFile(ZipOutputStream zos, String fsrc)
            throws IOException, FileNotFoundException {
        FileInputStream fis = new FileInputStream(fsrc);
        try {
            zip(zos, fis, getFileBaseName(fsrc));
        } finally {
            fis.close();
        }
    }

    public static void
    zip(ZipOutputStream zos, String fsrc, String comment)
            throws IOException, FileNotFoundException {
        File f = new File(fsrc);
        if (null != comment)
            zos.setComment(comment);

        if (f.isDirectory())
            zipDir(zos, fsrc, "");
        else if (f.isFile())
            zipFile(zos, fsrc);
        else
            throw new IOException();
    }

    public static void
    zip(ZipOutputStream zos, InputStream is, String entryName)
            throws IOException {
        final int BUFSZ = 4096;
        ZipEntry ze = new ZipEntry(entryName);
        byte[] buf = new byte[BUFSZ];
        zos.putNextEntry(ze);
        int br; // bytes read
        while (0 < (br = is.read(buf)))
            zos.write(buf, 0, br);
    }

    public static void
    zip(String zipFilePath, String srcFilePath, String comment)
            throws IOException, FileNotFoundException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
        zos.setLevel(8);
        try {
            zip(zos, srcFilePath, comment);
        } finally {
            zos.close();
        }
    }

    public static void
    unzip(OutputStream os, ZipInputStream zis)
            throws IOException {
        final int BUFSZ = 4096;
        while (null != zis.getNextEntry()) {
            int br;
            byte buf[] = new byte[BUFSZ];
            while (0 < (br = zis.read(buf)))
                os.write(buf, 0, br);
        }
    }

    public static void
    unzip(String outDir, ZipInputStream zis)
            throws IOException {
        final int BUFSZ = 4096;
        ZipEntry ze;
        while (null != (ze = zis.getNextEntry())) {
            File f = new File(outDir, ze.getName());
            //create directories if required.
            f.getParentFile().mkdirs();

            //if the entry is directory, leave it. Otherwise extract it.
            if (ze.isDirectory())
                continue;
            else {
                int br;
                byte buf[] = new byte[BUFSZ];
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    while (0 < (br = zis.read(buf)))
                        fos.write(buf, 0, br);
                } finally {
                    fos.close();
                }
            }
        }
    }

    public static void
    unzip(String outDir, String file)
            throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
        try {
            unzip(outDir, zis);
        } finally {
            zis.close();
        }
    }

    public static boolean
    removeFileRecursive(File f, HashSet<String> skips) {
        boolean ret = true;
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                if (!removeFileRecursive(c, skips))
                    ret = false;
        }

        if (ret && !skips.contains(f.getAbsolutePath()))
            return f.delete();
        return ret;
    }

    public static boolean
    removeFileRecursive(File f, File[] skips) {
        HashSet<String> skipSets = new HashSet<String>();
        for (File skf : skips)
            skipSets.add(skf.getAbsolutePath());

        return removeFileRecursive(f, skipSets);
    }

    public static boolean
    removeFileRecursive(File f, File skip) {
        return removeFileRecursive(f, new File[] { skip });
    }

    public static boolean
    removeFileRecursive(File f) {
        return removeFileRecursive(f, new File[0]);
    }
}
