package com.afollestad.overhear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    private static long mLastClickTime = 0;
    private static boolean mDown = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();
            int buttonId = intent.getIntExtra("buttonId", 0);

            // single quick press: pause/resume.
            // double press: next track
            // long press: TODO

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MusicService.ACTION_STOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MusicService.ACTION_TOGGLE_PLAYBACK;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MusicService.ACTION_SKIP;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MusicService.ACTION_REWIND;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                	command = MusicService.ACTION_PAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                	command = MusicService.ACTION_PLAY;
                    break;
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown && command.equals(MusicService.ACTION_PLAY)) {
                        //TODO long press action
                    	Toast.makeText(context, "Headset long pressed", Toast.LENGTH_LONG).show();
                    } else if (event.getRepeatCount() == 0) {
                        /**
                         * only consider the first event in a sequence, not the repeat events
                         * so that we don't trigger in cases where the first event went to
                         * a different app (e.g. when the user ends a phone call by long pressing 
                         * the headset button). The service may or may not be running, but we need to 
                         * send it a command.
                         */
                        Intent i = new Intent(context, MusicService.class);
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK
                                && eventtime - mLastClickTime < 300) {
                        	i.setAction(MusicService.ACTION_SKIP);
                            context.startService(i);
                            mLastClickTime = 0;
                        } else {
                        	i.setAction(command);
                            context.startService(i);
                            mLastClickTime = eventtime;
                        }                      
                        if (buttonId == 0) {
                            mDown = true;
                        }
                    }
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        }
    }
}
