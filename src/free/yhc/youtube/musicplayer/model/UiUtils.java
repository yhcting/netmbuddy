package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import free.yhc.youtube.musicplayer.R;

public class UiUtils {
    public interface EditTextAction {
        void prepare(Dialog dialog, EditText edit);
        void onOk(Dialog dialog, EditText edit);
    }

    public interface ConfirmAction {
        void onOk(Dialog dialog);
    }
    // ========================================================================
    //
    // Functions
    //
    // ========================================================================
    public static View
    inflateLayout(Context context, int layout) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, null);
    }

    public static void
    showTextToast(Context context, CharSequence text) {
        Toast t = Toast.makeText(context, text, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToast(Context context, int textid) {
        Toast t = Toast.makeText(context, textid, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    // ------------------------------------------------------------------------
    //
    // Dialog
    //
    // ------------------------------------------------------------------------
    public static AlertDialog
    createAlertDialog(Context context, int icon, CharSequence title, CharSequence message) {
        eAssert(null != title);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
        if (0 != icon)
            dialog.setIcon(icon);
        dialog.setTitle(title);
        if (null != message)
            dialog.setMessage(message);
        return dialog;
    }

    public static AlertDialog
    createAlertDialog(Context context, int icon, int title, int message) {
        eAssert(0 != title);
        CharSequence t = context.getResources().getText(title);
        CharSequence msg = (0 == message)? null: context.getResources().getText(message);
        return createAlertDialog(context, icon, t, msg);
    }

    public static AlertDialog
    buildConfirmDialog(final Context context,
                       final CharSequence title,
                       final CharSequence description,
                       final ConfirmAction action) {
        final AlertDialog dialog = createAlertDialog(context, R.drawable.ic_info, title, description);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                         context.getResources().getText(R.string.yes),
                         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface diag, int which) {
                dialog.dismiss();
                action.onOk(dialog);
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         context.getResources().getText(R.string.no),
                         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public static AlertDialog
    buildConfirmDialog(final Context context,
                       final int title,
                       final int description,
                       final ConfirmAction action) {
        return buildConfirmDialog(context,
                                  context.getResources().getText(title),
                                  context.getResources().getText(description),
                                  action);
    }

    private static AlertDialog
    createEditTextDialog(Context context, View layout, CharSequence title) {
        // Create "Enter Url" dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout);

        final AlertDialog dialog = builder.create();
        dialog.setTitle(title);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }

    private static AlertDialog
    createEditTextDialog(Context context, View layout, int title) {
        return createEditTextDialog(context, layout, context.getResources().getText(title));
    }

    public static AlertDialog
    buildOneLineEditTextDialog(final Context context,
                               final CharSequence title,
                               final CharSequence initText,
                               final EditTextAction action) {
        // Create "Enter Url" dialog
        View layout = inflateLayout(context, R.layout.edittext_dialog);
        final AlertDialog dialog = createEditTextDialog(context, layout, title);
        // Set action for dialog.
        final EditText edit = (EditText)layout.findViewById(R.id.edit);
        edit.setText(initText);
        edit.setSelection(initText.length());
        edit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((KeyEvent.ACTION_DOWN == event.getAction()) && (KeyEvent.KEYCODE_ENTER == keyCode)) {
                    dialog.dismiss();
                    if (!edit.getText().toString().isEmpty())
                        action.onOk(dialog, ((EditText)v));
                    return true;
                }
                return false;
            }
        });
        action.prepare(dialog, edit);

        dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                         context.getResources().getText(R.string.ok),
                         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dia, int which) {
                dialog.dismiss();
                if (!edit.getText().toString().isEmpty())
                    action.onOk(dialog, edit);
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         context.getResources().getText(R.string.cancel),
                         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context, CharSequence title, EditTextAction action) {
        return buildOneLineEditTextDialog(context, title, "", action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context, int title, CharSequence initText, EditTextAction action) {
        return buildOneLineEditTextDialog(context, context.getResources().getText(title), initText, action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context, int title, EditTextAction action) {
        return buildOneLineEditTextDialog(context, context.getResources().getText(title), action);
    }

    public static void
    setThumbnailImageView(ImageView v, byte[] imgdata) {
        Bitmap thumbnailBm = null;
        if (null == imgdata || imgdata.length > 0)
            thumbnailBm = BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length);

        if (null == thumbnailBm)
            v.setImageResource(R.drawable.ic_unknown_image);
        else
            v.setImageBitmap(thumbnailBm);
    }
}
