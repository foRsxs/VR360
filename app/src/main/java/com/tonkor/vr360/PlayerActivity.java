package com.tonkor.vr360;

import java.util.*;

import com.panframe.android.lib.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.support.v4.app.*;
import android.util.Log;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.SeekBar.*;

public class PlayerActivity extends FragmentActivity implements PFAssetObserver, OnSeekBarChangeListener, PFHotspotClickListener{

    PFView				_pfview;
    PFAsset 			_pfasset;
    PFNavigationMode 	_currentNavigationMode = PFNavigationMode.MOTION;

    boolean 			_updateThumb = true;;
    Timer 				_scrubberMonitorTimer;

    ViewGroup 			_frameContainer;
    Button				_stopButton;
    Button				_playButton;
    Button				_touchButton;

    Button				_main2Button;
    SeekBar				_scrubber;
    OrientationEventListener ol;

    ImageView 			_logoImage;

    /**
     * Creation and initalization of the Activitiy.
     * Initializes variables, listeners, and starts request of a movie list.
     *
     * @param  savedInstanceState  a saved instance of the Bundle
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);

        _frameContainer = (ViewGroup) findViewById(R.id.framecontainer);
        _frameContainer.setBackgroundColor(0xFF000000);

        _playButton = (Button)findViewById(R.id.playbutton);
        _stopButton = (Button)findViewById(R.id.stopbutton);
        _touchButton = (Button)findViewById(R.id.touchbutton);
        _scrubber = (SeekBar)findViewById(R.id.scrubber);
        _logoImage = (ImageView) findViewById(R.id.imageView);
        _main2Button = (Button) findViewById(R.id.main2Button);


        _playButton.setOnClickListener(playListener);
        _stopButton.setOnClickListener(stopListener);
        _touchButton.setOnClickListener(touchListener);
        _scrubber.setOnSeekBarChangeListener(this);

        _scrubber.setEnabled(false);

        showControls(true);
    }


    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (_pfview != null)
            _pfview.handleOrientationChange();
        Log.d("Player","configuration Orient Changed");
    }

    /**
     * Show/Hide the playback controls
     *
     * @param  bShow  Show or hide the controls. Pass either true or false.
     */
    public void showControls(boolean bShow)
    {
        int visibility = View.GONE;

        if (bShow)
            visibility = View.VISIBLE;

        _playButton.setVisibility(visibility);
        _stopButton.setVisibility(visibility);
        _touchButton.setVisibility(visibility);
        _scrubber.setVisibility(visibility);

        if (_pfview != null)
        {
            if (!_pfview.supportsNavigationMode(PFNavigationMode.MOTION))
                _touchButton.setVisibility(View.GONE);
        }
    }


    /**
     * Start the video with a local file path
     *
     * @param  filename  The file path on device storage
     */
    public void loadVideo(String filename)
    {
        Log.d("Player","start LoadVideo");
        _pfview = PFObjectFactory.view(this);
        _pfasset = PFObjectFactory.assetFromUri(this, Uri.parse(filename), this);

        _pfview.displayAsset(_pfasset);
        _pfview.setNavigationMode(_currentNavigationMode);
        _pfview.setBlindSpotImage(BitmapFactory.decodeResource(getResources(), R.raw.blackspot));
        _pfview.setBlindSpotPosition(1);
        _pfview.setBlindSpotScale(1.5f);


        PFHotspot hp1 = _pfview.createHotspot(BitmapFactory.decodeResource(getResources(), R.raw.hotspot));
        hp1.setTag(10);
        hp1.setCoordinates(60, 40, 0);
        hp1.setClickListener(this);

        PFHotspot hp2 = _pfview.createHotspot(BitmapFactory.decodeResource(getResources(), R.raw.hotspot));
        hp2.setTag(20);
        hp2.setCoordinates(0, 40, 0);
        hp2.setClickListener(this);

        _frameContainer.addView(_pfview.getView(), 0);
    }

    /**
     * Status callback from the PFAsset instance.
     * Based on the status this function selects the appropriate action.
     *
     * @param  asset  The asset who is calling the function
     * @param  status The current status of the asset.
     */
    public void onStatusMessage(final PFAsset asset, PFAssetStatus status) {
        switch (status)
        {
            case LOADED:
                Log.d("Player", "Loaded");
                break;
            case DOWNLOADING:
                Log.d("Player", "Downloading 360 movie: "+_pfasset.getDownloadProgress()+" percent complete");
                break;
            case DOWNLOADED:
                Log.d("Player", "Downloaded to "+asset.getUrl());
                break;
            case DOWNLOADCANCELLED:
                Log.d("Player", "Download cancelled");
                break;
            case PLAYING:
                Log.d("Player", "Playing");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                _scrubber.setEnabled(true);
                _scrubber.setMax((int) asset.getDuration());
                _playButton.setText("pause");
                if (_scrubberMonitorTimer == null)
                {
                    _scrubberMonitorTimer = new Timer();
                    final TimerTask task = new TimerTask() {
                        public void run() {
                            if (_updateThumb)
                            {
                                _scrubber.setMax((int) asset.getDuration());
                                _scrubber.setProgress((int) asset.getPlaybackTime());
                            }
                        }
                    };
                    _scrubberMonitorTimer.schedule(task, 0, 33);
                }
                break;
            case PAUSED:
                Log.d("Player", "Paused");
                _playButton.setText("play");
                //_stopButton.performClick();
                _pfview.injectImageFromResource(R.raw.pausescreen);
                break;
            case STOPPED:
                Log.d("Player", "Stopped");
                _playButton.setText("play");
                if (_scrubberMonitorTimer != null)
                {
                    _scrubberMonitorTimer.cancel();
                    _scrubberMonitorTimer.purge();
                    _scrubberMonitorTimer = null;
                }
                _scrubber.setProgress(0);
                _scrubber.setEnabled(false);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case COMPLETE:
                Log.d("Player", "Complete");
                _playButton.setText("play");
                if (_scrubberMonitorTimer != null)
                {
                    _scrubberMonitorTimer.cancel();
                    _scrubberMonitorTimer.purge();
                    _scrubberMonitorTimer = null;
                }
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case ERROR:
                Log.d("Player", "Error");
                break;
        }
    }


    public void main2Click(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }


    /**
     * Click listener for the play/pause button
     *
     */
    private OnClickListener playListener = new OnClickListener() {
        public void onClick(View v) {
            Log.d("Player","playListener0nClick click");

            if (_pfasset == null) {
                _logoImage.setVisibility(View.GONE); //hide logo
                loadVideo("file:///android_asset/skyrim360.mp4");
            }

            if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
            {
                _pfasset.pause();
                Log.d("Player","playListener0nClick set Pause");
            }
            else
            {
                Log.d("Player","playListener0nClick play after Pause");
                if (_pfview != null)
                {
                    _pfview.injectImage(null);
                }
                _pfasset.play();

            }
        }
    };

    /**
     * Click listener for the stop/back button
     *
     */
    private OnClickListener stopListener = new OnClickListener() {
        public void onClick(View v) {
            if (_pfasset == null)
                return;

            _pfasset.stop();

            _logoImage.setVisibility(View.VISIBLE);

            // cleanup
            _pfview.release();
            _pfasset.release();

            _pfasset = null;

            _frameContainer.removeView(_pfview.getView());
            _pfview = null;

            Log.d("Player","click stopped");

        }
    };

    /**
     * Click listener for the navigation mode (touch/motion (if available))
     *
     */
    private OnClickListener touchListener = new OnClickListener() {
        public void onClick(View v) {
            if (_pfview != null)
            {
                Button touchButton = (Button)findViewById(R.id.touchbutton);
                if (_currentNavigationMode == PFNavigationMode.TOUCH)
                {
                    _currentNavigationMode = PFNavigationMode.MOTION;
                    touchButton.setText("motion");
                }
                else
                {
                    _currentNavigationMode = PFNavigationMode.TOUCH;
                    touchButton.setText("touch");
                }
                _pfview.setNavigationMode(_currentNavigationMode);
            }
        }
    };

    /**
     * Setup the options menu
     *
     * @param menu The options menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    /**
     * Called when pausing the app.
     * This function pauses the playback of the asset when it is playing.
     *
     */
    public void onPause() {
        super.onPause();
        if (_pfasset != null)
        {
            if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
                _pfasset.pause();
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     *
     * @param seekbar The SeekBar whose progress has changed
     * @param progress The current progress level.
     * @param fromUser True if the progress change was initiated by the user.
     *
     */
    public void onProgressChanged (SeekBar seekbar, int progress, boolean fromUser) {
    }

    /**
     * Notification that the user has started a touch gesture.
     * In this function we signal the timer not to update the playback thumb while we are adjusting it.
     *
     * @param seekbar The SeekBar in which the touch gesture began
     *
     */
    public void onStartTrackingTouch(SeekBar seekbar) {
        _updateThumb = false;
    }

    /**
     * Notification that the user has finished a touch gesture.
     * In this function we request the asset to seek until a specific time and signal the timer to resume the update of the playback thumb based on playback.
     *
     * @param seekbar The SeekBar in which the touch gesture began
     *
     */
    public void onStopTrackingTouch(SeekBar seekbar) {
        _pfasset.setPLaybackTime(seekbar.getProgress());
        _updateThumb = true;
    }


    /**
     * Notification that the user has touched a hotspot.
     *
     * @param hotspot The hotspot that was touched or triggered.
     *
     */
    public void onClick(PFHotspot hotspot) {
//		hotspot.animate();
        hotspot.setEnabled(false);
        Log.d("SimplePlayer", "Hotspot clicked: "+hotspot.getTag());
    }


    @Override
    public void onFocusIn(PFHotspot arg0) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onFocusOut(PFHotspot arg0) {
        // TODO Auto-generated method stub

    }


}
