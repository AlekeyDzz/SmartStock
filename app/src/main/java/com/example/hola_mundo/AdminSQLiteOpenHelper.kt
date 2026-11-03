package com.example.hola_mundo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AdminSQLiteOpenHelper(
    context: Context,
    name: String,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        val crearTabla = """
            CREATE TABLE productos (
                codigo_qr TEXT PRIMARY KEY,
                nombre TEXT,
                descripcion TEXT,
                categoria TEXT,
                tipo TEXT,
                precio REAL,
                stock INTEGER,
                foto TEXT
            )
        """
        db.execSQL(crearTabla)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE productos ADD COLUMN foto TEXT DEFAULT ''")
        }
    }
}