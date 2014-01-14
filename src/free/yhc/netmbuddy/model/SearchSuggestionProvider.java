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

package free.yhc.netmbuddy.model;

import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;
import free.yhc.netmbuddy.utils.Utils;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(SearchSuggestionProvider.class);

    private final static String AUTHORITY   = "free.yhc.netmbuddy";
    private final static int    MODE        = DATABASE_MODE_QUERIES;

    public SearchSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    public static void
    saveRecentQuery(String query) {
        SearchRecentSuggestions suggestions
            = new SearchRecentSuggestions(Utils.getAppContext(),
                                          AUTHORITY,
                                          MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void
    clearHistory() {
        SearchRecentSuggestions suggestions
            = new SearchRecentSuggestions(Utils.getAppContext(),
                                          AUTHORITY,
                                          MODE);
        suggestions.clearHistory();
    }
}
