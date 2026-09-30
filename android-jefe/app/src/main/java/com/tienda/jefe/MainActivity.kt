package com.tienda.jefe

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** APK JEFE 6C: login JEFE + home + alertas. Base URL = https://tu-tienda.com o http://IP-PC:8080 */
class MainActivity : AppCompatActivity() {
    private var base = "https://tu-tienda.com"
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val txtBase = EditText(this).apply { hint = "https://tu-tienda.com"; setText(base) }
        val btn = Button(this).apply { text = "Ver stock bajo + ventas + vencen" }
        val out = TextView(this)
        // 3C: registra token para push con app cerrada
        FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
            thread { try { val u = URL("$base/api/push-token"); (u.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"; doOutput = true; outputStream.write("token=$t".toByteArray()); responseCode } } catch (_: Exception) {} }
        }
        btn.setOnClickListener { base = txtBase.text.toString().trimEnd('/')
            thread { val bajo = get("$base/api/stock-bajo"); val hoy = get("$base/api/ventas-hoy")
                val vence = get("$base/api/vencimientos?dias=30")
                runOnUiThread { out.text = "STOCK BAJO:\n$bajo\n\nHOY:\n$hoy\n\nVENCEN:\n$vence" } } }
        col.addView(txtBase); col.addView(btn); col.addView(out)
        setContentView(col)
    }
    private fun get(url: String) = try { (URL(url).openConnection() as HttpURLConnection).run {
        connectTimeout = 8000; inputStream.bufferedReader().readText() } } catch (e: Exception) { "Error: ${e.message}" }
}
