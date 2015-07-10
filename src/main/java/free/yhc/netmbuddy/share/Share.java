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

package free.yhc.netmbuddy.share;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import free.yhc.netmbuddy.utils.Utils;

// ============================================================================
//
// Interface class of this package.
//
// ============================================================================
public class Share {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(Share.class);

    public interface OnProgressListener {
        void onPreProgress(int maxProg);
        void onProgress(int prog);
    }

    public interface ImporterI {
        ImportPrepareResult prepare();
        /**
         * Synchronous call.
         * @param arg user argument. It depends on share type.
         * @param listener listener
         */
        ImportResult execute(Object arg, OnProgressListener listener);
        void cancel();
    }

    public interface ExporterI {
        Err execute();
    }

    public enum Err {
        NO_ERR,
        IO_FILE,
        IO_NET,
        PARAMETER,
        INTERRUPTED,
        INVALID_SHARE,
        UNSUPPORTED_VERSION,
        DB_UNKNOWN,
        UNKNOWN
    }

    public static class LocalException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err _mErr;

        public LocalException(Err err) {
            _mErr = err;
        }

        public Err
        error() {
            return _mErr;
        }
    }

    // Sharing type
    public enum Type {
        PLAYLIST
    }

    public static class ImportPrepareResult {
        public Err err = Err.UNKNOWN;
        public Type type = Type.PLAYLIST; // type of importing data
        // title of this import.
        // Value has it's own meaning dependent on 'type'
        public String message = "";
    }

    public static class ImportResult {
        public Err err = Err.UNKNOWN; // result of import
        public String message = "";
        // # of successfully imported
        public AtomicInteger success = new AtomicInteger(0);
        // # of fails to import.
        public AtomicInteger fail = new AtomicInteger(0);
    }

    // ========================================================================
    //
    // Interfaces
    //
    // ========================================================================
    @NonNull
    public static ImporterI
    buildImporter(ZipInputStream zis) {
        return new Importer(zis);
    }

    @NonNull
    public static ExporterI
    buildPlayerlistExporter(File file, long plid) {
        return new ExporterPlaylist(file, plid);
    }
}
