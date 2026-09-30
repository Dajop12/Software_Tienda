# APK JEFE (6C) + nube (2B) + push (3C) — contrato F4

## 1. Encender API en PC (caja principal)
Config > "Iniciar API :8080". Debe verse `🟢 :8080 para APK`.
En el mismo WiFi, la APK usa `http://IP-PC:8080` (ej: `http://192.168.1.50:8080`).

## 2. Endpoints para la APK (Kotlin/Retrofit o HttpURLConnection)
- `GET /api/salud` -> `{"ok":true}` (probar conexión)
- `GET /api/stock-bajo` -> `[{"id","nombre","stock","stockMin"}]` (poll cada 5 min + push)
- `GET /api/ventas-hoy` -> `{"ventas":N,"ganancia":N,"deudas":N}` (home jefe)
- `GET /api/deudas` -> `{"total":N,"clientes":[{"id","nombre","deuda"}]}`
- `POST /api/push-token` body `token=XXX` (registra FCM del jefe)
- `GET /api/vencimientos?dias=30` -> `[{"id","nombre","stock","vence","dias"}]` (F12)

Pantallas APK jefe: Login (mismo usuario, rol JEFE) > Home (ventas/ganancia/deudas) >
Alertas (stock-bajo) > Inventario (solo lectura) > Deudas.

## 3. Push con app cerrada (3C)
Hoy: cada venta encola en `data/push-cola.log` + `Bitacora` (outbox).
FCM real (1 cambio): Firebase Console > Cloud Messaging > server-key >
pegar en `PushService.MODO="FCM"` e implementar `enviarFCM()` con `payloadFCM()`.
La APK registra su token con `POST /api/push-token`.

## 4. A nube 2B (VPS)
Sube este mismo `.jar`/código a VPS, abre puerto 8080 con HTTPS (Caddy/Nginx),
apunta APK a `https://tu-tienda.com`. Sin cambiar endpoints.
