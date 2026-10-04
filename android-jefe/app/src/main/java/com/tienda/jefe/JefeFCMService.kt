package com.tienda.jefe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** 3C: push con app cerrada + re-registro de token. */
class JefeFCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val preferencias = getSharedPreferences("tienda_jefe", Context.MODE_PRIVATE)
        val base = preferencias.getString("api_base", null) ?: return
        val apiToken = preferencias.getString("api_token", null) ?: return
        thread {
            try {
                val conexion = URL("$base/api/push-token").openConnection() as HttpURLConnection
                try {
                    conexion.connectTimeout = 8_000
                    conexion.readTimeout = 8_000
                    conexion.instanceFollowRedirects = false
                    conexion.requestMethod = "POST"
                    conexion.doOutput = true
                    conexion.setRequestProperty("X-API-TOKEN", apiToken)
                    conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    val tokenCodificado = URLEncoder.encode(token, Charsets.UTF_8.name())
                    conexion.outputStream.use {
                        it.write("token=$tokenCodificado".toByteArray(Charsets.UTF_8))
                    }
                    if (conexion.responseCode !in 200..299) {
                        Log.w(TAG, "No se pudo actualizar el token push (HTTP ${conexion.responseCode}).")
                    }
                } finally {
                    conexion.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar el token push.", e)
            }
        }
    }
    override fun onMessageReceived(m: RemoteMessage) {
        val t = m.notification?.title ?: "Stock bajo"
        val b = m.notification?.body ?: ""
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(NotificationChannel("jefe", "Jefe", NotificationManager.IMPORTANCE_HIGH))
        mgr.notify(1, NotificationCompat.Builder(this, "jefe")
            .setContentTitle(t).setContentText(b).setSmallIcon(android.R.drawable.ic_dialog_alert).build())
    }

    companion object {
        private const val TAG = "JefeFCMService"
    }
}
