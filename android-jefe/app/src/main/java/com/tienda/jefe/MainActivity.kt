package com.tienda.jefe

import android.Manifest
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

/** Panel móvil de jefe: inicio de sesión seguro por token y resumen de la tienda. */
class MainActivity : AppCompatActivity() {
    private val fondo = Color.rgb(22, 27, 36)
    private val tarjeta = Color.rgb(40, 49, 65)
    private val texto = Color.rgb(238, 242, 248)
    private val textoSecundario = Color.rgb(176, 188, 204)
    private val acento = Color.rgb(72, 157, 181)

    private lateinit var baseUrl: EditText
    private lateinit var usuario: EditText
    private lateinit var contrasena: EditText
    private lateinit var boton: Button
    private lateinit var estado: TextView
    private lateinit var reporte: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        construirInterfaz()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                CODIGO_PERMISO_NOTIFICACIONES
            )
        }
    }

    private fun construirInterfaz() {
        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(fondo)
        }
        val scroll = ScrollView(this).apply { addView(contenido) }

        contenido.addView(etiqueta("TIENDA  /  PANEL DE JEFE", 12, acento, true))
        contenido.addView(etiqueta("Tu negocio, de un vistazo", 26, texto, true), margenAbajo(8))
        contenido.addView(etiqueta(
            "Conecta con la caja usando la dirección de la API y tu cuenta de administrador.",
            14, textoSecundario, false
        ), margenAbajo(24))

        val acceso = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = fondoRedondeado(tarjeta, dp(18))
        }
        acceso.addView(etiqueta("Conexión", 18, texto, true), margenAbajo(14))

        baseUrl = campo("URL de la caja (ej. http://192.168.1.20:8080)").apply {
            setText(getSharedPreferences(PREFERENCIAS, MODE_PRIVATE)
                .getString(CLAVE_BASE, "http://192.168.1.20:8080"))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        usuario = campo("Usuario administrador").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        contrasena = campo("Contraseña").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        acceso.addView(baseUrl, margenAbajo(10))
        acceso.addView(usuario, margenAbajo(10))
        acceso.addView(contrasena, margenAbajo(16))

        boton = Button(this).apply {
            text = "Conectar y ver resumen"
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            background = fondoRedondeado(Color.rgb(37, 111, 151), dp(12))
        }
        acceso.addView(boton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        contenido.addView(acceso, margenAbajo(16))

        estado = etiqueta(
            "Introduce la URL de la caja y tus credenciales. HTTP local solo debe usarse en una red Wi-Fi de confianza.",
            13, textoSecundario, false
        )
        contenido.addView(estado, margenAbajo(12))

        reporte = etiqueta("", 14, texto, false).apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = fondoRedondeado(tarjeta, dp(18))
            visibility = View.GONE
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        contenido.addView(reporte)
        setContentView(scroll)

        boton.setOnClickListener { iniciarSesion() }
    }

    private fun iniciarSesion() {
        val base = baseUrl.text.toString().trim().trimEnd('/')
        val user = usuario.text.toString().trim()
        val pass = contrasena.text.toString()
        if (user.isBlank() || pass.isBlank()) {
            estado.text = "Escribe tu usuario y contraseña."
            return
        }
        try {
            val parsed = java.net.URI(base)
            if (parsed.host.isNullOrBlank() || parsed.scheme !in listOf("http", "https")) {
                throw IllegalArgumentException()
            }
            if (parsed.scheme == "http" && !esDireccionPrivada(parsed.host)) {
                estado.text = "Para una dirección pública, usa HTTPS. HTTP solo se permite en IP privadas de tu red local."
                return
            }
        } catch (_: Exception) {
            estado.text = "Escribe una URL válida que empiece con http:// o https://."
            return
        }

        boton.isEnabled = false
        boton.text = "Conectando…"
        estado.text = "Validando acceso con la caja…"
        thread {
            try {
                val form = "user=${codificar(user)}&pass=${codificar(pass)}"
                val respuesta = JSONObject(solicitar("$base/api/login", "POST", form))
                val token = respuesta.getString("token")
                val rol = respuesta.optString("rol")
                if (rol != "ADMIN") throw IOException("Esta pantalla requiere una cuenta administradora.")

                getSharedPreferences(PREFERENCIAS, MODE_PRIVATE).edit()
                    .putString(CLAVE_BASE, base)
                    .putString(CLAVE_TOKEN, token)
                    .apply()
                val datos = cargarResumen(base, token)
                runOnUiThread {
                    estado.text = "Conectado como administrador."
                    reporte.text = datos
                    reporte.visibility = View.VISIBLE
                    boton.isEnabled = true
                    boton.text = "Actualizar resumen"
                    registrarTokenPush(base, token)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    estado.text = e.message ?: "No fue posible conectar con la caja."
                    boton.isEnabled = true
                    boton.text = "Reintentar conexión"
                }
            }
        }
    }

    private fun cargarResumen(base: String, token: String): String {
        val stock = solicitar("$base/api/stock-bajo", token = token)
        val hoy = solicitar("$base/api/ventas-hoy", token = token)
        val deudas = solicitar("$base/api/deudas", token = token)
        val vencen = solicitar("$base/api/vencimientos?dias=30", token = token)
        val informe = solicitar("$base/api/reporte", token = token)
        return """
            RESUMEN DE HOY
            ${JSONObject(hoy).let { "Ventas hoy: \$${String.format(Locale.US, "%.2f", it.optDouble("ventas", 0.0))}  ·  Ganancia: \$${String.format(Locale.US, "%.2f", it.optDouble("ganancia", 0.0))}" }}

            ALERTAS DE INVENTARIO
            ${resumirLista(stock, "Sin productos con stock bajo.")}

            PRODUCTOS POR VENCER
            ${resumirLista(vencen, "Sin productos próximos a vencer.")}

            CLIENTES CON SALDO
            ${JSONObject(deudas).let { "Deuda total: \$${String.format(Locale.US, "%.2f", it.optDouble("total", 0.0))}" }}

            REPORTE DEL DÍA
            ${resumirInforme(informe)}
        """.trimIndent()
    }

    private fun resumirLista(json: String, vacio: String): String {
        val lista = org.json.JSONArray(json)
        if (lista.length() == 0) return vacio
        return (0 until minOf(lista.length(), 8)).joinToString("\n") { i ->
            val item = lista.getJSONObject(i)
            "• ${item.optString("nombre")}  ·  stock ${item.optInt("stock")}" +
                if (item.has("vence")) "  ·  vence ${item.optString("vence")}" else ""
        }
    }

    private fun resumirInforme(json: String): String {
        val dato = JSONObject(json)
        return "Ventas: ${dato.optInt("ventas")}  ·  Total: \$${String.format(Locale.US, "%.2f", dato.optDouble("total", 0.0))}" +
            "  ·  Ganancia: \$${String.format(Locale.US, "%.2f", dato.optDouble("ganancia", 0.0))}"
    }

    private fun registrarTokenPush(base: String, tokenApi: String) {
        if (FirebaseApp.initializeApp(this) == null) {
            estado.append("\nNotificaciones push no configuradas (falta google-services.json).")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { tokenFcm ->
                thread {
                    try {
                        solicitar("$base/api/push-token", "POST",
                            "token=${codificar(tokenFcm)}", tokenApi)
                    } catch (e: Exception) {
                        runOnUiThread { estado.append("\nNo se pudo registrar el token de notificaciones: ${e.message}") }
                    }
                }
            }
            .addOnFailureListener { error ->
                estado.append("\nNo se pudo obtener el token push: ${error.message}")
            }
    }

    private fun solicitar(
        url: String,
        metodo: String = "GET",
        cuerpo: String? = null,
        token: String? = null
    ): String {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 8_000
            conexion.readTimeout = 12_000
            conexion.instanceFollowRedirects = false
            conexion.requestMethod = metodo
            conexion.setRequestProperty("Accept", "application/json")
            if (token != null) conexion.setRequestProperty("X-API-TOKEN", token)
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val contenido = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) {
                val mensaje = try { JSONObject(contenido).optString("error", contenido) }
                    catch (_: Exception) { contenido }
                throw IOException("La caja respondió HTTP $codigo: $mensaje")
            }
            return contenido
        } finally {
            conexion.disconnect()
        }
    }

    private fun campo(hintTexto: String) = EditText(this).apply {
        hint = hintTexto
        textSize = 15f
        setSingleLine(true)
        setTextColor(this@MainActivity.texto)
        setHintTextColor(textoSecundario)
        setPadding(dp(14), 0, dp(14), 0)
        background = fondoRedondeado(Color.rgb(31, 38, 51), dp(10), Color.rgb(63, 75, 96))
    }

    private fun etiqueta(valor: String, tamano: Int, color: Int, negrita: Boolean) =
        TextView(this).apply {
            text = valor
            textSize = tamano.toFloat()
            setTextColor(color)
            if (negrita) setTypeface(typeface, Typeface.BOLD)
        }

    private fun fondoRedondeado(color: Int, radio: Int, borde: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radio.toFloat()
            if (borde != null) setStroke(dp(1), borde)
        }

    private fun margenAbajo(dp: Int) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = this@MainActivity.dp(dp) }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private fun codificar(valor: String) = URLEncoder.encode(valor, Charsets.UTF_8.name())

    private fun esDireccionPrivada(hostOriginal: String): Boolean {
        val host = hostOriginal.removePrefix("[").removeSuffix("]").lowercase(Locale.ROOT)
        if (host == "localhost" || host == "::1" || host.startsWith("fc") ||
            host.startsWith("fd") || host.startsWith("fe80:")) return true
        val partes = host.split(".")
        if (partes.size != 4) return false
        val ip = partes.map { it.toIntOrNull() ?: return false }
        if (ip.any { it !in 0..255 }) return false
        return ip[0] == 10 ||
            (ip[0] == 172 && ip[1] in 16..31) ||
            (ip[0] == 192 && ip[1] == 168) ||
            ip[0] == 127 ||
            (ip[0] == 169 && ip[1] == 254)
    }

    companion object {
        private const val PREFERENCIAS = "tienda_jefe"
        private const val CLAVE_BASE = "api_base"
        private const val CLAVE_TOKEN = "api_token"
        private const val CODIGO_PERMISO_NOTIFICACIONES = 1001
    }
}
