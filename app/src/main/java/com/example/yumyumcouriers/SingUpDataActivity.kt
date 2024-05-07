package com.example.yumyumcouriers

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.yumyumcouriers.databinding.ActivitySingUpDataBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class SingUpDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingUpDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingUpDataBinding.inflate(layoutInflater) // Инициализация свойства binding
        setContentView(binding.root)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sing_up_data)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val spinner: Spinner = findViewById(R.id.spinnerGender)
        // Создаем адаптер для спиннера с выбором пола
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_array, // ваш массив строк
            R.layout.spinner_text // ваш кастомный стиль текста
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val checkBox: CheckBox = findViewById(R.id.checkBoxAccept)

        // Создаем SpannableString с текстом гиперссылки
        val policyText = "Я ознакомлен с политикой в отношении обработки персональных данных"
        val spannableString = SpannableString(policyText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Открываем Bottom Sheet Dialog при нажатии на ссылку
                val bottomSheetDialog = BottomSheetDialog(this@SingUpDataActivity)
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

        binding.nextBtn.setOnClickListener {
            if (!checkBox.isChecked) {
                // Если checkBox не отмечен, запустите анимацию мигания красным цветом
                val shake = AnimationUtils.loadAnimation(this@SingUpDataActivity, R.anim.shake)
                checkBox.startAnimation(shake)
            } else {
                var name = binding.nameEt.text.toString()
                var surname = binding.surnameEt.text.toString()
                var patronic = binding.parentEt.text.toString()
                var number = binding.phoneEt.text.toString()
                var gender = binding.spinnerGender.selectedItem.toString()
                val intent = Intent(this, SignUpActivity::class.java)
                intent.putExtra("NAME", name)
                intent.putExtra("SURNAME", surname)
                intent.putExtra("PATRONIC", patronic)
                intent.putExtra("NUMBER", number)
                intent.putExtra("GENDER", gender)
                startActivity(intent)
            }
        }

    }
}
