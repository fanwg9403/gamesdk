package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.wishfox.foxsdk.media.FSMediaFetcher;
import com.wishfox.foxsdk.media.FSMediaPolicy;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 原生全窗口媒体层，不启动 Activity、不修改窗口方向或游戏进程。所有播放器操作在主线程。 */
public final class FSMediaPreviewView extends FrameLayout implements TextureView.SurfaceTextureListener {
    public interface Listener {
        void onState(String state, String reason, int positionMs);
        void onClosed(String reason, int positionMs);
    }
    private final boolean video;
    private final boolean muted;
    private final Listener listener;
    private final Activity host;
    private Object backDispatcher, backCallback;
    private androidx.activity.OnBackPressedCallback legacyBack;
    // Only images use a private temporary file. Video goes directly to MediaPlayer's network source.
    private FSMediaFetcher imageFetcher;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AudioManager audio;
    private final AudioManager.OnAudioFocusChangeListener focusListener;
    private AudioFocusRequest focusRequest;
    private boolean hasFocus;
    private final FrameLayout content;
    private final TextView status;
    private final Button play;
    private TextureView texture;
    private FSZoomImageView image;
    private CustomTarget<Bitmap> imageTarget;
    private MediaPlayer player;
    private Surface surface;
    private File imageFile;
    private String videoUrl;
    private boolean closed, prepared, playing, foreground = true, autoPlay;
    private boolean buffering, seeking, failed, ended;
    private int bufferedPercent, lastProgressPosition;
    private int videoWidth, videoHeight, position;
    private float downX, downY;
    private boolean multiTouch;
    private final Runnable prepareTimeout = () -> fail("MEDIA_TIMEOUT");
    private final Runnable bufferTimeout = () -> {
        if (!closed && foreground && playing && buffering) fail("MEDIA_BUFFER_TIMEOUT");
    };
    // Some vendor players omit BUFFERING_START/END. Advancing playback position is the fallback.
    private final Runnable progressWatch = new Runnable() {
        @Override public void run() {
            if (closed || !prepared || !playing || !foreground || player == null) return;
            try {
                int now = player.getCurrentPosition();
                position = now;
                if (now > lastProgressPosition) finishBuffering("progress");
                else beginBuffering("waiting_for_data");
                lastProgressPosition = now;
                main.postDelayed(this, 1000);
            } catch (IllegalStateException failure) { fail("MEDIA_PLAYBACK_FAILED"); }
        }
    };

    public FSMediaPreviewView(Activity activity, boolean video, boolean muted, boolean autoPlay, Listener listener) {
        super(activity);
        this.host = activity;
        this.video = video;
        this.muted = muted;
        this.autoPlay = autoPlay;
        this.listener = listener;
        audio = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        focusListener = change -> main.post(() -> {
            if (!closed && change != AudioManager.AUDIOFOCUS_GAIN) pause("audio_focus_loss");
        });
        setBackgroundColor(0xFF000000);
        setClickable(true);
        setFocusableInTouchMode(true);
        content = new FrameLayout(activity);
        addView(content, new LayoutParams(-1, -1));
        if (video) {
            texture = new TextureView(activity);
            texture.setSurfaceTextureListener(this);
            content.addView(texture, new LayoutParams(-1, -1, Gravity.CENTER));
            content.setOnTouchListener((v, e) -> drag(e));
        } else {
            image = new FSZoomImageView(activity, () -> close("gesture_or_tap"));
            content.addView(image, new LayoutParams(-1, -1));
        }
        status = new TextView(activity);
        status.setTextColor(0xFFFFFFFF);
        status.setGravity(Gravity.CENTER);
        status.setText("正在加载…");
        addView(status, new LayoutParams(-1, -2, Gravity.CENTER));
        // Only controls consume safe insets; the black preview background fills the host window.
        FrameLayout controls = new FrameLayout(activity);
        addView(controls, new LayoutParams(-1, -1));
        FSOverlayInsets.applyToPadding(activity, controls);
        Button back = new Button(activity);
        back.setText("返回");
        back.setContentDescription("关闭媒体预览");
        back.setOnClickListener(v -> close("back_button"));
        controls.addView(back, new LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.CENTER);
        play = new Button(activity);
        play.setText("播放");
        play.setEnabled(false);
        play.setOnClickListener(v -> { if (playing) pause("user"); else startPlayback(); });
        if (video) bottom.addView(play);
        controls.addView(bottom, new LayoutParams(-1, -2, Gravity.BOTTOM));
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitVideo());
    }

    /** Call only after attaching to the host. Video preparation and image loading are asynchronous. */
    public void load(String url, List<String> origins) {
        if (closed) return;
        requestFocus();
        if (!FSMediaPolicy.allowed(url, origins)) { fail("MEDIA_URL_NOT_ALLOWED"); return; }
        if (video && !isHardwareAccelerated()) { fail("HARDWARE_ACCELERATION_REQUIRED"); return; }
        listener.onState("loading", "open", 0);
        if (video) {
            videoUrl = url;
            preparePlayer();
            return;
        }
        imageFetcher = new FSMediaFetcher();
        imageFetcher.fetchImage(getContext().getCacheDir(), url, origins, (result, error) -> {
            if (closed) return;
            if (error != null) { fail(error); return; }
            imageFile = result;
            loadImage();
        });
    }

    private void loadImage() {
        imageTarget = new CustomTarget<Bitmap>(2048, 2048) {
            @Override public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                if (closed) return;
                image.setBitmap(resource);
                status.setVisibility(GONE);
                listener.onState("ready", "loaded", 0);
            }
            @Override public void onLoadCleared(@Nullable Drawable placeholder) { image.setBitmap(null); }
            @Override public void onLoadFailed(@Nullable Drawable errorDrawable) { if (!closed) fail("MEDIA_DECODE_FAILED"); }
        };
        Glide.with(getContext().getApplicationContext()).asBitmap().load(imageFile)
                .override(2048, 2048).fitCenter().skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE).into(imageTarget);
    }

    private AudioAttributes attributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build();
    }

    private void preparePlayer() {
        if (closed || failed || !foreground || videoUrl == null || surface == null || player != null) return;
        try {
            status.setText("正在连接视频…"); status.setVisibility(VISIBLE);
            play.setEnabled(false);
            bufferedPercent = 0;
            MediaPlayer current = new MediaPlayer();
            player = current;
            current.setAudioAttributes(attributes());
            current.setSurface(surface);
            current.setVolume(muted ? 0 : 1, muted ? 0 : 1);
            current.setOnVideoSizeChangedListener((p, w, h) -> {
                if (p != player || closed) return;
                videoWidth = w; videoHeight = h; fitVideo();
            });
            current.setOnPreparedListener(p -> {
                if (p != player || closed) return;
                main.removeCallbacks(prepareTimeout);
                prepared = true;
                play.setEnabled(true);
                status.setVisibility(GONE);
                listener.onState("ready", "prepared", position);
                if (position > 0) {
                    seeking = true;
                    play.setEnabled(false);
                    status.setText("正在恢复播放位置…"); status.setVisibility(VISIBLE);
                    main.postDelayed(prepareTimeout, 30000);
                    p.setOnSeekCompleteListener(mp -> {
                        if (mp != player || closed) return;
                        mp.setOnSeekCompleteListener(null);
                        main.removeCallbacks(prepareTimeout);
                        seeking = false;
                        play.setEnabled(true); status.setVisibility(GONE);
                        if (autoPlay) startPlayback();
                    });
                    try { p.seekTo(position); }
                    catch (IllegalStateException failure) { fail("MEDIA_PLAYBACK_FAILED"); }
                } else if (autoPlay) startPlayback();
            });
            current.setOnCompletionListener(p -> {
                if (p != player || closed) return;
                playing = false; position = 0; autoPlay = false; ended = true;
                clearBuffering();
                main.removeCallbacks(progressWatch);
                status.setVisibility(GONE);
                play.setText("重播");
                setKeepScreenOn(false); abandonFocus();
                listener.onState("ended", "completed", 0);
            });
            current.setOnErrorListener((p, what, extra) -> {
                if (p == player && !closed) fail("MEDIA_PLAYBACK_FAILED");
                return true; // Never show MediaPlayer's default system dialog.
            });
            current.setOnInfoListener((p, what, extra) -> {
                if (p != player || closed) return true;
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) beginBuffering("network");
                else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                        || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) finishBuffering("buffer_ready");
                return false;
            });
            current.setOnBufferingUpdateListener((p, percent) -> {
                if (p != player || closed) return;
                bufferedPercent = Math.max(0, Math.min(100, percent));
                if (buffering && playing && foreground) updateBufferingText();
            });
            Map<String, String> headers = new HashMap<>();
            // Public MediaPlayer header option: reject cross-domain redirects. No SDK auth headers.
            headers.put("android-allow-cross-domain-redirect", "0");
            headers.put("Cache-Control", "no-store");
            // Context/Uri/headers is public since API 14; String/headers is not a public SDK overload.
            current.setDataSource(getContext().getApplicationContext(), Uri.parse(videoUrl), headers);
            current.prepareAsync();
            main.postDelayed(prepareTimeout, 30000);
        } catch (Exception failure) { fail("MEDIA_PLAYBACK_FAILED"); }
    }

    private void updateBufferingText() {
        status.setText(bufferedPercent > 0 ? "正在缓冲…（媒体缓冲 " + bufferedPercent + "%）" : "正在缓冲…");
        status.setVisibility(VISIBLE);
    }

    private void beginBuffering(String reason) {
        if (closed || !foreground || !playing || failed) return;
        if (!buffering) {
            buffering = true;
            main.postDelayed(bufferTimeout, 30000);
            listener.onState("buffering", reason, position);
        }
        updateBufferingText();
    }

    private void finishBuffering(String reason) {
        if (closed || !foreground || !playing || failed) return;
        if (!buffering) return;
        clearBuffering();
        status.setVisibility(GONE);
        listener.onState("playing", reason, position);
    }

    private void clearBuffering() {
        buffering = false;
        main.removeCallbacks(bufferTimeout);
    }

    private void fitVideo() {
        if (texture == null || videoWidth <= 0 || videoHeight <= 0) return;
        int[] size = FSMediaPolicy.fit(content.getWidth(), content.getHeight(), videoWidth, videoHeight);
        if (size[0] == 0 || size[1] == 0) return;
        LayoutParams previous = (LayoutParams) texture.getLayoutParams();
        if (previous.width != size[0] || previous.height != size[1])
            texture.setLayoutParams(new LayoutParams(size[0], size[1], Gravity.CENTER));
    }

    private boolean requestFocusForAudio() {
        if (muted || hasFocus) return true;
        if (audio == null) return false;
        int result;
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes()).setOnAudioFocusChangeListener(focusListener, main).build();
            result = audio.requestAudioFocus(focusRequest);
        } else {
            result = audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasFocus;
    }

    private void abandonFocus() {
        try {
            if (audio != null && hasFocus) {
                if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
                else audio.abandonAudioFocus(focusListener);
            }
        } catch (RuntimeException ignored) { /* A failed system audio service must not block cleanup. */ }
        finally { hasFocus = false; }
    }

    private void startPlayback() {
        if (closed || !foreground || !video || videoUrl == null) return;
        if (player == null) {
            failed = false; autoPlay = true;
            listener.onState("loading", "reconnect", position);
            preparePlayer();
            return;
        }
        if (!prepared || seeking) return;
        try {
            if (!requestFocusForAudio()) {
                autoPlay = false;
                status.setText("暂时无法播放声音，请稍后点击播放"); status.setVisibility(VISIBLE);
                listener.onState("paused", "audio_focus_denied", position); return;
            }
            player.start(); playing = true; autoPlay = false; ended = false;
            play.setText("暂停"); setKeepScreenOn(true);
            lastProgressPosition = position;
            beginBuffering("starting");
            main.removeCallbacks(progressWatch);
            main.postDelayed(progressWatch, 1000);
        } catch (Exception failure) { fail("MEDIA_PLAYBACK_FAILED"); }
    }

    public void pause(String reason) {
        if (closed || failed) return;
        autoPlay = false;
        try {
            if (prepared && !seeking && !ended && player != null) {
                position = player.getCurrentPosition();
                if (playing) player.pause();
            }
        } catch (IllegalStateException ignored) { }
        playing = false; play.setText("播放"); setKeepScreenOn(false); abandonFocus();
        clearBuffering(); main.removeCallbacks(progressWatch);
        if (prepared && !seeking) status.setVisibility(GONE);
        if (video) listener.onState("paused", reason, position);
    }

    public void onHostPaused() {
        foreground = false;
        pause("host_paused");
        // Release the network data source too: do not keep filling a stream buffer in background.
        if (video) {
            releasePlayer();
            play.setEnabled(videoUrl != null);
            if (!failed) {
                status.setText("视频已暂停，返回后点击播放继续"); status.setVisibility(VISIBLE);
            }
        }
    }
    public void onHostResumed() { foreground = true; /* User explicitly resumes playback. */ }

    private void releasePlayer() {
        main.removeCallbacks(prepareTimeout);
        main.removeCallbacks(progressWatch);
        clearBuffering();
        MediaPlayer previous = player; player = null;
        prepared = playing = seeking = false;
        if (previous != null) {
            try { previous.release(); }
            catch (RuntimeException ignored) { /* Some vendor players throw during host teardown. */ }
        }
        setKeepScreenOn(false); abandonFocus();
    }

    private void fail(String code) {
        if (closed) return;
        failed = true;
        autoPlay = false;
        releasePlayer();
        play.setEnabled(video && videoUrl != null);
        play.setText("重试");
        status.setText(video ? "视频加载失败，可重试或返回" : "图片加载失败，请返回后重试"); status.setVisibility(VISIBLE);
        listener.onState("error", code, position);
    }

    public void close(String reason) {
        if (closed) return;
        if (prepared && !seeking && !ended && player != null) {
            try { position = player.getCurrentPosition(); } catch (IllegalStateException ignored) { }
        }
        closed = true;
        unregisterBack();
        releasePlayer();
        videoUrl = null;
        if (imageFetcher != null) imageFetcher.cancel();
        imageFile = null;
        main.removeCallbacksAndMessages(null);
        if (texture != null) texture.setSurfaceTextureListener(null);
        if (surface != null) { surface.release(); surface = null; }
        if (imageTarget != null) Glide.with(getContext().getApplicationContext()).clear(imageTarget);
        listener.onClosed(reason, position);
    }

    private boolean drag(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX(); downY = event.getRawY(); multiTouch = false;
        }
        if (event.getPointerCount() > 1) multiTouch = true;
        float distance = event.getRawY() - downY;
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !multiTouch) texture.setTranslationY(distance);
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            texture.setTranslationY(0);
            if (event.getActionMasked() == MotionEvent.ACTION_UP && !multiTouch
                    && Math.abs(distance) > Math.max(96 * getResources().getDisplayMetrics().density, getHeight() * .2f)
                    && Math.abs(distance) > Math.abs(event.getRawX() - downX)) close("gesture");
        }
        return true;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) close("system_back");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture value, int width, int height) {
        if (closed) return;
        surface = new Surface(value); preparePlayer();
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture value, int width, int height) { }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture value) {
        pause("surface_lost"); releasePlayer();
        if (!closed) play.setEnabled(true);
        if (surface != null) { surface.release(); surface = null; }
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture value) { }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Reflection keeps compileSdk 30 compatibility. No host Manifest/window policy is changed.
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Class<?> type = Class.forName("android.window.OnBackInvokedCallback");
                backDispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher").invoke(host);
                backCallback = java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                        (proxy, method, args) -> {
                            if ("onBackInvoked".equals(method.getName())) { main.post(() -> close("system_back")); return null; }
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                            if ("toString".equals(method.getName())) return "WishFoxMediaBack";
                            return null;
                        });
                Class.forName("android.window.OnBackInvokedDispatcher")
                        .getMethod("registerOnBackInvokedCallback", int.class, type).invoke(backDispatcher, 1000000, backCallback);
            } catch (Exception ignored) { backDispatcher = backCallback = null; }
        }
        if (host instanceof androidx.activity.ComponentActivity) {
            legacyBack = new androidx.activity.OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() { close("system_back"); }
            };
            ((androidx.activity.ComponentActivity) host).getOnBackPressedDispatcher().addCallback(legacyBack);
        }
    }

    private void unregisterBack() {
        if (legacyBack != null) { legacyBack.remove(); legacyBack = null; }
        if (backDispatcher != null && backCallback != null) {
            try {
                Class.forName("android.window.OnBackInvokedDispatcher")
                        .getMethod("unregisterOnBackInvokedCallback", Class.forName("android.window.OnBackInvokedCallback"))
                        .invoke(backDispatcher, backCallback);
            } catch (Exception ignored) { }
        }
        backDispatcher = backCallback = null;
    }
    @Override protected void onDetachedFromWindow() { close("detached"); super.onDetachedFromWindow(); }
}
