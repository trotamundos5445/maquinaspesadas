package com.example.maquinaspesadas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ItemData(
    val titulo: String,
    val subtitulo: String,
    val detalle: String = ""
)

class SimpleListAdapter(private val items: MutableList<ItemData> = mutableListOf()) :
    RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitulo: TextView = view.findViewById(R.id.txtTituloItem)
        val txtSubtitulo: TextView = view.findViewById(R.id.txtSubtituloItem)
        val txtDetalle: TextView = view.findViewById(R.id.txtDetalleItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lista, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitulo.text = item.titulo
        holder.txtSubtitulo.text = item.subtitulo
        if (item.detalle.isNotEmpty()) {
            holder.txtDetalle.visibility = View.VISIBLE
            holder.txtDetalle.text = item.detalle
        } else {
            holder.txtDetalle.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ItemData>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
