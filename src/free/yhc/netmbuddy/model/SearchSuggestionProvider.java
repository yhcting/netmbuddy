package free.yhc.netmbuddy.model;

import free.yhc.netmbuddy.utils.Utils;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {
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
