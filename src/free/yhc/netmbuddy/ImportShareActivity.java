package free.yhc.netmbuddy;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import free.yhc.netmbuddy.utils.UiUtils;

public class ImportShareActivity extends Activity {
    private Importer        mImporter = null;
    private DiagAsyncTask   mTask   = null;
    private InputStream     mIs     = null;

    private static class Importer extends DiagAsyncTask.Worker {
        private final ZipInputStream _mIs;
        Importer(ZipInputStream is) {
            _mIs = is;
        }

        @Override
        public Err
        doBackgroundWork(DiagAsyncTask task) {

            return Err.NO_ERR;
        }

        @Override
        public void
        onPostExecute(DiagAsyncTask task, Err result) { }

        @Override
        public void
        onCancel(DiagAsyncTask task) { }

        @Override
        public void
        onCancelled(DiagAsyncTask task) { }
    };

    /**
     * ImportShareActivity has responsibility regarding closing input stream.
     * @param savedInstanceState
     * @param is
     */
    protected void
    onCreateInternal(Bundle savedInstanceState, InputStream is) {
        if (null == is) {
            UiUtils.showTextToast(this, R.string.msg_fail_to_access_data);
            finish();
            return;
        }

        mIs = is;
        mTask = new DiagAsyncTask(this,
                                  mImporter,
                                  DiagAsyncTask.Style.PROGRESS,
                                  R.string.analyzing_share,
                                  false);
        mTask.run();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
