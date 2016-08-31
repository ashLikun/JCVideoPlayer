package fm.jiecao.jcvideoplayer_lib;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Map;


/**
 * <p>统一管理MediaPlayer的地方,只有一个mediaPlayer实例，那么不会有多个视频同时播放，也节省资源。</p>
 * <p>Unified management MediaPlayer place, there is only one MediaPlayer instance, then there will be no more video broadcast at the same time, also save resources.</p>
 * Created by Nathen
 * On 2015/11/30 1:39
 */
public class JCMediaManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnInfoListener {
    public static String TAG = "JieCaoVideoPlayer";

    private static JCMediaManager JCMediaManager;
    public MediaPlayer mediaPlayer;
    public static TextureView textureView;

    public int currentVideoWidth = 0;
    public int currentVideoHeight = 0;
    public int lastState;

    public int currentPosition = 0;
    public int duration = 0;

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_SETDISPLAY = 1;
    public static final int HANDLER_RELEASE = 2;
    HandlerThread mMediaHandlerThread;
    MediaHandler mMediaHandler;
    Handler mainThreadHandler;

    public static JCMediaManager instance() {
        if (JCMediaManager == null) {
            JCMediaManager = new JCMediaManager();
        }
        return JCMediaManager;
    }

    public JCMediaManager() {
        mediaPlayer = new MediaPlayer();
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler((mMediaHandlerThread.getLooper()));
        mainThreadHandler = new Handler();
    }


    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    try {
                        currentVideoWidth = 0;
                        currentVideoHeight = 0;
                        currentPosition = 0;
                        duration = 0;
                        mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(((FuckBean) msg.obj).url);//, ((FuckBean) msg.obj).mapHeadData
                        mediaPlayer.setLooping(((FuckBean) msg.obj).looping);
                        mediaPlayer.setOnPreparedListener(JCMediaManager.this);
                        mediaPlayer.setOnCompletionListener(JCMediaManager.this);
                        mediaPlayer.setOnBufferingUpdateListener(JCMediaManager.this);
                        mediaPlayer.setScreenOnWhilePlaying(true);
                        mediaPlayer.setOnSeekCompleteListener(JCMediaManager.this);
                        mediaPlayer.setOnErrorListener(JCMediaManager.this);
                        mediaPlayer.setOnInfoListener(JCMediaManager.this);
                        mediaPlayer.setOnVideoSizeChangedListener(JCMediaManager.this);
                        mediaPlayer.prepareAsync();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_SETDISPLAY:
                    if (msg.obj == null) {
                        instance().mediaPlayer.setSurface(null);
                    } else {
                        Surface holder = (Surface) msg.obj;
                        if (holder.isValid()) {
                            Log.i(TAG, "set surface");
                            instance().mediaPlayer.setSurface(holder);
                        }
                    }
                    break;
                case HANDLER_RELEASE:
                    mediaPlayer.release();
                    break;
            }
        }
    }


    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        FuckBean fb = new FuckBean(url, mapHeadData, loop);
        msg.obj = fb;
        mMediaHandler.sendMessage(msg);
    }

    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
    }

    public void setDisplay(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_SETDISPLAY;
        msg.obj = holder;
        mMediaHandler.sendMessage(msg);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onBufferingUpdate(percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
        currentVideoWidth = mediaPlayer.getVideoWidth();
        currentVideoHeight = mediaPlayer.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (JCVideoPlayerManager.listener() != null) {
                    JCVideoPlayerManager.listener().onVideoSizeChanged();
                }
            }
        });
    }

    private class FuckBean {
        String url;
        Map<String, String> mapHeadData;
        boolean looping;

        FuckBean(String url, Map<String, String> mapHeadData, boolean loop) {
            this.url = url;
            this.mapHeadData = mapHeadData;
            this.looping = loop;
        }
    }


    public int getDuration() {
        try {
            int dd = mediaPlayer.getDuration();
            duration = dd;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    public int getCurrentPosition() {
        try {
            int dd = mediaPlayer.getCurrentPosition();
            currentPosition = dd;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return currentPosition;
        }
        return currentPosition;
    }
}
