package com.example.yumyumcouriers

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

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
    private lateinit var storageRef: StorageReference

    private lateinit var typeData: String
    private lateinit var courierUID: String

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
            dispatchTakePictureIntent("passport")
        }
        requestDataHome.setOnClickListener{
            dispatchTakePictureIntent("datahome")
        }
        requestINN.setOnClickListener{
            dispatchTakePictureIntent( "inn")
        }
        requestWorkBook.setOnClickListener{
            dispatchTakePictureIntent("workbook")
        }
        requestSNILS.setOnClickListener{
            dispatchTakePictureIntent("snils")
        }
        requestMed.setOnClickListener{
            dispatchTakePictureIntent("med")
        }

        // Получаем ссылку на Firebase Storage
        storageRef = FirebaseStorage.getInstance().reference
        courierUID = intent.getStringExtra("UID").toString()

        val checkBox: CheckBox = findViewById(R.id.checkBoxAccept)

        // Создаем SpannableString с текстом гиперссылки
        val policyText = "Я ознакомлен с политикой в отношении обработки персональных данных"
        val spannableString = SpannableString(policyText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Открываем Bottom Sheet Dialog при нажатии на ссылку
                val bottomSheetDialog = BottomSheetDialog(this@DocumentProblemActivity)
                bottomSheetDialog.setContentView(R.layout.bottom_sheet_politician)
                bottomSheetDialog.show()
            }
        }
        // Определяем начальный и конечный индексы для подстроки, которую мы хотим сделать гиперссылкой
        val start = policyText.indexOf("политикой в отношении обработки персональных данных")
        val end = start + "политикой в отношении обработки персональных данных".length
        // Применяем ClickableSpan только к подстроке
        spannableString.setSpan(clickableSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Устанавливаем SpannableString в CheckBox и делаем ссылку кликабельной
        checkBox.text = spannableString
        checkBox.movementMethod = LinkMovementMethod.getInstance()
    }


    private fun dispatchTakePictureIntent(typePhoto: String) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                typeData = typePhoto
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?

            if (imageBitmap != null) {
                indexPassport.setImageBitmap(imageBitmap)
                Toast.makeText(this, "фото загружается", Toast.LENGTH_SHORT).show()
                uploadImageToFirebaseStorage(imageBitmap, courierUID ?: "", typeData ?: "")
            } else {
                Toast.makeText(this, "Прикрепите изображение!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebaseStorage(bitmap: Bitmap, courierUID: String, typeData: String) {
        // Преобразовываем изображение в массив байтов
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Формируем путь для загрузки изображения в Firebase Storage
        val imagePath = "images/$courierUID/$typeData.jpg"

        // Получаем ссылку на файл в Firebase Storage
        val imageRef = storageRef.child(imagePath)

        // Загружаем изображение с шифрованием в Firebase Storage
        imageRef.putBytes(data)
            .addOnSuccessListener {
                // Обработка успешной загрузки
                Log.d("IMU", "Image uploaded successfully")
            }
            .addOnFailureListener { e ->
                // Обработка ошибки загрузки
                Log.e("IMU", "Error uploading image", e)
            }
    }
}