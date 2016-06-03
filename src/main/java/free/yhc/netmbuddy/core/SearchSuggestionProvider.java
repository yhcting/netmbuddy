/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015, 2016
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

package free.yhc.netmbuddy.core;

import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static Logger P = null;

    private final static String AUTHORITY = "free.yhc.netmbuddy";
    private final static int MODE = DATABASE_MODE_QUERIES;

    public SearchSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    /* This provider is called before initializing application.
     *
     *   > at android.app.ActivityThread.installProvider(ActivityThread.java:5141)
     *   > at android.app.ActivityThread.installContentProviders(ActivityThread.java:4748)
     */
    public static void
    init() {
        P = Logger.create(SearchSuggestionProvider.class, Logger.LOGLV_DEFAULT);
    }

    public static void
    saveRecentQuery(String query) {
        SearchRecentSuggestions suggestions
            = new SearchRecentSuggestions(AppEnv.getAppContext(),
                                          AUTHORITY,
                                          MODE);
        suggestions.saveRecentQuery(query, null);
    }

    public static void
    clearHistory() {
        SearchRecentSuggestions suggestions
            = new SearchRecentSuggestions(AppEnv.getAppContext(),
                                          AUTHORITY,
                                          MODE);
        suggestions.clearHistory();
    }
}
