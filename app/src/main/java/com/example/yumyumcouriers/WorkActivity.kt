package com.example.yumyumcouriers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.yumyumcouriers.databinding.ActivityWorkBinding

import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.map.MapObjectCollection

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlin.math.log

class WorkActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var btnShowBottomSheet: AppCompatButton
    private lateinit var userProfile: LinearLayout
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var binding: ActivityWorkBinding
    private lateinit var uid: String

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    val PERMISSION_ID = 1010


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация MapKit и DirectionsApi
        MapKitFactory.setApiKey("17ef2389-2ef1-4c0d-8e26-bcd08e83e535")
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_work)
        binding = ActivityWorkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = findViewById(R.id.mapview)

        // Получаем экземпляр Map из MapView
        mapView.map.move(
            CameraPosition(
                Point(55.763338, 37.606157), // Координаты улицы с зданиями
                /* zoom = */ 17.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 30.0f
            )
        )
        //setMarkerInStartLocation()

        uid = intent.getStringExtra("UID").toString()

        // Проверка на наличие ограничений

        val db = Firebase.firestore

        db.collection("staff").whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                // Счетчик пустых значений
                var emptyFieldsCount = 0

                // Обработка документов
                for (document in documents) {
                    // Получаем данные из документа
                    val employmentRecord = document.getString("employmentrecord")
                    val snils = document.getString("snils")
                    val medicalBook = document.getString("medicalbook")
                    val psn = document.getString("psn")
                    val homeRegistration = document.getString("homeregistration")
                    val inn = document.getString("inn")

                    // Проверяем, есть ли пустые значения
                    if (employmentRecord == "") emptyFieldsCount++
                    if (snils == "") emptyFieldsCount++
                    if (medicalBook == "") emptyFieldsCount++
                    if (psn == "") emptyFieldsCount++
                    if (homeRegistration == "") emptyFieldsCount++
                    if (inn == "") emptyFieldsCount++
                }

                // Выводим результат подсчета
                binding.numberProblem.text = emptyFieldsCount.toString()
            }
            .addOnFailureListener { exception ->
                println("Ошибка при получении данных из Firestore: $exception")
            }

        // Инициализация кнопки для отображения Bottom Sheet Dialog
        btnShowBottomSheet = findViewById(R.id.idBtnShowBottomSheet)
        userProfile = findViewById(R.id.userProfileFail)

        userProfile.setOnClickListener {
            val intent = Intent(this, DocumentProblemActivity::class.java)
            intent.putExtra("UID", uid)
            val options =
                ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d("Debug:", CheckPermission().toString())
        Log.d("Debug:", isLocationEnabled().toString())
        RequestPermission()
        getLastLocation()

    }

    private fun CheckPermission(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun RequestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLastLocation() {
        if (CheckPermission()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        NewLocationData()
                    } else {
                        Log.d(
                            "Last",
                            "You Current Location is : Long: " + location.longitude + " , Lat: " + location.latitude
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Please Turn on Your device Location", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            RequestPermission()
        }
    }

    private fun NewLocationData() {
        var locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var lastLocation: Location? = locationResult.lastLocation
            Log.d("Debug:", "your last last location: " + lastLocation?.longitude.toString())
        }
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

    private fun setMarkerInStartLocation() {
        val marker = R.drawable.back // Добавляем ссылку на картинку
        mapObjectCollection =
            mapView.map.mapObjects // Инициализируем коллекцию различных объектов на карте
        placemarkMapObject = mapObjectCollection.addPlacemark(
            Point(55.031091, 82.920675),
            ImageProvider.fromResource(this, marker)
        ) // Добавляем метку со значком
        placemarkMapObject.opacity = 0.5f // Устанавливаем прозрачность метке
        placemarkMapObject.setText("Обязательно к посещению!") // Устанавливаем текст сверху метки
    }
}