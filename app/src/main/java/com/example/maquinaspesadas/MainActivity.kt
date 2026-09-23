package com.example.maquinaspesadas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnInicio = findViewById<Button>(R.id.btnInicio)
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        val txtNombre = findViewById<EditText>(R.id.textNombre)
        val txtContrasena = findViewById<EditText>(R.id.textContrasena)

        btnInicio.setOnClickListener {
            val usuario = txtNombre.text.toString().trim()
            val pass = txtContrasena.text.toString().trim()

            if (usuario == "admin" && pass == "1234") {
                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, activity_1::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        }

        btnSalir.setOnClickListener {
            finishAffinity()
        }
    }
}
