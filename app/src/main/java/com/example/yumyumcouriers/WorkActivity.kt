package com.example.yumyumcouriers

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityOptionsCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.map.MapObjectCollection


import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class WorkActivity : AppCompatActivity(){
    private lateinit var mapView: MapView
    private lateinit var btnShowBottomSheet: AppCompatButton
    private lateinit var userProfile: LinearLayout
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация MapKit и DirectionsApi
        MapKitFactory.setApiKey("d39bb6d0-8d48-4c96-8e84-755ca88e07c6")
        MapKitFactory.setLocale("ru_RU")
        setContentView(R.layout.activity_work)
        mapView = findViewById(R.id.mapview)
        mapView.map.move(
            CameraPosition(
                Point(55.031091, 82.920675),
                /* zoom = */ 17.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 30.0f
            ))
        //setMarkerInStartLocation()

        uid = intent.getStringExtra("UID").toString()

        // Проверка на наличие ограничений

        val db = Firebase.firestore

        // Инициализация кнопки для отображения Bottom Sheet Dialog
        btnShowBottomSheet = findViewById(R.id.idBtnShowBottomSheet)
        userProfile = findViewById(R.id.userProfileFail)

        userProfile.setOnClickListener{
            val intent = Intent(this, DocumentProblemActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
            startActivity(intent, options.toBundle())
        }

        btnShowBottomSheet.setOnClickListener {
            // Создание Bottom Sheet Dialog
            val dialog = BottomSheetDialog(this)

            // Настройка контента Bottom Sheet Dialog
            val view = layoutInflater.inflate(R.layout.bottom_sheet_work, null)
            val btnClose = view.findViewById<Button>(R.id.idBtnDismiss)
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.setContentView(view)
            dialog.show()
        }
    }
    private fun setMarkerInStartLocation() {
        val marker = R.drawable.back // Добавляем ссылку на картинку
        mapObjectCollection = mapView.map.mapObjects // Инициализируем коллекцию различных объектов на карте
        placemarkMapObject = mapObjectCollection.addPlacemark(Point(55.031091, 82.920675), ImageProvider.fromResource(this, marker)) // Добавляем метку со значком
        placemarkMapObject.opacity = 0.5f // Устанавливаем прозрачность метке
        placemarkMapObject.setText("Обязательно к посещению!") // Устанавливаем текст сверху метки
    }
}