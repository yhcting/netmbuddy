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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.YTHacker;
import free.yhc.netmbuddy.model.YTSearchHelper;

public enum Err {
    NO_ERR                      (R.string.err_no_err),
    UNKNOWN                     (R.string.err_unknown),
    INTERRUPTED                 (R.string.err_interrupted),
    CANCELLED                   (R.string.err_cancelled),
    BAD_REQUEST                 (R.string.err_bad_request),
    MULTITHREADING              (R.string.err_unknown),
    NETWORK_UNAVAILABLE         (R.string.err_network_unavailable),
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
    END_OF_DATA                 (R.string.err_end_of_data);

    private final int mMsg;

    public static Err
    map(YTHacker.Err err) {
        switch (err) {
        case IO_NET:
            return IO_NET;

        case NETWORK_UNAVAILABLE:
            return NETWORK_UNAVAILABLE;

        case PARSE_HTML:
            return YTPARSEHTML;

        case INTERRUPTED:
            return INTERRUPTED;

        case NO_ERR:
            eAssert(false);
        default:
            return UNKNOWN;
        }
    }

    public static Err
    map(DB.Err err) {
        switch (err) {
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

        case NO_ERR:
            eAssert(false);
        default:
            return UNKNOWN;
        }
    }

    public static Err
    map(YTSearchHelper.Err err) {
        switch (err) {
        case IO_NET:
            return Err.IO_NET;

        case INTERRUPTED:
            return Err.INTERRUPTED;

        case BAD_REQUEST:
            return Err.BAD_REQUEST;

        case NETWORK_UNAVAILABLE:
            return Err.NETWORK_UNAVAILABLE;

        case PARAMETER:
            return Err.YTINVALID_PARAM;

        case FEED_FORMAT:
            return PARSER_UNKNOWN;

        case NO_ERR:
            eAssert(false);
        default:
            return UNKNOWN;
        }
    }

    private Err(int msg) {
        mMsg = msg;
    }

    public int
    getMessage() {
        return mMsg;
    }

}
