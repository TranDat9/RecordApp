package com.example.speechrecognizer

import android.content.res.Resources
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speechrecognizer.databinding.ActivityAudioPlayerBinding
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.time.Duration

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var mediaplayer:MediaPlayer
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumValue = 1000
    private var playbackSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tvFilename.text = fileName

         mediaplayer = MediaPlayer()
        mediaplayer.apply {
            setDataSource(filePath)
            prepare()
        }

        binding.tvTrackDuration.text = dateFormat(mediaplayer.duration)


        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            binding.seekBar.progress  = mediaplayer.currentPosition
            binding.tvTrackProgress.text = dateFormat(mediaplayer.currentPosition)
            handler.postDelayed(runnable,delay)
        }

        binding.btnPlay.setOnClickListener {
            playPausePlayer()
        }

        playPausePlayer()
        binding.seekBar.max = mediaplayer.duration
        mediaplayer.setOnCompletionListener {
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause,theme)
            handler.removeCallbacks(runnable)
        }

        binding.btnForward.setOnClickListener {
            mediaplayer.seekTo(mediaplayer.currentPosition+jumValue)
            binding.seekBar.progress += jumValue
        }

        binding.btnBackward.setOnClickListener {
            mediaplayer.seekTo(mediaplayer.currentPosition-jumValue)
            binding.seekBar.progress -= jumValue
        }

        binding.chip.setOnClickListener {
            if (playbackSpeed != 2f)
                playbackSpeed += 0.5f
            else
                playbackSpeed = 0.5f
            mediaplayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            binding.chip.text = "x$playbackSpeed"
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2)
                    mediaplayer.seekTo(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })



    }

    private fun playPausePlayer() {
        if(!mediaplayer.isPlaying){
            mediaplayer.start()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause,theme)
            handler.postDelayed(runnable,delay)
        }else{
            mediaplayer.pause()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.icon_play_circle,theme)
            handler.removeCallbacks(runnable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaplayer.stop()
        mediaplayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int):String{
        var d = duration/1000
        var s = duration%60
        var m = (d/60 % 60)
        var h = ((d-m*60)/360).toInt()

        val f: NumberFormat = DecimalFormat("00")
        var str = "$m:${f.format(s)}"

        if(h>0){
            str ="$h:$str"
        }
        return str
    }
}