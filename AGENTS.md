# AGENTS

Este archivo define reglas de trabajo persistentes para este proyecto.

## Codificacion

- Todo archivo de texto debe mantenerse en `UTF-8`.
- No introducir nunca `mojibake`.
- No reemplazar caracteres validos en espanol por versiones rotas.
- Si se toca un archivo con texto corrupto, corregirlo en esa misma edicion cuando sea razonable.

## Texto y contenido

- Mantener textos en espanol correctamente escritos.
- Si un archivo visible para usuario admite `UTF-8`, conservar tildes y la letra `n con tilde`.
- Si un archivo tecnico o de soporte da problemas de codificacion, preferir ASCII limpio antes que texto corrupto.
- Los textos visibles para usuario deben revisarse antes de cerrar una tarea.

## Firebase y datos

- Mantener sincronizados codigo, reglas y documentacion.
- Si se crea una nueva coleccion o campo en Firestore, actualizar tambien `FIREBASE_SETUP.md`.
- Si cambia la logica de acceso, revisar `firestore.rules`.

## Flujo visual

- Respetar la logica actual de la app:
  - `Pisos`
  - `Piso` con pestanas internas
  - `Perfil`
- Mantener CTA principal abajo centrado cuando la pantalla siga el patron tipo Tricount ya introducido.

## Antes de dar una tarea por cerrada

- Revisar textos visibles.
- Revisar que no haya referencias rotas por ids nuevos.
- Revisar que la documentacion relacionada quede actualizada.
