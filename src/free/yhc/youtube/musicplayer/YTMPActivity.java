package free.yhc.youtube.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;

public class YTMPActivity extends Activity {
    private static final int REQC_YTSEARCH  = 0;
    private static final int REQC_MUSICS    = 1;

    private ListView mListv;

    private PlayListAdapter
    getAdapter() {
        return (PlayListAdapter)mListv.getAdapter();
    }

    private void
    startSearchingYoutube(View anchor) {
        Intent i = new Intent(this, YTSearchActivity.class);
        startActivityForResult(i, REQC_YTSEARCH);
    }

    private void
    setupToolButtons() {
        ((ImageView)findViewById(R.id.btn_ytsearch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearchingYoutube(v);
            }
        });
    }

    private void
    onListItemClick(View view, int position, long itemId) {
        Intent i = new Intent(this, MusicsActivity.class);
        i.putExtra("plid", itemId);
        startActivityForResult(i, REQC_MUSICS);
    }

    @Override
    public boolean
    onContextItemSelected(MenuItem mItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)mItem.getMenuInfo();
        switch (mItem.getItemId()) {
        case R.id.rename:
            return true;
        case R.id.delete:
            return true;
        }
        return false;
    }

    @Override
    public void
    onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.playlist_context, menu);
        // AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo)menuInfo;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mListv = (ListView)findViewById(R.id.list);
        PlayListAdapter adapter = new PlayListAdapter(this);
        mListv.setAdapter(adapter);
        registerForContextMenu(mListv);
        mListv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                onListItemClick(view, position, itemId);
            }
        });

        setupToolButtons();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
    }

    @Override
    protected void
    onPause() {
        super.onPause();
    }

    @Override
    protected void
    onStop() {
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void
    onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode)
            return;

        switch (requestCode) {
        case REQC_YTSEARCH:
            boolean plChanged = data.getBooleanExtra(YTSearchActivity.KEY_PLCHANGED, false);
            if (plChanged)
                getAdapter().reloadCursor();
            break;
        }
    }

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing!
    }

    @Override
    public void
    onBackPressed() {
        super.onBackPressed();
    }
}