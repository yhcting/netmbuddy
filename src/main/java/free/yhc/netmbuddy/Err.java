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

package free.yhc.netmbuddy;

import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.YTHacker;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.share.Share;

public enum Err {
    NO_ERR                      (R.string.err_no_err),
    UNKNOWN                     (R.string.err_unknown),
    INTERRUPTED                 (R.string.err_interrupted),
    CANCELLED                   (R.string.err_cancelled),
    BAD_REQUEST                 (R.string.err_bad_request),
    HTTP_RESP                   (R.string.err_http_response),
    MULTITHREADING              (R.string.err_unknown),
    NETWORK_UNAVAILABLE         (R.string.err_network_unavailable),
    INVALID_DATA                (R.string.err_invalid_data),
    UNSUPPORTED_VERSION         (R.string.err_unsupported_version),
    IO_NET                      (R.string.err_io_net),
    IO_FILE                     (R.string.err_io_file),
    IO_UNKNOWN                  (R.string.err_io_unknown),
    DB_DUPLICATED               (R.string.err_db_duplicated),
    DB_VERSION_MISMATCH         (R.string.err_db_version_mismatch),
    DB_NODATA                   (R.string.err_db_unknown),
    DB_INVALID                  (R.string.err_db_invalid),
    DB_UNKNOWN                  (R.string.err_db_unknown),
    INVALID_URL                 (R.string.err_invalid_url),
    PARSER_UNEXPECTED_FORMAT    (R.string.err_parser_unknown),
    PARSER_UNKNOWN              (R.string.err_parser_unknown),
    NO_MATCH                    (R.string.err_no_match),
    YTSEARCH                    (R.string.err_ytsearch),
    YTHTTPGET                   (R.string.err_ytprotocol),
    YTPARSEHTML                 (R.string.err_ytparsehtml),
    YTNOT_SUPPORTED_VIDFORMAT   (R.string.err_ytnot_supported_vidformat),
    YTINVALID_PARAM             (R.string.err_ytinvalid_param),
    NOT_IMPLEMENTED             (R.string.err_not_implemented),
    END_OF_DATA                 (R.string.err_end_of_data);

    private final int mMsg;

    public static Err
    map(YTHacker.Err err) {
        switch (err) {
        case NO_ERR:
            return NO_ERR;
        case IO_NET:
            return IO_NET;
        case NETWORK_UNAVAILABLE:
            return NETWORK_UNAVAILABLE;
        case PARSE_HTML:
            return YTPARSEHTML;
        case INTERRUPTED:
            return INTERRUPTED;
        default:
            return UNKNOWN;
        }
    }

    public static Err
    map(DB.Err err) {
        switch (err) {
        case NO_ERR:
            return NO_ERR;
        case VERSION_MISMATCH:
            return DB_VERSION_MISMATCH;
        case IO_FILE:
            return IO_FILE;
        case DUPLICATED:
            return DB_DUPLICATED;
        case INTERRUPTED:
            return INTERRUPTED;
        case INVALID_DB:
            return DB_INVALID;
        default:
            return UNKNOWN;
        }
    }

    public static Err
    map(YTDataAdapter.Err err) {
        switch (err) {
        case NO_ERR:
            return NO_ERR;
        case IO_NET:
            return Err.IO_NET;
        case INTERRUPTED:
            return Err.INTERRUPTED;
        case BAD_REQUEST:
            return Err.BAD_REQUEST;
        case NETWORK_UNAVAILABLE:
            return Err.NETWORK_UNAVAILABLE;
        case INVALID_PARAM:
            return Err.YTINVALID_PARAM;
        case BAD_RESPONSE:
            return PARSER_UNKNOWN;
        default:
            return UNKNOWN;
        }
    }

    public static Err
    map(Share.Err err) {
        switch (err) {
        case NO_ERR:
            return NO_ERR;
        case IO_FILE:
            return Err.IO_FILE;
        case IO_NET:
            return Err.IO_NET;
        case INTERRUPTED:
            return Err.INTERRUPTED;
        case INVALID_SHARE:
            return Err.INVALID_DATA;
        case UNSUPPORTED_VERSION:
            return Err.UNSUPPORTED_VERSION;
        case DB_UNKNOWN:
            return Err.DB_UNKNOWN;
        default:
            return Err.UNKNOWN;
        }
    }

    Err(int msg) {
        mMsg = msg;
    }

    public int
    getMessage() {
        return mMsg;
    }

}
