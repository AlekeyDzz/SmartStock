package com.example.hola_mundo

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StockBottomSheetFragment : BottomSheetDialogFragment() {

    private var codigoQrProducto: String? = null
    private var stockActual = 0
    private var nuevoStock = 0
    private var fotoString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        codigoQrProducto = arguments?.getString("CODIGO_QR")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_stock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (codigoQrProducto == null) return

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreProducto)
        val tvStock = view.findViewById<TextView>(R.id.tvStockActual)
        val tvNuevoStock = view.findViewById<TextView>(R.id.tvNuevoStock)
        val ivProducto = view.findViewById<ImageView>(R.id.ivProducto)

        // 1. Cargar datos desde SQLite
        val admin = AdminSQLiteOpenHelper(requireContext(), "inventario", null, 2)
        val db = admin.readableDatabase
        val cursor = db.rawQuery(
            "SELECT nombre, stock, foto FROM productos WHERE codigo_qr = ?",
            arrayOf(codigoQrProducto)
        )

        if (cursor.moveToFirst()) {
            tvNombre.text = cursor.getString(0)
            stockActual = cursor.getInt(1)
            nuevoStock = stockActual
            fotoString = cursor.getString(2) ?: ""

            tvStock.text = "Stock actual: $stockActual unidades"
            tvNuevoStock.text = nuevoStock.toString()

            // 2. Cargar la imagen y activar el visor al tocarla
            if (fotoString.isNotEmpty()) {
                try {
                    val primeraUri = fotoString.split(",").firstOrNull()
                    if (!primeraUri.isNullOrEmpty()) {
                        val uri = Uri.parse(primeraUri)
                        ivProducto.setImageURI(uri)

                        // Abrir imagen en pantalla completa al tocarla
                        ivProducto.setOnClickListener {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(uri, "image/*")
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "No se puede abrir la imagen", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Evitar crasheo si la ruta de la imagen falla
                }
            }
        }
        cursor.close()
        db.close()

        // 3. Lógica matemática de los botones de ajuste de stock
        view.findViewById<Button>(R.id.btnMas1).setOnClickListener {
            nuevoStock += 1
            tvNuevoStock.text = nuevoStock.toString()
        }

        view.findViewById<Button>(R.id.btnMas5).setOnClickListener {
            nuevoStock += 5
            tvNuevoStock.text = nuevoStock.toString()
        }

        view.findViewById<Button>(R.id.btnMenos1).setOnClickListener {
            if (nuevoStock > 0) nuevoStock -= 1
            tvNuevoStock.text = nuevoStock.toString()
        }

        view.findViewById<Button>(R.id.btnMenos5).setOnClickListener {
            if (nuevoStock >= 5) nuevoStock -= 5 else nuevoStock = 0
            tvNuevoStock.text = nuevoStock.toString()
        }

        // 4. Cancelar y Guardar Ajuste (Con recarga de lista)
        view.findViewById<Button>(R.id.btnCancelarAjuste).setOnClickListener {
            dismiss()
        }

        view.findViewById<Button>(R.id.btnGuardarAjuste).setOnClickListener {
            val dbUpdate = admin.writableDatabase
            val cv = ContentValues()
            cv.put("stock", nuevoStock)
            dbUpdate.update("productos", cv, "codigo_qr = ?", arrayOf(codigoQrProducto))
            dbUpdate.close()

            Toast.makeText(requireContext(), "Stock actualizado", Toast.LENGTH_SHORT).show()
            (activity as? principal)?.recreate() // RECARGA LA LISTA PRINCIPAL
            dismiss()
        }

        // 5. Botón Ver Detalle (Abre alerta)
        view.findViewById<TextView>(R.id.tvVerDetalle).setOnClickListener {
            mostrarDialogoDetalles()
        }
    }

    private fun mostrarDialogoDetalles() {
        if (codigoQrProducto == null) return

        val admin = AdminSQLiteOpenHelper(requireContext(), "inventario", null, 2)
        val db = admin.readableDatabase

        val cursor = db.rawQuery(
            "SELECT nombre, descripcion, categoria, tipo, precio, stock, foto FROM productos WHERE codigo_qr = ?",
            arrayOf(codigoQrProducto)
        )

        if (cursor.moveToFirst()) {
            val nombre = cursor.getString(0)
            val descripcion = cursor.getString(1)
            val categoria = cursor.getString(2)
            val tipo = cursor.getString(3)
            val precio = cursor.getDouble(4)
            val stock = cursor.getInt(5)
            val foto = cursor.getString(6) ?: ""

            val mensaje = """
                Código: $codigoQrProducto
                Categoría: $categoria
                Tipo: $tipo Precio de venta: $$precio
                
                Descripción:
                $descripcion
            """.trimIndent()

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(nombre)
            builder.setMessage(mensaje)

            builder.setPositiveButton("CERRAR") { dialog, _ ->
                dialog.dismiss()
            }

            // Enviar datos al MODO EDICIÓN, incluyendo las fotos
            builder.setNeutralButton("EDITAR INFORMACIÓN") { _, _ ->
                val intent = Intent(requireContext(), AgregarProductoActivity::class.java).apply {
                    putExtra("MODO_EDICION", true)
                    putExtra("CODIGO_QR", codigoQrProducto)
                    putExtra("NOMBRE", nombre)
                    putExtra("DESCRIPCION", descripcion)
                    putExtra("CATEGORIA", categoria)
                    putExtra("TIPO", tipo)
                    putExtra("PRECIO", precio.toString())
                    putExtra("STOCK", stock.toString())
                    putExtra("FOTO", foto)
                }
                startActivity(intent)
                dismiss()
            }

            // ELIMINAR y recargar la lista
            builder.setNegativeButton("ELIMINAR DEL INVENTARIO") { _, _ ->
                val dbDelete = admin.writableDatabase
                dbDelete.delete("productos", "codigo_qr = ?", arrayOf(codigoQrProducto))
                dbDelete.close()

                Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
                (activity as? principal)?.recreate() // RECARGA LA LISTA PRINCIPAL
                dismiss()
            }

            builder.show()
        }
        cursor.close()
        db.close()
    }

    companion object {
        const val TAG = "StockBottomSheet"

        fun newInstance(codigoQr: String): StockBottomSheetFragment {
            val fragment = StockBottomSheetFragment()
            val args = Bundle()
            args.putString("CODIGO_QR", codigoQr)
            fragment.arguments = args
            return fragment
        }
    }
}