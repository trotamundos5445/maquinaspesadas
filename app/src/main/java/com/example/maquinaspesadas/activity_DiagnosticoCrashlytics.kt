package com.example.maquinaspesadas

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics

class activity_DiagnosticoCrashlytics : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diagnostico_crashlytics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnErrorNoFatal = findViewById<Button>(R.id.btnErrorNoFatal)
        val btnFalloFatal = findViewById<Button>(R.id.btnFalloFatal)
        val btnVolver = findViewById<Button>(R.id.btnVolverCrash)

        btnErrorNoFatal.setOnClickListener {
            val exception = Exception("Error no fatal de prueba - Diagnóstico Crashlytics")
            FirebaseCrashlytics.getInstance().recordException(exception)
            Toast.makeText(this, "Excepción no fatal registrada en Crashlytics. La app continúa ejecutándose.", Toast.LENGTH_LONG).show()
        }

        btnFalloFatal.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar prueba de Fallo Fatal")
                .setMessage("Esta acción provocará el cierre inesperado e intencional de la aplicación para registrar un Crash fatal en Crashlytics. ¿Desea continuar?")
                .setPositiveButton("Sí, Forzar Fallo") { _, _ ->
                    throw RuntimeException("Fallo Fatal de prueba - Diagnóstico Crashlytics")
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}
