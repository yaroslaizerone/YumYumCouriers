package com.example.yumyumcouriers

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.map.MapObjectCollection


import com.google.android.material.bottomsheet.BottomSheetDialog

class WorkActivity : AppCompatActivity(){
    private lateinit var mapView: MapView
    private lateinit var btnShowBottomSheet: Button
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация MapKit и DirectionsApi
        MapKitFactory.setApiKey("17ef2389-2ef1-4c0d-8e26-bcd08e83e535")
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
        setMarkerInStartLocation()


        // Инициализация кнопки для отображения Bottom Sheet Dialog
        btnShowBottomSheet = findViewById(R.id.idBtnShowBottomSheet)
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