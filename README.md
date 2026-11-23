# 🤖 Spring AI RAG con Ollama y pgvector

Sistema de consulta de documentos con **RAG** (*Retrieval Augmented Generation*): subes un PDF, se guarda en una base de datos vectorial y luego le haces preguntas en lenguaje natural. La IA responde **usando la información de tu documento**.

Todo corre en tu máquina, sin claves de API ni costos: los modelos los ejecuta **Ollama** y la base vectorial es **PostgreSQL con pgvector**.

## 📑 Tabla de contenido

1. [¿Qué es RAG?](#-qué-es-rag)
2. [Tecnologías usadas](#️-tecnologías-usadas)
3. [Cómo funciona](#-cómo-funciona)
4. [Requisitos](#-requisitos)
5. [Instalar Ollama y los modelos](#-instalar-ollama-y-los-modelos)
6. [Inicio rápido con Docker Compose](#-inicio-rápido-con-docker-compose)
7. [Paso a paso manual con Docker](#-paso-a-paso-manual-con-docker)
8. [Uso de la API](#-uso-de-la-api)
9. [Verificar los datos en la base](#-verificar-los-datos-en-la-base)
10. [Variables de entorno](#️-variables-de-entorno)
11. [Publicar imágenes multiplataforma](#-publicar-imágenes-multiplataforma)
12. [Solución de problemas](#-solución-de-problemas)

---

## 🧠 ¿Qué es RAG?

**RAG** significa *Retrieval Augmented Generation* (generación aumentada por recuperación). Almacena tu información propia en una **base de datos vectorial**, que guarda vectores y busca similitudes **semánticas**, es decir, por significado y no solo por palabras exactas.

Para eso trabaja con un **modelo de embeddings**, que convierte texto en vectores numéricos. Así la IA puede interpretar, comprender y comparar textos a gran velocidad.

### ¿Qué es un embedding?

Una lista de números que representa el significado de un texto. Un modelo de IA convierte una frase en esa lista, y así la computadora puede comparar significados con matemáticas.

Si buscas "mascota canina", encuentra el texto sobre "perros" aunque nunca diga esa palabra.

### ¿Cómo se usa en RAG?

1. Conviertes tus documentos en embeddings y los guardas en la base de datos (aquí entra `vector`).
2. Cuando el usuario pregunta algo, conviertes la pregunta en otro embedding.
3. Buscas los textos cuyos números estén **más cerca** de los de la pregunta.
4. Esos textos se le pasan a la IA para que responda con tu información.

> 💡 El modelo de embeddings es **distinto** al modelo de chat, y el tamaño del vector debe coincidir con el de la columna de la base de datos (con `nomic-embed-text` son **768** números).

---

## 🛠️ Tecnologías usadas

| Tecnología | Para qué se usa |
| --- | --- |
| ☕ **Java 25** | Lenguaje de la aplicación |
| 🍃 **Spring Boot 4.1.2** | Framework de la aplicación y API REST |
| 🧩 **Spring AI** | Integración con modelos de IA, lectura de PDF, divisor de texto y vector store |
| 📄 **PagePdfDocumentReader** | Lee el PDF página por página |
| ✂️ **TextSplitter** | Divide el texto en fragmentos (*chunks*) |
| 🐘 **PostgreSQL 18** | Base de datos relacional |
| 🧮 **pgvector** | Extensión para guardar vectores y buscar por similitud (índice **HNSW**, distancia **coseno**) |
| 🗂️ **hstore** | Extensión de PostgreSQL para pares clave-valor |
| 🆔 **uuid-ossp** | Extensión de PostgreSQL para generar UUID |
| 🦙 **Ollama** | Ejecuta los modelos de IA en local |
| 🔢 **nomic-embed-text** | Modelo de embeddings (vectores de 768 números) |
| 💬 **qwen3:0.6b** | Modelo de chat que redacta la respuesta |
| 📦 **Maven** | Gestión de dependencias y compilación |
| 🐳 **Docker** | Contenedores para la base de datos y la aplicación |
| 🧱 **Docker Compose** | Levanta todos los servicios con un comando |
| 🏗️ **Docker Buildx** | Construcción de imágenes multiplataforma (`amd64` y `arm64`) |
| ☁️ **Docker Hub** | Registro donde se publican las imágenes |
| 📬 **Postman** | Para probar los endpoints |
| 🌿 **Git** | Control de versiones |

---

## 🔄 Cómo funciona

**Indexar un documento**

```
PDF ──► se divide en fragmentos (chunking)
    ──► cada fragmento se convierte en un vector (nomic-embed-text)
    ──► se guarda en PostgreSQL (pgvector, tabla vector_store)
```

**Hacer una pregunta**

```
Pregunta ──► se convierte en un vector
         ──► pgvector busca los 4 fragmentos más cercanos (distancia coseno, operador <=>)
         ──► se arma un prompt con esos fragmentos como contexto
         ──► el modelo de chat (qwen3:0.6b) redacta la respuesta
```

Si la respuesta no está en el documento, el modelo debe contestar: *"No encontré esa información en los documentos."*

---

## 📋 Requisitos

- 🐳 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- 🦙 [Ollama](https://ollama.com/) instalado en tu máquina
- 📄 Un PDF para probar
- 📬 Postman (o `curl`)
- ☕ Java 25 y Maven, **solo** si quieres ejecutar la app fuera de Docker

> ⚡ Sin tarjeta gráfica compatible, Ollama corre en la **CPU**, así que las respuestas pueden tardar. Con `qwen3:0.6b` es tolerable para una demo.

---

## 🦙 Instalar Ollama y los modelos

### Instalación en Windows

En PowerShell:

```powershell
irm https://ollama.com/install.ps1 | iex
```

También puedes descargar el instalador desde la página oficial.

### Comandos útiles

```bash
ollama --version     # ver la versión
ollama list          # ver los modelos descargados
ollama pull <modelo> # descargar un modelo
```

### Descargar los modelos del proyecto

```bash
ollama pull nomic-embed-text   # modelo de embeddings
ollama pull qwen3:0.6b         # modelo de chat
```

### ¿Qué hace cada uno?

| Modelo | Tipo | Qué hace |
| --- | --- | --- |
| `nomic-embed-text` | Embeddings | No conversa. Solo convierte texto en una lista de 768 números que representa su significado |
| `qwen3:0.6b` | Chat | Redacta la respuesta final usando el contexto recuperado |

Al subir el PDF, cada fragmento pasa por `nomic-embed-text` y se guarda en la columna `embedding`. Al preguntar, la pregunta pasa por el **mismo** modelo y se buscan los vectores más cercanos.

> ⚠️ Si cambias de modelo de embeddings, cambia la dimensión: hay que recrear la tabla y volver a indexar, porque los vectores de modelos distintos no se pueden comparar.

---

## 🚀 Inicio rápido con Docker Compose

La forma más corta de levantar el proyecto:

```bash
git clone <url-de-tu-repositorio>
cd <carpeta-del-proyecto>
docker compose -f docker-compose-ai-rag.yml up --build
```

Antes de ejecutarlo, comprueba que **Ollama está abierto** y que los dos modelos están descargados (`ollama list`).

La primera vez tarda, porque descarga las imágenes y compila la aplicación. Cuando termine, la API queda en `http://localhost:8080`.

Para apagar todo:

```bash
docker compose -f docker-compose-ai-rag.yml down      # conserva los datos
docker compose -f docker-compose-ai-rag.yml down -v   # borra también los datos
```

---

## 🐳 Paso a paso manual con Docker

Útil para entender cada pieza por separado.

### 1️⃣ Crear la red

```bash
docker network create rag-net
```

Los contenedores de una red propia se encuentran por nombre. Así la aplicación llega a la base con `postgresvg-rag` y no con `localhost`.

### 2️⃣ Probar la base con la imagen oficial (opcional)

```bash
docker pull pgvector/pgvector:pg18-trixie

docker run -d --name postgres --restart always \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -e POSTGRES_DB=spring_ai_rag \
  pgvector/pgvector:pg18-trixie
```

Para entrar a la consola de PostgreSQL:

```bash
docker exec -it postgres psql -U postgres -d spring_ai_rag
```

Y crear las extensiones a mano (cada comando termina con `;`):

```sql
create extension if not exists vector;
create extension if not exists hstore;
create extension if not exists "uuid-ossp";
```

| Extensión | Qué hace |
| --- | --- |
| `vector` | Guarda embeddings y permite buscar los más parecidos |
| `hstore` | Guarda pares clave-valor en una columna, útil para metadatos |
| `uuid-ossp` | Genera identificadores únicos (UUID) que Spring AI usa como `id` de cada documento |

### 3️⃣ Dockerizar la base de datos

El `Dockerfile.postgresvectordb` parte de la imagen de pgvector y ejecuta el script que crea las extensiones.

```bash
# construir la imagen
docker build -t sebashh26/spring_ai_rag -f Dockerfile.postgresvectordb .

# levantar el contenedor
docker run -d --name postgresvg-rag --network rag-net -p 5432:5432 \
  -v pgdata:/var/lib/postgresql \
  sebashh26/spring_ai_rag
```

> ⚠️ Con PostgreSQL **18 o superior**, el volumen se monta en `/var/lib/postgresql` y no en `/var/lib/postgresql/data`.

Para entrar a la consola y ver las tablas:

```bash
docker exec -it postgresvg-rag psql -U postgres -d spring_ai_rag
```

```sql
\dt
```

### 4️⃣ Dockerizar la aplicación

```bash
# construir la imagen
docker build -t sebashh26/springai-rag-app -f Dockerfile.app .

# levantar el contenedor
docker run -d --name springai-rag --network rag-net -p 8080:8080 \
  -e DB_HOST=postgresvg-rag \
  -e DB_PASSWORD=root \
  -e OLLAMA_URL=http://host.docker.internal:11434 \
  sebashh26/springai-rag-app
```

- `DB_HOST` debe ser el **nombre del contenedor** de la base.
- `OLLAMA_URL` apunta a tu Ollama local: desde un contenedor, `localhost` no es tu máquina, y `host.docker.internal` sí lo es.

### 5️⃣ Indexar el PDF y comprobar

Sube el PDF (ver [Uso de la API](#-uso-de-la-api)). Debe responder:

```json
{
    "message": "Document has been indexed"
}
```

Luego revisa que se guardó:

```sql
select content from vector_store limit 1;
```

Con los datos ya cargados en el volumen, no hace falta volver a subir el PDF cuando reinicies la aplicación.

---

## 📡 Uso de la API

### 📥 Indexar un PDF

| | |
| --- | --- |
| **Método** | `POST` |
| **URL** | `http://localhost:8080/index-upload/pdf` |
| **Body** | `form-data`, campo `file` de tipo **File** |

En Postman: método **POST**, pestaña **Body**, opción **form-data**, agregar un campo llamado `file`, cambiar su tipo a **File** y elegir el PDF.

Con `curl`:

```bash
curl -X POST http://localhost:8080/index-upload/pdf -F "file=@mi-documento.pdf"
```

### 💬 Hacer una pregunta

Envía la pregunta al endpoint de consulta de tu controlador:

```bash
curl -X POST http://localhost:8080/{ruta-de-chat} \
  -H "Content-Type: text/plain" \
  -d "¿Cuál es la tasa del Crédito de Consumo?"
```

> 💡 Para comprobar que funciona, haz una pregunta cuya respuesta esté **solo** en tu PDF.

---

## 🔍 Verificar los datos en la base

Entra a `psql`:

```bash
docker exec -it postgresvg-rag psql -U postgres -d spring_ai_rag
```

| Qué quieres ver | Comando |
| --- | --- |
| Extensiones instaladas | `\dx` |
| Tablas | `\dt` |
| Estructura de la tabla y dimensión del vector | `\d vector_store` |
| Cuántos fragmentos hay guardados | `select count(*) from vector_store;` |
| El inicio de cada fragmento | `select id, left(replace(content, E'\n', ' '), 80) from vector_store limit 5;` |
| La dimensión del embedding | `select vector_dims(embedding) from vector_store limit 1;` |

Si un resultado se abre en un paginador (`(END)`), pulsa `q` para salir. Para que no se abra, ejecuta `\pset pager off`. Para salir de `psql`, escribe `\q`.

---

## ⚙️ Variables de entorno

La aplicación lee estas variables, con un valor por defecto para ejecutarla desde el IDE:

| Variable | Por defecto | Para qué sirve |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Servidor de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `spring_ai_rag` | Nombre de la base de datos |
| `DB_USER` | `postgres` | Usuario |
| `DB_PASSWORD` | `root` | Contraseña |
| `OLLAMA_URL` | `http://localhost:11434` | Dirección de Ollama |
| `CHAT_MODEL` | `qwen3:0.6b` | Modelo de chat |
| `EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embeddings |

### Configuración del vector store

```properties
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.schema-name=public
spring.ai.vectorstore.pgvector.table-name=vector_store
spring.ai.vectorstore.pgvector.max-document-batch-size=10000
```

| Propiedad | Qué hace |
| --- | --- |
| `dimensions` | Tamaño del vector. Debe coincidir con el modelo de embeddings |
| `initialize-schema` | Crea la tabla al arrancar |
| `index-type=HNSW` | Crea un índice para que la búsqueda sea rápida |
| `distance-type=COSINE_DISTANCE` | Mide la cercanía con el ángulo entre vectores (operador `<=>`) |
| `max-document-batch-size` | Cuántos documentos se insertan por lote |

> 🔐 La contraseña `root` es solo para practicar. En un entorno real, no la escribas en el Dockerfile ni en el repositorio: pásala con `-e` o con un archivo `.env` que esté en el `.gitignore`.

---

## 📦 Publicar imágenes multiplataforma

Para que la imagen funcione en Windows/Linux (`amd64`) y en Mac con chip Apple (`arm64`).

### 1️⃣ Iniciar sesión

```bash
docker login
```

### 2️⃣ Respaldar los datos de la base (opcional)

```bash
docker exec -i postgresvg-rag pg_dump -U postgres -d spring_ai_rag > respaldo_pgvector.sql
```

Para que la imagen restaure ese respaldo al crearse, agrega esta línea al `Dockerfile.postgresvectordb`:

```dockerfile
COPY respaldo_pgvector.sql /docker-entrypoint-initdb.d/02-respaldo.sql
```

> ℹ️ PostgreSQL ejecuta los scripts de `/docker-entrypoint-initdb.d/` **solo la primera vez**, cuando el volumen está vacío.

### 3️⃣ Construir y publicar la base de datos

```bash
docker buildx build --push --platform linux/amd64,linux/arm64 \
  -f Dockerfile.postgresvectordb \
  -t sebashh26/spring_ai_rag_bd:v1 .
```

### 4️⃣ Construir y publicar la aplicación

```bash
docker buildx build --push --platform linux/amd64,linux/arm64 \
  -f Dockerfile.app \
  -t sebashh26/spring_ai_rag_app:v1 .
```

Los modelos de la aplicación están parametrizados con variables de entorno (`CHAT_MODEL` y `EMBEDDING_MODEL`), así que se pueden cambiar sin reconstruir la imagen.

---

## 🧯 Solución de problemas

### ❌ `Failed to obtain JDBC Connection`

El mensaje real está en la línea `Caused by:` del log de la aplicación. Revisa en orden:

- **El puerto 5432 está ocupado por otro PostgreSQL.** Si tienes un PostgreSQL instalado en Windows, puede estar atendiendo ese puerto. Compruébalo con `netstat -ano | findstr :5432`: debe aparecer un solo proceso. Detén el servicio de Windows o publica el contenedor en otro puerto (por ejemplo `5433:5432`).
- **La contraseña no coincide.** La de `POSTGRES_PASSWORD` y la de `DB_PASSWORD` deben ser iguales. PostgreSQL solo usa `POSTGRES_PASSWORD` la primera vez; si el volumen ya existe con otra clave, bórralo con `docker volume rm pgdata`.

### ❌ `UnknownHostException: postgres`

La aplicación busca un servidor que no existe en la red de Docker. `DB_HOST` debe tener **el nombre exacto del contenedor o del servicio** de la base de datos.

### ❌ El contenedor de PostgreSQL no arranca y habla de `/var/lib/postgresql`

Con PostgreSQL 18 o superior, el volumen se monta en `/var/lib/postgresql`, no en `/var/lib/postgresql/data`. Corrige la ruta en el `docker run` o en el Compose. Si el volumen ya se creó mal, bórralo para empezar de cero.

### ❌ `model 'nomic-embed-text' not found` (HTTP 404 de Ollama)

El modelo no está descargado. Ejecuta `ollama pull nomic-embed-text`. Si el nombre que sale en el error es otro, revisa que `EMBEDDING_MODEL` y `CHAT_MODEL` estén bien escritos.

### ❌ `model cannot be null or empty`

Una variable de modelo llegó **vacía**. Spring solo usa el valor por defecto cuando la variable **no existe**; si existe y está vacía, usa el texto vacío. Pasa el valor por defecto en el Compose con la sintaxis `${CHAT_MODEL:-qwen3:0.6b}`, o no declares la variable.

### ❌ La aplicación no conecta con Ollama

Dentro de un contenedor, `localhost` no es tu máquina. Usa `OLLAMA_URL=http://host.docker.internal:11434`. Para comprobar que un contenedor llega a tu Ollama:

```bash
docker run --rm curlimages/curl http://host.docker.internal:11434
```

Debe responder `Ollama is running`. Si no, define la variable de entorno de Windows `OLLAMA_HOST=0.0.0.0` y reinicia Ollama.

### ❌ `no configuration file provided: not found`

Docker Compose busca el archivo en la carpeta actual. Entra a la carpeta correcta o indica el archivo con `-f`:

```bash
docker compose -f docker-compose-ai-rag.yml up
```

### ❌ La respuesta llega vacía o tarda mucho

- `qwen3:0.6b` **razona** antes de responder, y en CPU eso puede tardar. Un modelo más grande o sin razonamiento mejora la calidad y el tiempo percibido.
- Comprueba que el prompt se está enviando al modelo con `.user(prompt)`.
- Revisa que el PDF se indexó: `select count(*) from vector_store;` debe ser mayor que 0.

### ❌ Un contenedor se levanta solo al abrir Docker Desktop

Tiene una política de reinicio. Quítala con:

```bash
docker update --restart=no <nombre-del-contenedor>
```

---

## 🧰 Comandos útiles de Docker

```bash
docker ps                     # contenedores en ejecución
docker ps -a                  # todos los contenedores
docker logs -f <contenedor>   # ver los logs en vivo
docker images                 # imágenes descargadas
docker stop <contenedor>      # detener
docker rm <contenedor>        # eliminar
docker volume ls              # ver volúmenes
docker volume rm pgdata       # borrar un volumen (¡borra los datos!)
```

---

⭐ Si te sirvió, deja una estrella en el repositorio.