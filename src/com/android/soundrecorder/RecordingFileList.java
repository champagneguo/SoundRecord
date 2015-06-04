package com.android.soundrecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ntian.nguiwidget.actionbar.NTActionBarSplitManager;

public class RecordingFileList extends Activity implements
		ImageButton.OnClickListener, Player.PlayerListener {
	private static final String TAG = "SR/RecordingFileList";
	private static final String DIALOG_TAG_DELETE = "Delete";
	private static final String DIALOG_TAG_PROGRESS = "Progress";
	private static final String RECORDING_FILELIST_DATA = "recording_filelist_data";
	private static final String REMOVE_PROGRESS_DIALOG_KEY = "remove_progress_dialog";
	public static final int NORMAL = 1;
	public static final int EDIT = 2;

	private static final String PATH = "path";
	private static final String DURATION = "duration";
	private static final String FILE_NAME = "filename";
	private static final String CREAT_DATE = "creatdate";
	private static final String FORMAT_DURATION = "formatduration";
	private static final String RECORD_ID = "recordid";
	private static final int PATH_INDEX = 2;
	private static final int DURATION_INDEX = 3;
	private static final int CREAT_DATE_INDEX = 6;
	private static final int RECORD_ID_INDEX = 7;
	private static final int ONE_SECOND = 1000;
	private static final int TIME_BASE = 60;
	private static final int NO_CHECK_POSITION = -1;
	private static final int DEFAULT_SLECTION = -1;
	private int mCurrentAdapterMode = NORMAL;
	private int mSelection = 0;
	private int mTop = 0;
	private boolean mNeedRemoveProgressDialog = false;

	// public boolean isShow = false;
	private boolean showUp = false;
	private RelativeLayout frameUp,list_progress_up;

	private ProgressBar mStateProgressBar;
	private ActionBar ruimeiActionBar;
	private TextView tv_count, all_pick, tv_cancel;
	private ImageButton list_play_btn, list_stop_btn, list_record_btn,
			list_file_btn;
	private ImageButton frame_up_btn_rename, frame_up_btn_del,
			frame_up_btn_send;

	
	private String str_choose_pre;
	private String str_choose_aft;
	
	private TextView tv_progress, tv_duration;
	private final ArrayList<HashMap<String, Object>> mArrlist = new ArrayList<HashMap<String, Object>>();
	private final ArrayList<String> mNameList = new ArrayList<String>();
	private final ArrayList<String> mPathList = new ArrayList<String>();
	private final ArrayList<String> mTitleList = new ArrayList<String>();
	private final ArrayList<String> mDurationList = new ArrayList<String>();
	private final List<Integer> mIdList = new ArrayList<Integer>();
	private List<Integer> mCheckedList = new ArrayList<Integer>();

	private BroadcastReceiver mSDCardMountEventReceiver = null;
	private boolean mActivityForeground = true;

	private ListView mRecordingFileListView;
	// private ImageButton mRecordButton;
	// private ImageButton mDeleteButton;
	private View mEmptyView;
	
	private RelativeLayout list_back_rela;

	private int mCurrentState = 1;

	public static final int STATE_PLAYING = 4;
	public static final int STATE_PAUSE_PLAYING = 5;
	public static final int STATE_IDLE = 1;

	private int pre = -1;
	private int curr = -1;
	
	private Player mPlayer = null;
	private final Handler mHandler = new Handler();
	private final Runnable mUpdateTimer = new Runnable() {

//		@Override
		public void run() {
//			Log.v("TAG", "RUN");
			if (mCurrentState == STATE_PLAYING) {
				updateView();
				mHandler.postDelayed(mUpdateTimer, 100);
			}
		}
	};

	// private SoundRecorderService mService = null;

	private final DialogInterface.OnClickListener mDeleteDialogListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			LogUtils.i(TAG, "<mDeleteDialogListener onClick>");
			deleteItems();
			arg0.dismiss();
		}
	};

	private void updateView() {

		if (mCurrentState == STATE_IDLE) {
			tv_progress.setText("00:00");
			
			if (mPlayer != null) {
				tv_duration.setText(getTimer(mPlayer.getFileDuration()));
			}else{
				Toast.makeText(RecordingFileList.this, "mPlayer == null", Toast.LENGTH_SHORT).show();
			}
			
			mStateProgressBar.setProgress(0);
			mStateProgressBar.setVisibility(View.GONE);
		} else if (mCurrentState == STATE_PAUSE_PLAYING) {
			
			mStateProgressBar.setVisibility(View.VISIBLE);
			mStateProgressBar.setProgress((int) (100 * mPlayer
					.getCurrentProgress() / mPlayer.getFileDuration()));

		} else {
			mStateProgressBar.setVisibility(View.VISIBLE);
			mStateProgressBar.setProgress((int) (100 * mPlayer
					.getCurrentProgress() / mPlayer.getFileDuration()));

			tv_duration.setText(getTimer(mPlayer.getFileDuration()));
			tv_progress.setText(getTimer(mPlayer.getCurrentProgress()));
		}

	}

	public String getTimer(int ms) {
		SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
		String hms = formatter.format(ms);
		return hms;
	}
	
	public String getCreateDate(int ms) {
		SimpleDateFormat myFmt = new SimpleDateFormat(getResources().getString(R.string.date_fromat_pattern));
		String hms = myFmt.format(ms);
		return hms;
	}
	
	public String getCreateNum(int ms) {
		SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd");
		String hms = myFmt.format(ms);
		return hms;
	}

	@Override
	public void onCreate(Bundle icycle) {
		super.onCreate(icycle);
		LogUtils.i(TAG, "<onCreate> begin");
		setContentView(R.layout.recording_file_list);

		str_choose_pre = this.getResources().getString(R.string.ruiemi_choose_str);
		str_choose_aft = this.getResources().getString(R.string.ruiemi_choose_last);
		mPlayer = new Player(this);

		pre = 0;
		curr = 0;
		
		list_progress_up = (RelativeLayout) this.findViewById(R.id.bottom_rela);
		mStateProgressBar = (ProgressBar) this
				.findViewById(R.id.list_stateProgressBar);
		
		//		mStateProgressBar.setVisibility(View.VISIBLE);
		// list_play_btn,list_stop_btn,list_record_btn,list_file_btn;
		list_play_btn = (ImageButton) this.findViewById(R.id.list_playButton);
		list_stop_btn = (ImageButton) this.findViewById(R.id.list_stopButton);
		list_record_btn = (ImageButton) this
				.findViewById(R.id.list_recordButton);
		list_file_btn = (ImageButton) this
				.findViewById(R.id.list_recordListButton);
		// ImageButton frame_up_btn_rename,frame_up_btn_del,frame_up_btn_send

		tv_progress = (TextView) this.findViewById(R.id.text_progress);
		tv_duration = (TextView) this.findViewById(R.id.text_duration);

		frame_up_btn_rename = (ImageButton) this
				.findViewById(R.id.frame_up_btn_rename);
		frame_up_btn_del = (ImageButton) this
				.findViewById(R.id.frame_up_btn_del);
		frame_up_btn_send = (ImageButton) this
				.findViewById(R.id.frame_up_btn_share);

		list_file_btn.setOnClickListener(this);
		list_play_btn.setOnClickListener(this);
		list_record_btn.setOnClickListener(this);
		list_stop_btn.setOnClickListener(this);

		frame_up_btn_rename.setOnClickListener(this);
		frame_up_btn_del.setOnClickListener(this);
		frame_up_btn_send.setOnClickListener(this);

		frameUp = (RelativeLayout) this.findViewById(R.id.frame_up_view);

		// ActionBar actionBar = getActionBar();
		// Resources r = getResources();
		// Drawable myDrawable =
		// r.getDrawable(R.drawable.ruimei_new_actionbar_bg);
		// actionBar.setBackgroundDrawable(myDrawable);
		ruimeiActionBar = getActionBar();
		ruimeiActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		ruimeiActionBar.setCustomView(R.layout.simple_actionbar);

		list_back_rela = (RelativeLayout) getActionBar().getCustomView().findViewById(R.id.list_back_rela);
		list_back_rela.setOnClickListener(this);
		
		mRecordingFileListView = (ListView) findViewById(R.id.recording_file_list_view);
		// mRecordButton = (ImageButton) findViewById(R.id.recordButton);
		// mDeleteButton = (ImageButton) findViewById(R.id.deleteButton);
		mEmptyView = findViewById(R.id.list_empty_view);
		// mRecordButton.setOnClickListener(this);
		// mDeleteButton.setOnClickListener(this);
		
		mRecordingFileListView.setOnCreateContextMenuListener(this);

		mRecordingFileListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long itemId) {
						if (EDIT == mCurrentAdapterMode) {
							int id = (int) ((EditViewAdapter) mRecordingFileListView
									.getAdapter()).getItemId(position);
							CheckBox checkBox = (CheckBox) view
									.findViewById(R.id.record_file_checkbox);
							if (checkBox.isChecked()) {
								checkBox.setChecked(false);
								((EditViewAdapter) mRecordingFileListView
										.getAdapter()).setCheckBox(id, false);
								int count = ((EditViewAdapter) mRecordingFileListView
										.getAdapter()).getCheckedItemsCount();
								if (0 == count) {
									saveLastSelection();
									mCurrentAdapterMode = NORMAL;
									swicthAdapterView(NO_CHECK_POSITION);
								} else {
										if (tv_count == null) {
											tv_count = (TextView) getActionBar()
													.getCustomView()
													.findViewById(
															R.id.nt_multichoice_text_select);
										}
										tv_count.setText(str_choose_pre + count
												+ str_choose_aft);
								}

								if (all_pick == null) {
									all_pick = (TextView) getActionBar()
											.getCustomView()
											.findViewById(
													R.id.nt_multichoice_all_pick);
								}
								all_pick.setText(R.string.ruiemi_choose_all);

							} else {
								checkBox.setChecked(true);
								((EditViewAdapter) mRecordingFileListView
										.getAdapter()).setCheckBox(id, true);
								int count_all = ((EditViewAdapter) mRecordingFileListView
										.getAdapter()).getCheckedItemsCount();
								
								if (tv_count == null) {
									tv_count = (TextView) getActionBar()
											.getCustomView()
											.findViewById(
													R.id.nt_multichoice_text_select);
								}
								tv_count.setText(str_choose_pre + count_all
										+ str_choose_aft);
								
								if (count_all == mRecordingFileListView
										.getCount()) {
									if (all_pick == null) {
										all_pick = (TextView) getActionBar()
												.getCustomView()
												.findViewById(
														R.id.nt_multichoice_all_pick);
									}
									all_pick.setText(R.string.ruiemi_choose_none);
								} else {
									if (all_pick == null) {
										all_pick = (TextView) getActionBar()
												.getCustomView()
												.findViewById(
														R.id.nt_multichoice_all_pick);
									}
									all_pick.setText(R.string.ruiemi_choose_all);
								}
							}
							
							try {
								int final_count = ((EditViewAdapter) mRecordingFileListView
										.getAdapter()).getCheckedItemsCount();
								if (final_count == 0) {
									// do nothing
								} else if (final_count == 1) {
									frame_up_btn_rename.setClickable(true);
									frame_up_btn_rename.setImageResource(R.drawable.ruimei_list_btn_rename);
								}else{
									frame_up_btn_rename.setClickable(false);
									frame_up_btn_rename.setImageResource(R.drawable.ruimei_rename_unuse);
								}
							} catch (Exception e) {
								//do nothing
//								Toast.makeText(RecordingFileList.this, "meet error", Toast.LENGTH_SHORT).show();
							}
							
						} else {
							pre = position;
							((RuimeiSimpleAdapter)mRecordingFileListView.getAdapter()).notifyDataSetChanged();
//							TextView curr = (TextView)view.findViewById(R.id.record_file_name);
//							curr.setTextColor(Color.RED);
							
							showProgress(true);
							
							mPlayer.stopPlayback();
							mPlayer.reset();

							HashMap<String, Object> map = (HashMap<String, Object>) mRecordingFileListView
									.getItemAtPosition(position);
							if (map != null) {
								mPlayer.setCurrentFilePath(map.get(PATH)
										.toString());
								if (mPlayer != null) {
									mPlayer.startPlayback();
								}
							}

							// Intent intent = new Intent();
							// HashMap<String, Object> map = (HashMap<String,
							// Object>) mRecordingFileListView
							// .getItemAtPosition(position);
							// intent.putExtra(SoundRecorder.DOWHAT,
							// SoundRecorder.PLAY);
							// if (null != map) {
							// if (null != map.get(PATH)) {
							// intent.putExtra(PATH, map.get(PATH)
							// .toString());
							// }
							// if (null != map.get(DURATION)) {
							// intent.putExtra(DURATION, Integer
							// .parseInt(map.get(DURATION)
							// .toString()));
							// }
							// }
							// intent.setClass(RecordingFileList.this,
							// SoundRecorder.class);
							// setResult(RESULT_OK, intent);
							// finish();
						}
					}
				});

		mRecordingFileListView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long itemId) {

						curr = position;
						mPlayer.stopPlayback();
						mPlayer.reset();
						mStateProgressBar.setProgress(0);
						mStateProgressBar.setVisibility(View.GONE);
						
						frame_up_btn_rename.setClickable(true);
						frame_up_btn_rename.setImageResource(R.drawable.ruimei_list_btn_rename);
						
						int id = 0;
						if (EDIT == mCurrentAdapterMode) {
							id = (int) ((EditViewAdapter) mRecordingFileListView
									.getAdapter()).getItemId(position);
						} else {
							HashMap<String, Object> map = (HashMap<String, Object>) mRecordingFileListView
									.getItemAtPosition(position);
							id = (Integer) map.get(RECORD_ID);
						}
						if (mCurrentAdapterMode == NORMAL) {
							saveLastSelection();
							mCurrentAdapterMode = EDIT;
							swicthAdapterView(id);
						}
						return true;
					}
				});
		
		registerExternalStorageListener();
		LogUtils.i(TAG, "<onCreate> end");
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		LogUtils.i(TAG, "<onRetainNonConfigurationInstance> begin");
		List<Integer> checkedList = null;
		saveLastSelection();
		if (EDIT == mCurrentAdapterMode) {
			if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
				checkedList = ((EditViewAdapter) mRecordingFileListView
						.getAdapter()).getCheckedPosList();
				LogUtils.i(TAG,
						"<onRetainNonConfigurationInstance> checkedList.size() = "
								+ checkedList.size());
			}
		}
		ListViewProperty listViewProperty = new ListViewProperty(checkedList,
				mSelection, mTop);

		SharedPreferences prefs = getSharedPreferences(RECORDING_FILELIST_DATA,
				0);
		SharedPreferences.Editor ed = prefs.edit();
		LogUtils.i(TAG,
				"<onRetainNonConfigurationInstance> mNeedRemoveProgressDialog = "
						+ mNeedRemoveProgressDialog);
		ed.putBoolean(REMOVE_PROGRESS_DIALOG_KEY, mNeedRemoveProgressDialog);
		ed.commit();
		LogUtils.i(TAG, "<onRetainNonConfigurationInstance> end");
		return listViewProperty;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		LogUtils.i(TAG, "<onRestoreInstanceState> begin");
		Fragment fragment = getFragmentManager().findFragmentByTag(
				DIALOG_TAG_DELETE);
		LogUtils.i(TAG, "<onRestoreInstanceState> getFragmentManager() = "
				+ getFragmentManager());
		if (null != fragment) {
			((DeleteDialogFragment) fragment)
					.setOnClickListener(mDeleteDialogListener);
			LogUtils.i(TAG, "<onRestoreInstanceState> getFragmentManager() = "
					+ getFragmentManager());
		}
		SharedPreferences prefs = getSharedPreferences(RECORDING_FILELIST_DATA,
				0);
		if (prefs.getBoolean(REMOVE_PROGRESS_DIALOG_KEY, false)) {
			removeOldFragmentByTag(DIALOG_TAG_PROGRESS);
		}
		mNeedRemoveProgressDialog = false;
		LogUtils.i(TAG, "<onRestoreInstanceState> end");
	}

	@Override
	protected void onResume() {
		super.onResume();
		LogUtils.i(TAG, "<onResume> begin");
		setListData(mCheckedList);
		mActivityForeground = true;
		LogUtils.i(TAG, "<onResume> end");
		
//		if (mRecordingFileListView.getCount() == 0) {
			showProgress(false);
//		}else{
//			showProgress(true);
//		}
		if (mPlayer == null) {
			mPlayer = new Player(this);
		}
		
		// NTActionBarSplitManager.getInstance(this).setMaxDisplayCount(3);
		// NTActionBarSplitManager.getInstance(this).buildMenu(this, this);
	}

	/**
	 * This method save the selection of list view on present screen
	 */
	protected void saveLastSelection() {
		LogUtils.i(TAG, "<saveLastSelection>");
		if (null != mRecordingFileListView) {
			mSelection = mRecordingFileListView.getFirstVisiblePosition();
			View cv = mRecordingFileListView.getChildAt(0);
			if (null != cv) {
				mTop = cv.getTop();
			}
		}
	}

	/**
	 * This method restore the selection saved before
	 */
	protected void restoreLastSelection() {
		LogUtils.i(TAG, "<restoreLastSelection>");
		if (mSelection >= 0) {
			mRecordingFileListView.setSelectionFromTop(mSelection, mTop);
			mSelection = DEFAULT_SLECTION;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		LogUtils.i(TAG, "<onStart> begin");
		ListViewProperty listViewProperty = (ListViewProperty) getLastNonConfigurationInstance();
		if (null != listViewProperty) {
			if (null != listViewProperty.getCheckedList()) {
				mCheckedList = listViewProperty.getCheckedList();
			}
			mSelection = listViewProperty.getCurPos();
			mTop = listViewProperty.getTop();
		}
		LogUtils.i(TAG, "<onStart> end");
	}

	/**
	 * bind data to list view
	 * 
	 * @param list
	 *            the index list of current checked items
	 */
	private void setListData(List<Integer> list) {
		LogUtils.i(TAG, "<setListData>");
		mRecordingFileListView.setAdapter(null);
		QueryDataTask queryTask = new QueryDataTask(list);
		queryTask.execute();
	}

	/**
	 * query sound recorder recording file data
	 * 
	 * @return the query list of the map from String to Object
	 */
	public ArrayList<HashMap<String, Object>> queryData() {
		LogUtils.i(TAG, "<queryData>");
		mArrlist.clear();
		mNameList.clear();
		mPathList.clear();
		mTitleList.clear();
		mDurationList.clear();
		mIdList.clear();

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(MediaStore.Audio.Media.IS_MUSIC);
		stringBuilder.append(" =0 and ");
		stringBuilder.append(MediaStore.Audio.Media.DATA);
		stringBuilder.append(" LIKE '%");
		stringBuilder.append("/");
		stringBuilder.append(Recorder.RECORD_FOLDER);
		stringBuilder.append("%'");
		String selection = stringBuilder.toString();
		Cursor recordingFileCursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Audio.Media.ARTIST,
						MediaStore.Audio.Media.ALBUM,
						MediaStore.Audio.Media.DATA,
						MediaStore.Audio.Media.DURATION,
						MediaStore.Audio.Media.DISPLAY_NAME,
						MediaStore.Audio.Media.DATE_ADDED,
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media._ID }, selection, null, null);
		try {
			if ((null == recordingFileCursor)
					|| (0 == recordingFileCursor.getCount())) {
				LogUtils.i(TAG, "<queryData> the data return by query is null");
				return null;
			}
			LogUtils.i(TAG, "<queryData> the data return by query is available");
			recordingFileCursor.moveToLast();
			int num = recordingFileCursor.getCount();
			final int sizeOfHashMap = 6;
			for (int j = 0; j < num; j++) {
				HashMap<String, Object> map = new HashMap<String, Object>(
						sizeOfHashMap);
				String path = recordingFileCursor.getString(PATH_INDEX);
				String fileName = null;
				if (null != path) {
					fileName = path.substring(path.lastIndexOf("/") + 1,
							path.length());
				}
				int duration = recordingFileCursor.getInt(DURATION_INDEX);
				if (duration < ONE_SECOND) {
					duration = ONE_SECOND;
				}
//				String createDate = recordingFileCursor
//						.getString(CREAT_DATE_INDEX);
				
				String createDate = recordingFileCursor
						.getString(CREAT_DATE_INDEX);
				
				int recordId = recordingFileCursor.getInt(RECORD_ID_INDEX);

				map.put(FILE_NAME, fileName);
//				map.put(FILE_NAME, createDate);
				map.put(PATH, path);
				map.put(DURATION, duration);
				map.put(CREAT_DATE, createDate);
				map.put(FORMAT_DURATION, getTimer(duration));
				map.put(RECORD_ID, recordId);

				mNameList.add(fileName);
				mPathList.add(path);
				mTitleList.add(createDate);
				mDurationList.add(formatDuration(duration));
				mIdList.add(recordId);

				Log.v("TAG", "path->" + path);
				recordingFileCursor.moveToPrevious();
				mArrlist.add(map);
			}
		} catch (IllegalStateException e) {
			ErrorHandle.showErrorInfo(this,
					ErrorHandle.ERROR_ACCESSING_DB_FAILED_WHEN_QUERY);
			e.printStackTrace();
		} finally {
			if (null != recordingFileCursor) {
				recordingFileCursor.close();
			}
		}
		return mArrlist;
	}

	public void isCursorEmpty(){
//		set empty view for bottom view
		tv_progress.setText("00:00");
		tv_duration.setText("00:00");
		mStateProgressBar.setProgress(0);
	}
	
	/**
	 * update UI after query data
	 * 
	 * @param list
	 *            the list of query result
	 */
	public void afterQuery(List<Integer> list) {
		LogUtils.i(TAG, "<afterQuery>");
		if (null == list) {
			mCurrentAdapterMode = NORMAL;
			swicthAdapterView(NO_CHECK_POSITION);
//			isCursorEmpty();
		} else {
			list.retainAll(mIdList);
			if (list.isEmpty()) {
				removeOldFragmentByTag(DIALOG_TAG_DELETE);
				mCurrentAdapterMode = NORMAL;
				swicthAdapterView(NO_CHECK_POSITION);
//				isCursorEmpty();
			} else {
				// for refresh status of DeleteDialogFragment(single/multi)
				LogUtils.i(TAG, "<afterQuery> list.size() = " + list.size());
				setDeleteDialogSingle(1 == list.size());
				mCurrentAdapterMode = EDIT;
				EditViewAdapter adapter = new EditViewAdapter(
						getApplicationContext(), mNameList, mPathList,
						mTitleList, mDurationList, mIdList, list);
				mRecordingFileListView.setAdapter(adapter);
				
				// mDeleteButton.setVisibility(View.VISIBLE);
				// mRecordButton.setVisibility(View.GONE);
				restoreLastSelection();
			}
		}
	}

	/**
	 * format duration to display as 00:00
	 * 
	 * @param duration
	 *            the duration to be format
	 * @return the String after format
	 */
	private String formatDuration(int duration) {
		String timerFormat = getResources().getString(R.string.timer_format);
		int time = duration / ONE_SECOND;
		return String.format(timerFormat, time / TIME_BASE, time % TIME_BASE);
	}

	@Override
	public void onClick(View button) {
		switch (button.getId()) {

		// list_play_btn = (ImageButton)this.findViewById(R.id.list_playButton);
		// list_stop_btn = (ImageButton)this.findViewById(R.id.list_stopButton);
		// list_record_btn =
		// (ImageButton)this.findViewById(R.id.list_recordButton);
		// list_file_btn =
		// (ImageButton)this.findViewById(R.id.list_recordListButton);
		//
		// frame_up_btn_rename =
		// (ImageButton)this.findViewById(R.id.frame_up_btn_rename);
		// frame_up_btn_del =
		// (ImageButton)this.findViewById(R.id.frame_up_btn_del);
		// frame_up_btn_send =
		// (ImageButton)this.findViewById(R.id.frame_up_btn_share);
		case R.id.list_back_rela:
			this.finish();
//			this.overridePendingTransition(R.anim.activity_close , 0);
			break;
		case R.id.list_playButton:
			if (mCurrentState == STATE_PAUSE_PLAYING) {
				mPlayer.goonPlayback();
			}else if(mCurrentState == STATE_IDLE){
				HashMap<String, Object> map = (HashMap<String, Object>) mRecordingFileListView
						.getItemAtPosition(pre);
				if (map != null) {
					mPlayer.reset();
					mPlayer.setCurrentFilePath(map.get(PATH)
							.toString());
					mPlayer.startPlayback();
				}else{
					Toast.makeText(RecordingFileList.this, R.string.ruiemi_no_audio, Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case R.id.list_stopButton:
			if (mCurrentState == STATE_PLAYING) {
				mPlayer.pausePlayback();
				list_stop_btn.setVisibility(View.GONE);
				list_play_btn.setVisibility(View.VISIBLE);
			}else{
				Toast.makeText(RecordingFileList.this, "unknwn error", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.list_recordButton:
			// return to record
			Intent intent = new Intent();
			intent.setClass(this, SoundRecorder.class);
			intent.putExtra(SoundRecorder.DOWHAT, SoundRecorder.RECORD);
			setResult(RESULT_OK, intent);
			finish();
//			this.overridePendingTransition(R.anim.activity_close, 0);

			break;
		case R.id.list_recordListButton:
			
			Intent intent_new = new Intent();
			intent_new.setAction(Intent.ACTION_GET_CONTENT);
			intent_new.setType("file/*");
		    startActivity(intent_new);
//            Intent mIntent = new Intent( ); 
//            ComponentName comp = new ComponentName("com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerSelectPathActivity");
//            mIntent.setComponent(comp); 
//            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            mIntent.setAction("android.intent.action.VIEW"); 
//            startActivity(mIntent);
            
//			Intent intent_list = new Intent();
//			intent_list.setComponent(new ComponentName("com.mediatek.filemanager","com.mediatek.filemanager.FileManagerSelectPathActivity"));
////			"/storage/emulated/0/Recording"
////			download_path
//			Bundle b = new Bundle();
//			b.putString("download path" , "/storage/emulated/0/Recording");
//			intent_list.putExtras(b);
//			//intent_list.setData("download path");
////			intent_list.setData(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator + "Recording")));
//			startActivity(intent_list);
			
			break;
		case R.id.frame_up_btn_rename:
			
//			final EditText editText = new EditText(RecordingFileList.this);
//			editText.setBackground(null);
			
			View v = View.inflate(RecordingFileList.this, R.layout.ruimei_edittext, null);
			final EditText editText = (EditText)v.findViewById(R.id.ruimei_edit); 
			
			new AlertDialog.Builder(RecordingFileList.this)
					.setTitle(R.string.ruiemi_choose_enter)
					.setView(v)
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (!editText.getText().toString().trim().equals("")) {
								String str_change = editText.getText().toString().trim();
								
//								HashMap my_map = (HashMap) ((RuimeiSimpleAdapter) mRecordingFileListView
//										.getAdapter()).getItem(curr);
								int query_id = 0;
								query_id = (Integer) mArrlist.get(curr).get(RECORD_ID);
//								for (int i = 0; i < mArrlist.size(); i++) {
//									if (mArrlist.get(i).get(PATH).equals(my_map.get())) {
//										Toast.makeText(RecordingFileList.this,
//												"find same ->" + mArrlist.get(i).get(RECORD_ID),
//												Toast.LENGTH_SHORT).show();
//										query_id = (Integer) mArrlist.get(i).get(RECORD_ID);
//									}
//								}
								ContentValues updateValues = new ContentValues();
								updateValues.put(MediaStore.Audio.Media.TITLE, str_change);
								Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
								Uri updateIdUri = ContentUris.withAppendedId(base, query_id);
								getApplicationContext().getContentResolver().update(updateIdUri, updateValues, null,
										null);
								if (EDIT == mCurrentAdapterMode) {
									mCurrentAdapterMode = NORMAL;
									queryData();
									saveLastSelection();
									swicthAdapterView(NO_CHECK_POSITION);
								}
								return;
							}else{
								//do nothing
//								Toast.makeText(RecordingFileList.this, "nothing changed", Toast.LENGTH_SHORT).show();
							}
						}
					})
					.setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).show();
			
			
			break;
		case R.id.frame_up_btn_del:
			// dodelete
			int count = ((EditViewAdapter) mRecordingFileListView.getAdapter())
					.getCheckedItemsCount();
			showDeleteDialog(count == 1);
			
			break;
		case R.id.frame_up_btn_share:

			List<String> path_list = ((EditViewAdapter) mRecordingFileListView
					.getAdapter()).getCheckedItemsList();
			ArrayList<Uri> uris = new ArrayList<Uri>();

			for (int i = 0; i < path_list.size(); i++) {
				File file = new File(path_list.get(i));
				Uri u = Uri.fromFile(file);
				uris.add(u);
			}
			boolean multiple = uris.size() > 1;
			Intent share = new Intent(
					multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
							: android.content.Intent.ACTION_SEND);
			if (multiple) {
				share.setType("*/*");
				share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			} else {
				share.setType("*/*");
				share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
			}
			String send = getResources().getString(R.string.ruiemi_choose_send);
			startActivity(Intent.createChooser(share, send));
			break;
		// case R.id.recordButton:
		// LogUtils.i(TAG, "<onClick> recordButton");
		// mRecordButton.setEnabled(false);
		// Intent intent = new Intent();
		// intent.setClass(this, SoundRecorder.class);
		// intent.putExtra(SoundRecorder.DOWHAT, SoundRecorder.RECORD);
		// setResult(RESULT_OK, intent);
		// finish();
		// break;
		// case R.id.deleteButton:
		// LogUtils.i(TAG, "<onClick> deleteButton");
		// int count = ((EditViewAdapter) mRecordingFileListView.getAdapter())
		// .getCheckedItemsCount();
		// showDeleteDialog(count == 1);
		// break;

		default:
			break;
		}
	}

	/**
	 * show DeleteDialogFragment
	 * 
	 * @param single
	 *            if the number of files to be deleted == 0 ?
	 */
	private void showDeleteDialog(boolean single) {
		LogUtils.i(TAG, "<showDeleteDialog> single = " + single);
		removeOldFragmentByTag(DIALOG_TAG_DELETE);
		FragmentManager fragmentManager = getFragmentManager();
		LogUtils.i(TAG, "<showDeleteDialog> fragmentManager = "
				+ fragmentManager);
		DialogFragment newFragment = DeleteDialogFragment.newInstance(single);
		((DeleteDialogFragment) newFragment)
				.setOnClickListener(mDeleteDialogListener);
		newFragment.show(fragmentManager, DIALOG_TAG_DELETE);
		fragmentManager.executePendingTransactions();
	}

	/**
	 * remove old DialogFragment
	 * 
	 * @param tag
	 *            the tag of DialogFragment to be removed
	 */
	private void removeOldFragmentByTag(String tag) {
		LogUtils.i(TAG, "<removeOldFragmentByTag> tag = " + tag);
		FragmentManager fragmentManager = getFragmentManager();
		LogUtils.i(TAG, "<removeOldFragmentByTag> fragmentManager = "
				+ fragmentManager);
		DialogFragment oldFragment = (DialogFragment) fragmentManager
				.findFragmentByTag(tag);
		LogUtils.i(TAG, "<removeOldFragmentByTag> oldFragment = " + oldFragment);
		if (null != oldFragment) {
			oldFragment.dismissAllowingStateLoss();
		}
	}

	/**
	 * update the message of delete dialog
	 * 
	 * @param single
	 *            if single file to be deleted
	 */
	private void setDeleteDialogSingle(boolean single) {
		FragmentManager fragmentManager = getFragmentManager();
		LogUtils.i(TAG, "<setDeleteDialogSingle> fragmentManager = "
				+ fragmentManager);
		DeleteDialogFragment oldFragment = (DeleteDialogFragment) fragmentManager
				.findFragmentByTag(DIALOG_TAG_DELETE);
		if (null == oldFragment) {
			LogUtils.i(TAG, "<setDeleteDialogSingle> no old delete dialog");
		} else {
			oldFragment.setSingle(single);
			LogUtils.i(TAG, "<setDeleteDialogSingle> setSingle single = "
					+ single);
		}
	}

	/**
	 * call delete file task
	 */
	public void deleteItems() {
		LogUtils.i(TAG, "<deleteItems> call FileTask to delete");
		FileTask fileTask = new FileTask();
		fileTask.execute();
	}

	/**
	 * switch adapter mode between NORMAL and EDIT
	 * 
	 * @param pos
	 *            the index of current clicked item
	 */
	
	class RuimeiSimpleAdapter extends SimpleAdapter{

		public RuimeiSimpleAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);

		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			View v = View.inflate(RecordingFileList.this, R.layout.navigation_adapter, null);
//			R.id.record_file_name, R.id.record_file_title, R.id.record_file_duration 
			TextView tv_file_name = (TextView)v.findViewById(R.id.record_file_name);
			TextView tv_file_title = (TextView)v.findViewById(R.id.record_file_title);
			TextView tv_file_duration = (TextView)v.findViewById(R.id.record_file_duration);

			tv_file_name.setText((CharSequence) mArrlist.get(position).get(CREAT_DATE));
			tv_file_title.setText((CharSequence) mArrlist.get(position).get(FILE_NAME));
			tv_file_duration.setText((CharSequence) mArrlist.get(position).get(FORMAT_DURATION));
			if (position == pre) {
				tv_file_name.setTextColor(Color.rgb(55, 159, 239));
				tv_duration.setText((CharSequence)mArrlist.get(position).get(FORMAT_DURATION));
			}
			
			return v;
		}
		
		
	}
	public void swicthAdapterView(int pos) {
		if (NORMAL == mCurrentAdapterMode) {
			showUp = false;
			frameUp.setVisibility(View.GONE);
			setBottomBtnAble(true);
			showProgress(true);
			LogUtils.i(TAG, "<swicthAdapterView> from edit mode to normal mode");
			RuimeiSimpleAdapter adapter = new RuimeiSimpleAdapter(getApplicationContext(),
					mArrlist, R.layout.navigation_adapter, new String[] {
							FILE_NAME, CREAT_DATE, FORMAT_DURATION },
					new int[] { R.id.record_file_name, R.id.record_file_title,
							R.id.record_file_duration });
			mRecordingFileListView.setAdapter(adapter);
			// mDeleteButton.setVisibility(View.GONE);
			// mRecordButton.setVisibility(View.VISIBLE);
			ruimeiActionBar.setCustomView(R.layout.simple_actionbar);
			list_back_rela = (RelativeLayout) getActionBar().getCustomView().findViewById(R.id.list_back_rela);
			list_back_rela.setOnClickListener(this);
//			mPlayer.goonPlayback();
			
		} else {
			showUp = true;
			frameUp.setVisibility(View.VISIBLE);
			showProgress(false);
			setBottomBtnAble(false);
			LogUtils.i(TAG, "<swicthAdapterView> from normal mode to edit mode");
			final EditViewAdapter adapter = new EditViewAdapter(
					getApplicationContext(), mNameList, mPathList, mTitleList,
					mDurationList, mIdList, pos);
			mRecordingFileListView.setAdapter(adapter);
			// mDeleteButton.setVisibility(View.VISIBLE);
			ruimeiActionBar.setCustomView(R.layout.customer_actionmode);
			tv_count = (TextView) getActionBar().getCustomView().findViewById(
					R.id.nt_multichoice_text_select);
			all_pick = (TextView) getActionBar().getCustomView().findViewById(
					R.id.nt_multichoice_all_pick);
			tv_count = (TextView) getActionBar().getCustomView().findViewById(
					R.id.nt_multichoice_text_select);
			tv_cancel = (TextView) getActionBar().getCustomView().findViewById(
					R.id.nt_multichoice_cancel);
			all_pick.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					if (adapter.getCheckedItemsCount() == mRecordingFileListView
							.getCount()) {
						for (int i = 0; i < mRecordingFileListView.getCount(); i++) {
							int id = (int) mRecordingFileListView.getAdapter()
									.getItemId(i);
							adapter.setCheckBox(id, false);
						}
						all_pick.setText(R.string.ruiemi_choose_all);
						tv_count.setText(str_choose_pre + adapter.getCheckedItemsCount()
								+ str_choose_aft);
						if (EDIT == mCurrentAdapterMode) {
							mCurrentAdapterMode = NORMAL;
							saveLastSelection();
							swicthAdapterView(NO_CHECK_POSITION);
						}
					} else {
						for (int i = 0; i < mRecordingFileListView.getCount(); i++) {
							int id = (int) mRecordingFileListView.getAdapter()
									.getItemId(i);
							adapter.setCheckBox(id, true);
						}
						all_pick.setText(R.string.ruiemi_choose_none);
						tv_count.setText(str_choose_pre+ adapter.getCheckedItemsCount()
								+ str_choose_aft);
						if (mRecordingFileListView.getCount() > 1) {
							frame_up_btn_rename.setClickable(false);
							frame_up_btn_rename.setImageResource(R.drawable.ruimei_rename_unuse);
						}else{
							frame_up_btn_rename.setClickable(true);
							frame_up_btn_rename.setImageResource(R.drawable.ruimei_list_btn_rename);
						}
					}
					adapter.notifyDataSetChanged();
				}
			});

			tv_cancel.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (EDIT == mCurrentAdapterMode) {
						mCurrentAdapterMode = NORMAL;
						saveLastSelection();
						swicthAdapterView(NO_CHECK_POSITION);
					}
				}
			});
			tv_count.setText(R.string.ruiemi_choose_chooseone);
			if (adapter.getCheckedItemsCount() == mRecordingFileListView
					.getCount()){
				all_pick.setText(R.string.ruiemi_choose_none);
			}else{
				all_pick.setText(R.string.ruiemi_choose_all);
			}
			// mRecordButton.setVisibility(View.GONE);
		}
		restoreLastSelection();
		
	}

	/**
	 * The method gets the selected items and create a list of File objects
	 * 
	 * @return a list of File objects
	 */
	protected List<File> getSelectedFiles() {
		LogUtils.i(TAG, "<getSelectedFiles> begin");
		List<File> list = new ArrayList<File>();
		if (EDIT != mCurrentAdapterMode) {
			LogUtils.i(TAG, "<getSelectedFiles> end");
			return list;
		}
		if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
			List<String> checkedList = ((EditViewAdapter) mRecordingFileListView
					.getAdapter()).getCheckedItemsList();
			int listSize = checkedList.size();
			for (int i = 0; i < listSize; i++) {
				File file = new File(checkedList.get(i));
				list.add(file);
			}
		}
		LogUtils.i(TAG, "<getSelectedFiles> end");
		return list;
	}

	@Override
	protected void onPause() {
		LogUtils.i(TAG, "<onPause> begin");
		List<Integer> checkedList = null;
		if (EDIT == mCurrentAdapterMode) {
			if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
				checkedList = ((EditViewAdapter) mRecordingFileListView
						.getAdapter()).getCheckedPosList();
				if (!checkedList.isEmpty()) {
					mCheckedList = checkedList;
					LogUtils.i(TAG,
							"<onPause> save checkedList; mCheckedList.size() = "
									+ mCheckedList.size());
				}
			}
		} else {
			mCheckedList = null;
		}
		mActivityForeground = false;
		saveLastSelection();
		LogUtils.i(TAG, "<onPause> end");
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		LogUtils.i(TAG, "onBackPressed");
		if (EDIT == mCurrentAdapterMode) {
			mCurrentAdapterMode = NORMAL;
			saveLastSelection();
			swicthAdapterView(NO_CHECK_POSITION);
		} else {
			finishSelf();
			super.onBackPressed();
//			this.overridePendingTransition(R.anim.activity_close, 0);
		}
	}

	/**
	 * FileTask for delete some recording file
	 */
	public class FileTask extends AsyncTask<Void, Object, Boolean> {
		/**
		 * A callback method to be invoked before the background thread starts
		 * running
		 */
		@Override
		protected void onPreExecute() {
			LogUtils.i(TAG, "<FileTask.onPreExecute>");
			if (mActivityForeground) {
				LogUtils.i(TAG,
						"<FileTask.onPreExecute> Activity is running in foreground");
				FragmentManager fragmentManager = getFragmentManager();
				LogUtils.i(TAG, "<FileTask.onPreExecute> fragmentManager = "
						+ fragmentManager);
				DialogFragment newFragment = ProgressDialogFragment
						.newInstance();
				newFragment.show(fragmentManager, DIALOG_TAG_PROGRESS);
				fragmentManager.executePendingTransactions();
			} else {
				LogUtils.i(TAG,
						"<FileTask.onPreExecute> Activity is running in background");
			}
		}

		/**
		 * A callback method to be invoked when the background thread starts
		 * running
		 * 
		 * @param params
		 *            the method need not parameters here
		 * @return true/false, success or fail
		 */
		@Override
		protected Boolean doInBackground(Void... params) {
			LogUtils.i(TAG, "<FileTask.doInBackground> begin");
			// delete files
			List<File> list = getSelectedFiles();
			int listSize = list.size();
			LogUtils.i(TAG,
					"<FileTask.doInBackground> the number of delete files: "
							+ listSize);
			for (int i = 0; i < listSize; i++) {
				File file = list.get(i);
				if (null != file) {
					LogUtils.i(TAG,
							"<doInBackground>, the file to be delete is:"
									+ file.getAbsolutePath());
				}
				if (!file.delete()) {
					LogUtils.i(TAG, "<FileTask.doInBackground> delete file ["
							+ list.get(i).getAbsolutePath() + "] fail");
				}
				if (!SoundRecorderUtils.deleteFileFromMediaDB(
						getApplicationContext(), file.getAbsolutePath())) {
					return false;
				}
			}
			LogUtils.i(TAG, "<FileTask.doInBackground> end");
			return true;
		}

		/**
		 * A callback method to be invoked after the background thread performs
		 * the task
		 * 
		 * @param result
		 *            the value returned by doInBackground()
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			LogUtils.i(TAG, "<FileTask.onPostExecute>");
			removeOldFragmentByTag(DIALOG_TAG_PROGRESS);
			mNeedRemoveProgressDialog = true;
			if (mActivityForeground) {
				mCurrentAdapterMode = NORMAL;
				if (!result) {
					ErrorHandle.showErrorInfo(RecordingFileList.this,
							ErrorHandle.ERROR_DELETING_FAILED);
				}
				setListData(null);
			}
		}

		/**
		 * A callback method to be invoked when the background thread's task is
		 * cancelled
		 */
		@Override
		protected void onCancelled() {
			LogUtils.i(TAG, "<FileTask.onCancelled>");
			FragmentManager fragmentManager = getFragmentManager();
			LogUtils.i(TAG, "<FileTask.onCancelled> fragmentManager = "
					+ fragmentManager);
			DialogFragment oldFragment = (DialogFragment) fragmentManager
					.findFragmentByTag(DIALOG_TAG_PROGRESS);
			if (null != oldFragment) {
				oldFragment.dismissAllowingStateLoss();
			}
		}
	}

	/**
	 * through AsyncTask to query recording file data from database
	 */
	public class QueryDataTask extends
			AsyncTask<Void, Object, ArrayList<HashMap<String, Object>>> {
		List<Integer> mList;

		/**
		 * the construction of QueryDataTask
		 * 
		 * @param list
		 *            the index list of current checked items
		 */
		QueryDataTask(List<Integer> list) {
			mList = list;
		}

		/**
		 * query data from database
		 * 
		 * @param params
		 *            no parameter
		 * @return the query result
		 */
		protected ArrayList<HashMap<String, Object>> doInBackground(
				Void... params) {
			LogUtils.i(TAG, "<QueryDataTask.doInBackground>");
			return queryData();
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, Object>> result) {
			LogUtils.i(TAG, "<QueryDataTask.onPostExecute>");
			if (mActivityForeground) {
				if (null == result) {
					removeOldFragmentByTag(DIALOG_TAG_DELETE);
					mRecordingFileListView.setEmptyView(mEmptyView);
					// mDeleteButton.setVisibility(View.GONE);
					// mRecordButton.setVisibility(View.VISIBLE);
					ruimeiActionBar.setCustomView(R.layout.simple_actionbar);
					list_back_rela = (RelativeLayout) getActionBar().getCustomView().findViewById(R.id.list_back_rela);
					list_back_rela.setOnClickListener(RecordingFileList.this);
					frameUp.setVisibility(View.GONE);
					setBottomBtnAble(true);
				} else {
					afterQuery(mList);
				}
			}
		}
	}

	/**
	 * setResult to SoundRecorder and finish self
	 */
	public void finishSelf() {
		LogUtils.i(TAG, "<finishSelf>");
		mCurrentAdapterMode = NORMAL;
		Intent intent = new Intent();
		intent.setClass(this, SoundRecorder.class);
		intent.putExtra(SoundRecorder.DOWHAT, SoundRecorder.INIT);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onDestroy() {
		LogUtils.i(TAG, "<onDestroy> begin");
		if (null != mSDCardMountEventReceiver) {
			unregisterReceiver(mSDCardMountEventReceiver);
			mSDCardMountEventReceiver = null;
		}
		LogUtils.i(TAG, "<onDestroy> end");
		super.onDestroy();
		this.overridePendingTransition(R.anim.activity_close, 0);
	}

	/**
	 * deal with SDCard mount and eject event
	 */
	private void registerExternalStorageListener() {
		LogUtils.i(TAG, "<registerExternalStorageListener>");
		if (null == mSDCardMountEventReceiver) {
			mSDCardMountEventReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if (action.equals(Intent.ACTION_MEDIA_EJECT)
							|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
						mRecordingFileListView.setAdapter(null);
						mCheckedList = null;
						ErrorHandle.showErrorInfo(RecordingFileList.this,
								ErrorHandle.ERROR_SD_UNMOUNTED_ON_FILE_LIST);
						finishSelf();
					} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
						setListData(mCheckedList);
					}
				}
			};
			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
			iFilter.addDataScheme("file");
			registerReceiver(mSDCardMountEventReceiver, iFilter);
		}
	}

	public void setBottomBtnAble(boolean canUse) {
		// ImageButton
		// list_play_btn,list_stop_btn,list_record_btn,list_file_btn;
		list_play_btn.setFocusable(canUse);
		list_play_btn.setEnabled(canUse);
		list_stop_btn.setFocusable(canUse);
		list_stop_btn.setEnabled(canUse);
		list_record_btn.setFocusable(canUse);
		list_record_btn.setEnabled(canUse);
		list_file_btn.setFocusable(canUse);
		list_file_btn.setEnabled(canUse);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			NTActionBarSplitManager.getInstance(this).onMenuKeyToggled(this);
		}
		return super.onKeyUp(keyCode, event);
	}

	// update timer
	// update remaining time

	@Override
	public void onError(Player player, int errorCode) {
		Toast.makeText(RecordingFileList.this, "play error", Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public void onStateChanged(Player player, int stateCode) {
//		Toast.makeText(RecordingFileList.this, "stateCode->" + stateCode, Toast.LENGTH_SHORT).show();
		mCurrentState = stateCode;
		if (stateCode == STATE_PLAYING) {
			list_play_btn.setVisibility(View.GONE);
			list_stop_btn.setVisibility(View.VISIBLE);
		}else if (stateCode == STATE_PAUSE_PLAYING) {
			list_play_btn.setVisibility(View.VISIBLE);
			list_stop_btn.setVisibility(View.GONE);
		}else{
			list_play_btn.setVisibility(View.VISIBLE);
			list_stop_btn.setVisibility(View.GONE);
			mStateProgressBar.setProgress(0);
			tv_progress.setText("00:00");
		}
		mHandler.post(mUpdateTimer);
	}

	@Override
	protected void onStop() {
		super.onStop();
		mPlayer.stopPlayback();
		mPlayer.reset();
		mPlayer = null;
	}
	
	public void showProgress(boolean isShow){
		if (isShow) {
			list_progress_up.setVisibility(View.VISIBLE);
			mStateProgressBar.setVisibility(View.VISIBLE);
		}else{
			list_progress_up.setVisibility(View.GONE);
			mStateProgressBar.setVisibility(View.GONE);
		}
	}
	
}
