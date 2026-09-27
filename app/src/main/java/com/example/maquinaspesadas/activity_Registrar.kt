package com.example.maquinaspesadas

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class activity_Registrar : AppCompatActivity() {

    private lateinit var oFirebaseAnalytics: FirebaseAnalytics
    private lateinit var oFirebaseAuth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "EmailPassword"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle().apply { putString("Mensaje", "Entro_al_registro") }
        oFirebaseAnalytics.logEvent("Formulario_registro", bundle)
        title = "Formulario Registro"

        oFirebaseAuth = FirebaseAuth.getInstance()

        val btnCrear = findViewById<Button>(R.id.btnGuardarRegistrar)
        val btnVolver = findViewById<Button>(R.id.btnVolverRegistrar)

        btnCrear.setOnClickListener {
            crearRegistro()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        oFirebaseAuth.currentUser?.let { reload() }
    }

    private fun reload() {
    }

    private fun crearRegistro() {
        val txtCorreo = findViewById<EditText>(R.id.txtNombreRegistro)
        val txtPass = findViewById<EditText>(R.id.txtContrasenaRegistrar)

        val correo = txtCorreo.text.toString().trim()
        val pass = txtPass.text.toString().trim()

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (!pass.any { it.isUpperCase() }) {
            Toast.makeText(this, "La contraseña debe incluir al menos una letra MAYÚSCULA", Toast.LENGTH_SHORT).show()
            return
        }

        if (!pass.any { it.isLowerCase() }) {
            Toast.makeText(this, "La contraseña debe incluir al menos una letra MINÚSCULA", Toast.LENGTH_SHORT).show()
            return
        }

        if (!pass.any { !it.isLetterOrDigit() }) {
            Toast.makeText(this, "La contraseña debe incluir al menos un símbolo especial (ej: @, #, $, !, *)", Toast.LENGTH_SHORT).show()
            return
        }

        oFirebaseAuth.createUserWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    crearUsuario()
                    Toast.makeText(this, "¡Usuario creado con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    val exception = task.exception
                    val mensajeError = when (exception) {
                        is FirebaseAuthUserCollisionException -> "El correo '$correo' ya está registrado. Inicie sesión o use otro correo."
                        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil para Firebase."
                        is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico es inválido."
                        else -> "Error en autenticación: ${exception?.localizedMessage}"
                    }
                    Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun crearUsuario() {
        val txtNombre = findViewById<EditText>(R.id.txtNombreRegistro)

        val user = hashMapOf(
            "nombre" to txtNombre.text.toString(),
            "correo" to getUsuarioActual()
        )

        db.collection("Usuarios").add(user)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error cargando documento", e)
            }
    }

    private fun getUsuarioActual(): String {
        val user = oFirebaseAuth.currentUser
        return user?.email ?: "Sin sesión"
    }
}
