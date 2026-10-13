# Limpieza del proyecto

- `target/` se excluye del proyecto fuente mediante `.gitignore`.
- Se eliminó la duplicación de CSS públicos: ahora `Información` y `Bancos` usan `css/public.css`.
- Se mantiene un único `templates/register/register.html`.
- La carpeta raíz del proyecto se normalizó a `DonateBlood`.
- Los archivos Java no se duplicaron por copia exacta en la versión revisada.
- `target/` se genera automáticamente al ejecutar Maven y no debe versionarse ni copiarse al ZIP fuente.
