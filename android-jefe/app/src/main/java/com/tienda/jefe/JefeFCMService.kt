package com.tienda.jefe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** 3C: push con app cerrada + re-registro de token. */
class JefeFCMService : FirebaseMessagingService() {
    override fun onNewToken(t: String) {
        thread { try { val u = URL("https://tu-tienda.com/api/push-token")
            (u.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"; doOutput = true
                outputStream.write("token=$t".toByteArray()); responseCode } } catch (_: Exception) {} }
    }
    override fun onMessageReceived(m: RemoteMessage) {
        val t = m.notification?.title ?: "Stock bajo"
        val b = m.notification?.body ?: ""
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(NotificationChannel("jefe", "Jefe", NotificationManager.IMPORTANCE_HIGH))
        mgr.notify(1, NotificationCompat.Builder(this, "jefe")
            .setContentTitle(t).setContentText(b).setSmallIcon(android.R.drawable.ic_dialog_alert).build())
    }
}
