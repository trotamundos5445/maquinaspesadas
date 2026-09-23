package com.example.maquinaspesadas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class activity_1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnMaquinas = findViewById<Button>(R.id.btnMaquinarias)
        val btnClientes = findViewById<Button>(R.id.btnClientes)
        val btnArriendos = findViewById<Button>(R.id.btnArriendos)
        val btnVolver = findViewById<Button>(R.id.btnVolver1)

        btnMaquinas.setOnClickListener {
            val intent = Intent(this, activity_Maquinarias::class.java)
            startActivity(intent)
        }

        btnClientes.setOnClickListener {
            val intent = Intent(this, activity_Clientes::class.java)
            startActivity(intent)
        }

        btnArriendos.setOnClickListener {
            val intent = Intent(this, activity_Alquileres::class.java)
            startActivity(intent)
        }

        btnVolver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
