package com.afollestad.overhear.views;

import com.afollestad.overhear.R;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Playlist;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public class PlaylistCreationDialog extends Dialog {

	public PlaylistCreationDialog(Context context) {
		super(context);
	}
	public PlaylistCreationDialog(Context context, int theme) {
		super(context, theme);
	}
	public PlaylistCreationDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	private Playlist toRename;
	private Playlist newList;
	private Handler mHandler = new Handler();
	private Runnable mHandlerRunner = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.input_dialog);
		setTitle(R.string.create_playlist);
		setCancelable(true);
		initializeInput();
	}

	private void scheduleInputCheck() {
		if(mHandlerRunner != null)
			mHandler.removeCallbacks(mHandlerRunner);
		mHandlerRunner = new Runnable() {
			@Override
			public void run() {
				String name = ((EditText)findViewById(R.id.input)).getText().toString().trim();
				boolean foundName = Playlist.get(getContext(), name) != null;
				findViewById(R.id.ok).setEnabled(!name.isEmpty() && !foundName);
			}
		};
		mHandler.postDelayed(mHandlerRunner, 300);
	}
	
	private void initializeInput() {
		final EditText input = (EditText)findViewById(R.id.input);
		input.setHint(R.string.new_playlist_name_hint);

		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
			}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
				findViewById(R.id.ok).setEnabled(false);
				scheduleInputCheck();
			}
			@Override
			public void afterTextChanged(Editable editable) {
			}
		});
		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
				if (toRename != null) {
					toRename.rename(getContext(), input.getText().toString().trim());
				} else {
					newList = Playlist.create(getContext(), input.getText().toString().trim());
				}
				getContext().sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));
			}
		});
		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
	}


	public PlaylistCreationDialog setRenamePlaylist(Playlist list) {
		toRename = list;
		if(list != null)
			setTitle(R.string.rename);
		else
			setTitle(R.string.create_playlist);
		return this;
	}

	public Playlist getCreatedPlaylist() {
		return newList;
	}
}