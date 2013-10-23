package com.afollestad.overhear.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static final int DOUBLE_CLICK = 500;
    private static long mLastClickTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;
            final int keycode = event.getKeyCode();
            final int action = event.getAction();
            final long eventtime = event.getEventTime();

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MusicService.ACTION_STOP;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MusicService.ACTION_TOGGLE_PLAYBACK;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MusicService.ACTION_SKIP;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MusicService.ACTION_REWIND;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MusicService.ACTION_PAUSE;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) {
                        /**
                         * If another app received the broadcast first, this if statement will skip.
                         */
                        final Intent i = new Intent(context, MusicService.class);
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK
                                && eventtime - mLastClickTime < DOUBLE_CLICK) {
                            i.setAction(MusicService.ACTION_SKIP);
                            mLastClickTime = 0;
                        } else {
                            i.setAction(command);
                            mLastClickTime = eventtime;
                        }
                        context.startService(i);
                    }
                }
                if (isOrderedBroadcast())
                    abortBroadcast();
            }
        }
    }
}
