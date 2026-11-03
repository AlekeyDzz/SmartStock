package com.example.hola_mundo

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ProductoAdapter(
    private val listaProductos: List<Producto>,
    private val umbralStock: Int,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreProducto)
        val tvDetalle = itemView.findViewById<TextView>(R.id.tvDetalleProducto)
        val tvPrecio = itemView.findViewById<TextView>(R.id.tvPrecioProducto)
        val tvCantidad = itemView.findViewById<TextView>(R.id.tvCantidadStock)
        val tvEstado = itemView.findViewById<TextView>(R.id.tvEstadoStock)
        val cardEstado = itemView.findViewById<MaterialCardView>(R.id.cardEstadoStock)
        val ivImagen = itemView.findViewById<ImageView>(R.id.ivProductoLista)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        holder.itemView.setOnClickListener { onItemClick(producto) }

        holder.tvNombre.text = producto.nombre
        holder.tvDetalle.text = "${producto.codigoQr} • ${producto.categoria}"
        holder.tvPrecio.text = "$ ${String.format("%.2f", producto.precio)}"
        holder.tvCantidad.text = "${producto.stock} uds."

        if (producto.foto.isNotBlank()) {
            holder.ivImagen.imageTintList = null
            holder.ivImagen.scaleType = ImageView.ScaleType.CENTER_CROP
            val primeraFoto = producto.foto.split(",").firstOrNull { it.isNotBlank() } ?: ""
            if (primeraFoto.isNotBlank()) {
                holder.ivImagen.setImageURI(Uri.parse(primeraFoto))
            }
        } else {
            holder.ivImagen.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivImagen.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
            holder.ivImagen.scaleType = ImageView.ScaleType.CENTER
        }

        when {
            producto.stock <= 0 -> {
                holder.tvEstado.text = "Agotado"
                holder.tvEstado.setTextColor(Color.parseColor("#D81B60"))
                holder.cardEstado.setCardBackgroundColor(Color.parseColor("#FCE4EC"))
            }
            producto.stock < umbralStock -> {
                holder.tvEstado.text = "Stock bajo"
                holder.tvEstado.setTextColor(Color.parseColor("#F57F17"))
                holder.cardEstado.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
            }
            else -> {
                holder.tvEstado.text = "Disponible"
                holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
                holder.cardEstado.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            }
        }
    }

    override fun getItemCount() = listaProductos.size
}