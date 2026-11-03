package com.example.hola_mundo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator

class principal : AppCompatActivity() {

    private lateinit var rvProductos: RecyclerView
    private lateinit var llEstadoVacio: LinearLayout
    private lateinit var tvTotalProductos: TextView
    private lateinit var tvValorTotal: TextView
    private lateinit var tvTotalCard: TextView
    private lateinit var tvAlertasCard: TextView
    private lateinit var layoutTutorial: RelativeLayout
    private lateinit var llCategoriasFiltro: LinearLayout
    private lateinit var etBuscador: TextInputEditText

    private var listaCompletaProductos = mutableListOf<Producto>()
    private var categoriaSeleccionada = "Todos"
    private var textoBusqueda = ""

    private var umbralStock = 5
    private var ordenStockAscendente = true
    private var pasoTutorialActual = 1 // Variable del tutorial por pasos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_principal)

        rvProductos = findViewById(R.id.rvProductos)
        llEstadoVacio = findViewById(R.id.llEstadoVacio)
        tvTotalProductos = findViewById(R.id.tvTotalProductos)

        tvValorTotal = findViewById(R.id.tvValorTotal)
        tvTotalCard = findViewById(R.id.tvTotalCard)
        tvAlertasCard = findViewById(R.id.tvAlertasCard)
        layoutTutorial = findViewById(R.id.layoutTutorial)
        llCategoriasFiltro = findViewById(R.id.llCategoriasFiltro)
        etBuscador = findViewById(R.id.etBuscador)

        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        val btnEscanear = findViewById<View>(R.id.btnEscanear)
        val cardResumen = findViewById<MaterialCardView>(R.id.cardResumen)
        val tvOrdenarStock = findViewById<TextView>(R.id.tvOrdenarStock)

        val prefs = getSharedPreferences("SmartStockPrefs", Context.MODE_PRIVATE)
        umbralStock = prefs.getInt("UmbralStock", 5)

        // BORRAR/COMENTAR ESTA LÍNEA CUANDO YA NO QUIERAS QUE SALGA EL TUTORIAL:
        prefs.edit().clear().apply()

        verificarTutorial()
        cargarProductosYCategorias()

        cardResumen.setOnClickListener {
            mostrarDialogoAjusteUmbral()
        }

        tvOrdenarStock.setOnClickListener {
            ordenStockAscendente = !ordenStockAscendente
            aplicarFiltrosYOrden()
            val modo = if (ordenStockAscendente) "Menor a mayor stock" else "Mayor a menor stock"
            Toast.makeText(this, "Ordenado por: $modo", Toast.LENGTH_SHORT).show()
        }

        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textoBusqueda = s.toString().trim()
                aplicarFiltrosYOrden()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnAgregar.setOnClickListener {
            val intent = Intent(this, AgregarProductoActivity::class.java)
            startActivity(intent)
        }

        btnEscanear.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("Enfoca el código de barras o QR")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarProductosYCategorias()
    }

    private fun mostrarDialogoAjusteUmbral() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(umbralStock.toString())

        AlertDialog.Builder(this)
            .setTitle("Ajustar nivel de alerta")
            .setMessage("Menciona el número de stock mínimo para considerar un producto en alerta:")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoUmbral = input.text.toString().toIntOrNull()
                if (nuevoUmbral != null && nuevoUmbral >= 0) {
                    umbralStock = nuevoUmbral
                    getSharedPreferences("SmartStockPrefs", Context.MODE_PRIVATE).edit()
                        .putInt("UmbralStock", umbralStock).apply()
                    cargarProductosYCategorias()
                    Toast.makeText(this, "Las alertas ahora aplican desde $umbralStock unidades", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarProductosYCategorias() {
        listaCompletaProductos.clear()

        var valorInventario = 0.0
        var totalAlertas = 0
        val categoriasSet = mutableSetOf<String>()

        val admin = AdminSQLiteOpenHelper(this, "inventario", null, 2)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT codigo_qr, nombre, descripcion, categoria, tipo, precio, stock, foto FROM productos", null)

        while (cursor.moveToNext()) {
            val cat = cursor.getString(3)
            if (!cat.isNullOrBlank()) {
                categoriasSet.add(cat)
            }

            val precio = cursor.getDouble(5)
            val stock = cursor.getInt(6)

            valorInventario += (precio * stock)
            if (stock <= umbralStock) {
                totalAlertas++
            }

            val producto = Producto(
                codigoQr = cursor.getString(0),
                nombre = cursor.getString(1),
                descripcion = cursor.getString(2),
                categoria = cat ?: "",
                tipo = cursor.getString(4),
                precio = precio,
                stock = stock,
                foto = cursor.getString(7) ?: ""
            )
            listaCompletaProductos.add(producto)
        }
        cursor.close()
        db.close()

        tvValorTotal.text = "$${String.format("%.2f", valorInventario)}"
        tvTotalCard.text = "${listaCompletaProductos.size} productos"
        tvAlertasCard.text = "$totalAlertas alertas"

        construirBotonesCategorias(categoriasSet.toList())
        aplicarFiltrosYOrden()
    }

    private fun construirBotonesCategorias(categorias: List<String>) {
        llCategoriasFiltro.removeAllViews()

        val todasCategorias = listOf("Todos") + categorias

        for (cat in todasCategorias) {
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = cat
                isAllCaps = false
                textSize = 14f
                cornerRadius = 50
                setPadding(40, 12, 40, 12)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }

                if (cat == categoriaSeleccionada) {
                    backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#212121"))
                    setTextColor(android.graphics.Color.WHITE)
                } else {
                    backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
                    setTextColor(android.graphics.Color.parseColor("#000000"))
                }

                setOnClickListener {
                    categoriaSeleccionada = cat
                    construirBotonesCategorias(categorias)
                    aplicarFiltrosYOrden()
                }
            }
            llCategoriasFiltro.addView(btn)
        }
    }

    private fun aplicarFiltrosYOrden() {
        var listaFiltrada = listaCompletaProductos.asIterable()

        if (categoriaSeleccionada != "Todos") {
            listaFiltrada = listaFiltrada.filter { it.categoria.equals(categoriaSeleccionada, ignoreCase = true) }
        }

        if (textoBusqueda.isNotEmpty()) {
            listaFiltrada = listaFiltrada.filter {
                it.nombre.contains(textoBusqueda, ignoreCase = true) ||
                        it.codigoQr.contains(textoBusqueda, ignoreCase = true)
            }
        }

        val listaOrdenada = if (ordenStockAscendente) {
            listaFiltrada.sortedBy { it.stock }
        } else {
            listaFiltrada.sortedByDescending { it.stock }
        }

        if (listaOrdenada.isEmpty()) {
            rvProductos.visibility = View.GONE
            llEstadoVacio.visibility = View.VISIBLE
            tvTotalProductos.text = "0 resultados"
        } else {
            rvProductos.visibility = View.VISIBLE
            llEstadoVacio.visibility = View.GONE
            tvTotalProductos.text = "${listaOrdenada.size} resultados"

            rvProductos.layoutManager = LinearLayoutManager(this)
            rvProductos.adapter = ProductoAdapter(listaOrdenada.toMutableList(), umbralStock) { productoClickeado ->
                val bottomSheet = StockBottomSheetFragment.newInstance(productoClickeado.codigoQr)
                bottomSheet.show(supportFragmentManager, "StockBottomSheet")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                verificarCodigoEscaneado(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun verificarCodigoEscaneado(codigo: String) {
        val admin = AdminSQLiteOpenHelper(this, "inventario", null, 2)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT codigo_qr, nombre, descripcion, categoria, tipo, precio, stock, foto FROM productos WHERE codigo_qr = ?", arrayOf(codigo))

        var productoEncontrado: Producto? = null

        if (cursor.moveToFirst()) {
            productoEncontrado = Producto(
                codigoQr = cursor.getString(0),
                nombre = cursor.getString(1),
                descripcion = cursor.getString(2),
                categoria = cursor.getString(3),
                tipo = cursor.getString(4),
                precio = cursor.getDouble(5),
                stock = cursor.getInt(6),
                foto = cursor.getString(7) ?: ""
            )
        }
        cursor.close()
        db.close()

        if (productoEncontrado != null) {
            val bottomSheet = StockBottomSheetFragment.newInstance(productoEncontrado.codigoQr)
            bottomSheet.show(supportFragmentManager, "StockBottomSheet")
        } else {
            val intent = Intent(this, AgregarProductoActivity::class.java).apply {
                putExtra("CODIGO_ESCANEADO", codigo)
            }
            startActivity(intent)
        }
    }

    // --- LÓGICA DEL TUTORIAL INTERACTIVO CON BURBUJAS ---
    private fun verificarTutorial() {
        val prefs = getSharedPreferences("SmartStockPrefs", Context.MODE_PRIVATE)
        val tutorialVisto = prefs.getBoolean("TutorialVisto", false)

        if (!tutorialVisto) {
            layoutTutorial.visibility = View.VISIBLE
            findViewById<View>(R.id.paso1).visibility = View.VISIBLE
            findViewById<View>(R.id.paso2).visibility = View.GONE
            findViewById<View>(R.id.paso3).visibility = View.GONE

            iniciarParpadeo(findViewById(R.id.burbuja1))

            layoutTutorial.setOnClickListener {
                when (pasoTutorialActual) {
                    1 -> {
                        findViewById<View>(R.id.paso1).visibility = View.GONE
                        findViewById<View>(R.id.paso2).visibility = View.VISIBLE
                        findViewById<View>(R.id.burbuja1).clearAnimation()
                        iniciarParpadeo(findViewById(R.id.burbuja2))
                        pasoTutorialActual = 2
                    }
                    2 -> {
                        findViewById<View>(R.id.paso2).visibility = View.GONE
                        findViewById<View>(R.id.paso3).visibility = View.VISIBLE
                        findViewById<View>(R.id.burbuja2).clearAnimation()
                        iniciarParpadeo(findViewById(R.id.burbuja3))
                        pasoTutorialActual = 3
                    }
                    3 -> {
                        findViewById<View>(R.id.burbuja3).clearAnimation()
                        layoutTutorial.visibility = View.GONE
                        prefs.edit().putBoolean("TutorialVisto", true).apply()
                    }
                }
            }
        }
    }

    private fun iniciarParpadeo(view: View) {
        val anim = ScaleAnimation(
            0.8f, 1.3f, 0.8f, 1.3f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 600
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        view.startAnimation(anim)
    }
}