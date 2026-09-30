# F5 — APK + FCM + HTTPS (2B/3C/6C)

## FCM real (5 min)
1. Firebase Console > nuevo proyecto > Cloud Messaging > copia **server key**.
2. En PC caja: `setx FCM_SERVER_KEY "tu-key"` (Windows) y reinicia la app.
3. Vende hasta stock bajo > llega push al jefe con app cerrada + queda en `data/push-cola.log`.
4. APK: `google-services.json` en `android-jefe/app/`, compila en Android Studio > Run.

## HTTPS nube 2B (Caddy, 2 comandos, cert gratis)
En VPS con este código corriendo en :8080:
```
caddy reverse-proxy --from https://tu-tienda.com --to localhost:8080
```
APK apunta a `https://tu-tienda.com`. Sin cambiar código. Alternativa: Nginx con certbot.

## Probar APK sin compilar (ahorro tokens)
1. Config > Iniciar API. 2. En celular mismo WiFi abre `http://IP-PC:8080/api/stock-bajo`.
Si ves JSON, la APK verá lo mismo.
