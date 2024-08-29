package com.example.speechrecognizer

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.speechrecognizer.databinding.ActivityGalleryBinding
import com.example.speechrecognizer.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var allChecked = false

    private lateinit var binding: ActivityGalleryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        var bottomSheet = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.toolbar.setNavigationOnClickListener{
            onBackPressed()
        }

        records = ArrayList()
        mAdapter = Adapter(records,this)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        binding.recyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchAll()

        binding.searchInput.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query = p0.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        binding.btnClose.setOnClickListener {


            leaveEditMode()

        }

        binding.btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.map { it.isChecked = allChecked }
            mAdapter.notifyDataSetChanged()
            if(allChecked){
                disableRename()
                enableDelete()
            }else{
                disableRename()
                disableDelete()
            }
        }

        binding.btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete Record?")
                val nbRecord = records.count{it.isChecked}
                builder.setMessage("Are you sure you want to delete $nbRecord record(s) ?")

            builder.setPositiveButton("Delete"){_,_ ->
                val toDelete = records.filter{it.isChecked}.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)
                    runOnUiThread {
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }

            }

            builder.setNegativeButton("Cancel"){_,_ ->

            }
            val dialog = builder.create()
            dialog.show()
        }
        binding.btnEdit.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = this.layoutInflater.inflate(R.layout.rename_layout,null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val record = records.filter { it.isChecked }.get(0)
            val textInput = dialogView.findViewById<TextInputEditText>(R.id.filenameinput)
            textInput.setText(record.filename)

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
                val  input = textInput.text.toString()
                if(input.isEmpty()){
                    Toast.makeText(this, "A name is required",Toast.LENGTH_SHORT).show()

                }else{
                    record.filename = input
                    GlobalScope.launch {
                        db.audioRecordDao().update(record)
                        runOnUiThread{
                            mAdapter.notifyItemChanged(records.indexOf(record))
                            dialog.dismiss()
                            leaveEditMode()

                        }
                    }
                }

            }

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                      dialog.dismiss()
            }
            dialog.show()

        }
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().searchDatabse("%$query%")
            records.addAll(queryResult)
            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)


        binding.editbar.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED


        records.map { it.isChecked = false }
        mAdapter.setEditMode(false)
    }


    private fun disableRename(){
        binding.btnEdit.isClickable = false
        binding.btnEdit.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme)
        binding.tvEdit.setTextColor( ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme))
    }

    private fun disableDelete(){
        binding.btnDelete.isClickable = false
        binding.btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme)
        binding.tvDelete.setTextColor( ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme))
    }

    private fun enableRename(){
        binding.btnEdit.isClickable = true
        binding.btnEdit.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme)
        binding.tvEdit.setTextColor( ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme))
    }

    private fun enableDelete(){
        binding.btnDelete.isClickable = true
        binding.btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme)
        binding.tvDelete.setTextColor( ResourcesCompat.getColorStateList(resources,R.color.grayDark,theme))
    }

    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord = records[position]

        if(mAdapter.isEditMode()){
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var nbSelected = records.count{it.isChecked}

            when(nbSelected){
                0 ->{
                    disableDelete()
                    disableRename()
                }
                1 ->{
                    enableDelete()
                    enableRename()
                }
                3 ->{
                    enableDelete()
                    disableRename()
                }
            }
        }else{
            var intent = Intent(this,AudioPlayerActivity::class.java)
            intent.putExtra("filepath",audioRecord.filePath)
            intent.putExtra("filename",audioRecord.filename)
            startActivity(intent)
        }

    }

    override fun onItemLongClickListener(position: Int) {
       mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        if(mAdapter.isEditMode() && binding.editbar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            binding.editbar.visibility = View.VISIBLE
            enableDelete()
            enableRename()

        }
    }
}