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

package free.yhc.netmbuddy;

import android.os.Bundle;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTVideoSearchPlaylistActivity extends YTVideoSearchActivity {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoSearchPlaylistActivity.class);

    @Override
    protected YTSearchHelper.SearchType
    getSearchType() {
        return YTSearchHelper.SearchType.VID_PLAYLIST;
    }

    @Override
    protected String
    getTitlePrefix() {
        return (String)getResources().getText(R.string.playlist);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateInternal(getIntent().getStringExtra(MAP_KEY_SEARCH_TEXT),
                         getIntent().getStringExtra(MAP_KEY_SEARCH_TITLE));
    }
}
