# 🚀 Guía de Desarrollo y Merge

Este proyecto ha sido refactorizado para soportar trabajo en paralelo en las pestañas de **Voz**, **Cámara** y **Texto**.

## 🏗 Arquitectura
La App usa una `MainActivity` contenedora con 3 pestañas (Fragments).

| Pestaña | Fragmento | Archivo Layout | Responsable |
| :--- | :--- | :--- | :--- |
| **Voz** | `VoiceFragment.java` | `fragment_voice.xml` | **Equipo Voz** (NO TOCAR) |
| **Cámara** | `CameraFragment.java` | `fragment_camera.xml` | **Equipo Cámara** |
| **Texto** | `TextFragment.java` | `fragment_text.xml` | **Equipo Texto** |

---

## ⚠️ Reglas de Oro para Merge
1.  **NO TOQUES `MainActivity.java`** salvo que sea estrictamente necesario para la navegación global.
2.  **NO TOQUES `VoiceFragment.java`**.
3.  **TRABAJA EXCLUSIVAMENTE** en tu Fragmento y tu Layout asignado.

## 🎨 Reuso de UI (Estilos)
Para que toda la app se vea igual, usa estos estilos en tus XML:

*   **Tarjetas (Cards)**: `style="@style/Widget.Practica.Card"`
    *   *Usa `@drawable/card_title_background` para el fondo del título.*
*   **Títulos de sección**: `style="@style/TextAppearance.Practica.SectionTitle"`
*   **Texto normal**: `style="@style/TextAppearance.Practica.Body"`
*   **Botones redondos**: `@drawable/record_button_halo_background`

## 🔄 Cómo Mergear
1.  Haz pull de `feature/traduccion-voz`.
2.  **Si eres Cámara**: Copia tu lógica dentro de `CameraFragment`.
3.  **Si eres Texto**: Copia tu lógica dentro de `TextFragment`.
4.  Resuelve conflictos solo si ocurren en `strings.xml` o `AndroidManifest.xml`.
