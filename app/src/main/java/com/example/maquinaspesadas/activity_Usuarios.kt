package com.example.maquinaspesadas

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UsuarioData(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "Cliente"
)

class activity_Usuarios : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private val listAdapter = SimpleListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuarios)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val txtNombre = findViewById<EditText>(R.id.txtNombreUsuario)
        val txtCorreo = findViewById<EditText>(R.id.txtCorreoUsuario)
        val txtRol = findViewById<EditText>(R.id.txtRolUsuario)
        val txtPass = findViewById<EditText>(R.id.txtContrasenaUsuario)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarUsuario)
        val btnModificar = findViewById<Button>(R.id.btnModificarUsuario)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarUsuario)
        val btnVolver = findViewById<Button>(R.id.btnVolverUsuario)
        val rvUsuarios = findViewById<RecyclerView>(R.id.recyclerViewUsuarios)

        rvUsuarios.layoutManager = LinearLayoutManager(this)
        rvUsuarios.adapter = listAdapter

        cargarUsuariosEnLista()

        btnGuardar.setOnClickListener {
            val nombre = txtNombre.text.toString().trim()
            val correo = txtCorreo.text.toString().trim()
            val rol = if (txtRol.text.toString().trim().isNotEmpty()) txtRol.text.toString().trim() else "Cliente"
            val pass = txtPass.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese nombre, correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!pass.any { it.isUpperCase() }) {
                Toast.makeText(this, "La contraseña debe incluir al menos una letra MAYÚSCULA", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!pass.any { it.isLowerCase() }) {
                Toast.makeText(this, "La contraseña debe incluir al menos una letra MINÚSCULA", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!pass.any { !it.isLetterOrDigit() }) {
                Toast.makeText(this, "La contraseña debe incluir al menos un símbolo especial (ej: @, #, $, !, *)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(correo, pass)
                .addOnCompleteListener(this) { task ->
                    val userMap = hashMapOf(
                        "nombre" to nombre,
                        "correo" to correo,
                        "rol" to rol
                    )

                    db.collection("Usuarios").add(userMap)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putString("usuario_correo", correo)
                            }
                            FirebaseAnalytics.getInstance(this).logEvent("crear_usuario", bundle)

                            Toast.makeText(this, "¡Usuario y contraseña registrados con éxito para acceder!", Toast.LENGTH_SHORT).show()
                            txtNombre.text.clear()
                            txtCorreo.text.clear()
                            txtPass.text.clear()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al guardar en base de datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }

        btnModificar.setOnClickListener {
            val nombre = txtNombre.text.toString().trim()
            val correo = txtCorreo.text.toString().trim()
            val rol = if (txtRol.text.toString().trim().isNotEmpty()) txtRol.text.toString().trim() else "Cliente"

            if (correo.isNotEmpty() && nombre.isNotEmpty()) {
                db.collection("Usuarios")
                    .whereEqualTo("correo", correo)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (doc in querySnapshot) {
                                db.collection("Usuarios").document(doc.id)
                                    .update("nombre", nombre, "rol", rol)
                            }
                            Toast.makeText(this, "¡Usuario modificado con éxito!", Toast.LENGTH_SHORT).show()
                            txtNombre.text.clear()
                            txtCorreo.text.clear()
                            txtPass.text.clear()
                        } else {
                            Toast.makeText(this, "No se encontró ningún usuario con el correo $correo", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al modificar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ingrese el correo del usuario y el nuevo nombre/rol para modificar", Toast.LENGTH_SHORT).show()
            }
        }

        btnEliminar.setOnClickListener {
            val correo = txtCorreo.text.toString().trim()

            if (correo.isNotEmpty()) {
                db.collection("Usuarios")
                    .whereEqualTo("correo", correo)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (doc in querySnapshot) {
                                db.collection("Usuarios").document(doc.id).delete()
                            }
                            Toast.makeText(this, "¡Usuario eliminado con éxito!", Toast.LENGTH_SHORT).show()
                            txtNombre.text.clear()
                            txtCorreo.text.clear()
                            txtPass.text.clear()
                        } else {
                            Toast.makeText(this, "No se encontró usuario con el correo $correo", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ingrese el correo del usuario a eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarUsuariosEnLista() {
        db.collection("Usuarios")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar usuarios: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val nombre = doc.getString("nombre") ?: doc.getString("Nombre") ?: "Sin nombre"
                        val correo = doc.getString("correo") ?: doc.getString("Correo") ?: "Sin correo"
                        val rol = doc.getString("rol") ?: doc.getString("Rol") ?: "Cliente"

                        items.add(
                            ItemData(
                                titulo = nombre,
                                subtitulo = "Correo: $correo | Rol: $rol",
                                detalle = ""
                            )
                        )
                    }
                    listAdapter.updateData(items)
                }
            }
    }
}
