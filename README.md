# 🛒 Software Tienda (Minimarket) — Gestión Empresarial v2.0

Sistema de punto de venta avanzado con control de inventario, gestión de créditos (fiados) y administración de usuarios.

## 🚀 1. Ejecución y Lanzamiento

### Compilación y Arranque (PowerShell)
```powershell
$files = (Get-ChildItem -Recurse -Filter "*.java" -Path "src").FullName
javac -encoding UTF-8 -cp "lib/*" -d bin $files
java -cp "bin;lib/*" App
```

> **Framework Visual:** El diseño utiliza **FlatLaf** para una experiencia de usuario moderna y oscura.
> **Acceso Inicial:** Usuario: `admin` / Clave: `1234`. Se solicitará el cambio de contraseña al primer ingreso.

## 🛡️ Seguridad y Datos
- **Carga Segura:** La app crea datos demo solo en la primera ejecución.
- **Integridad:** Si un archivo de datos (`.dat`) está dañado, la app detendrá el inicio para evitar pérdida de información. Se recomienda restaurar desde un backup válido.
- **Escritura Atómica:** Los cambios se guardan en archivos temporales antes de sustituir el original para prevenir errores por cortes de energía.

## 🛠️ Funcionalidades Principales

### 💰 Punto de Venta y Fiados
- **Flujo de Venta:** Obligatorio abrir turno $\rightarrow$ Vender $\rightarrow$ Cerrar turno (Genera reporte de ganancias).
- **Gestión de Créditos (Fiado):** 
  - Ventas a crédito vinculadas a clientes.
  - **Módulo de Clientes:** Panel dedicado para crear clientes, registrar nuevas deudas o aplicar abonos/pagos.
  - **Control Visual:** Alertas en rojo para clientes con deuda pendiente.
- **Anulaciones:** Capacidad de anular folios con motivo, devolviendo el stock automáticamente y revirtiendo la deuda del cliente.

### 📦 Control de Inventario
- **Gestión de Productos:** CRUD completo con validación de precios y stock.
- **Entradas de Proveedor:** Registro de facturas que incrementan el stock y definen fechas de vencimiento.
- **Kardex:** Historial detallado de cada movimiento (entradas/salidas) por producto.
- **Alertas de Caducidad:** Semáforo visual en la tabla (Rojo: Vencido, Amarillo: $\le 30$ días).

### 📊 Administración y Cierre
- **Reportes:** Generación de reportes diarios (ventas, anulaciones, top 5 productos, deuda total).
- **Respaldo:** Sistema de backup cifrado y sincronización entre múltiples cajas vía exportación/importación.

### 📱 Acceso Remoto (Módulo Jefe)
- **Mini API Server:** Servidor interno en el puerto `:8080` para consulta desde dispositivos móviles.
- **Endpoints Seguros:** Acceso a stock bajo, ventas del día, deudas y reportes mediante token.
- **Notificaciones Push:** Integración con Firebase Cloud Messaging (FCM) para alertas críticas en el celular del dueño.

## 🎨 Interfaz y Experiencia de Usuario (UX)
- **Estética Moderna:** Paleta de colores *Deep Night & Electric Blue* diseñada para reducir la fatiga visual.
- **Tipografía Amigable:** Uso de fuentes modernas y legibles (**Arial Black / Inter**) para una lectura rápida en entorno de negocio.
- **Atajos de Teclado:** Navegación rápida mediante `Ctrl+1` hasta `Ctrl+6`.

## 🔑 Roles de Usuario
| Rol | Permisos |
| :--- | :--- |
| **ADMIN (JEFE)** | Acceso total: costos, entradas de proveedor, ganancias, gestión de usuarios y deudas. |
| **VENDEDOR** | Ventas, consulta de productos (sin ver costos) y gestión de turnos. |
| **CONSULTA** | Solo visualización de datos. |
