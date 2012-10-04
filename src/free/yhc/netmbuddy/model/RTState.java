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

package free.yhc.netmbuddy.model;


public class RTState {
    private static RTState sInstance = null;

    private String  mLastSearchWord = "";
    // TODO
    // Proxy string should be changed if user changes proxy setting.
    private String  mProxy          = "";

    private RTState() {
        mProxy = System.getenv("http_proxy");
        if (null == mProxy)
            mProxy = "";
    }

    public static RTState
    get() {
        if (null == sInstance)
            sInstance = new RTState();
        return sInstance;
    }

    public void
    setLastSearchWord(String word) {
        mLastSearchWord = word;
    }

    public String
    getLastSearchWord() {
        return mLastSearchWord;
    }

    public String
    getProxy() {
        return mProxy;
    }
}
