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

package free.yhc.youtube.musicplayer.model;

public class HttpUtils {
    // Statuc Codes
    // ============
    // Informational    : 1xx

    // Successful       : 2xx
    public static final int SC_OK           = 200;
    public static final int SC_NO_CONTENT   = 204;

    // Redirection      : 3xx
    public static final int SC_FOUND        = 302;

    // Client Error     : 4xx
    public static final int SC_NOT_FOUND    = 404;

    // Server Error     : 5xx

}
