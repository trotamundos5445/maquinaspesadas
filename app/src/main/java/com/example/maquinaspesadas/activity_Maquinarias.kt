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
import com.google.firebase.firestore.FirebaseFirestore

class activity_Maquinarias : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listAdapter = SimpleListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_maquinarias)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val txtNombre = findViewById<EditText>(R.id.txtNombreMaquina)
        val txtCategoria = findViewById<EditText>(R.id.txtCategoria)
        val txtTarifa = findViewById<EditText>(R.id.txtTarifa)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)
        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val rvMaquinas = findViewById<RecyclerView>(R.id.recyclerViewMaquinarias)

        rvMaquinas.layoutManager = LinearLayoutManager(this)
        rvMaquinas.adapter = listAdapter

        ChileFormatUtils.setupPriceAutoFormat(txtTarifa)

        cargarMaquinariasEnLista()

        btnGuardar.setOnClickListener {
            val nombre = txtNombre.text.toString().trim()
            val categoria = txtCategoria.text.toString().trim()
            val tarifaStr = txtTarifa.text.toString().replace("[^0-9]".toRegex(), "")

            if (nombre.isNotEmpty() && categoria.isNotEmpty() && tarifaStr.isNotEmpty()) {
                val tarifa = tarifaStr.toDoubleOrNull() ?: 0.0

                val id = db.collection("maquinarias").document().id

                val maquinaData = hashMapOf(
                    "id" to id,
                    "Nombre" to nombre,
                    "Categoria" to categoria,
                    "Tarifa" to tarifa,
                    "Estado" to "Disponible"
                )

                db.collection("maquinarias").document(id)
                    .set(maquinaData)
                    .addOnSuccessListener {
                        val bundle = Bundle().apply {
                            putString("nombre_maquina", nombre)
                            putString("categoria", categoria)
                        }
                        FirebaseAnalytics.getInstance(this).logEvent("guardar_maquinaria", bundle)

                        Toast.makeText(this, "¡Maquinaria guardada con éxito!", Toast.LENGTH_SHORT).show()
                        txtNombre.text.clear()
                        txtCategoria.text.clear()
                        txtTarifa.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Por favor complete todos los campos...", Toast.LENGTH_SHORT).show()
            }
        }

        btnEliminar.setOnClickListener {
            val nombreAEliminar = txtNombre.text.toString().trim()

            if (nombreAEliminar.isNotEmpty()) {
                db.collection("maquinarias")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        var eliminados = 0
                        for (document in querySnapshot) {
                            val nomDoc = document.getString("Nombre") ?: document.getString("nombre") ?: ""
                            if (nomDoc.equals(nombreAEliminar, ignoreCase = true)) {
                                db.collection("maquinarias").document(document.id).delete()
                                eliminados++
                            }
                        }
                        if (eliminados > 0) {
                            Toast.makeText(this, "Maquinaria eliminada con éxito", Toast.LENGTH_SHORT).show()
                            txtNombre.text.clear()
                        } else {
                            Toast.makeText(this, "No se encontró la maquinaria '$nombreAEliminar'", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ingresa el nombre de la máquina a eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarMaquinariasEnLista() {
        db.collection("maquinarias")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar maquinarias: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val items = mutableListOf<ItemData>()
                    for (doc in querySnapshot) {
                        val nombre = doc.getString("Nombre") ?: doc.getString("nombre") ?: "Sin nombre"
                        val categoria = doc.getString("Categoria") ?: doc.getString("categoria") ?: "Sin categoría"
                        val tarifa = doc.getDouble("Tarifa") ?: doc.getDouble("tarifa") ?: 0.0

                        val tarifaFormateada = ChileFormatUtils.formatPesos(tarifa)
                        items.add(
                            ItemData(
                                titulo = nombre,
                                subtitulo = "Categoría: $categoria",
                                detalle = "Tarifa: $$tarifaFormateada / hora"
                            )
                        )
                    }
                    listAdapter.updateData(items)
                }
            }
    }
}
