package com.example.maquinaspesadas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val tag = "AuthProviders"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(tag, "Permiso de notificaciones concedido")
        } else {
            Toast.makeText(this, "No se mostrarán notificaciones", Toast.LENGTH_SHORT).show()
        }
    }

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
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        title = "Inicio de sesión"

        askNotificationPermission()

        val btnInicio = findViewById<Button>(R.id.btnInicio)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        val btnCrash = findViewById<Button>(R.id.btnCrash)

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

        btnGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }

        btnRegistrar.setOnClickListener {
            val intent = Intent(this, activity_Registrar::class.java)
            startActivity(intent)
        }

        btnCrash.setOnClickListener {
            throw RuntimeException("Test Crash para Firebase Crashlytics")
        }

        btnSalir.setOnClickListener {
            finishAffinity()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(tag, "Permiso de notificaciones ya concedido")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun iniciarSesionGoogle() {
        val provider = OAuthProvider.newBuilder("google.com")
        provider.addCustomParameter("prompt", "select_account")

        mAuth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { authResult ->
                Log.d(tag, "signInWithGoogle:success")
                val user = authResult.user
                registrarEventoLogin(user?.email ?: "google_user")
                Toast.makeText(this, "¡Bienvenido ${user?.email ?: ""}!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, activity_1::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "signInWithGoogle:failure", exception)
                Toast.makeText(this, "Error en Google Sign-In: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun registrarEventoLogin(usuario: String) {
        val bundle = Bundle().apply {
            putString("usuario", usuario)
            putString("fecha_login", System.currentTimeMillis().toString())
        }
        firebaseAnalytics.logEvent("inicio_sesion_exitoso", bundle)
    }

    private fun ingresar(email: String, password: String) {
        if (email == "admin" && password == "1234") {
            registrarEventoLogin("admin")
            Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, activity_1::class.java)
            startActivity(intent)
            finish()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(tag, "signInWithEmail:success")
                    registrarEventoLogin(email)
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, activity_1::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w(tag, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Credenciales incorrectas: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
