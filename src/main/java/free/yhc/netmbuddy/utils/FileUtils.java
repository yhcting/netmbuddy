/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
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
            throws IOException {
        final int BUFSZ = 4096;
        File zipDir = new File(directory);

        // get a listing of the directory content
        String[] dirList = zipDir.list();
        byte[] buf = new byte[BUFSZ];
        // loop through dirList, and zip the files
        for (String aDirList : dirList) {
            File f = new File(zipDir, aDirList);
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
            throws IOException {
        FileInputStream fis = new FileInputStream(fsrc);
        try {
            zip(zos, fis, getFileBaseName(fsrc));
        } finally {
            fis.close();
        }
    }

    public static void
    zip(ZipOutputStream zos, String fsrc, String comment)
            throws IOException {
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

    @SuppressWarnings("unused")
    public static void
    zip(String zipFilePath, String srcFilePath, String comment)
            throws IOException {
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
            // return value is ignored intentionally
            //noinspection ResultOfMethodCallIgnored
            f.getParentFile().mkdirs();

            //if the entry is directory, leave it. Otherwise extract it.
            if (!ze.isDirectory()) {
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

    @SuppressWarnings("unused")
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
        HashSet<String> skipSets = new HashSet<>();
        for (File skf : skips)
            skipSets.add(skf.getAbsolutePath());

        return removeFileRecursive(f, skipSets);
    }

    public static boolean
    removeFileRecursive(File f, File skip) {
        return removeFileRecursive(f, new File[] { skip });
    }

    @SuppressWarnings("unused")
    public static boolean
    removeFileRecursive(File f) {
        return removeFileRecursive(f, new File[0]);
    }

    // ========================================================================
    //
    // Handling File Contents
    //
    // ========================================================================
    /**
     *
     * @param file Text file.
     * @return value when reading non-text files, is not defined.
     */
    public static String
    readTextFile(File file) {
        try {
            StringBuilder fileData = new StringBuilder(4096);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            char[] buf = new char[4096];
            int bytes;
            while(-1 != (bytes = reader.read(buf)))
                fileData.append(buf, 0, bytes);
            reader.close();
            return fileData.toString();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
