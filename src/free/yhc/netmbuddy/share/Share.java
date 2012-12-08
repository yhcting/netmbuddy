/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
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

package free.yhc.netmbuddy.share;

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
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(Share.class);

    public static interface OnProgressListener {
        void onProgress(float prog);
    }

    public static interface ImporterI {
        ImportPrepareResult prepare();
        /**
         * Synchronous call.
         * @param arg
         *   user argument. It depends on share type.
         * @param listener
         * @return
         */
        ImportResult        execute(Object arg, OnProgressListener listener);
        void                cancel();
    }

    public static interface ExporterI {
        Err                 execute();
    }

    public static enum Err {
        NO_ERR,
        IO_FILE,
        PARAMETER,
        INTERRUPTED,
        INVALID_SHARE,
        UNSUPPORTED_VERSION,
        DB_UNKNOWN,
        UNKNOWN
    }

    public static class LocalException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err   _mErr;

        public LocalException(Err err) {
            _mErr = err;
        }

        public Err
        error() {
            return _mErr;
        }
    }

    // Sharing type
    public static enum Type {
        PLAYLIST (1);

        private final int _mVersion;
        Type(int version) {
            _mVersion = version;
        }

        int
        getVersion() {
            return _mVersion;
        }
    }

    public static class ImportPrepareResult {
        public Err      err     = Err.UNKNOWN;
        public Type     type    = Type.PLAYLIST;       // type of importing data
        // title of this import.
        // Value has it's own meaning dependent on 'type'
        public String   message = "";
    }

    public static class ImportResult {
        public Err              err     = Err.UNKNOWN;        // result of import
        public String           message = "";
        // # of successfully imported
        public AtomicInteger    success = new AtomicInteger(0);
        // # of fails to import.
        public AtomicInteger    fail    = new AtomicInteger(0);
    }

    // ========================================================================
    //
    // Common Utilities
    //
    // ========================================================================



    // ========================================================================
    //
    //
    //
    // ========================================================================


    // ========================================================================
    //
    // Interfaces
    //
    // ========================================================================
    public static ImporterI
    buildImporter(ZipInputStream zis) {
        return new Importer(zis);
    }

    public static ExporterI
    buildPlayerlistExporter(File file, long plid) {
        return new ExporterPlaylist(file, plid);
    }
}
