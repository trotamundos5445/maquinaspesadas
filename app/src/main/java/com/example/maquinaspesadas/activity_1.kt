package com.example.maquinaspesadas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

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

        title = getUsuarioActual()

        val btnMaquinas = findViewById<Button>(R.id.btnMaquinarias)
        val btnClientes = findViewById<Button>(R.id.btnClientes)
        val btnArriendos = findViewById<Button>(R.id.btnArriendos)
        val btnUsuarios = findViewById<Button>(R.id.btnUsuarios)

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

        btnUsuarios.setOnClickListener {
            val intent = Intent(this, activity_Usuarios::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
            finish()
        }
    }

    fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
    }

    private fun getUsuarioActual(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email ?: "Sin sesión"
    }
}
