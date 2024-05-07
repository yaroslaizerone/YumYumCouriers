package com.example.yumyumcouriers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yumyumcouriers.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passEt.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    // Проверяем, зарегистрирован ли email
                    firebaseAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val result = task.result
                            if (result?.signInMethods?.isEmpty() == true) {
                                // Email не зарегистрирован, можно создать нового пользователя
                                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            currentUser?.let { user ->
                                                // Получаем uid пользователя
                                                val uid = user.uid
                                                intent.putExtra("UID", uid)
                                                // Создаем объект данных для записи в коллекцию Firestore
                                                val userData = hashMapOf(
                                                    "uid" to uid,
                                                    "role" to "4"
                                                )
                                                val db = FirebaseFirestore.getInstance()
                                                val authCollectionRef = db.collection("authentication")

                                                // Добавляем данные в коллекцию Firestore
                                                authCollectionRef.add(userData)
                                                    .addOnSuccessListener { documentReference ->
                                                        // Обработка успешного добавления записи
                                                        Log.d(
                                                            "TAG",
                                                            "DocumentSnapshot added with ID: ${documentReference.id}"
                                                        )
                                                    }
                                                    .addOnFailureListener { e ->
                                                        // Обработка ошибок при добавлении записи
                                                        Log.e("TAG", "Error adding document", e)
                                                    }

                                                // Генерируем случайный ID
                                                val randomId = Random().nextInt(10000) + 1

                                                // Получаем значения из intent
                                                val name = intent.getStringExtra("NAME").toString()
                                                val surname = intent.getStringExtra("SURNAME").toString()
                                                val patronic = intent.getStringExtra("PATRONIC").toString()
                                                val number = intent.getStringExtra("NUMBER").toString()
                                                val gender = intent.getStringExtra("GENDER").toString()

                                                // Создаем объект данных для записи в коллекцию Firestore
                                                val staffData = hashMapOf(
                                                    "name" to name,
                                                    "surname" to surname,
                                                    "patronic" to patronic,
                                                    "number" to number,
                                                    "gender" to gender,
                                                    "employmentrecord" to "",
                                                    "snils" to "",
                                                    "medicalbook" to "",
                                                    "psn" to "",
                                                    "id" to randomId,
                                                    "uid" to uid
                                                )

                                                val staffCollectionRef = db.collection("staff")

                                                // Добавляем данные в коллекцию Firestore
                                                staffCollectionRef.add(staffData)
                                                    .addOnSuccessListener { documentReference ->
                                                        // Обработка успешного добавления записи
                                                        Log.d(
                                                            "TAG",
                                                            "DocumentSnapshot added with ID: ${documentReference.id}"
                                                        )
                                                    }
                                                    .addOnFailureListener { e ->
                                                        // Обработка ошибок при добавлении записи
                                                        Log.e("TAG", "Error adding document", e)
                                                    }


                                            }
                                            val intent = Intent(this, WorkActivity::class.java)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(
                                                this,
                                                it.exception.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                // Email уже зарегистрирован
                                Toast.makeText(
                                    this,
                                    "Этот email уже зарегистрирован",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Ошибка при проверке email: ${task.exception}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Необходимо заполнить все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}