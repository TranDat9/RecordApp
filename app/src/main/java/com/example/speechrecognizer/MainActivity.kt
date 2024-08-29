package com.example.speechrecognizer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.speechrecognizer.databinding.ActivityMainBinding
import com.example.speechrecognizer.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200
class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private lateinit var binding : ActivityMainBinding
    private lateinit var bottomSheetBinding: BottomSheetBinding

    private var permissionGranted = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private var duration = ""

    private lateinit var vibrator : Vibrator

    private lateinit var timer: Timer

    private lateinit var db: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomSheetBinding = BottomSheetBinding.bind(binding.root.findViewById(R.id.bottomSheet))

     permissionGranted = ActivityCompat.checkSelfPermission(this , permissions[0]) == PackageManager.PERMISSION_GRANTED
     if(!permissionGranted){
         ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
     }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetBinding.bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED


        timer = Timer(this)
        vibrator=getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

binding.btnRecord.setOnClickListener {
     when{
         isPaused -> resumeRecording()
         isRecording -> pausedRecording()
         else -> startRecording()
     }

    vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
}

        binding.btnList.setOnClickListener {
            //TODO
            startActivity(Intent(this,GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecorder()
            //TODO
            Toast.makeText(this,"Record saved",Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE

            bottomSheetBinding.filenameinput.setText(fileName)
        }

        bottomSheetBinding.btnCancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()


        }

        bottomSheetBinding.btnOk.setOnClickListener {
                 dismiss()
                 save()
        }

        binding.bottomSheetBG.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }


        binding.btnDelete.setOnClickListener {
            stopRecorder()
            Toast.makeText(this,"Record deleted",Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false

    }
    private fun save(){
        val newFilename = bottomSheetBinding.filenameinput.text.toString()
        if(newFilename != fileName){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath  = "$dirPath$newFilename"


       // Mở một FileOutputStream để ghi dữ liệu vào tệp có đường dẫn ampsPath.
        //Sử dụng ObjectOutputStream để ghi đối tượng amplitudes vào tệp
        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (e: IOException){

        }
        var record = AudioRecord(newFilename,filePath,timestamp,duration,ampsPath)

        GlobalScope.launch {

            db.audioRecordDao().insert(record)
        }

    }

    private fun dismiss(){
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(bottomSheetBinding.filenameinput)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            },100
        )
    }

    private fun hideKeyboard(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }


    private fun resumeRecording() {
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)

        timer.start()
    }

    private fun pausedRecording() {
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)

        timer.pause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this , permissions, REQUEST_CODE)
            return
        }
        // start record
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())

        fileName = "audio_record_$date"
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")
            try {
                prepare()
            }catch (e:Exception){}
            start()
        }
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
       isRecording = true
        isPaused = false

        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun stopRecorder(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }
        isPaused = false
        isRecording = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disabled)

        binding.btnRecord.setImageResource(R.drawable.ic_record)

        binding.tvTimer.text = "00:00.00"

        amplitudes = binding.waveformView.clear()
    }


    override fun onTimerTick(duration: String) {
         binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)
         binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}
