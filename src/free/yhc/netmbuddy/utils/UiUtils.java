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

package free.yhc.netmbuddy.utils;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.File;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.YTHacker;

public class UiUtils {
    public interface EditTextAction {
        void prepare(Dialog dialog, EditText edit);
        void onOk(Dialog dialog, EditText edit);
    }

    public interface ConfirmAction {
        void onOk(Dialog dialog);
        void onCancel(Dialog dialog);
    }

    public interface OnPlaylistSelectedListener {
        void onUserMenu(int pos, Object user);
        void onPlaylist(long plid, Object user);
    }

    // ========================================================================
    //
    // Functions
    //
    // ========================================================================

    // ------------------------------------------------------------------------
    //
    // Error to feedback message
    //
    // ------------------------------------------------------------------------
    public static View
    inflateLayout(Context context, int layout) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, null);
    }

    public static void
    showTextToast(Context context, CharSequence text) {
        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToast(Context context, int textid) {
        Toast t = Toast.makeText(context, textid, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToastAtBottom(Context context, int textid, boolean lengthLong) {
        Toast t = Toast.makeText(context, textid, lengthLong? Toast.LENGTH_LONG: Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
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
                action.onOk(dialog);
                dialog.dismiss();
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         context.getResources().getText(R.string.no),
                         new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface diag, int which) {
                action.onCancel(dialog);
                diag.dismiss();
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
    buildOneLineEditTextDialog(Context context, int title, int initText, EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          context.getResources().getText(title),
                                          context.getResources().getText(initText),
                                          action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context, int title, EditTextAction action) {
        return buildOneLineEditTextDialog(context, context.getResources().getText(title), action);
    }

    public static AlertDialog
    buildSelectPlaylistDialog(final DB                          db,
                              final Context                     context,
                              final int                         diagTitle,
                              final String[]                    userMenuStrings,
                              final OnPlaylistSelectedListener  action,
                              long                              excludedPlid,
                              final Object                      user) {
        final String[] userMenus = (null == userMenuStrings)? new String[0]: userMenuStrings;

        // Create menu list
        final Cursor c = db.queryPlaylist(new DB.ColPlaylist[] { DB.ColPlaylist.ID,
                                                                 DB.ColPlaylist.TITLE });

        final int iTitle = c.getColumnIndex(DB.ColPlaylist.TITLE.getName());
        final int iId    = c.getColumnIndex(DB.ColPlaylist.ID.getName());

        LinkedList<String> menul = new LinkedList<String>();
        LinkedList<Long>   idl   = new LinkedList<Long>();

        for (int i = 0; i < userMenus.length; i++) {
            menul.add(userMenus[i]);
            idl.add((long)i);
        }

        // Add slot for 'new list' menu
        menul.add(context.getResources().getText(R.string.new_playlist).toString());
        idl.add((long)userMenus.length); // dummy value

        if (c.moveToFirst()) {
            do {
                if (excludedPlid != c.getLong(iId)) {
                    menul.add(c.getString(iTitle));
                    idl.add(c.getLong(iId));
                }
            } while (c.moveToNext());
        }
        c.close();

        final String[] menus = menul.toArray(new String[0]);
        final long[]   ids   = Utils.convertArrayLongTolong(idl.toArray(new Long[0]));

        AlertDialog.Builder bldr = new AlertDialog.Builder(context);
        if (diagTitle > 0)
            bldr.setTitle(diagTitle);
        ArrayAdapter<String> adapter
            = new ArrayAdapter<String>(context, android.R.layout.select_dialog_item, menus);
        bldr.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eAssert(which >= 0);
                dialog.dismiss();
                if (userMenus.length > which) {
                    // User menu is selected.
                    action.onUserMenu(which,  user);
                } else if (userMenus.length == which) {
                    // Need to get new playlist name.
                    UiUtils.EditTextAction edAction = new UiUtils.EditTextAction() {
                        @Override
                        public void prepare(Dialog dialog, EditText edit) { }

                        @Override
                        public void onOk(Dialog dialog, EditText edit) {
                            String title = edit.getText().toString();
                            if (db.containsPlaylist(title)) {
                                UiUtils.showTextToast(context, R.string.msg_existing_playlist);
                                return;
                            }

                            long plid = db.insertPlaylist(title, "");
                            if (plid < 0) {
                                UiUtils.showTextToast(context, R.string.err_db_unknown);
                                return;
                            }

                            action.onPlaylist(plid, user);
                        }
                    };
                    UiUtils.buildOneLineEditTextDialog(context,
                                                       R.string.enter_playlist_title,
                                                       R.string.enter_playlist_title,
                                                       edAction)
                           .show();
                } else
                    action.onPlaylist(ids[which], user);
            }
        });
        return bldr.create();
    }

    public static void
    setThumbnailImageView(ImageView v, byte[] imgdata) {
        // NOTE
        // Why Bitmap instance is created even if R.drawable.ic_unknown_image?
        // ImageView has BitmapDrawable to draw it's canvas, and every input-drawble
        //   - resource, uri etc - is converted to BitmapDrawable eventually.
        // But, ImageView.setImageResource() doesn't update drawable if current image resource
        //   is same with new one - See "ImageView.java for details"
        // In this case, ImageView may try to use recycled BitmapDrawable.
        // To avoid this, Bitmap instance is used in all cases.
        Bitmap thumbnailBm;
        if (null != imgdata && imgdata.length > 0)
            thumbnailBm = BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length);
        else
            thumbnailBm = BitmapFactory.decodeResource(Utils.getAppContext().getResources(),
                                                       R.drawable.ic_unknown_image);

        // This assumes that Drawable of ImageView is set only by this function.
        // => Setting drawable directly through ImageView interface may lead to exception
        // (Exception : try to used recycled bitmap)
        // Recycle old Bitmap
        Drawable drawable = v.getDrawable();
        if (drawable instanceof BitmapDrawable) { // to make sure.
            BitmapDrawable bmd = (BitmapDrawable)drawable;
            Bitmap bitmap = bmd.getBitmap();
            bitmap.recycle();
        }

        // change to new one.
        v.setImageBitmap(thumbnailBm);
    }

    public static void
    playAsVideo(Context context, String ytvid) {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse(YTHacker.getYtVideoPageUrl(ytvid)));
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            UiUtils.showTextToast(context, R.string.msg_fail_find_app);
        }
    }

    public static void
    sendMail(Context      context,
             String       receiver,
             CharSequence diagTitle,
             CharSequence subject,
             CharSequence text,
             File attachment) {
        if (!Utils.isNetworkAvailable())
            return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        if (null != receiver)
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { receiver });
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (null != attachment)
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));
        intent.setType("message/rfc822");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.showTextToast(context, R.string.msg_fail_find_app);
        }
    }

}
