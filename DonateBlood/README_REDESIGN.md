# Donate Blood — versión rediseñada

Proyecto Spring Boot + Thymeleaf + MongoDB.

## Cambios principales

- Landing page rediseñada casi por completo.
- UI responsive con Bootstrap 5.3.3.
- Iconos modernos con Lucide.
- Tipografías DM Sans + Manrope.
- Login completamente renovado.
- Botón de Google OAuth2 conectado al flujo real de Spring Security.
- Se corrigió el flujo OAuth2 para permitir la sesión temporal necesaria durante el callback y conservar JWT para las siguientes peticiones.
- Los usuarios que entran con Google se crean automáticamente como `ROLE_USER` si no existen.
- Se reutiliza el usuario existente por email.
- Se mantiene el índice único de `username` en MongoDB.
- La estructura del ZIP contiene una sola carpeta raíz del proyecto, sin carpetas duplicadas.

## Activar Google Login

Google no permite que una aplicación se autentique con credenciales ficticias. Debes crear un OAuth Client ID de tipo **Web application** en Google Cloud.

Configura como URI de redirección autorizada:

`http://localhost:8080/login/oauth2/code/google`

Después define estas variables de entorno antes de ejecutar:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI` (opcional; por defecto usa la URL anterior)

Ejemplo en Windows PowerShell:

`$env:GOOGLE_CLIENT_ID="TU_CLIENT_ID"`
`$env:GOOGLE_CLIENT_SECRET="TU_CLIENT_SECRET"`
`$env:GOOGLE_REDIRECT_URI="http://localhost:8080/login/oauth2/code/google"`

En producción, cambia la URI por tu dominio HTTPS y registra exactamente esa URI en Google Cloud.

## Ejecutar

Con Maven instalado:

`mvn spring-boot:run`

o:

`mvn clean package`

Nota: el entorno donde se realizó esta actualización no tenía acceso al repositorio Maven, por lo que no fue posible ejecutar una compilación completa aquí. Se revisaron los archivos modificados y la estructura del proyecto.
