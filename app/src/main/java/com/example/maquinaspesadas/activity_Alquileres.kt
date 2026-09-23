package com.example.maquinaspesadas

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private val listaMaquinas = mutableListOf<Maquina>()
    private val nombresMaquinas = mutableListOf<String>()

    private val listaClientes = mutableListOf<Cliente>()
    private val nombresClientes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alquileres)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val spnMaquinas = findViewById<Spinner>(R.id.spnMaquinas)
        val spnClientes = findViewById<Spinner>(R.id.spnClientes)
        val txtDias = findViewById<EditText>(R.id.txtDiasAlquiler)
        val lblTotal = findViewById<TextView>(R.id.txtTotalCalculado)

        val btnCalcular = findViewById<Button>(R.id.btnCalcularTotal)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAlquiler)
        val btnEliminar = findViewById<Button>(R.id.btnEliminarAlquiler)
        val btnVolver = findViewById<Button>(R.id.btnVolverAlquiler)
        val rvAlquileres = findViewById<RecyclerView>(R.id.recyclerViewAlquileres)

        rvAlquileres.layoutManager = LinearLayoutManager(this)
        rvAlquileres.adapter = listAdapter

        cargarMaquinasEnSpinner(spnMaquinas)
        cargarClientesEnSpinner(spnClientes)
        cargarAlquileresEnLista()

        btnCalcular.setOnClickListener {
            val posMaquina = spnMaquinas.selectedItemPosition
            val dias = txtDias.text.toString().toIntOrNull() ?: 0

            if (posMaquina >= 0 && posMaquina < listaMaquinas.size && dias > 0) {
                val maquina = listaMaquinas[posMaquina]
                val total = dias * maquina.tarifaPorHora
                val totalFmt = ChileFormatUtils.formatPesos(total)
                lblTotal.text = "Total a Pagar: $$totalFmt"
            } else {
                Toast.makeText(this, "Selecciona una máquina e ingresa los días", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardar.setOnClickListener {
            val posMaquina = spnMaquinas.selectedItemPosition
            val posCliente = spnClientes.selectedItemPosition
            val dias = txtDias.text.toString().toIntOrNull() ?: 0

            if (posMaquina >= 0 && posMaquina < listaMaquinas.size && posCliente >= 0 && posCliente < listaClientes.size && dias > 0) {
                val maquinaElegida = listaMaquinas[posMaquina]
                val clienteElegido = listaClientes[posCliente]
                val total = dias * maquinaElegida.tarifaPorHora

                val id = db.collection("alquileres").document().id
                val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                val alquiler = Alquiler(
                    id = id,
                    nombreMaquina = maquinaElegida.nombre,
                    rutCliente = clienteElegido.rut,
                    diasArriendo = dias,
                    tarifaDiaria = maquinaElegida.tarifaPorHora,
                    totalPagar = total,
                    fecha = fechaActual
                )

                db.collection("alquileres").document(id)
                    .set(alquiler)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Alquiler registrado con éxito!", Toast.LENGTH_SHORT).show()
                        txtDias.text.clear()
                        lblTotal.text = "Total a Pagar: $0"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Asegúrate de seleccionar máquina, cliente y días válidos", Toast.LENGTH_SHORT).show()
            }
        }

        btnEliminar.setOnClickListener {
            val posCliente = spnClientes.selectedItemPosition

            if (posCliente >= 0 && posCliente < listaClientes.size) {
                val rutAEliminar = listaClientes[posCliente].rut

                db.collection("alquileres")
                    .whereEqualTo("rutCliente", rutAEliminar)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (doc in querySnapshot) {
                                db.collection("alquileres").document(doc.id).delete()
                            }
                            Toast.makeText(this, "Alquiler eliminado con éxito", Toast.LENGTH_SHORT).show()
                            txtDias.text.clear()
                            lblTotal.text = "Total a Pagar: $0"
                        } else {
                            Toast.makeText(this, "No hay arriendos para este cliente", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Selecciona un cliente para eliminar su arriendo", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarAlquileresEnLista() {
        db.collection("alquileres")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar alquileres: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val alquiler = doc.toObject(Alquiler::class.java)
                        val totalFmt = ChileFormatUtils.formatPesos(alquiler.totalPagar)
                        items.add(
                            ItemData(
                                titulo = "${alquiler.nombreMaquina} -> ${alquiler.rutCliente}",
                                subtitulo = "Días: ${alquiler.diasArriendo} | Total: $$totalFmt",
                                detalle = "Fecha: ${alquiler.fecha}"
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
                Toast.makeText(this, "Error al cargar maquinarias: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarClientesEnSpinner(spinner: Spinner) {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaClientes.clear()
                nombresClientes.clear()

                for (doc in querySnapshot) {
                    val cliente = doc.toObject(Cliente::class.java)
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
                Toast.makeText(this, "Error al cargar clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
