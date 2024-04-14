package com.example.yumyumcouriers

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.yumyumcouriers.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point as MapPoint
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.google.android.material.bottomsheet.BottomSheetDialog

class WorkActivity : AppCompatActivity(){
    private lateinit var mapView: MapView
    private lateinit var btnShowBottomSheet: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация MapKit и DirectionsApi
        MapKitFactory.setApiKey("17ef2389-2ef1-4c0d-8e26-bcd08e83e535")
        MapKitFactory.setLocale("ru_RU")
        setContentView(R.layout.activity_work)
        mapView = findViewById(R.id.mapview)
        mapView.map.move(
            CameraPosition(
                MapPoint(55.031091, 82.920675),
                /* zoom = */ 17.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 30.0f
            ))

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

        // Запуск построения маршрута
        val start = MapPoint(55.031091, 82.920675) // Начальная точка
        val end = MapPoint(55.041091, 82.920675) // Конечная точка
    }
}
