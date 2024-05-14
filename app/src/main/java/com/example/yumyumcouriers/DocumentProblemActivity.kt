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
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

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
    private lateinit var pressImageView: ImageView
    private lateinit var storageRef: StorageReference
    private lateinit var documentReady: AppCompatButton

    private lateinit var typeData: String
    private lateinit var courierUID: String
    private val dictionary = mutableMapOf<String, ByteArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_document_problem)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val photoImageViews = listOf<ImageView>(
            findViewById(R.id.SendDataPassport),
            findViewById(R.id.SendDataHome),
            findViewById(R.id.SendDataINN),
            findViewById(R.id.SendDataWorkBook),
            findViewById(R.id.SendDataSNILS),
            findViewById(R.id.SendDataMed),
            findViewById(R.id.indPasport),
            findViewById(R.id.indHome),
            findViewById(R.id.indINN),
            findViewById(R.id.indWorkBook),
            findViewById(R.id.indSNILS),
            findViewById(R.id.indMed)
        )

        // Устанавливаем обработчики кликов для каждого ImageView
        photoImageViews.forEach { imageView ->
            imageView.setOnClickListener {
                val typePhoto = when (imageView.id) {
                    R.id.SendDataPassport, R.id.indPasport -> "passport"
                    R.id.SendDataHome, R.id.indHome -> "datahome"
                    R.id.SendDataINN, R.id.indINN -> "inn"
                    R.id.SendDataWorkBook, R.id.indWorkBook -> "workbook"
                    R.id.SendDataSNILS, R.id.indSNILS -> "snils"
                    R.id.SendDataMed, R.id.indMed -> "med"
                    else -> return@setOnClickListener
                }
                dispatchTakePictureIntent(imageView, typePhoto)
            }
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

        documentReady = findViewById(R.id.DocumentReady)

        documentReady.setOnClickListener {
            uploadAllImagesToFirebaseStorage(dictionary, courierUID)
        }
    }


    private fun dispatchTakePictureIntent( imageViewSend: ImageView, typePhoto: String) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                typeData = typePhoto
                pressImageView = imageViewSend
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun uploadAllImagesToFirebaseStorage(dictionary: Map<String, ByteArray>, courierUID: String) {
        for ((typeData, imageBitmap) in dictionary) {
            uploadImageToFirebaseStorage(imageBitmap, courierUID, typeData)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?

            if (imageBitmap != null) {
                changeImage(pressImageView, imageBitmap)
                val encryptedImage = encryptImage(imageBitmap, courierUID)
                dictionary[typeData] = encryptedImage
                if (dictionary.size == 6) {
                    // Изменяем цвет кнопки DocumentReady и разрешаем ее нажатие
                    documentReady.setBackgroundResource(R.drawable.rb_open_panel)
                    documentReady.isEnabled = true
                }
            } else {
                Toast.makeText(this, "Прикрепите изображение!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encryptImage(image: Bitmap, key: String): ByteArray {
        // Преобразуем изображение в массив байтов
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val byteArray = baos.toByteArray()

        // Создаем объект SecretKeySpec для использования в шифровании
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")

        // Инициализируем объект Cipher для шифрования
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)

        // Шифруем изображение
        return cipher.doFinal(byteArray)
    }

    private fun changeImage(indexView: ImageView, image: Bitmap){
        indexView.setImageBitmap(image)
        indexView.setBackgroundResource(0)
    }

    private fun uploadImageToFirebaseStorage(imageByteArray: ByteArray, courierUID: String, typeData: String) {
        // Формируем путь для загрузки изображения в Firebase Storage
        val imagePath = "images/$courierUID/$typeData.jpg"

        // Получаем ссылку на файл в Firebase Storage
        val imageRef = storageRef.child(imagePath)

        // Загружаем изображение с шифрованием в Firebase Storage
        imageRef.putBytes(imageByteArray)
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