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

package free.yhc.netmbuddy.utils;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import free.yhc.netmbuddy.DiagAsyncTask;
import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DB.Bookmark;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTHacker;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.scmp.SCmp;

public class UiUtils {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(UiUtils.class);

    public static final long PLID_INVALID       = DB.INVALID_PLAYLIST_ID;
    // Special playlist id that represents that this is unknown non-user playlist.
    public static final long PLID_UNKNOWN       = PLID_INVALID - 1;
    public static final long PLID_RECENT_PLAYED = PLID_INVALID - 2;
    public static final long PLID_SEARCHED      = PLID_INVALID - 3;

    // NOTE
    // To save time for decoding image, pre-decoded bitmap is uses for unknown thumbnail image.
    private static final Bitmap sBmIcUnknownImage
        = BitmapFactory.decodeResource(Utils.getAppContext().getResources(), R.drawable.ic_unknown_image);

    public interface EditTextAction {
        void prepare(Dialog dialog, EditText edit);
        void onOk(Dialog dialog, EditText edit);
    }

    public interface ConfirmAction {
        void onOk(Dialog dialog);
        void onCancel(Dialog dialog);
    }

    public interface OnPlaylistSelected {
        void onUserMenu(int pos, Object user);
        void onPlaylist(long plid, Object user);
    }

    public interface OnMenuSelected {
        void onSelected(int pos, int menuTitle);
    }

    public interface OnPostExecuteListener {
        void    onPostExecute(Err result, Object user);
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
    showTextToast(Context context, CharSequence text, boolean lengthLong) {
        Toast t = Toast.makeText(context, text, lengthLong? Toast.LENGTH_LONG: Toast.LENGTH_SHORT);
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
            public void
            onClick(DialogInterface diag, int which) {
                action.onOk(dialog);
                dialog.dismiss();
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         context.getResources().getText(R.string.no),
                         new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface diag, int which) {
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
                               final CharSequence hintText,
                               final EditTextAction action) {
        // Create "Enter Url" dialog
        View layout = inflateLayout(context, R.layout.edittext_dialog);
        final AlertDialog dialog = createEditTextDialog(context, layout, title);
        // Set action for dialog.
        final EditText edit = (EditText)layout.findViewById(R.id.edit);
        if (null != initText
            && initText.length() > 0) {
            edit.setText(initText);
            edit.setSelection(initText.length());
        }

        if (null != hintText
            && hintText.length() > 0)
            edit.setHint(hintText);

        edit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean
            onKey(View v, int keyCode, KeyEvent event) {
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
            public void
            onClick(DialogInterface dia, int which) {
                dialog.dismiss();
                if (!edit.getText().toString().isEmpty())
                    action.onOk(dialog, edit);
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         context.getResources().getText(R.string.cancel),
                         new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return dialog;
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context,
                               CharSequence title,
                               EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          title,
                                          "",
                                          "",
                                          action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context,
                               int title,
                               EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          context.getResources().getText(title),
                                          action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context,
                               int title,
                               CharSequence initText,
                               EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          context.getResources().getText(title),
                                          initText,
                                          "",
                                          action);
    }

    public static AlertDialog
    buildOneLineEditTextDialog(Context context,
                               CharSequence title,
                               int initText,
                               EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          title,
                                          context.getResources().getText(initText),
                                          "",
                                          action);
    }

    public static AlertDialog
    buildOneLineEditTextDialogWithHint(Context context,
                                       CharSequence title,
                                       int hintText,
                                       EditTextAction action) {
        return buildOneLineEditTextDialog(context,
                                          title,
                                          "",
                                          context.getResources().getText(hintText),
                                          action);
    }

    public static AlertDialog
    buildPopupMenuDialog(final Activity         activity,
                         final OnMenuSelected   action,
                         final int              diagTitle,
                         final int[]            menuTitles) {
        final CharSequence[] items = new CharSequence[menuTitles.length];
        for (int i = 0; i < menuTitles.length; i++)
            items[i] = activity.getResources().getText(menuTitles[i]);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (diagTitle >= 0)
            builder.setTitle(diagTitle);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface dialog, int item) {
                action.onSelected(item, menuTitles[item]);
            }
        });
        return builder.create();
    }

    public static AlertDialog
    buildSelectPlaylistDialog(final DB                  db,
                              final Context             context,
                              final int                 diagTitle,
                              final String[]            userMenuStrings,
                              final OnPlaylistSelected  action,
                              long                      plidExcluded,
                              final Object              user) {
        final String[] userMenus = (null == userMenuStrings)? new String[0]: userMenuStrings;

        // Create menu list
        final Cursor c = db.queryPlaylist(new ColPlaylist[] { ColPlaylist.ID,
                                                              ColPlaylist.TITLE });

        final int iTitle = c.getColumnIndex(ColPlaylist.TITLE.getName());
        final int iId    = c.getColumnIndex(ColPlaylist.ID.getName());

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
                if (plidExcluded != c.getLong(iId)) {
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
            public void
            onClick(DialogInterface dialog, int which) {
                eAssert(which >= 0);
                dialog.dismiss();
                if (userMenus.length > which) {
                    // User menu is selected.
                    action.onUserMenu(which,  user);
                } else if (userMenus.length == which) {
                    // Need to get new playlist name.
                    UiUtils.EditTextAction edAction = new UiUtils.EditTextAction() {
                        @Override
                        public void
                        prepare(Dialog dialog, EditText edit) { }

                        @Override
                        public void
                        onOk(Dialog dialog, EditText edit) {
                            String title = edit.getText().toString();
                            if (db.containsPlaylist(title)) {
                                UiUtils.showTextToast(context, R.string.msg_existing_playlist);
                                return;
                            }

                            long plid = db.insertPlaylist(title);
                            if (plid < 0) {
                                UiUtils.showTextToast(context, R.string.err_db_unknown);
                                return;
                            }

                            action.onPlaylist(plid, user);
                        }
                    };
                    UiUtils.buildOneLineEditTextDialog(context, R.string.enter_playlist_title, edAction)
                           .show();
                } else
                    action.onPlaylist(ids[which], user);
            }
        });
        return bldr.create();
    }

    /**
     * This function is a kind of HACK - actually FULL OF HACK - to save memory used by thumbnail.
     * Very dangerous and difficult at maintenance.
     * But, I failed to find any better way to save memory for thumbnail display.
     *
     * NOTE
     * If exception like "Exception : try to used recycled bitmap ..." is shown up,
     *   read and understand what this function does with highest priority!
     * @param v
     * @param imgdata
     */
    public static void
    setThumbnailImageView(ImageView v, byte[] imgdata) {
        // NOTE
        // Why Bitmap instance is created even if R.drawable.ic_unknown_image?
        // ImageView has BitmapDrawable to draw it's canvas, and every input-drawable
        //   - resource, uri etc - is converted to BitmapDrawable eventually.
        // But, ImageView.setImageResource() doesn't update drawable if current image resource
        //   is same with new one - See "ImageView.java for details"
        // In this case, ImageView may try to use recycled BitmapDrawable.
        // To avoid this, Bitmap instance is used in all cases.
        Bitmap thumbnailBm;
        if (null != imgdata && imgdata.length > 0)
            thumbnailBm = BitmapFactory.decodeByteArray(imgdata, 0, imgdata.length);
        else
            thumbnailBm = sBmIcUnknownImage;

        // This assumes that Drawable of ImageView is set only by this function.
        // => Setting drawable directly through ImageView interface may lead to exception
        // (Exception : try to used recycled bitmap)
        // Recycle old Bitmap
        Drawable drawable = v.getDrawable();
        if (drawable instanceof BitmapDrawable) { // to make sure.
            BitmapDrawable bmd = (BitmapDrawable)drawable;
            Bitmap bitmap = bmd.getBitmap();
            if (bitmap != sBmIcUnknownImage)
                bitmap.recycle();
        }

        // change to new one.
        v.setImageBitmap(thumbnailBm);
    }

    public static void
    playAsVideo(Context context, String ytvid) {
        YTPlayer.get().stopVideos();
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse(YTHacker.getYtVideoPageUrl(ytvid)));
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            UiUtils.showTextToast(context, R.string.msg_fail_find_app);
        }
    }

    public static boolean
    isUserPlaylist(long plid) {
        return plid >= 0;
    }

    public static void
    doDeleteVideos(final Activity activity,
                   final Object user,
                   final OnPostExecuteListener listener,
                   final long plid,
                   final long[] mids) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                if (null != listener)
                    listener.onPostExecute(result, user);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                DB db = DB.get();
                db.beginTransaction();
                try {
                    for (long mid : mids) {
                        if (isUserPlaylist(plid))
                            db.deleteVideoFrom(plid, mid);
                        else
                            db.deleteVideoFromAll(mid);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(activity,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.deleting)
            .run();
    }

    public static void
    deleteVideos(final Activity activity,
                 final Object user, // passed to callback
                 final OnPostExecuteListener listener,
                 final long plid,
                 final long[] vids) {
        UiUtils.ConfirmAction action = new UiUtils.ConfirmAction() {
            @Override
            public void
            onOk(Dialog dialog) {
                doDeleteVideos(activity, user, listener, plid, vids);
            }

            @Override
            public void
            onCancel(Dialog dialog) { }
        };
        UiUtils.buildConfirmDialog(activity,
                                   R.string.delete,
                                   isUserPlaylist(plid)? R.string.msg_delete_musics
                                                       : R.string.msg_delete_musics_completely,
                                   action)
               .show();
    }

    public static void
    doAddVideosTo(final Activity                activity,
                  final Object                  user,
                  final OnPostExecuteListener   listener,
                  final long                    dstPlid,
                  final long                    srcPlid,
                  final long[]                  vids,
                  final boolean                 move) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                listener.onPostExecute(result, user);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                DB db = DB.get();
                db.beginTransaction();
                try {
                    for (long mid : vids) {
                        DB.Err err = db.insertVideoToPlaylist(dstPlid, mid);
                        if (DB.Err.NO_ERR != err) {
                            // Error Case
                            if (DB.Err.DUPLICATED != err)
                                return Err.DB_DUPLICATED;
                            // From here : DB_DUPLICATED Case.
                            else if (1 == vids.length && !move)
                                return Err.map(err);
                        }

                        // "Insertion is OK"
                        // OR "DB_DUPLICATED but [ 'move == true' or "mids.length > 1" ]
                        if (move) {
                            if (UiUtils.isUserPlaylist(srcPlid))
                                db.deleteVideoFrom(srcPlid, mid);
                            else
                                db.deleteVideoExcept(dstPlid, mid);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(activity,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          move? R.string.moving: R.string.adding)
            .run();
    }

    public static void
    addVideosTo(final Activity                activity,
                final Object                  user,
                final OnPostExecuteListener   listener,
                final long                    plid,
                final long[]                  vids,
                final boolean                 move) {
        final long srcPlid = UiUtils.isUserPlaylist(plid)? plid: DB.INVALID_PLAYLIST_ID;
        UiUtils.OnPlaylistSelected action = new UiUtils.OnPlaylistSelected() {
            @Override
            public void
            onPlaylist(long plid, Object user) {
                doAddVideosTo(activity, user, listener, plid, srcPlid, vids, move);
            }

            @Override
            public void
            onUserMenu(int pos, Object user) {}
        };

        // exclude current playlist
        UiUtils.buildSelectPlaylistDialog(DB.get(),
                                          activity,
                                          move? R.string.move_to: R.string.add_to,
                                          null,
                                          action,
                                          srcPlid,
                                          null)
               .show();
    }

    public static void
    showVideoDetailInfo(final Activity activity, final long vid) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            final class VideoDetailInfo {
                String          title           = "";
                String          author          = "";
                String          timeAdded       = "";
                String          timeLastPlayed  = "";
                String          volume          = "";
                String          playTime        = "";
                DB.Bookmark[]   bookmarks       = null;
                // titles of playlists contain the video
                String[]        pls             = new String[0];
            }

            private VideoDetailInfo _mVdi = new VideoDetailInfo();
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                String author = Utils.getResText(R.string.author) + " : ";
                if (Utils.isValidValue(_mVdi.author))
                    author +=  _mVdi.author;
                else
                    author += Utils.getResText(R.string.unknown);

                String playbackTm = Utils.getResText(R.string.playback_time) + " : " + _mVdi.playTime
                                        + Utils.getResText(R.string.seconds);
                String volume = Utils.getResText(R.string.volume) + " : " + _mVdi.volume + " / 100";
                String timeAdded = Utils.getResText(R.string.time_added) + " : " + _mVdi.timeAdded;
                String timePlayed = Utils.getResText(R.string.time_last_played) + " : " + _mVdi.timeLastPlayed;
                String msg = _mVdi.title + "\n\n"
                             + author + "\n"
                             + playbackTm + "\n"
                             + volume + "\n"
                             + timeAdded + "\n"
                             + timePlayed + "\n";
                if (_mVdi.bookmarks.length > 0) {
                    msg += "[ " + Utils.getResText(R.string.bookmarks) + " ]\n";
                    for (DB.Bookmark bm : _mVdi.bookmarks)
                        msg += "    <" + Utils.secsToMinSecText(bm.pos / 1000) + "> " + bm.name + "\n";
                }

                msg += "\n[ " + Utils.getResText(R.string.playlist) + " ]\n";
                for (String title : _mVdi.pls)
                    msg += "* " + title + "\n";

                UiUtils.createAlertDialog(activity,
                                          0,
                                          Utils.getResText(R.string.detail_info),
                                          msg)
                       .show();
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                final int COLI_TITLE        = 0;
                final int COLI_AUTHOR       = 1;
                final int COLI_VOLUME       = 2;
                final int COLI_PLAYTIME     = 3;
                final int COLI_TIME_ADD     = 4;
                final int COLI_TIME_PLAYED  = 5;
                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                DB db = DB.get();
                Cursor c = db.queryVideo(vid, new ColVideo[] {
                        ColVideo.TITLE,
                        ColVideo.AUTHOR,
                        ColVideo.VOLUME,
                        ColVideo.PLAYTIME,
                        ColVideo.TIME_ADD,
                        ColVideo.TIME_PLAYED,
                });

                if (!c.moveToFirst()) {
                    c.close();
                    return Err.BAD_REQUEST;
                }

                _mVdi.title = c.getString(COLI_TITLE);
                _mVdi.author = c.getString(COLI_AUTHOR);
                _mVdi.volume = "" + c.getInt(COLI_VOLUME);
                _mVdi.playTime = Utils.secsToMinSecText(c.getInt(COLI_PLAYTIME));
                _mVdi.timeAdded = df.format(new Date(c.getLong(COLI_TIME_ADD)));
                long tm = c.getLong(COLI_TIME_PLAYED);
                if (0 == tm)
                    _mVdi.timeLastPlayed = Utils.getResText(R.string.not_played_yet);
                else
                    _mVdi.timeLastPlayed =df.format(new Date(c.getLong(COLI_TIME_PLAYED)));
                c.close();

                _mVdi.bookmarks = db.getBookmarks(vid);
                if (null == _mVdi.bookmarks)
                    // Unexpected error is ignored.
                    _mVdi.bookmarks = new DB.Bookmark[0];

                long[] plids = db.getPlaylistsContainVideo(vid);
                _mVdi.pls = new String[plids.length];
                for (int i = 0; i < plids.length; i++)
                    _mVdi.pls[i] = (String)db.getPlaylistInfo(plids[i], ColPlaylist.TITLE);

                return Err.NO_ERR;
            }
        };

        new DiagAsyncTask(activity,
                          worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.loading)
            .run();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // For bookmark
    // ----------------------------------------------------------------------------------------------------------------
    public static void
    showBookmarkDialog(final Activity activity, final String ytvid, String title) {
        ListView lv = (ListView)UiUtils.inflateLayout(activity, R.layout.bookmark_dialog);
        final DB db = DB.get();

        DB.Bookmark[] bms = db.getBookmarks(ytvid);
        if (0 == bms.length) {
            showTextToast(activity, R.string.msg_empty_bookmarks);
            return;
        }

        AlertDialog.Builder bldr = new AlertDialog.Builder(activity);
        bldr.setTitle("[" + Utils.getResText(R.string.bookmarks) + "]\n" + title);
        bldr.setView(lv);
        final AlertDialog diag = bldr.create();

        BookmarkListAdapter.OnItemAction action = new BookmarkListAdapter.OnItemAction() {
            @Override
            public void
            onDelete(BookmarkListAdapter adapter, int pos, Bookmark bm) {
                db.deleteBookmark(ytvid, bm.name, bm.pos);
                adapter.removeItem(pos);
                adapter.notifyDataSetChanged();
                if (0 == adapter.getCount())
                    diag.dismiss();
            }
        };
        final BookmarkListAdapter adapter = new BookmarkListAdapter(activity,
                                                                    bms,
                                                                    action);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void
            onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                DB.Bookmark bm = (DB.Bookmark)adapter.getItem(pos);
                YTPlayer mp = YTPlayer.get();
                String activeYtvid = mp.getActiveVideoYtId();
                if (ytvid.equals(activeYtvid))
                    mp.playerSeekTo(bm.pos);
                else
                    showTextToast(activity, R.string.msg_fail_seek_to_bookmark);
                diag.dismiss();
            }
        });
        diag.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // For searching similar titles
    // ----------------------------------------------------------------------------------------------------------------
    public static void
    showSimilarTitlesDialog(final Activity activity, final String title) {
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private AtomicReference<Boolean>    mCancelled = new AtomicReference<Boolean>(false);
            private LinkedList<Long>            mVids = new LinkedList<Long>();

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                float similarityThreshold = Utils.getPrefTitleSimilarityThreshold();
                SCmp scmp = new SCmp();
                scmp.setCmpParameter(title, true, null);
                Cursor c = null;
                try {
                    final int COLI_ID       = 0;
                    final int COLI_TITLE    = 1;
                    c = DB.get().queryVideos(new ColVideo[] { ColVideo.ID,
                                                              ColVideo.TITLE },
                                             null,
                                             false);
                    if (!c.moveToFirst())
                        return Err.NO_ERR;

                    int maxCnt = c.getCount();
                    int cnt = 0;
                    int prevPercent = 0;
                    int curPercent = 0;

                    task.publishProgress(0);
                    do {
                        if (DBG) {
                            String title = c.getString(COLI_TITLE);
                            P.v("Calculating similarity : " + title);
                            float sim = scmp.similarity(title);
                            P.v("    " + sim);
                        }
                        if (similarityThreshold < scmp.similarity(c.getString(COLI_TITLE))) {
                            mVids.addLast(c.getLong(COLI_ID));
                            if (mVids.size() >= Policy.MAX_SIMILAR_TITLES_RESULT)
                                return Err.NO_ERR;
                        }

                        ++cnt;
                        curPercent = cnt * 100 / maxCnt;
                        if (curPercent > prevPercent) {
                            task.publishProgress(curPercent);
                            prevPercent = curPercent;
                        }

                        if (mCancelled.get())
                            return Err.CANCELLED;
                    } while (c.moveToNext());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != c)
                        c.close();
                }
                return Err.NO_ERR;

            }

            @Override
            public void
            onPreExecute(DiagAsyncTask task) {

            }

            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                if (0 == mVids.size()) {
                    UiUtils.showTextToast(activity, R.string.msg_no_similar_titles);
                    return;
                }

                ListView lv = (ListView)UiUtils.inflateLayout(activity, R.layout.similar_title_dialog);
                AlertDialog.Builder bldr = new AlertDialog.Builder(activity);
                bldr.setTitle("[" + Utils.getResText(R.string.search_similar_titles) + "]\n" + title);
                bldr.setView(lv);
                final AlertDialog diag = bldr.create();
                final SimilarTitlesListAdapter adapter
                    = new SimilarTitlesListAdapter(activity,
                                                   Utils.convertArrayLongTolong(mVids.toArray(new Long[0])));
                lv.setAdapter(adapter);
                diag.show();
            }

            @Override
            public void
            onCancel(DiagAsyncTask task) {
                mCancelled.set(true);
            }

            @Override
            public void
            onCancelled(DiagAsyncTask task) {
            }
        };

        new DiagAsyncTask(activity,
                          worker,
                          DiagAsyncTask.Style.PROGRESS,
                          R.string.searching,
                          true,
                          false)
            .run();
    }
}
