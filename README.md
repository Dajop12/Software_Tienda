# Software Tienda (minimarket) — operación diaria

## 1. Abrir
```powershell
$files = (Get-ChildItem -Recurse -Filter "*.java" -Path "src").FullName
javac -encoding UTF-8 -d bin $files
java -cp bin App
```
Entra `admin/1234` (cám adv/res/reset.

## 2. Turno (obligatorio para vender)
Ventas > Abrir turno. Vende. Al final: Cerrar turno (imprime ventas + ganancia).

## 3. Vender / fiado / anular
CONTADO o CREDITO (elige cliente). Fiado suma deuda; Clientes/Fiado abona.
Historial > Anular folio (pide motivo, devuelve stock, revierte fiado).

## 4. Inventario
Productos: agregar/editar (costo y stock-mín solo JEFE), Entrada = factura
proveedor con vencimiento, Kardex = entradas/salidas, columna Vence
(rojo vencido, amarillo ≤30d).

## 5. Cierre del día
Config > Reporte día (imprimir): ventas, anuladas, por caja, top 5, deuda.
Config > Backup cifrado (guarda `backup/*.enc`). Sync USB para 2 cajas:
Exportar en A > Importar en B (no duplica).

## 6. Jefe en celular
Config > Iniciar API :8080. APK apunta a `http://IP-PC:8080` (o HTTPS VPS).
Endpoints con token: `/login, /stock-bajo, /ventas-hoy, /deudas, /vencimientos, /reporte`.
Push FCM con `FCM_SERVER_KEY` + token registrado. Cola en `data/push-cola.log`.

## Roles
ADMIN todo (costos, entradas, ganancias, deudas, usuarios).
VENDEDOR vende + productos sin costo. CONSULTA solo ver.
API: VENDEDOR+ stock/ventas, solo JEFE deudas/push.
