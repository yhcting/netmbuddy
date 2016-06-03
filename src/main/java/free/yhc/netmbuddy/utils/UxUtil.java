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

package free.yhc.netmbuddy.utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

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
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.async.Task;
import free.yhc.abaselib.util.AUtil;
import free.yhc.abaselib.ux.DialogTask;
import free.yhc.netmbuddy.Err;
import free.yhc.netmbuddy.R;
import free.yhc.netmbuddy.db.ColPlaylist;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.db.DB.Bookmark;
import free.yhc.netmbuddy.core.PolicyConstant;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.scmp.SCmp;
import free.yhc.netmbuddy.task.YTHackTask;

public class UxUtil extends free.yhc.abaselib.util.UxUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(free.yhc.netmbuddy.utils.UxUtil.class, Logger.LOGLV_DEFAULT);

    public static final long PLID_INVALID = DB.INVALID_PLAYLIST_ID;
    // Special playlist id that represents that this is unknown non-user playlist.
    public static final long PLID_UNKNOWN = PLID_INVALID - 1;
    public static final long PLID_RECENT_PLAYED = PLID_INVALID - 2;
    public static final long PLID_SEARCHED = PLID_INVALID - 3;

    // NOTE
    // To save time for decoding image, pre-decoded bitmap is uses for unknown thumbnail image.
    private static final Bitmap sBmIcUnknownImage
        = BitmapFactory.decodeResource(AppEnv.getAppContext().getResources(), R.drawable.ic_unknown_image);

    public interface EditTextAction {
        void prepare(Dialog dialog, EditText edit);
        void onOk(Dialog dialog, EditText edit);
    }

    public interface OnPlaylistSelected {
        void onUserMenu(int pos, Object tag);
        void onPlaylist(long plid, Object tag);
    }

    public interface OnMenuSelected {
        void onSelected(int pos, int menuTitle);
    }

    public interface OnPostExecuteListener {
        void onPostExecute(Err result, Object tag);
    }

    // ========================================================================
    //
    // Functions
    //
    // ========================================================================

    // ------------------------------------------------------------------------
    //
    // Dialog
    //
    // ------------------------------------------------------------------------
    public static AlertDialog
    buildConfirmDialog(@NonNull final Context context,
                       @NonNull final CharSequence title,
                       @NonNull final CharSequence description,
                       @NonNull final ConfirmAction action) {
        return free.yhc.abaselib.util.UxUtil.buildConfirmDialog(
                context,
                title,
                description,
                R.drawable.ic_info,
                AUtil.getResText(R.string.yes),
                AUtil.getResText(R.string.no),
                action);
    }

    public static AlertDialog
    buildConfirmDialog(@NonNull final Context context,
                       final int title,
                       final int description,
                       @NonNull final ConfirmAction action) {
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

    @SuppressWarnings("unused")
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
        View layout = AUtil.inflateLayout(R.layout.edittext_dialog);
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

    @SuppressWarnings("unused")
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
    buildPopupMenuDialog(final Activity activity,
                         final OnMenuSelected action,
                         final int diagTitle,
                         final int[] menuTitles) {
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
    buildSelectPlaylistDialog(final DB db,
                              final Context context,
                              final int diagTitle,
                              final String[] userMenuStrings,
                              final OnPlaylistSelected action,
                              long plidExcluded,
                              final Object tag) {
        final String[] userMenus = (null == userMenuStrings)? new String[0]: userMenuStrings;

        // Create menu list
        final Cursor c = db.queryPlaylist(new ColPlaylist[] { ColPlaylist.ID,
                                                              ColPlaylist.TITLE });

        final int iTitle = c.getColumnIndex(ColPlaylist.TITLE.getName());
        final int iId = c.getColumnIndex(ColPlaylist.ID.getName());

        LinkedList<String> menul = new LinkedList<>();
        LinkedList<Long> idl = new LinkedList<>();

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

        final String[] menus = menul.toArray(new String[menul.size()]);
        final long[] ids = Util.convertArrayLongTolong(idl.toArray(new Long[idl.size()]));

        AlertDialog.Builder bldr = new AlertDialog.Builder(context);
        if (diagTitle > 0)
            bldr.setTitle(diagTitle);
        ArrayAdapter<String> adapter
            = new ArrayAdapter<>(context, android.R.layout.select_dialog_item, menus);
        bldr.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface dialog, int which) {
                P.bug(which >= 0);
                dialog.dismiss();
                if (userMenus.length > which) {
                    // User menu is selected.
                    action.onUserMenu(which, tag);
                } else if (userMenus.length == which) {
                    // Need to get new playlist name.
                    free.yhc.netmbuddy.utils.UxUtil.EditTextAction edAction = new free.yhc.netmbuddy.utils.UxUtil.EditTextAction() {
                        @Override
                        public void
                        prepare(Dialog dialog, EditText edit) {
                        }

                        @Override
                        public void
                        onOk(Dialog dialog, EditText edit) {
                            String title = edit.getText().toString();
                            if (db.containsPlaylist(title)) {
                                showTextToast(R.string.msg_existing_playlist);
                                return;
                            }

                            long plid = db.insertPlaylist(title);
                            if (plid < 0) {
                                showTextToast(R.string.err_db_unknown);
                                return;
                            }

                            action.onPlaylist(plid, tag);
                        }
                    };
                    free.yhc.netmbuddy.utils.UxUtil.buildOneLineEditTextDialog(context, R.string.enter_playlist_title, edAction)
                           .show();
                } else
                    action.onPlaylist(ids[which], tag);
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
                Uri.parse(YTHackTask.getYtVideoPageUrl(ytvid)));
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            showTextToast(R.string.msg_fail_find_app);
        }
    }

    public static boolean
    isUserPlaylist(long plid) {
        return plid >= 0;
    }

    public static void
    doDeleteVideos(@NonNull final Activity activity,
                   final Object user,
                   final OnPostExecuteListener listener,
                   final long plid,
                   @NonNull final long[] mids) {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void
            doAsync() {
                DB db = DB.get();
                db.beginTransaction();
                try {
                    for (long mid : mids) {
                        if (isUserPlaylist(plid))
                            db.deleteVideoFrom(plid, mid);
                        else
                            db.deleteVideoFromAllPlaylist(mid);
                    }
                    db.setTransactionSuccessful();
                    if (null != listener)
                        listener.onPostExecute(Err.NO_ERR, user);
                } finally {
                    db.endTransaction();
                }
                return null;
            }
        };
        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(activity, t);
        b.setCancelButtonText(R.string.cancel)
                .setMessage(R.string.deleting);
        if (!b.create().start())
            P.bug();
    }

    public static void
    deleteVideos(@NonNull final Activity activity,
                 final Object user, // passed to callback
                 final OnPostExecuteListener listener,
                 final long plid,
                 @NonNull final long[] vids) {
        ConfirmAction action
                = new ConfirmAction() {
            @Override
            public void
            onPositive(@NonNull Dialog dialog) {
                doDeleteVideos(activity, user, listener, plid, vids);
            }

            @Override
            public void
            onNegative(@NonNull Dialog dialog) { }
        };
        buildConfirmDialog(activity,
                           R.string.delete,
                           isUserPlaylist(plid)? R.string.msg_delete_musics
                                   : R.string.msg_delete_musics_completely,
                           action)
               .show();
    }

    public static void
    doAddVideosTo(@NonNull final Activity activity,
                  final Object user,
                  final OnPostExecuteListener listener,
                  final long dstPlid,
                  final long srcPlid,
                  @NonNull final long[] vids,
                  final boolean move) {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void
            doAsync() {
                Err err = Err.NO_ERR;
                DB db = DB.get();
                db.beginTransaction();
                try {
                    for (long mid : vids) {
                        DB.Err dbErr = db.insertVideoToPlaylist(dstPlid, mid);
                        if (DB.Err.NO_ERR != dbErr) {
                            // Error Case
                            if (DB.Err.DUPLICATED != dbErr
                                    || 1 == vids.length && !move)
                                err = Err.map(dbErr);
                            // From here : DB_DUPLICATED Case.
                        } else {
                            // "Insertion is OK"
                            // OR "DB_DUPLICATED but [ 'move == true' or "mids.length > 1" ]
                            if (move) {
                                if (free.yhc.netmbuddy.utils.UxUtil.isUserPlaylist(srcPlid))
                                    db.deleteVideoFrom(srcPlid, mid);
                                else
                                    db.deleteVideoExcept(dstPlid, mid);
                            }
                        }
                        db.setTransactionSuccessful();
                    }
                } finally {
                    db.endTransaction();
                }

                final Err result = err;
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPostExecute(result, user);
                    }
                });
                return null;
            }
        };
        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(activity, t);
        b.setCancelButtonText(R.string.cancel)
                .setMessage(move? R.string.moving: R.string.adding);
        if (!b.create().start())
            P.bug();
    }

    public static void
    addVideosTo(final Activity activity,
                final Object tag,
                final OnPostExecuteListener listener,
                final long plid,
                final long[] vids,
                final boolean move) {
        final long srcPlid = free.yhc.netmbuddy.utils.UxUtil.isUserPlaylist(plid)? plid: DB.INVALID_PLAYLIST_ID;
        free.yhc.netmbuddy.utils.UxUtil.OnPlaylistSelected action = new free.yhc.netmbuddy.utils.UxUtil.OnPlaylistSelected() {
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
        free.yhc.netmbuddy.utils.UxUtil.buildSelectPlaylistDialog(DB.get(),
                                                                  activity,
                                                                  move? R.string.move_to: R.string.add_to,
                                                                  null,
                                                                  action,
                                                                  srcPlid,
                                                                  tag)
               .show();
    }

    public static void
    showVideoDetailInfo(final Activity activity, final long vid) {
        Task<Void> t = new Task<Void>() {
            private void
            postExecute(DMVideo dmv, String[] pls) {
                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String channel = AUtil.getResString(R.string.channel) + " : ";
                if (Util.isValidValue(dmv.channelTitle))
                    channel += dmv.channelTitle;
                else
                    channel += AUtil.getResString(R.string.unknown);

                String strTimePlayed;
                if (0 == dmv.extra.timePlayed)
                    strTimePlayed = AUtil.getResString(R.string.not_played_yet);
                else
                    strTimePlayed = df.format(new Date(dmv.extra.timePlayed));

                DB.Bookmark[] bookmarks;
                if (null == dmv.bookmarks)
                    // Unexpected error is ignored.
                    bookmarks = new DB.Bookmark[0];
                else
                    bookmarks = DB.Bookmark.decode(dmv.bookmarks);

                String playbackTm = AUtil.getResString(R.string.playback_time) + " : " + dmv.playtime
                        + AUtil.getResString(R.string.seconds);
                String volume = AUtil.getResString(R.string.volume) + " : " + dmv.volume + " / 100";
                String timeAdded = AUtil.getResString(R.string.time_added) + " : " + dmv.extra.timeAdd;
                String timePlayed = AUtil.getResString(R.string.time_last_played) + " : " + strTimePlayed;
                String ytvid = AUtil.getResString(R.string.youtube_id) + " : " + dmv.ytvid;
                String msg = dmv.title + "\n\n"
                        + ytvid + "\n"
                        + channel + "\n"
                        + playbackTm + "\n"
                        + volume + "\n"
                        + timeAdded + "\n"
                        + timePlayed + "\n";
                if (bookmarks.length > 0) {
                    msg += "[ " + AUtil.getResString(R.string.bookmarks) + " ]\n";
                    for (DB.Bookmark bm : bookmarks)
                        msg += "    <" + Util.secsToMinSecText(bm.pos / 1000) + "> " + bm.name + "\n";
                }

                msg += "\n[ " + AUtil.getResString(R.string.playlist) + " ]\n";
                for (String title : pls)
                    msg += "* " + title + "\n";

                createAlertDialog(activity,
                                  0,
                                  AUtil.getResString(R.string.detail_info),
                                  msg)
                        .show();
            }

            @Override
            protected Void
            doAsync() {
                final DB db = DB.get();
                final DMVideo dmv = db.getVideoInfo(vid, DMVideo.sDBProjectionExtraWithoutThumbnail);
                long[] plids = db.getPlaylistsContainVideo(vid);
                final String[] pls = new String[plids.length];
                for (int i = 0; i < plids.length; i++)
                    pls[i] = (String)db.getPlaylistInfo(plids[i], ColPlaylist.TITLE);
                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        postExecute(dmv, pls);
                    }
                });
                return null;
            }
        };

        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(activity, t);
        b.setCancelButtonText(R.string.cancel);
        b.setMessage(R.string.loading);
        if (!b.create().start())
            P.bug();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // For bookmark
    // ----------------------------------------------------------------------------------------------------------------
    public static void
    showBookmarkDialog(final Activity activity, final String ytvid, String title) {
        ListView lv = (ListView)AUtil.inflateLayout(R.layout.bookmark_dialog);
        final DB db = DB.get();

        DB.Bookmark[] bms = db.getBookmarks(ytvid);
        if (0 == bms.length) {
            showTextToast(R.string.msg_empty_bookmarks);
            return;
        }

        AlertDialog.Builder bldr = new AlertDialog.Builder(activity);
        bldr.setTitle("[" + AUtil.getResString(R.string.bookmarks) + "]\n" + title);
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
                    showTextToast(R.string.msg_fail_seek_to_bookmark);
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
        Task<Void> t = new Task<Void>() {
            private void
            postExecute(final LinkedList<Long> vidl) {
                if (0 == vidl.size()) {
                    showTextToast(R.string.msg_no_similar_titles);
                    return;
                }

                ListView lv = (ListView)AUtil.inflateLayout(R.layout.similar_title_dialog);
                AlertDialog.Builder bldr = new AlertDialog.Builder(activity);
                bldr.setTitle("[" + AUtil.getResString(R.string.search_similar_titles) + "]\n" + title);
                bldr.setView(lv);
                final AlertDialog diag = bldr.create();
                final SimilarTitlesListAdapter adapter
                        = new SimilarTitlesListAdapter(
                        activity,
                        Util.convertArrayLongTolong(vidl.toArray(new Long[vidl.size()])));
                lv.setAdapter(adapter);
                diag.show();
            }

            @Override
            protected Void
            doAsync() throws InterruptedException {
                final LinkedList<Long> vidl = new LinkedList<>();
                float similarityThreshold = Util.getPrefTitleSimilarityThreshold();
                SCmp scmp = new SCmp();
                scmp.setCmpParameter(title, true, null);
                try (Cursor c = DB.get().queryVideos(
                        new ColVideo[] { ColVideo.ID,
                                         ColVideo.TITLE },
                        null,
                        false)) {
                    final int COLI_ID = 0;
                    final int COLI_TITLE = 1;
                    if (!c.moveToFirst())
                        return null;

                    int maxCnt = c.getCount();

                    long progcnt = 0;
                    publishProgressInit(maxCnt);
                    publishProgress(progcnt);

                    do {
                        if (DBG) {
                            String title = c.getString(COLI_TITLE);
                            P.v("Calculating similarity : " + title);
                            float sim = scmp.similarity(title);
                            P.v("    " + sim);
                        }
                        if (similarityThreshold < scmp.similarity(c.getString(COLI_TITLE))) {
                            vidl.addLast(c.getLong(COLI_ID));
                            if (vidl.size() >= PolicyConstant.MAX_SIMILAR_TITLES_RESULT)
                                return null;
                        }

                        publishProgress(++progcnt);
                        if (isCancel())
                            throw new InterruptedException();
                    } while (c.moveToNext());
                }

                AppEnv.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        postExecute(vidl);
                    }
                });
                return null;
            }
        };

        DialogTask.Builder<DialogTask.Builder> b
                = new DialogTask.Builder<>(activity, t);
        b.setStyle(DialogTask.Style.PROGRESS);
        b.setCancelButtonText(R.string.cancel);
        b.setMessage(R.string.searching);
        if (!b.create().start())
            P.bug();
    }
}
