package com.example.maquinaspesadas

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Maquina(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val tarifaPorHora: Double = 0.0
)

data class Alquiler(
    val id: String = "",
    val nombreMaquina: String = "",
    val rutCliente: String = "",
    val diasArriendo: Int = 0,
    val tarifaDiaria: Double = 0.0,
    val totalPagar: Double = 0.0,
    val fecha: String = ""
)

class activity_Alquileres : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listAdapter = SimpleListAdapter()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val listaMaquinas = mutableListOf<Maquina>()
    private val nombresMaquinas = mutableListOf<String>()

    private val listaClientes = mutableListOf<Cliente>()
    private val nombresClientes = mutableListOf<String>()

    private var selectedAlquilerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alquileres)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val spnMaquinas = findViewById<Spinner>(R.id.spnMaquinas)
        val spnClientes = findViewById<Spinner>(R.id.spnClientes)
        val txtDias = findViewById<EditText>(R.id.txtDiasAlquiler)
        val lblTotal = findViewById<TextView>(R.id.txtTotalCalculado)

        val btnCalcular = findViewById<Button>(R.id.btnCalcularTotal)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAlquiler)
        val btnModificar = findViewById<Button>(R.id.btnModificarAlquiler)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarAlquiler)
        val btnVolver = findViewById<Button>(R.id.btnVolverAlquiler)
        val progressBar = findViewById<ProgressBar>(R.id.pbLoadingAlquiler)
        val rvAlquileres = findViewById<RecyclerView>(R.id.recyclerViewAlquileres)

        rvAlquileres.layoutManager = LinearLayoutManager(this)
        rvAlquileres.adapter = listAdapter

        listAdapter.onItemClick = { item ->
            val alquiler = item.rawData as? Alquiler
            if (alquiler != null) {
                selectedAlquilerId = alquiler.id
                txtDias.setText(alquiler.diasArriendo.toString())

                val indexMaq = listaMaquinas.indexOfFirst {
                    it.nombre.trim().equals(alquiler.nombreMaquina.trim(), ignoreCase = true)
                }
                if (indexMaq >= 0) {
                    spnMaquinas.setSelection(indexMaq)
                }

                val cleanRutAlquiler = alquiler.rutCliente.replace("[^0-9kK]".toRegex(), "")
                val indexCli = listaClientes.indexOfFirst {
                    it.rut.replace("[^0-9kK]".toRegex(), "").equals(cleanRutAlquiler, ignoreCase = true)
                }
                if (indexCli >= 0) {
                    spnClientes.setSelection(indexCli)
                }

                val totalFmt = ChileFormatUtils.formatPesos(alquiler.totalPagar)
                lblTotal.text = "Total a Pagar: $$totalFmt"

                Toast.makeText(this, "Alquiler seleccionado: ${alquiler.nombreMaquina} (${alquiler.rutCliente})", Toast.LENGTH_SHORT).show()
            }
        }

        cargarMaquinasEnSpinner(spnMaquinas)
        cargarClientesEnSpinner(spnClientes)
        cargarAlquileresEnLista(progressBar)

        btnCalcular.setOnClickListener {
            calcularTotal(spnMaquinas, txtDias, lblTotal)
        }

        // CREAR ALQUILER
        btnGuardar.setOnClickListener {
            val posMaquina = spnMaquinas.selectedItemPosition
            val posCliente = spnClientes.selectedItemPosition
            val dias = txtDias.text.toString().toIntOrNull() ?: 0

            if (posMaquina < 0 || posMaquina >= listaMaquinas.size) {
                Toast.makeText(this, "Seleccione una maquinaria válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (posCliente < 0 || posCliente >= listaClientes.size) {
                Toast.makeText(this, "Seleccione un cliente válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dias <= 0) {
                Toast.makeText(this, "Los días de alquiler deben ser mayores a cero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maquinaElegida = listaMaquinas[posMaquina]
            val clienteElegido = listaClientes[posCliente]
            val tarifaDiaria = maquinaElegida.tarifaPorHora
            val total = dias * tarifaDiaria

            btnGuardar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val id = db.collection("alquileres").document().id
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val alquiler = Alquiler(
                id = id,
                nombreMaquina = maquinaElegida.nombre,
                rutCliente = clienteElegido.rut,
                diasArriendo = dias,
                tarifaDiaria = tarifaDiaria,
                totalPagar = total,
                fecha = fechaActual
            )

            db.collection("alquileres").document(id)
                .set(alquiler)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "¡Alquiler registrado con éxito!", Toast.LENGTH_SHORT).show()

                    registrarEventoAnalytics("registro_creado")

                    txtDias.text.clear()
                    lblTotal.text = "Total a Pagar: $0"
                    selectedAlquilerId = null
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al guardar alquiler: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // MODIFICAR ALQUILER
        btnModificar.setOnClickListener {
            val docId = selectedAlquilerId
            if (docId.isNullOrEmpty()) {
                Toast.makeText(this, "Seleccione un alquiler de la lista para modificar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val posMaquina = spnMaquinas.selectedItemPosition
            val posCliente = spnClientes.selectedItemPosition
            val dias = txtDias.text.toString().toIntOrNull() ?: 0

            if (posMaquina < 0 || posMaquina >= listaMaquinas.size || posCliente < 0 || posCliente >= listaClientes.size || dias <= 0) {
                Toast.makeText(this, "Asegúrese de seleccionar maquinaria, cliente y días válidos (> 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maquinaElegida = listaMaquinas[posMaquina]
            val clienteElegido = listaClientes[posCliente]
            val tarifaDiaria = maquinaElegida.tarifaPorHora
            val total = dias * tarifaDiaria

            btnModificar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val alquilerActualizado = Alquiler(
                id = docId,
                nombreMaquina = maquinaElegida.nombre,
                rutCliente = clienteElegido.rut,
                diasArriendo = dias,
                tarifaDiaria = tarifaDiaria,
                totalPagar = total,
                fecha = fechaActual
            )

            db.collection("alquileres").document(docId)
                .set(alquilerActualizado)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnModificar.isEnabled = true
                    Toast.makeText(this, "Alquiler actualizado con éxito", Toast.LENGTH_SHORT).show()

                    registrarEventoAnalytics("registro_actualizado")

                    txtDias.text.clear()
                    lblTotal.text = "Total a Pagar: $0"
                    selectedAlquilerId = null
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnModificar.isEnabled = true
                    Toast.makeText(this, "Error al actualizar alquiler: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // ELIMINAR ALQUILER CON CONFIRMACIÓN
        btnEliminar.setOnClickListener {
            val docId = selectedAlquilerId
            if (docId.isNullOrEmpty()) {
                Toast.makeText(this, "Seleccione un alquiler de la lista para eliminar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro de que desea eliminar este registro de alquiler?")
                .setPositiveButton("Eliminar") { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    db.collection("alquileres").document(docId).delete()
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Registro eliminado correctamente", Toast.LENGTH_SHORT).show()

                            registrarEventoAnalytics("registro_eliminado")

                            txtDias.text.clear()
                            lblTotal.text = "Total a Pagar: $0"
                            selectedAlquilerId = null
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Error al eliminar alquiler: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun calcularTotal(spnMaquinas: Spinner, txtDias: EditText, lblTotal: TextView) {
        val posMaquina = spnMaquinas.selectedItemPosition
        val dias = txtDias.text.toString().toIntOrNull() ?: 0

        if (posMaquina >= 0 && posMaquina < listaMaquinas.size && dias > 0) {
            val maquina = listaMaquinas[posMaquina]
            val total = dias * maquina.tarifaPorHora
            val totalFmt = ChileFormatUtils.formatPesos(total)
            lblTotal.text = "Total a Pagar: $$totalFmt"
        } else {
            Toast.makeText(this, "Selecciona una máquina e ingresa los días válidos (> 0)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarAlquileresEnLista(progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        db.collection("alquileres")
            .addSnapshotListener { querySnapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    Toast.makeText(this, "Error de conexión al cargar alquileres: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    registrarEventoAnalytics("listado_consultado")

                    if (querySnapshot.isEmpty) {
                        listAdapter.updateData(emptyList())
                        Toast.makeText(this, "No existen alquileres registrados", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val alquiler = doc.toObject(Alquiler::class.java).copy(id = doc.id)
                        val totalFmt = ChileFormatUtils.formatPesos(alquiler.totalPagar)
                        items.add(
                            ItemData(
                                titulo = "${alquiler.nombreMaquina} -> ${alquiler.rutCliente}",
                                subtitulo = "Días: ${alquiler.diasArriendo} | Total: $$totalFmt",
                                detalle = "Fecha: ${alquiler.fecha}",
                                rawData = alquiler
                            )
                        )
                    }
                    listAdapter.updateData(items)
                }
            }
    }

    private fun cargarMaquinasEnSpinner(spinner: Spinner) {
        db.collection("maquinarias")
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaMaquinas.clear()
                nombresMaquinas.clear()

                for (doc in querySnapshot) {
                    val id = doc.id
                    val nombre = doc.getString("Nombre") ?: doc.getString("nombre") ?: ""
                    val categoria = doc.getString("Categoria") ?: doc.getString("categoria") ?: ""
                    val tarifa = doc.getDouble("Tarifa") ?: doc.getDouble("tarifa") ?: 0.0

                    val maquina = Maquina(id, nombre, categoria, tarifa)
                    listaMaquinas.add(maquina)
                    nombresMaquinas.add("$nombre ($categoria)")
                }

                if (nombresMaquinas.isEmpty()) {
                    nombresMaquinas.add("Sin máquinas disponibles")
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresMaquinas)
                spinner.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar maquinarias: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarClientesEnSpinner(spinner: Spinner) {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaClientes.clear()
                nombresClientes.clear()

                for (doc in querySnapshot) {
                    val cliente = doc.toObject(Cliente::class.java).copy(id = doc.id)
                    listaClientes.add(cliente)
                    nombresClientes.add("${cliente.rut} - ${cliente.nombreEmpresa}")
                }

                if (nombresClientes.isEmpty()) {
                    nombresClientes.add("Sin clientes registrados")
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresClientes)
                spinner.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar clientes: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "alquiler")
        }
        firebaseAnalytics.logEvent(nombreEvento, bundle)
    }
}
