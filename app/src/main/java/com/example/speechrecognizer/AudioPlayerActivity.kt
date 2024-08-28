package com.example.speechrecognizer

import android.content.res.Resources
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speechrecognizer.databinding.ActivityAudioPlayerBinding

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var mediaplayer:MediaPlayer
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")

         mediaplayer = MediaPlayer()
        mediaplayer.apply {
            setDataSource(filePath)
            prepare()
        }


        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            binding.seekBar.progress  = mediaplayer.currentPosition
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

    }

    private fun playPausePlayer() {
        if(!mediaplayer.isPlaying){
            mediaplayer.start()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause,theme)
            handler.postDelayed(runnable,0)
        }else{
            mediaplayer.pause()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.icon_play_circle,theme)
            handler.removeCallbacks(runnable)
        }
    }
}