package free.yhc.netmbuddy;

import java.io.FileNotFoundException;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import free.yhc.netmbuddy.utils.UiUtils;

public class ImportShareContentActivity extends ImportShareActivity {
    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        try {
            // ImportShareActivity has responsibility regarding closing input stream.
            onCreateInternal(savedInstanceState,
                             getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
        }
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
    public void
    onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
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
