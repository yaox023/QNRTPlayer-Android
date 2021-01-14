package com.qiniu.droid.rtplayer.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qiniu.droid.rtplayer.QNConfiguration;
import com.qiniu.droid.rtplayer.QNError;
import com.qiniu.droid.rtplayer.QNLogLevel;
import com.qiniu.droid.rtplayer.QNRTPlayer;
import com.qiniu.droid.rtplayer.QNRTPlayerFactory;
import com.qiniu.droid.rtplayer.QNRTPlayerSetting;
import com.qiniu.droid.rtplayer.QNRTPlayerStats;
import com.qiniu.droid.rtplayer.QNRTPlayerUrl;
import com.qiniu.droid.rtplayer.QNSize;
import com.qiniu.droid.rtplayer.demo.R;
import com.qiniu.droid.rtplayer.demo.utils.Config;
import com.qiniu.droid.rtplayer.demo.utils.ToastUtils;
import com.qiniu.droid.rtplayer.render.QNSurfaceView;
import com.qiniu.droid.rtplayer.render.QNTextureView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.qiniu.droid.rtplayer.QNRTPlayer.STATE_PLAYING;

public class PlayingActivity extends AppCompatActivity {
    public static final String TAG = "PlayingActivity";

    private Handler mMainHandler;

    private QNRTPlayer mRTPlayer;
    private QNRTPlayerUrl mRTUrl = new QNRTPlayerUrl();
    private QNRTPlayerSetting mRTPlayerSetting;

    private boolean mMuteAudio;
    private boolean mMuteVideo;
    private boolean mStopPlay;

    @BindView(R.id.qn_video_view)
    QNSurfaceView mRenderView;

    @BindView(R.id.playing_log_text)
    TextView mLogText;

    @BindView(R.id.play_stop)
    ImageButton mStopButton;

    @BindView(R.id.playing_progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.mute_audio)
    ImageButton mMuteAudioButton;

    @BindView(R.id.mute_video)
    ImageButton mMuteVideoButton;

    private QNSize mFrameSize;
    private QNRTPlayerStats mStats;
    private String mLastError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_playing);
        ButterKnife.bind(this);

        mMainHandler = new Handler(getMainLooper());

        String videoPath = getIntent().getStringExtra(Config.PLAYING_URL);
        mRTUrl.setURL(videoPath);
        Log.i(TAG, "video path:" + videoPath);

        mRTPlayer = QNRTPlayerFactory.createQNRTPlayer(getApplicationContext());
        mRTPlayerSetting = new QNRTPlayerSetting();
        mRTPlayerSetting.setLogLevel(QNLogLevel.INFO);
        mRTPlayer.initPlayer(mRTPlayerSetting);
        mRTPlayer.setEventListener(new RTPlayerListener());
        mRTPlayer.setSurfaceRenderWindow(mRenderView);

        mMainHandler.postDelayed(new InfoUpdateTask(), 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRTPlayer.playUrl(mRTUrl);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRTPlayer.releasePlayer();
    }

    public void onClickClosePlayer(View v) {
        finish();
    }

    public void onClickStopPlayer(View v) {
        mStopPlay = !mStopPlay;
        mStopButton.setImageResource(mStopPlay ? R.drawable.ic_play : R.drawable.ic_stop);
        if (mStopPlay) {
            mRTPlayer.stopPlay();
        } else {
            mRTPlayer.playUrl(mRTUrl);
        }
    }

    public void onClickMuteAudio(View v) {
        if (mRTPlayer.hasAudio()) {
            mMuteAudio = !mMuteAudio;
            mRTPlayer.muteAudio(mMuteAudio);
            mMuteAudioButton.setImageResource(mMuteAudio ? R.drawable.ic_mute : R.drawable.ic_speaker);
        }
    }

    public void onClickMuteVideo(View v) {
        if (mRTPlayer.hasVideo()) {
            mMuteVideo = !mMuteVideo;
            mRTPlayer.muteVideo(mMuteVideo);
            mMuteVideoButton.setImageResource(mMuteVideo ? R.drawable.ic_video_hide : R.drawable.ic_video_show);
        }
    }

    public void onClickLogButton(View v) {
        mLogText.setVisibility(mLogText.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
    }

    private class RTPlayerListener implements QNRTPlayer.EventListener {
        @Override
        public void onPlayerStateChanged(@QNRTPlayer.PlayerState int state) {
            if (state == STATE_PLAYING) {
                QNConfiguration configuration = new QNConfiguration();
                configuration.setConfigure(QNConfiguration.CONF_PLAY_STAT, 2000L);
                mRTPlayer.configurePlayer(configuration);
            } else if (state == QNRTPlayer.STATE_READY) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPlayerInfo(@QNRTPlayer.PlayerInfo int type, Object info) {
            Log.i(TAG, "info: " + type + ", " + info);
            if (type == QNRTPlayer.INFO_STATS_UPDATE) {
                mStats = (QNRTPlayerStats) info;
            } else if (type == QNRTPlayer.INFO_VIDEO_SIZE_CHANGE) {
                mFrameSize = (QNSize) info;
            } else if (type == QNRTPlayer.INFO_FIRST_VIDEO_DECODE) {
                mProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPlayerError(QNError e) {
            mLastError = e.mDescription;
            ToastUtils.l(getApplicationContext(), e.mDescription);
        }
    }

    private class InfoUpdateTask implements Runnable {
        @Override
        public void run() {
            if (!isFinishing()) {
                final String text = "STATE: " + readablePlayState(mRTPlayer.getPlayerState()) + "\n"
                        + "音频码率: " + (mStats == null ? 0 : mStats.audioBitrate / 1000) + "kbps\n"
                        + "视频码率: " + (mStats == null ? 0 : mStats.videoBitrate / 1000) + "kbps\n"
                        + "视频大小: " + (mFrameSize != null ? mFrameSize.width : 0) + "x" + (mFrameSize != null ? mFrameSize.height : 0) + "\n"
                        + "视频帧率: " + (mStats == null ? 0 : mStats.frameRate) + "fps";
                mLogText.setText(text);

                mMainHandler.postDelayed(this, 1000);
            }
       }
    }

    String readablePlayState(@QNRTPlayer.PlayerState int state) {
        switch (state) {
            case QNRTPlayer.STATE_IDLE:
                return "STATE_IDLE";
            case QNRTPlayer.STATE_INIT:
                return "STATE_INIT";
            case QNRTPlayer.STATE_READY:
                return "STATE_READY";
            case QNRTPlayer.STATE_PLAYING:
                return "STATE_PLAYING";
            case QNRTPlayer.STATE_STOP:
                return "STATE_STOP";
            case QNRTPlayer.STATE_ERROR:
                return "STATE_ERROR";
            default:
                return "UNKNOWN";
        }
    }
}
