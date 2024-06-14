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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.yumyumcouriers.ListClass.ListAdapterDish
import com.example.yumyumcouriers.ListClass.ListDataDish
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yandex.mapkit.geometry.Polyline
import kotlin.math.log


class WorkActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var btnShowBottomSheet: AppCompatButton
    private lateinit var userProfile: LinearLayout
    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var binding: ActivityWorkBinding
    private lateinit var uid: String
    private lateinit var longitude: String
    private lateinit var latitude: String


    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val PERMISSION_ID = 1010
    var id_courier = 0
    var status = -1

    private var isOrderInProgress = false
    private var isBottomSheetShowing = false
    private var isOrderTrackingStarted = false
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private var ordersListener: ListenerRegistration? = null
    val db = Firebase.firestore
    // Счетчик пустых значений

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация MapKit и DirectionsApi
        MapKitFactory.setApiKey("17ef2389-2ef1-4c0d-8e26-bcd08e83e535")
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this)

        // Инициализация макета
        binding = ActivityWorkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация элементов
        userProfile = binding.userProfileFail
        mapView = binding.mapview

        // Получаем экземпляр Map из MapView
        mapView.map.move(
            CameraPosition(
                Point(55.029468, 82.92058), // Координаты улицы с зданиями
                /* zoom = */ 14.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 30.0f
            )
        )

        // Инициализация кнопки для отображения Bottom Sheet Dialog
        btnShowBottomSheet = binding.idBtnShowBottomSheet

        // Получение UID
        uid = intent.getStringExtra("UID").toString()

        btnShowBottomSheet.setOnClickListener {
            showBottomSheet()
        }

        // Проверка на наличие ограничений
        db.collection("staff").whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                var emptyFieldsCount = 0

                // Обработка документов
                for (document in documents) {
                    id_courier = document.getLong("id")?.toInt()!!
                    val employmentRecord = document.getString("employmentrecord")
                    val snils = document.getString("snils")
                    val medicalBook = document.getString("medicalbook")
                    val psn = document.getString("psn")
                    val homeRegistration = document.getString("homeregistration")
                    val inn = document.getString("inn")
                    status = document.getLong("status")?.toInt()!!


                    // Проверяем, есть ли пустые значения
                    if (employmentRecord == "0") emptyFieldsCount++
                    if (snils == "0") emptyFieldsCount++
                    if (medicalBook == "0") emptyFieldsCount++
                    if (psn == "0") emptyFieldsCount++
                    if (homeRegistration == "0") emptyFieldsCount++
                    if (inn == "0") emptyFieldsCount++

                    if(emptyFieldsCount == 0 && status == 1){
                        startOrderTracking()
                    }

                    // Обновление UI на основе значения emptyFieldsCount
                    updateUIBasedOnEmptyFields(emptyFieldsCount)
                    break
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Ошибка при получении данных из Firestore: $exception")
            }
    }

    private fun showBottomSheet() {
        if (isBottomSheetShowing) return
        isBottomSheetShowing = true

        val view = layoutInflater.inflate(R.layout.bottom_sheet_work, null)
        val dialog = BottomSheetDialog(this)

        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()

        dialog.setOnDismissListener {
            isBottomSheetShowing = false
        }

        db.collection("staff").whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val status = document.getLong("status")?.toInt()
                    setupBottomSheetContent(view, status, dialog)
                    break
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Ошибка при получении данных из Firestore: $exception")
            }
    }

    private fun showOrderTrackingBottomSheet(
        listAdapter: ListAdapterDish,
        restaurantName: String,
        orderCode: String?,
        totalAmount: Long?,
        street: String?,
        floor: String?,
        entrance: String?,
        flat: String?,
        intercom: String?,
        path: String,
        devision: Int?,
        id_rest: Int,
        documentId: String
    ) {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_order, null)
        view.findViewById<ListView>(R.id.OrderDishes).adapter = listAdapter

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.NameRest).text = restaurantName
        view.findViewById<TextView>(R.id.additionalInfo).text = "Ресторан · $restaurantName"
        view.findViewById<TextView>(R.id.orderCode).text = orderCode
        view.findViewById<TextView>(R.id.SummaOrder).text = totalAmount?.toString()
        view.findViewById<TextView>(R.id.SteetOrder).text = street
        view.findViewById<TextView>(R.id.FlatOrder).text = floor
        view.findViewById<TextView>(R.id.EntranceOrder).text = entrance
        view.findViewById<TextView>(R.id.FlatOrder).text = flat
        view.findViewById<TextView>(R.id.IntercomOrder).text = intercom

        val buttonOrder = view.findViewById<LinearLayout>(R.id.buttonOrder)
        val conditionOrder = view.findViewById<TextView>(R.id.conditionOrder)
        val additionalInfo = view.findViewById<TextView>(R.id.additionalInfo)

        buttonOrder.visibility = View.GONE
        conditionOrder.visibility = View.GONE

        db.collection("orders").document(documentId).get()
            .addOnSuccessListener { querySnapshot ->
                    val status = querySnapshot.getString("order_status").toString()
                    if (status == "Заказ оформлен"){
                        buttonOrder.visibility = View.VISIBLE
                        conditionOrder.visibility = View.VISIBLE
                        additionalInfo.visibility = View.VISIBLE
                    }
                    else if (status == "Передан курьеру") {
                        buttonOrder.visibility = View.VISIBLE
                        conditionOrder.visibility = View.VISIBLE
                        additionalInfo.visibility = View.VISIBLE
                        conditionOrder.text = "Завершить заказ"
                    }

            }


        // Обновляем статус сотрудника при нажатии на кнопку заказа
        buttonOrder.setOnClickListener {
            updateStaffStatus(2)
            val route = convertPathStringToList(path)
            if (street != null && devision != null) {
                drawPolyline(route)
                placeMarkers(uid, id_rest, devision, street)
            }
            buttonOrder.visibility = View.GONE
            conditionOrder.visibility = View.GONE
            additionalInfo.visibility = View.GONE
        }

        // Добавляем слушатель для отслеживания изменений статуса заказа
        ordersCollection.document(documentId).addSnapshotListener { docSnapshot, error ->
            if (error != null) {
                Log.w("Firestore", "Listen failed.", error)
                return@addSnapshotListener
            }
            if (docSnapshot != null && docSnapshot.exists()) {
                val updatedStatus = docSnapshot.getString("order_status")
                if (updatedStatus == "Передан курьеру") {
                    buttonOrder.visibility = View.VISIBLE
                    additionalInfo.visibility = View.VISIBLE
                    conditionOrder.text = "Завершить заказ"

                    buttonOrder.setOnClickListener {
                        ordersCollection.document(docSnapshot.id).update("order_status", "Завершён")
                        updateStaffStatus(1)
                        mapView.map.mapObjects.clear()
                        isOrderInProgress = false // Заказ завершен
                        dialog.dismiss()
                    }
                } else {
                    buttonOrder.visibility = View.GONE
                    conditionOrder.visibility = View.GONE
                    additionalInfo.visibility = View.GONE
                }
            }
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()
        dialog.setOnDismissListener {
            isOrderInProgress = true // Заказ в процессе
        }

        btnShowBottomSheet.setOnClickListener {
            if (isOrderInProgress) {
                showOrderTrackingBottomSheet(
                    listAdapter,
                    restaurantName,
                    orderCode,
                    totalAmount,
                    street,
                    floor,
                    entrance,
                    flat,
                    intercom,
                    path,
                    devision,
                    id_rest,
                    documentId
                )
            } else {
                // Открываем другой диалог
                showBottomSheet()
            }
        }
    }

    private fun setupBottomSheetContent(view: View, status: Int?, dialog: BottomSheetDialog) {
        if (status == 1) {
            view.findViewById<TextView>(R.id.ReadyCourier).text = "Ожидание заказа"
            view.findViewById<TextView>(R.id.ProblemsInfo).text = "Ищем подходящий заказ для вас"
            view.findViewById<Button>(R.id.idBtnStart).text = "Закончить смену"
            view.findViewById<Button>(R.id.idBtnStart).setOnClickListener {
                updateStaffStatus(0)
                dialog.dismiss()
                Toast.makeText(this, "Спасибо за отработанную смену!", Toast.LENGTH_LONG).show()
            }
            startOrderTracking()
            view.findViewById<ImageView>(R.id.indicatorOrders).apply {
                setImageResource(R.drawable.search)
                setBackgroundResource(R.drawable.round_blue_card)
            }
        } else if (status == 0) {
            view.findViewById<TextView>(R.id.ReadyCourier).text = "Выйдите на смену"
            view.findViewById<TextView>(R.id.ProblemsInfo).text = "Чтобы начать поиск заказов"
            view.findViewById<Button>(R.id.idBtnStart).text = "Начать смену"
            view.findViewById<Button>(R.id.idBtnStart).setOnClickListener {
                updateStaffStatus(1)
                dialog.dismiss()
                Toast.makeText(this, "Ожидайте ваш заказ, мы уже ищем его", Toast.LENGTH_LONG).show()
                startOrderTracking()
            }
            view.findViewById<ImageView>(R.id.indicatorOrders).apply {
                setImageResource(R.drawable.search)
                setBackgroundResource(R.drawable.round_blue_card)
            }
        }
    }

    private fun updateStaffStatus(newStatus: Int) {
        val staffRef = db.collection("staff").whereEqualTo("uid", uid)
        staffRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("status", newStatus)
                }
            }
            .addOnFailureListener { e ->
                Log.w("STATUS_UPDATE", "Ошибка при обновлении статуса", e)
            }
    }

    private fun updateUIBasedOnEmptyFields(emptyFieldsCount: Int) {
        if (emptyFieldsCount != 0) {
            binding.numberProblem.text = emptyFieldsCount.toString()
        } else {
            binding.textViewReasons.visibility = View.GONE
            binding.textViewAccess.visibility = View.GONE
            binding.numberProblem.visibility = View.GONE
            binding.userProfileFail.setBackgroundResource(R.drawable.round_green_card)
            binding.imageViewAvatar.setImageResource(R.drawable.accept)
            binding.textViewFail.setPadding(0, 0, 0, 0)

            val params = binding.imageViewAvatar.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(0, 0, 0, 0)
            params.marginStart = 0
            params.marginEnd = 0
            binding.imageViewAvatar.layoutParams = params
        }



        userProfile.setOnClickListener {
            Log.d("Debug", "userProfile clicked")
            val intent = Intent(this, DocumentProblemActivity::class.java)
            intent.putExtra("UID", uid)
            val options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
            startActivity(intent, options.toBundle())
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        RequestPermission()
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

    private fun getLastLocation(): Pair<Double, Double>? {
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
                    return null
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        NewLocationData()
                    } else {
                        latitude = location.latitude.toString()
                        longitude = location.longitude.toString()

                        Log.d(
                            "Last",
                            "Your Current Location is: Lat: $latitude, Long: $longitude"
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
        return null // Возвращаем null, если координаты недоступны
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

    // Функция для отслеживания новых записей в коллекции "orders"
    private fun startOrderTracking() {
        if (isOrderTrackingStarted) return
        isOrderTrackingStarted = true

        ordersListener = ordersCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                for (document in snapshot.documents) {
                    val status = document.getString("order_status")
                    val id_rest = document.getLong("restaurant")?.toInt()
                    val devision = document.getLong("division")?.toInt()
                    val street = document.getString("street")
                    val id_courier_order = document.getLong("courier")?.toInt()
                    val path = document.getString("route")
                    val code = document.getString("code")
                    val summa = document.getLong("summa")
                    val floor = document.getString("floor")
                    val entrance = document.getString("entrance")
                    val flat = document.getString("flat")
                    val intercom = document.getString("intercom")
                    val id_document_order = document.id

                    if (status == "Заказ оформлен" && path != null && id_courier_order == id_courier) {
                        Toast.makeText(this, "Вам назначен новый заказ!", Toast.LENGTH_SHORT).show()

                        val dishesArray = document.get("dishes") as List<Map<String, Any>>
                        val dishesList = dishesArray.map { dishMap ->
                            ListDataDish(
                                name = dishMap["name"] as String,
                                quantity = (dishMap["quantity"] as Long).toInt(),
                                cost = (dishMap["cost"] as String).toDouble(),
                                photo = dishMap["photo"] as String
                            )
                        }

                        val listAdapter = ListAdapterDish(this@WorkActivity, ArrayList(dishesList))

                        db.collection("restaurant").whereEqualTo("id", id_rest).get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    val name = document.getString("name").toString()
                                    showOrderTrackingBottomSheet(
                                        listAdapter,
                                        name,
                                        code,
                                        summa,
                                        street,
                                        floor,
                                        entrance,
                                        flat,
                                        intercom,
                                        path,
                                        devision,
                                        id_rest!!.toInt(),
                                        id_document_order
                                    )
                                }
                            }
                    }
                }
                getLastLocation()
                sendLocationToFirebase()
            } else {
                Log.d("Firestore", "No new orders")
            }
        }
    }

    private fun convertPathStringToList(path: String): List<List<Double>> {
        val gson = Gson()
        val type = object : TypeToken<List<List<Double>>>() {}.type
        return gson.fromJson(path, type)
    }

    private fun drawPolyline(path: List<List<Double>>) {
        val points = path.map { Point(it[0], it[1]) }
        val polyline = Polyline(points)
        val polylineObject = mapView.map.mapObjects.addPolyline(polyline)

        polylineObject.apply {
            strokeWidth = 5f
            setStrokeColor(ContextCompat.getColor(this@WorkActivity, R.color.purple_200))
            outlineWidth = 1f
            setOutlineColor(ContextCompat.getColor(this@WorkActivity, R.color.black))
        }
    }

    private fun sendLocationToFirebase() {
        Log.d("Firestore", uid)
        val docRef = db.collection("staff").whereEqualTo("uid", uid)

        latitude = 55.044546.toString()
        longitude = 82.926609.toString()

        docRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val docId = document.id
                    val updateData = hashMapOf(
                        "latitude" to latitude,
                        "longitude" to longitude
                    )

                    db.collection("staff").document(docId)
                        .update(updateData as Map<String, Any>)  // Приведение к Map<String, Any>
                        .addOnSuccessListener {
                            Log.d("Firestore", "DocumentSnapshot successfully updated!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error updating document", e)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }

    private fun placeMarkers(uid: String, idRest: Int, devision: Int, street: String) {
        // Получение координат курьера
        db.collection("staff").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val latitudeField = document.get("latitude")
                    val longitudeField = document.get("longitude")

                    val latitude: Double? = if (latitudeField is Number) latitudeField.toDouble() else null
                    val longitude: Double? = if (longitudeField is Number) longitudeField.toDouble() else null

                    if (latitude != null && longitude != null) {
                        placeMarker(latitude, longitude, R.drawable.courier_map) // Синяя метка
                    }
                }
            }

        Log.d("Mark", "Devision: ${devision.toString()}")

    // Получение координат ресторана
        db.collection("restaurant").whereEqualTo("id", idRest).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("Mark", "No documents found for idRest: $idRest")
                } else {
                    for (document in querySnapshot.documents) {
                        val addressJson = document.getString("address")
                        Log.d("Mark", "address: ${addressJson.toString()}")
                        if (addressJson != null) {
                            try {
                                val gson = Gson()
                                val addressListType = object : TypeToken<List<Map<String, Any>>>() {}.type
                                val addressList: List<Map<String, Any>> = gson.fromJson(addressJson, addressListType)

                                Log.d("Mark", "Parsed addressList: $addressList")

                                val restaurant = addressList.firstOrNull { it["id"] == devision.toDouble() }?.get("ресторан") as? List<Double>
                                if (restaurant != null) {
                                    Log.d("Mark", "Restaurant coordinates: $restaurant")
                                    placeMarker(restaurant[0], restaurant[1], R.drawable.rest_map) // Красная метка
                                } else {
                                    Log.d("Mark", "No matching restaurant found for devision: ${devision.toString()}")
                                }
                            } catch (e: Exception) {
                                Log.e("Mark", "Error parsing address JSON", e)
                            }
                        } else {
                            Log.d("Mark", "addressJson is null")
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Mark", "Error getting documents: ", exception)
            }


        // Получение координат адреса доставки
        db.collection("user_address").whereEqualTo("streetAndNumber", street).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val latitude = document.getString("latitude")?.toDouble()
                    val longitude = document.getString("longitude")?.toDouble()
                    Log.d("placeMarker", "Marker placed at: ($latitude, $longitude)")
                    if (latitude != null && longitude != null) {
                        placeMarker(longitude, latitude, R.drawable.address_map) // Зеленая метка
                    }
                }
            }
    }
    private fun placeMarker(latitude: Double, longitude: Double, marker: Int) {
        val marker = ImageProvider.fromResource(this, marker)
        Log.d("MARKER", marker.toString())
        mapObjectCollection = mapView.map.mapObjects
        mapObjectCollection.clear() // Clear existing markers
        mapObjectCollection.addPlacemark(Point(latitude, longitude), marker)
    }
}