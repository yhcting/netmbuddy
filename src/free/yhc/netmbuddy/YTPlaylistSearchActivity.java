/******************************************************************************
 *    Copyright (C) 2012, 2013, 2014 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of NetMBuddy.
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
 *    along with this program.	If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.netmbuddy;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import free.yhc.netmbuddy.model.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTPlaylistSearchActivity extends YTSearchActivity implements
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlaylistSearchActivity.class);

    @Override
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    protected YTSearchHelper.SearchType
    getSearchType() {
        return YTSearchHelper.SearchType.PL_USER;
    }

    @Override
    protected Class<? extends YTSearchFragment>
    getFragmentClass() {
        return YTPlaylistSearchFragment.class;
    }

    @Override
    protected void
    onSearchMetaInformationReady(String text, String title, int totalResults) {
        String titleText = text + getResources().getText(R.string.user_playlist_title_suffix);
        ((TextView)findViewById(R.id.title)).setText(titleText);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UnexpectedExceptionHandler.get().registerModule(this);

        setupBottomBar(R.drawable.ic_ytsearch,
                       new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                doNewSearch();
            }
        },
                       0, null);

        String stext = getIntent().getStringExtra(MAP_KEY_SEARCH_TEXT);
        if (null != stext)
            startNewSearch(stext, stext);
        else
            doNewSearch();
    }

    @Override
    protected void
    onDestroy() {
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }
}
