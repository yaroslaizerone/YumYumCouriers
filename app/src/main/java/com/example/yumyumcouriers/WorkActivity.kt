package com.example.yumyumcouriers

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView


class WorkActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var drivingRouter: DrivingRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
        mapView = findViewById(R.id.mapview)

        MapKitFactory.setApiKey("17ef2389-2ef1-4c0d-8e26-bcd08e83e535")
        MapKitFactory.setLocale("ru_RU")


        // Начальная и конечная точки маршрута
        val start = Point(55.036457, 82.921606)
        val end = Point(55.052655, 82.893037)

        mapView.map.move(
            CameraPosition(start, 15.0f, 0.0f, 0.0f),
            null
        )
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
