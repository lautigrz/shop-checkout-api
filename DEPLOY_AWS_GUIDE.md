# 🚀 Guía de Despliegue en la Nube: AWS (EC2 + RDS) + MongoDB Atlas + CI/CD

Esta guía explica paso a paso, desde cero y de forma didáctica, cómo llevar tu API de comercio electrónico (**shop-api**) desde tu entorno de desarrollo local a una arquitectura **Cloud Multi-Tier de Producción**.

---

## 🏗️ 1. Arquitectura Cloud del Sistema

La arquitectura sigue el patrón estándar de tres capas desacopladas en la nube:

```mermaid
flowchart TD
    subgraph GitHub ["GitHub Platform"]
        Code[Repositorio Git] -->|Push a main| Pipeline[GitHub Actions CI/CD]
        Pipeline -->|1. Test & Build| Artifact[shop-api.jar]
        Pipeline -->|2. SSH & SCP Deploy| EC2
    end

    subgraph AWS ["Amazon Web Services (AWS VPC)"]
        subgraph EC2_Box ["Servidor de Cómputo (EC2)"]
            Docker[Docker Compose Engine]
            App[Contenedor: shop-api:prod<br>Spring Boot + OpenJDK 21]
            Docker --> App
        end

        subgraph RDS_Box ["Base Relacional Transaccional"]
            RDS[(AWS RDS PostgreSQL 16<br>Usuarios, Órdenes, Pagos)]
        end
    end

    subgraph Atlas ["MongoDB Cloud"]
        Mongo[(MongoDB Atlas M0 Cluster<br>Catálogo de Productos)]
    end

    Client([Clientes / Frontend / Swagger]) -->|HTTP Port 80 / 8080| App
    App -->|JDBC Port 5432| RDS
    App -->|TLS / SRV Port 27017| Mongo
```

### Componentes de la Arquitectura:
1. **Cómputo (AWS EC2):** Máquina virtual Ubuntu 24.04 (Capa gratuita `t3.micro` o `t2.micro`) ejecutando la aplicación dentro de un contenedor Docker aislado y seguro.
2. **Base Relacional (AWS RDS PostgreSQL):** Maneja la integridad transaccional ACID de usuarios, auditoría, estados de pedido y transacciones de pago.
3. **Base NoSQL (MongoDB Atlas):** Clúster gestionado de alta disponibilidad para el catálogo flexible de productos.
4. **CI/CD (GitHub Actions):** Pipeline automatizado que corre los tests unitarios, empaqueta el artefacto y lo despliega automáticamente al servidor mediante SSH en cada push.

---

## 🍃 2. Paso 1: Configurar MongoDB Atlas (Catálogo NoSQL)

MongoDB Atlas ofrece un clúster **M0 Sandbox 100% gratuito de por vida** (512 MB de almacenamiento), ideal para el catálogo.

### 2.1. Crear el Clúster
1. Registrate o iniciá sesión en [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2. Creá una nueva organización o proyecto (ej. `shop-api-prod`).
3. Hacé clic en **Create a Deployment**:
   - Elegí la opción **M0 Free** (Shared).
   - Proveedor: **AWS**.
   - Región: Seleccioná la misma región que usarás en AWS (ej. `us-east-1` N. Virginia o `sa-east-1` São Paulo) para minimizar latencia.
   - Nombre del Cluster: `Cluster0`.
   - Clic en **Create Deployment**.

### 2.2. Crear el Usuario de Base de Datos
En la pantalla de seguridad que aparece (o en el menú lateral **Database Access**):
1. Tipo de autenticación: **Username / Password**.
2. Usuario: `shop_admin`.
3. Contraseña: Generá una contraseña segura (guárdala en un bloc de notas temporal).
4. Privilegios: `Read and write to any database`.
5. Clic en **Create User**.

### 2.3. Configurar Acceso de Red (Network Access)
En el menú lateral **Network Access**:
1. Hacé clic en **Add IP Address**.
2. Para comenzar de forma sencilla, seleccioná: **Allow Access from Anywhere** (`0.0.0.0/0`).
   > *Nota de producción:* Una vez que tengas la IP pública de tu servidor EC2, podés reemplazar esta regla para permitir únicamente la IP de tu EC2.
3. Clic en **Confirm**.

### 2.4. Obtener la URI de Conexión
1. En el menú **Database**, hacé clic en el botón **Connect** de tu cluster.
2. Seleccioná **Drivers** (Java).
3. Copiá la cadena de conexión que se parece a esto:
   ```text
   mongodb+srv://shop_admin:<password>@cluster0.xxxx.mongodb.net/?retryWrites=true&w=majority
   ```
4. Agregale el nombre de la base de datos `shop` antes del signo de pregunta `?` y reemplazá `<password>` por tu contraseña real:
   ```text
   mongodb+srv://shop_admin:MiPasswordSegura123@cluster0.xxxx.mongodb.net/shop?retryWrites=true&w=majority
   ```
   *(Esta será tu variable `SPRING_DATA_MONGODB_URI`).*

---

## 🐘 3. Paso 2: Configurar AWS RDS PostgreSQL (Transacciones)

AWS RDS provee PostgreSQL gestionado con backups automáticos y alta disponibilidad.

### 3.1. Crear la Instancia de Base de Datos
1. Ingresá a la consola de **AWS** y buscá el servicio **RDS**.
2. Asegurate de estar en tu región elegida (ej. `us-east-1` N. Virginia).
3. Hacé clic en **Create database**:
   - **Método:** Standard create.
   - **Motor:** PostgreSQL (versión `PostgreSQL 16.x`).
   - **Plantillas (Templates):** Seleccioná **Free tier** (Capa gratuita).
   - **Configuración de la instancia:**
     - DB instance identifier: `shop-postgres-db`
     - Master username: `shop`
     - Master password: `TuPasswordPostgresSegura123` (guárdala).
   - **Configuración de instancia:** `db.t3.micro` o `db.t4g.micro` (1 vCPU, 1 GB RAM).
   - **Almacenamiento:** General Purpose SSD (gp2 o gp3), 20 GB (desmarcá "Enable storage autoscaling").

### 3.2. Conectividad y Red
- **VPC:** Default VPC.
- **Acceso público (Public access):** Seleccioná **Yes** *(necesario si querés conectarte con DBeaver/PgAdmin desde tu casa para auditar tablas)*.
- **VPC security group:** Crear nuevo (ej. `rds-shop-sg`).
- **Initial database name:** Desplegá **Additional configuration** y en *Initial database name* escribí: `shop`.
- Hacé clic en **Create database** (tardará unos 5 a 10 minutos en crearse).

### 3.3. Configurar el Security Group de RDS (Permitir tráfico en puerto 5432)
1. En la lista de bases de datos de RDS, hacé clic en `shop-postgres-db`.
2. En la sección **Connectivity & security**, hacé clic en el enlace del **VPC security groups**.
3. En la pestaña **Inbound rules (Reglas de entrada)**, hacé clic en **Edit inbound rules**.
4. Añadí una regla:
   - **Tipo:** PostgreSQL (Puerto `5432`).
   - **Origen:** `Anywhere-IPv4` (`0.0.0.0/0`) para pruebas, o la IP de tu EC2 para máxima seguridad.
5. Guardá las reglas.

### 3.4. Obtener el JDBC URL
En la pestaña **Connectivity & security** de RDS, copiá el **Endpoint** generado (ej. `shop-postgres-db.c123456.us-east-1.rds.amazonaws.com`).

Tu URL de conexión JDBC final quedará así:
```text
jdbc:postgresql://shop-postgres-db.c123456.us-east-1.rds.amazonaws.com:5432/shop?options=-c%20timezone=UTC
```

---

## 🖥️ 4. Paso 3: Configurar la Instancia AWS EC2 (Servidor)

### 4.1. Lanzar la Instancia
1. En la consola de AWS, andá a **EC2** → **Instances** → **Launch instances**.
2. **Nombre:** `shop-api-server`.
3. **AMI (Sistema Operativo):** **Ubuntu Server 24.04 LTS (HVM)**, SSD Volume Type (Free tier eligible).
4. **Instance type:** `t3.micro` o `t2.micro` (Free tier eligible).
5. **Key pair (Par de claves para SSH):**
   - Hacé clic en **Create new key pair**.
   - Nombre: `shop-key`.
   - Tipo de par de claves: `RSA`.
   - Formato de archivo de clave privada: `.pem` (para OpenSSH / GitHub Actions).
   - Guardá el archivo `shop-key.pem` descargado en un lugar seguro.

### 4.2. Configuración de Red (Security Group de EC2)
En la sección **Network settings**, creá un Security Group con estas 3 reglas de entrada obligatorias:

| Tipo | Protocolo | Puerto | Origen | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| **SSH** | TCP | `22` | `0.0.0.0/0` | Para GitHub Actions y conexión remota |
| **HTTP** | TCP | `80` | `0.0.0.0/0` | Tráfico web público |
| **Custom TCP** | TCP | `8080` | `0.0.0.0/0` | Puerto de la aplicación Spring Boot / Swagger |

Hacé clic en **Launch instance**.

### 4.3. Preparar el Servidor EC2 (Instalar Docker)
Una vez que el estado de la instancia pase a **Running**:
1. Copiá la **Dirección IPv4 pública** de tu EC2 (ej. `54.210.88.120`).
2. Abrí tu terminal local (PowerShell o Git Bash) donde guardaste `shop-key.pem` y conectate:
   ```bash
   ssh -i shop-key.pem ubuntu@54.210.88.120
   ```
3. Dentro de la EC2, ejecutá este comando de una sola línea para instalar Docker y Docker Compose:
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-v2
   sudo usermod -aG docker ubuntu
   mkdir -p ~/app
   exit
   ```
   *(Al hacer `exit`, la próxima vez que el usuario `ubuntu` inicie sesión ya tendrá permisos de Docker sin necesidad de `sudo`).*

---

## 🔐 5. Paso 4: Configurar Secretos en GitHub

Para que el pipeline de GitHub Actions pueda conectarse a tu EC2 y configurar las credenciales sin exponer ninguna contraseña en el código fuente, usamos **GitHub Secrets**:

1. En tu repositorio de GitHub, andá a:
   **Settings** → **Secrets and variables** → **Actions**.
2. Hacé clic en **New repository secret** y agregá los siguientes **7 secretos**:

| Nombre del Secreto | Ejemplo de Valor | Descripción |
| :--- | :--- | :--- |
| **`EC2_HOST`** | `54.210.88.120` | La IP pública de tu instancia EC2 |
| **`EC2_USER`** | `ubuntu` | Usuario SSH por defecto de Ubuntu en AWS |
| **`EC2_SSH_KEY`** | `-----BEGIN RSA PRIVATE KEY-----...` | El contenido completo de tu archivo `shop-key.pem` |
| **`RDS_JDBC_URL`** | `jdbc:postgresql://shop-postgres-db.c123.rds.amazonaws.com:5432/shop?options=-c%20timezone=UTC` | Endpoint JDBC de RDS |
| **`RDS_USERNAME`** | `shop` | Usuario maestro de RDS |
| **`RDS_PASSWORD`** | `TuPasswordPostgresSegura123` | Contraseña maestra de RDS |
| **`MONGO_ATLAS_URI`** | `mongodb+srv://shop_admin:Pass123@cluster0.mongodb.net/shop?retryWrites=true&w=majority` | URI de conexión a Atlas |
| **`JWT_SECRET_KEY`** | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Clave secreta hexadecimal de 256 bits |

---

## ⚙️ 6. Paso 5: Cómo opera el Pipeline Desacoplado (CI/CD)

El pipeline está dividido en **dos workflows independientes y encadenados** para garantizar eficiencia y cero desperdicio de recursos:

```
[ Desarrollador hace git push origin main ]
                     │
                     ▼
       ┌──────────────────────────────────────────────┐
       │ 1. Workflow: Build and Test                  │
       │ (build-and-test.yml)                         │
       │ - Descarga JDK 21                            │
       │ - Corre los 32 tests unitarios y de slice    │
       │ - Compila y empaqueta el .jar (UNA SOLA VEZ) │
       │ - Sube el .jar como artefacto validado       │
       └──────────────────────┬───────────────────────┘
                              │
                              ▼ (Disparo automático tras éxito: workflow_run)
       ┌──────────────────────────────────────────────┐
       │ 2. Workflow: Deploy to AWS EC2               │
       │ (deploy.yml)                                 │
       │ - Descarga el .jar ya compilado de la etapa 1│
       │ - Conecta por SSH a la instancia EC2         │
       │ - Transfiere el JAR y archivos Docker por SCP│
       │ - Escribe .env con los 7 GitHub Secrets      │
       │ - Docker en EC2 solo envuelve el JAR en JRE  │
       │   (Tarda 2 segundos, no compila en EC2)      │
       │ - Reinicio limpio del contenedor             │
       └──────────────────────────────────────────────┘
```

---

## 🧪 7. Paso 6: Verificación en Vivo del Despliegue

Una vez que el workflow de GitHub Actions termine con tilde verde ✅:

### 7.1. Ver los logs en vivo en tu EC2
Podés entrar a tu servidor por SSH y ver cómo Spring Boot arranca en la nube:
```bash
ssh -i shop-key.pem ubuntu@TU_IP_PUBLICA
cd ~/app
docker compose -f docker-compose.prod.yaml logs -f
```
Verás la inicialización de Flyway aplicando las migraciones en AWS RDS, el handshake TLS con MongoDB Atlas y el arranque de Tomcat:
```text
Tomcat started on port 8080 (http) with context path '/'
Started ShopApiApplication in 18.234 seconds
```

### 7.2. Probar Swagger UI en el navegador
Abrí tu navegador web y navegá a:
```text
http://TU_IP_PUBLICA/swagger-ui.html
```
*(O también `http://TU_IP_PUBLICA:8080/swagger-ui.html`)*.

Podrás:
1. Autenticarte con `admin@shop.com` y `admin123`.
2. Crear un producto en MongoDB Atlas.
3. Ejecutar un checkout completo orquestando Flyway, PostgreSQL en RDS, MongoDB Atlas y los eventos de dominio.
