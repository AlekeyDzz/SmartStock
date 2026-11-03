package com.example.hola_mundo

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AgregarProductoActivity : AppCompatActivity() {

    private lateinit var autoCompleteCategoria: AutoCompleteTextView
    private lateinit var autoCompleteTipo: AutoCompleteTextView
    private lateinit var etCategoriaNueva: TextInputEditText
    private lateinit var etTipoNuevo: TextInputEditText

    private lateinit var layoutCategoriaExistente: LinearLayout
    private lateinit var tilCategoriaNueva: TextInputLayout
    private lateinit var layoutTipoExistente: LinearLayout
    private lateinit var tilTipoNuevo: TextInputLayout

    private lateinit var btnCatExistente: Button
    private lateinit var btnCatNueva: Button
    private lateinit var btnTipoExistente: Button
    private lateinit var btnTipoNuevo: Button

    private lateinit var btnEliminarCategoria: ImageButton
    private lateinit var btnEliminarTipo: ImageButton

    private lateinit var llContainerFotos: LinearLayout
    private lateinit var cardTomarFoto: MaterialCardView

    private val listaFotos = mutableListOf<Uri>()
    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST = 2

    private val PERMISO_CAMARA_ID = 101
    private val PERMISO_GALERIA_ID = 102

    private var modoCategoriaNueva = false
    private var modoTipoNuevo = false
    private var esEdicion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_agregar_producto)

        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etDescripcion = findViewById<TextInputEditText>(R.id.etDescripcion)
        val etCodigoQr = findViewById<TextInputEditText>(R.id.etCodigoQr)
        val etPrecio = findViewById<TextInputEditText>(R.id.etPrecio)
        val etStock = findViewById<TextInputEditText>(R.id.etStock)

        autoCompleteCategoria = findViewById(R.id.autoCompleteCategoria)
        autoCompleteTipo = findViewById(R.id.autoCompleteTipo)
        etCategoriaNueva = findViewById(R.id.etCategoriaNueva)
        etTipoNuevo = findViewById(R.id.etTipoNuevo)

        layoutCategoriaExistente = findViewById(R.id.layoutCategoriaExistente)
        tilCategoriaNueva = findViewById(R.id.tilCategoriaNueva)
        layoutTipoExistente = findViewById(R.id.layoutTipoExistente)
        tilTipoNuevo = findViewById(R.id.tilTipoNuevo)

        btnCatExistente = findViewById(R.id.btnCatExistente)
        btnCatNueva = findViewById(R.id.btnCatNueva)
        btnTipoExistente = findViewById(R.id.btnTipoExistente)
        btnTipoNuevo = findViewById(R.id.btnTipoNuevo)

        btnEliminarCategoria = findViewById(R.id.btnEliminarCategoria)
        btnEliminarTipo = findViewById(R.id.btnEliminarTipo)

        llContainerFotos = findViewById(R.id.llContainerFotos)
        cardTomarFoto = findViewById(R.id.cardTomarFoto)

        val btnEscanearAgregar = findViewById<Button>(R.id.btnEscanearAgregar)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)

        configurarDropdownsDinamicos()

        esEdicion = intent.getBooleanExtra("MODO_EDICION", false)
        if (esEdicion) {
            btnGuardar.text = "Actualizar producto"
            etCodigoQr.setText(intent.getStringExtra("CODIGO_QR"))
            etCodigoQr.isEnabled = false
            btnEscanearAgregar.isEnabled = false

            etNombre.setText(intent.getStringExtra("NOMBRE"))
            etDescripcion.setText(intent.getStringExtra("DESCRIPCION"))
            autoCompleteCategoria.setText(intent.getStringExtra("CATEGORIA"), false)
            autoCompleteTipo.setText(intent.getStringExtra("TIPO"), false)
            etPrecio.setText(intent.getStringExtra("PRECIO"))
            etStock.setText(intent.getStringExtra("STOCK"))

            val fotoStr = intent.getStringExtra("FOTO") ?: ""
            if (fotoStr.isNotEmpty()) {
                val uris = fotoStr.split(",")
                for (uriString in uris) {
                    if (uriString.isNotBlank()) {
                        try {
                            agregarFotoMiniatura(Uri.parse(uriString))
                        } catch (e: Exception) { }
                    }
                }
            }
        }

        btnCatExistente.setOnClickListener {
            modoCategoriaNueva = false
            btnCatExistente.setBackgroundColor(Color.WHITE)
            btnCatExistente.setTextColor(Color.BLACK)
            btnCatNueva.setBackgroundColor(Color.TRANSPARENT)
            btnCatNueva.setTextColor(Color.parseColor("#666666"))
            layoutCategoriaExistente.visibility = View.VISIBLE
            tilCategoriaNueva.visibility = View.GONE
        }

        btnCatNueva.setOnClickListener {
            modoCategoriaNueva = true
            btnCatNueva.setBackgroundColor(Color.WHITE)
            btnCatNueva.setTextColor(Color.BLACK)
            btnCatExistente.setBackgroundColor(Color.TRANSPARENT)
            btnCatExistente.setTextColor(Color.parseColor("#666666"))
            layoutCategoriaExistente.visibility = View.GONE
            tilCategoriaNueva.visibility = View.VISIBLE
        }

        btnTipoExistente.setOnClickListener {
            modoTipoNuevo = false
            btnTipoExistente.setBackgroundColor(Color.WHITE)
            btnTipoExistente.setTextColor(Color.BLACK)
            btnTipoNuevo.setBackgroundColor(Color.TRANSPARENT)
            btnTipoNuevo.setTextColor(Color.parseColor("#666666"))
            layoutTipoExistente.visibility = View.VISIBLE
            tilTipoNuevo.visibility = View.GONE
        }

        btnTipoNuevo.setOnClickListener {
            modoTipoNuevo = true
            btnTipoNuevo.setBackgroundColor(Color.WHITE)
            btnTipoNuevo.setTextColor(Color.BLACK)
            btnTipoExistente.setBackgroundColor(Color.TRANSPARENT)
            btnTipoExistente.setTextColor(Color.parseColor("#666666"))
            layoutTipoExistente.visibility = View.GONE
            tilTipoNuevo.visibility = View.VISIBLE
        }

        btnEliminarCategoria.setOnClickListener {
            val categoriaSeleccionada = autoCompleteCategoria.text.toString().trim()
            if (categoriaSeleccionada.isEmpty()) {
                Toast.makeText(this, "Selecciona una categoría primero", Toast.LENGTH_SHORT).show()
            } else {
                verificarYEliminar("categoria", categoriaSeleccionada)
            }
        }

        btnEliminarTipo.setOnClickListener {
            val tipoSeleccionado = autoCompleteTipo.text.toString().trim()
            if (tipoSeleccionado.isEmpty()) {
                Toast.makeText(this, "Selecciona un tipo primero", Toast.LENGTH_SHORT).show()
            } else {
                verificarYEliminar("tipo", tipoSeleccionado)
            }
        }

        cardTomarFoto.setOnClickListener {
            if (listaFotos.size >= 5) {
                Toast.makeText(this, "Máximo 5 fotos permitidas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val opciones = arrayOf("Tomar foto", "Elegir de la galería")
            AlertDialog.Builder(this)
                .setTitle("Agregar foto de producto")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> verificarPermisoCamara()
                        1 -> verificarPermisoGaleria()
                    }
                }
                .show()
        }

        val codigoEscaneado = intent.getStringExtra("CODIGO_ESCANEADO")
        if (!codigoEscaneado.isNullOrEmpty() && !esEdicion) {
            etCodigoQr.setText(codigoEscaneado)
        }

        btnEscanearAgregar.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("Enfoca el código de barras o QR")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        btnGuardar.setOnClickListener {
            val codigo = etCodigoQr.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            val categoria = if (modoCategoriaNueva) {
                etCategoriaNueva.text.toString().trim()
            } else {
                autoCompleteCategoria.text.toString().trim()
            }

            val tipo = if (modoTipoNuevo) {
                etTipoNuevo.text.toString().trim()
            } else {
                autoCompleteTipo.text.toString().trim()
            }

            val precioStr = etPrecio.text.toString().trim()
            val stockStr = etStock.text.toString().trim()

            if (codigo.isEmpty() || nombre.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty() || categoria.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos obligatorios y la categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val precio = precioStr.toDoubleOrNull() ?: 0.0
            val stock = stockStr.toIntOrNull() ?: 0
            val fotosStr = listaFotos.joinToString(",") { it.toString() }

            val admin = AdminSQLiteOpenHelper(this, "inventario", null, 2)
            val bd = admin.writableDatabase

            try {
                if (esEdicion) {
                    val sentencia = bd.compileStatement("UPDATE productos SET nombre=?, descripcion=?, categoria=?, tipo=?, precio=?, stock=?, foto=? WHERE codigo_qr=?")
                    sentencia.bindString(1, nombre)
                    sentencia.bindString(2, descripcion)
                    sentencia.bindString(3, categoria)
                    sentencia.bindString(4, tipo)
                    sentencia.bindDouble(5, precio)
                    sentencia.bindLong(6, stock.toLong())
                    sentencia.bindString(7, fotosStr)
                    sentencia.bindString(8, codigo)
                    sentencia.executeUpdateDelete()

                    Toast.makeText(this, "¡Producto actualizado exitosamente!", Toast.LENGTH_SHORT).show()
                } else {
                    val sentencia = bd.compileStatement("INSERT INTO productos (codigo_qr, nombre, descripcion, categoria, tipo, precio, stock, foto) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                    sentencia.bindString(1, codigo)
                    sentencia.bindString(2, nombre)
                    sentencia.bindString(3, descripcion)
                    sentencia.bindString(4, categoria)
                    sentencia.bindString(5, tipo)
                    sentencia.bindDouble(6, precio)
                    sentencia.bindLong(7, stock.toLong())
                    sentencia.bindString(8, fotosStr)
                    sentencia.executeInsert()

                    Toast.makeText(this, "¡Producto guardado exitosamente!", Toast.LENGTH_SHORT).show()
                }

                bd.close()
                val intentRefresh = Intent(this, principal::class.java)
                intentRefresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentRefresh)
                finish()

            } catch (e: Exception) {
                bd.close()
                Toast.makeText(this, "Error al guardar el producto", Toast.LENGTH_LONG).show()
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun verificarPermisoCamara() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISO_CAMARA_ID)
            } else {
                abrirCamara()
            }
        } catch (e: Exception) {
            abrirCamara()
        }
    }

    private fun verificarPermisoGaleria() {
        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        try {
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permiso), PERMISO_GALERIA_ID)
            } else {
                abrirGaleria()
            }
        } catch (e: Exception) {
            abrirGaleria()
        }
    }

    private fun abrirCamara() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la cámara. Verifica los permisos de tu dispositivo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun abrirGaleria() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show()
        }
    }

    // CORRECCIÓN: Si los permisos fallan silenciosamente, intenta abrir de todas formas
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISO_CAMARA_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara()
                } else {
                    abrirCamara()
                }
            }
            PERMISO_GALERIA_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirGaleria()
                } else {
                    abrirGaleria()
                }
            }
        }
    }

    private fun configurarDropdownsDinamicos() {
        val admin = AdminSQLiteOpenHelper(this, "inventario", null, 2)
        val db = admin.readableDatabase

        val listaCategorias = mutableSetOf<String>()
        val cursorCat = db.rawQuery("SELECT DISTINCT categoria FROM productos WHERE categoria IS NOT NULL AND categoria != ''", null)
        while (cursorCat.moveToNext()) {
            listaCategorias.add(cursorCat.getString(0))
        }
        cursorCat.close()

        val listaTipos = mutableSetOf<String>()
        val cursorTipo = db.rawQuery("SELECT DISTINCT tipo FROM productos WHERE tipo IS NOT NULL AND tipo != ''", null)
        while (cursorTipo.moveToNext()) {
            listaTipos.add(cursorTipo.getString(0))
        }
        cursorTipo.close()
        db.close()

        val adapterCat = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaCategorias.toList())
        autoCompleteCategoria.setAdapter(adapterCat)

        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaTipos.toList())
        autoCompleteTipo.setAdapter(adapterTipo)
    }

    private fun verificarYEliminar(columna: String, valor: String) {
        val admin = AdminSQLiteOpenHelper(this, "inventario", null, 2)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM productos WHERE $columna = ?", arrayOf(valor))

        var cantidad = 0
        if (cursor.moveToFirst()) {
            cantidad = cursor.getInt(0)
        }
        cursor.close()
        db.close()

        if (cantidad > 0) {
            AlertDialog.Builder(this)
                .setTitle("No se puede eliminar")
                .setMessage("No puedes eliminar esta opción porque hay $cantidad producto(s) usando esta $columna en tu inventario.")
                .setPositiveButton("Entendido", null)
                .show()
        } else {
            if (columna == "categoria") {
                autoCompleteCategoria.setText("")
            } else {
                autoCompleteTipo.setText("")
            }
            configurarDropdownsDinamicos()
            Toast.makeText(this, "Opción eliminada y limpiada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                findViewById<TextInputEditText>(R.id.etCodigoQr).setText(result.contents)
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri -> agregarFotoMiniatura(uri) }
                }
                CAMERA_REQUEST -> {
                    val imageBitmap = data?.extras?.get("data") as? android.graphics.Bitmap
                    if (imageBitmap != null) {
                        try {
                            val file = File(cacheDir, "foto_${UUID.randomUUID()}.jpg")
                            val fos = FileOutputStream(file)
                            imageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.flush()
                            fos.close()

                            agregarFotoMiniatura(Uri.fromFile(file))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No se capturó la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun agregarFotoMiniatura(uri: Uri) {
        if (listaFotos.size >= 5) return
        listaFotos.add(uri)

        val cardThumbnail = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250).apply { setMargins(0, 0, 16, 0) }
            radius = 16f
            cardElevation = 0f
        }

        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setImageURI(uri)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        cardThumbnail.addView(imageView)
        llContainerFotos.addView(cardThumbnail, llContainerFotos.childCount)
    }
}