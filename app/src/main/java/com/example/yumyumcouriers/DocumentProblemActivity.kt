package com.example.yumyumcouriers

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DocumentProblemActivity : AppCompatActivity() {
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var requestPassport: ImageView
    private lateinit var requestDataHome: ImageView
    private lateinit var requestINN: ImageView
    private lateinit var requestWorkBook: ImageView
    private lateinit var requestSNILS: ImageView
    private lateinit var requestMed: ImageView
    private lateinit var indexPassport: ImageView
    private lateinit var indexDataHome: ImageView
    private lateinit var indexINN: ImageView
    private lateinit var indexWorkBook: ImageView
    private lateinit var indexSNILS: ImageView
    private lateinit var indexMed: ImageView

    private var courierID: String = "87613hc81hf811hf971ghf908"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_document_problem)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestPassport = findViewById(R.id.SendDataPassport)
        requestDataHome = findViewById(R.id.SendDataHome)
        requestINN = findViewById(R.id.SendDataINN)
        requestWorkBook = findViewById(R.id.SendDataWorkBook)
        requestSNILS = findViewById(R.id.SendDataSNILS)
        requestMed = findViewById(R.id.SendDataMed)
        indexPassport = findViewById(R.id.indPasport)
        indexDataHome = findViewById(R.id.indHome)
        indexINN = findViewById(R.id.indINN)
        indexWorkBook = findViewById(R.id.indWorkBook)
        indexSNILS = findViewById(R.id.indSNILS)
        indexMed = findViewById(R.id.indMed)

        requestPassport.setOnClickListener{
            dispatchTakePictureIntent(courierID, "passport")
        }
        requestDataHome.setOnClickListener{
            dispatchTakePictureIntent(courierID, "datahome")
        }
        requestINN.setOnClickListener{
            dispatchTakePictureIntent(courierID, "inn")
        }
        requestWorkBook.setOnClickListener{
            dispatchTakePictureIntent(courierID, "workbook")
        }
        requestSNILS.setOnClickListener{
            dispatchTakePictureIntent(courierID, "snils")
        }
        requestMed.setOnClickListener{
            dispatchTakePictureIntent(courierID, "med")
        }
    }


    private fun dispatchTakePictureIntent(courierID: String, typeData: String) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Добавляем данные в Intent
                takePictureIntent.putExtra("courierID", courierID)
                takePictureIntent.putExtra("typeData", typeData)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Получаем данные из Intent
            val courierID = data?.getStringExtra("courierID")
            val typeData = data?.getStringExtra("typeData")

            // Получаем изображение из файла
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            if (imageBitmap != null) {
                Log.d("TAG", "ImageBitmap: $imageBitmap")
                // Устанавливаем изображение в ImageView
                when (typeData) {
                    "passport" -> {
                        indexPassport.setImageBitmap(imageBitmap)
                        indexPassport.setBackgroundResource(R.drawable.round_green_card)
                        Toast.makeText(this, "ура!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Прикрепите изображение!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}