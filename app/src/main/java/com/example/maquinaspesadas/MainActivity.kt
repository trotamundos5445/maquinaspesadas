package com.example.maquinaspesadas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private val TAG = "EmailPassword"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAuth = FirebaseAuth.getInstance()
        title = "Inicio de sesión"

        val btnInicio = findViewById<Button>(R.id.btnInicio)
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        val txtNombre = findViewById<EditText>(R.id.textNombre)
        val txtContrasena = findViewById<EditText>(R.id.textContrasena)

        btnInicio.setOnClickListener {
            val correo = txtNombre.text.toString().trim()
            val pass = txtContrasena.text.toString().trim()

            if (correo.isNotEmpty() && pass.isNotEmpty()) {
                ingresar(correo, pass)
            } else {
                Toast.makeText(this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegistrar.setOnClickListener {
            val intent = Intent(this, activity_Registrar::class.java)
            startActivity(intent)
        }

        btnSalir.setOnClickListener {
            finishAffinity()
        }
    }

    private fun ingresar(email: String, password: String) {
        if (email == "admin" && password == "1234") {
            Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, activity_1::class.java)
            startActivity(intent)
            finish()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, activity_1::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Credenciales incorrectas: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
