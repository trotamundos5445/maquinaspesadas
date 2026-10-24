package com.example.maquinaspesadas

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging

class activity_DiagnosticoFCM : AppCompatActivity() {

    private val TAG = "DiagnosticoFCM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diagnostico_fcm)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val txtEstadoPermiso = findViewById<TextView>(R.id.txtEstadoPermisoFCM)
        val txtCanal = findViewById<TextView>(R.id.txtCanalFCM)
        val txtToken = findViewById<EditText>(R.id.txtTokenFCM)
        val btnCopiarToken = findViewById<Button>(R.id.btnCopiarToken)
        val txtUltimoMsg = findViewById<TextView>(R.id.txtUltimaNotificacion)
        val btnVolver = findViewById<Button>(R.id.btnVolverFCM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                txtEstadoPermiso.text = "Concedido (POST_NOTIFICATIONS)"
                txtEstadoPermiso.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            } else {
                txtEstadoPermiso.text = "No Concedido / Denegado"
                txtEstadoPermiso.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
        } else {
            txtEstadoPermiso.text = "Concedido (API < 33)"
            txtEstadoPermiso.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }

        txtCanal.text = getString(R.string.default_notification_channel_id)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    txtToken.setText("Error al obtener token: ${task.exception?.localizedMessage}")
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Registration Token: $token")
                txtToken.setText(token)
            }

        btnCopiarToken.setOnClickListener {
            val tokenStr = txtToken.text.toString()
            if (tokenStr.isNotEmpty() && !tokenStr.startsWith("Obteniendo") && !tokenStr.startsWith("Error")) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FCM Token", tokenStr)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "¡Token copiado al portapapeles!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Token no disponible para copiar", Toast.LENGTH_SHORT).show()
            }
        }

        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        val lastTitle = prefs.getString("last_title", null)
        val lastBody = prefs.getString("last_body", null)
        if (lastTitle != null || lastBody != null) {
            txtUltimoMsg.text = "Título: ${lastTitle ?: "Sin título"}\nMensaje: ${lastBody ?: "Sin contenido"}"
        } else {
            txtUltimoMsg.text = "Sin notificaciones recibidas aún."
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}
