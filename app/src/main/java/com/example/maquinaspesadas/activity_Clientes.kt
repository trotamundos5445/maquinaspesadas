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
import com.google.firebase.firestore.FirebaseFirestore

data class Cliente(
    val id: String = "",
    val rut: String = "",
    val nombreEmpresa: String = "",
    val telefono: String = "",
    val correo: String = "",
    val direccionObra: String = ""
)

class activity_Clientes : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listAdapter = SimpleListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clientes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val txtRut = findViewById<EditText>(R.id.txtRutCliente)
        val txtEmpresa = findViewById<EditText>(R.id.txtEmpresa)
        val txtTelefono = findViewById<EditText>(R.id.txtTelefono)
        val txtCorreo = findViewById<EditText>(R.id.txtCorreo)
        val txtDireccion = findViewById<EditText>(R.id.txtDireccionObra)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarCliente)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarCliente)
        val btnVolver = findViewById<Button>(R.id.btnVolverCliente)
        val rvClientes = findViewById<RecyclerView>(R.id.recyclerViewClientes)

        rvClientes.layoutManager = LinearLayoutManager(this)
        rvClientes.adapter = listAdapter

        ChileFormatUtils.setupRutAutoFormat(txtRut)
        ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)

        cargarClientesEnLista()

        btnGuardar.setOnClickListener {
            val rut = txtRut.text.toString().trim()
            val empresa = txtEmpresa.text.toString().trim()
            val telefono = txtTelefono.text.toString().trim()
            val correo = txtCorreo.text.toString().trim()
            val direccion = txtDireccion.text.toString().trim()

            if (rut.isEmpty() || empresa.isEmpty()) {
                Toast.makeText(this, "Ingresa al menos el RUT y el Nombre de la Empresa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (correo.isNotEmpty() && !ChileFormatUtils.isValidEmail(correo)) {
                Toast.makeText(this, "Por favor ingrese un correo válido (ej: usuario@gmail.com, @hotmail.com, etc.)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = db.collection("clientes").document().id
            val cliente = Cliente(
                id = id,
                rut = rut,
                nombreEmpresa = empresa,
                telefono = telefono,
                correo = correo,
                direccionObra = direccion
            )

            db.collection("clientes").document(id)
                .set(cliente)
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Cliente guardado con éxito!", Toast.LENGTH_SHORT).show()
                    limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                    ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnEliminar.setOnClickListener {
            val rutAEliminar = txtRut.text.toString().trim()

            if (rutAEliminar.isNotEmpty()) {
                db.collection("clientes")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        var eliminados = 0
                        for (document in querySnapshot) {
                            val rDoc = document.getString("rut") ?: ""
                            if (rDoc.equals(rutAEliminar, ignoreCase = true)) {
                                db.collection("clientes").document(document.id).delete()
                                eliminados++
                            }
                        }
                        if (eliminados > 0) {
                            Toast.makeText(this, "Cliente eliminado con éxito", Toast.LENGTH_SHORT).show()
                            limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                            ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                        } else {
                            Toast.makeText(this, "No se encontró ningún cliente con ese RUT", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Escribe el RUT del cliente a eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarClientesEnLista() {
        db.collection("clientes")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar clientes: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val cliente = doc.toObject(Cliente::class.java)
                        items.add(
                            ItemData(
                                titulo = "${cliente.rut} - ${cliente.nombreEmpresa}",
                                subtitulo = "Tel: ${cliente.telefono} | Correo: ${cliente.correo}",
                                detalle = "Obra: ${cliente.direccionObra}"
                            )
                        )
                    }
                    listAdapter.updateData(items)
                }
            }
    }

    private fun limpiarCampos(vararg campos: EditText) {
        for (campo in campos) {
            campo.text.clear()
        }
    }
}
