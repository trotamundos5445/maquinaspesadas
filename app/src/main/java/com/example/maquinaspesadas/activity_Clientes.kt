package com.example.maquinaspesadas

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
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
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var selectedClienteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clientes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val txtRut = findViewById<EditText>(R.id.txtRutCliente)
        val txtEmpresa = findViewById<EditText>(R.id.txtEmpresa)
        val txtTelefono = findViewById<EditText>(R.id.txtTelefono)
        val txtCorreo = findViewById<EditText>(R.id.txtCorreo)
        val txtDireccion = findViewById<EditText>(R.id.txtDireccionObra)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarCliente)
        val btnModificar = findViewById<Button>(R.id.btnModificarCliente)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarCliente)
        val btnVolver = findViewById<Button>(R.id.btnVolverCliente)
        val progressBar = findViewById<ProgressBar>(R.id.pbLoadingCliente)
        val rvClientes = findViewById<RecyclerView>(R.id.recyclerViewClientes)

        rvClientes.layoutManager = LinearLayoutManager(this)
        rvClientes.adapter = listAdapter

        ChileFormatUtils.setupRutAutoFormat(txtRut)
        ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)

        listAdapter.onItemClick = { item ->
            val cliente = item.rawData as? Cliente
            if (cliente != null) {
                selectedClienteId = cliente.id
                txtRut.setText(cliente.rut)
                txtEmpresa.setText(cliente.nombreEmpresa)
                txtTelefono.setText(cliente.telefono)
                txtCorreo.setText(cliente.correo)
                txtDireccion.setText(cliente.direccionObra)
                Toast.makeText(this, "Cliente seleccionado: ${cliente.nombreEmpresa}", Toast.LENGTH_SHORT).show()
            }
        }

        cargarClientesEnLista(progressBar)

        // CREAR CLIENTE
        btnGuardar.setOnClickListener {
            val rut = txtRut.text.toString().trim()
            val empresa = txtEmpresa.text.toString().trim()
            val telefono = txtTelefono.text.toString().trim()
            val correo = txtCorreo.text.toString().trim()
            val direccion = txtDireccion.text.toString().trim()

            if (!validarFormulario(rut, empresa, telefono, correo, direccion)) return@setOnClickListener

            btnGuardar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            // Verificar si RUT ya existe
            db.collection("clientes")
                .whereEqualTo("rut", rut)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true
                        Toast.makeText(this, "Ya existe un cliente registrado con el RUT $rut", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
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
                            progressBar.visibility = View.GONE
                            btnGuardar.isEnabled = true
                            Toast.makeText(this, "Cliente registrado con éxito", Toast.LENGTH_SHORT).show()

                            registrarEventoAnalytics("registro_creado")

                            limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                            selectedClienteId = null
                            ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnGuardar.isEnabled = true
                            Toast.makeText(this, "Error al crear cliente: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al verificar RUT: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // MODIFICAR CLIENTE
        btnModificar.setOnClickListener {
            val docId = selectedClienteId
            if (docId.isNullOrEmpty()) {
                Toast.makeText(this, "Seleccione un cliente de la lista para modificar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rut = txtRut.text.toString().trim()
            val empresa = txtEmpresa.text.toString().trim()
            val telefono = txtTelefono.text.toString().trim()
            val correo = txtCorreo.text.toString().trim()
            val direccion = txtDireccion.text.toString().trim()

            if (!validarFormulario(rut, empresa, telefono, correo, direccion)) return@setOnClickListener

            btnModificar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val clienteActualizado = Cliente(
                id = docId,
                rut = rut,
                nombreEmpresa = empresa,
                telefono = telefono,
                correo = correo,
                direccionObra = direccion
            )

            db.collection("clientes").document(docId)
                .set(clienteActualizado)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnModificar.isEnabled = true
                    Toast.makeText(this, "Cliente actualizado con éxito", Toast.LENGTH_SHORT).show()

                    registrarEventoAnalytics("registro_actualizado")

                    limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                    selectedClienteId = null
                    ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnModificar.isEnabled = true
                    Toast.makeText(this, "Error al actualizar cliente: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // ELIMINAR CLIENTE CON CONFIRMACIÓN
        btnEliminar.setOnClickListener {
            val rutABuscar = txtRut.text.toString().trim()

            if (selectedClienteId == null && rutABuscar.isEmpty()) {
                Toast.makeText(this, "Seleccione un cliente o ingrese su RUT para eliminar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Confirmación AlertDialog
            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro de que desea eliminar este registro de cliente?")
                .setPositiveButton("Eliminar") { _, _ ->
                    ejecutarEliminacionCliente(rutABuscar, txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion, progressBar)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun ejecutarEliminacionCliente(
        rutABuscar: String,
        txtRut: EditText,
        txtEmpresa: EditText,
        txtTelefono: EditText,
        txtCorreo: EditText,
        txtDireccion: EditText,
        progressBar: ProgressBar
    ) {
        progressBar.visibility = View.VISIBLE

        val docId = selectedClienteId

        if (!docId.isNullOrEmpty()) {
            db.collection("clientes").document(docId).delete()
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registro eliminado correctamente", Toast.LENGTH_SHORT).show()
                    registrarEventoAnalytics("registro_eliminado")
                    limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                    selectedClienteId = null
                    ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("clientes")
                .whereEqualTo("rut", rutABuscar)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val docToDelete = querySnapshot.documents.first()
                        db.collection("clientes").document(docToDelete.id).delete()
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Registro eliminado correctamente", Toast.LENGTH_SHORT).show()
                                registrarEventoAnalytics("registro_eliminado")
                                limpiarCampos(txtRut, txtEmpresa, txtTelefono, txtCorreo, txtDireccion)
                                selectedClienteId = null
                                ChileFormatUtils.setupPhoneAutoFormat(txtTelefono)
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "No se encontró ningún cliente con ese RUT", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error de red al consultar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cargarClientesEnLista(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        db.collection("clientes")
            .addSnapshotListener { querySnapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    Toast.makeText(this, "Error de conexión al cargar clientes: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    registrarEventoAnalytics("listado_consultado")

                    if (querySnapshot.isEmpty) {
                        listAdapter.updateData(emptyList())
                        Toast.makeText(this, "No existen clientes registrados", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val cliente = doc.toObject(Cliente::class.java).copy(id = doc.id)
                        items.add(
                            ItemData(
                                titulo = "${cliente.rut} - ${cliente.nombreEmpresa}",
                                subtitulo = "Tel: ${cliente.telefono} | Correo: ${cliente.correo}",
                                detalle = "Obra: ${cliente.direccionObra}",
                                rawData = cliente
                            )
                        )
                    }
                    listAdapter.updateData(items)
                }
            }
    }

    private fun validarFormulario(
        rut: String,
        empresa: String,
        telefono: String,
        correo: String,
        direccion: String
    ): Boolean {
        if (rut.isEmpty()) {
            Toast.makeText(this, "El campo RUT es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (empresa.isEmpty()) {
            Toast.makeText(this, "El campo Nombre de Empresa es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (telefono.isEmpty() || telefono == "+56 9 ") {
            Toast.makeText(this, "El campo Teléfono es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (correo.isEmpty()) {
            Toast.makeText(this, "El campo Correo Electrónico es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!ChileFormatUtils.isValidEmail(correo)) {
            Toast.makeText(this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (direccion.isEmpty()) {
            Toast.makeText(this, "El campo Dirección de la Obra es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "cliente")
        }
        firebaseAnalytics.logEvent(nombreEvento, bundle)
    }

    private fun limpiarCampos(vararg campos: EditText) {
        for (campo in campos) {
            campo.text.clear()
        }
    }
}
