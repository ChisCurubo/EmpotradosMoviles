# Documentación de Refactorización - Arquitectura por Componentes

## Resumen
Este proyecto ha sido refactorizado para seguir una arquitectura modular basada en componentes reutilizables, mejorando la escalabilidad, mantenibilidad y testeabilidad del código.

## Estructura de la Refactorización

### 📦 1. Managers (utils/)
**Propósito:** Centralizar la lógica de negocio compartida entre múltiples pantallas.

#### SessionManager
- **Responsabilidad:** Gestión completa de sesiones de usuario
- **Métodos clave:**
  - `saveSession(String email)`: Guarda la sesión del usuario
  - `getActiveUser()`: Obtiene el usuario activo
  - `isLoggedIn()`: Verifica si hay sesión activa
  - `logout()`: Cierra la sesión
  - `clearSession()`: Limpia completamente la sesión
- **Uso:**
  ```java
  SessionManager sessionManager = new SessionManager(context);
  sessionManager.saveSession("user@email.com");
  boolean isLoggedIn = sessionManager.isLoggedIn();
  ```

#### NavigationManager
- **Responsabilidad:** Centralizar la navegación entre Activities
- **Métodos clave:**
  - `navigateToMain()`: Navega a la pantalla principal
  - `navigateToStatistics()`: Navega a estadísticas
  - `navigateToLogin()`: Navega al login
  - `logoutAndNavigateToMain()`: Cierra sesión y navega a inicio
- **Ventajas:**
  - Un único punto de control para la navegación
  - Gestión automática de datos de usuario al navegar
  - Facilita cambios en el flujo de navegación

### 🛠️ 2. Helpers (utils/)
**Propósito:** Proporcionar funciones utilitarias reutilizables.

#### LanguageHelper
- **Responsabilidad:** Gestión de idiomas y traducciones
- **Funcionalidades:**
  - Conversión entre códigos y posiciones de idiomas
  - Obtención de frases rápidas por idioma
  - Lista de idiomas disponibles
- **Uso:**
  ```java
  String code = LanguageHelper.getLanguageCode(0); // "es"
  String[] phrases = LanguageHelper.getQuickPhrases("es");
  ```

#### ValidationHelper
- **Responsabilidad:** Validación de campos de formulario
- **Funcionalidades:**
  - Validación de email con formato correcto
  - Validación de contraseñas con requisitos mínimos
  - Validación de campos vacíos
  - Mensajes de error descriptivos
- **Uso:**
  ```java
  ValidationResult result = ValidationHelper.validateLoginFields(email, password);
  if (result.isValid()) {
      // Proceder con login
  } else {
      showError(result.getMessage());
  }
  ```

### 🏗️ 3. Base Classes (ui/base/)

#### BaseActivity
- **Responsabilidad:** Funcionalidad común para todas las Activities
- **Características:**
  - Inicialización automática de managers
  - Gestión de sesión centralizada
  - Métodos utilitarios comunes (showMessage, getCurrentUser, etc.)
  - Ciclo de vida de sesión (onSessionUpdated)
- **Ventajas:**
  - Reduce código duplicado
  - Garantiza comportamiento consistente
  - Facilita mantenimiento

### 🧩 4. Componentes Reutilizables (ui/components/)

#### BottomNavigationComponent
- **Propósito:** Barra de navegación inferior reutilizable
- **Características:**
  - Puede usarse en cualquier Activity mediante XML
  - Listeners personalizables
  - Gestión automática de navegación por defecto
  - Actualización dinámica de estado
- **Uso en XML:**
  ```xml
  <com.example.snap.ui.components.BottomNavigationComponent
      android:id="@+id/bottomNavigation"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"/>
  ```
- **Uso en Java:**
  ```java
  bottomNavigation.setNavigationListener(new NavigationListener() {
      @Override
      public void onTextoClicked() { /* lógica */ }
      // ...
  });
  ```

#### TranslationInputComponent
- **Propósito:** Componente para la entrada de traducciones
- **Responsabilidades:**
  - Gestión de spinners de idiomas
  - Manejo de texto de entrada
  - Chips de traducción rápida
  - Botones de limpiar e intercambiar
  - Indicadores de progreso
- **Interface TranslationInputListener:**
  ```java
  interface TranslationInputListener {
      void onTranslateRequested(String text, String sourceLang, String targetLang);
      void onLanguageChanged(int inputPosition, int outputPosition);
      void onClearRequested();
      void onSwapRequested();
  }
  ```

#### TranslationOutputComponent
- **Propósito:** Componente para mostrar resultados de traducción
- **Responsabilidades:**
  - Visualización del texto traducido
  - Animaciones de entrada
  - Botón de copiar al portapapeles
  - Botón de guardar favorito
  - Botón de reproducir audio
- **Interface TranslationOutputListener:**
  ```java
  interface TranslationOutputListener {
      void onSaveAsFavorite(String translatedText);
      void onPlayAudio(String translatedText);
  }
  ```

#### HistoryAdapter
- **Propósito:** Adapter reutilizable para mostrar historial
- **Características:**
  - Formato consistente de fechas
  - Click listeners opcionales
  - Actualización dinámica de datos
- **Uso:**
  ```java
  HistoryAdapter adapter = new HistoryAdapter(historyList);
  recyclerView.setAdapter(adapter);
  adapter.updateData(newHistoryList);
  ```

## 🎯 Activities Refactorizadas

### MainActivityRefactored
**Antes:** 332 líneas con lógica mezclada
**Después:** 210 líneas con responsabilidades claras

**Mejoras:**
- ✅ Separación de concerns mediante componentes
- ✅ Lógica de UI delegada a componentes especializados
- ✅ Uso de managers para sesión y navegación
- ✅ Código más legible y mantenible

**Estructura:**
```
MainActivityRefactored
├── TranslationInputComponent (entrada)
├── TranslationOutputComponent (salida)
├── BottomNavigationComponent (navegación)
└── Métodos de coordinación
```

### StatisticsActivityRefactored
**Antes:** 290 líneas con lógica acoplada
**Después:** 180 líneas con componentes reutilizables

**Mejoras:**
- ✅ Uso de HistoryAdapter reutilizable
- ✅ Navegación mediante NavigationManager
- ✅ Sesión gestionada por SessionManager
- ✅ Métodos específicos para cada sección de estadísticas

**Estructura:**
```
StatisticsActivityRefactored
├── HistoryAdapter (historial)
├── BottomNavigationComponent (navegación)
└── Métodos de visualización de estadísticas
```

### LoginActivityRefactored
**Antes:** 230 líneas con validaciones inline
**Después:** 175 líneas con validaciones centralizadas

**Mejoras:**
- ✅ Validaciones mediante ValidationHelper
- ✅ Sesión gestionada por SessionManager
- ✅ Navegación mediante NavigationManager
- ✅ Métodos pequeños con responsabilidad única
- ✅ Estados de carga bien definidos

**Estructura:**
```
LoginActivityRefactored
├── ValidationHelper (validaciones)
├── SessionManager (sesión)
├── NavigationManager (navegación)
└── Métodos de autenticación
```

## 📊 Beneficios de la Refactorización

### 1. Reutilización de Código
- **BottomNavigationComponent** se usa en todas las pantallas
- **SessionManager** y **NavigationManager** son compartidos
- **HistoryAdapter** puede usarse en múltiples contextos
- **ValidationHelper** centraliza todas las validaciones

### 2. Escalabilidad
- Agregar nuevas pantallas es más fácil (hereda de BaseActivity)
- Nuevos componentes pueden crearse siguiendo el mismo patrón
- Fácil agregar nuevas validaciones en ValidationHelper
- Nuevos idiomas se agregan solo en LanguageHelper

### 3. Mantenibilidad
- Código más limpio y organizado
- Responsabilidades claras para cada clase
- Fácil localizar y corregir bugs
- Cambios en un componente no afectan a otros

### 4. Testeabilidad
- Componentes independientes son más fáciles de testear
- Managers pueden ser mockeados en tests
- Validaciones centralizadas facilitan tests unitarios
- Lógica de negocio separada de la UI

## 🚀 Cómo Usar la Nueva Arquitectura

### Para crear una nueva Activity:

```java
public class NewActivity extends BaseActivity {
    
    private BottomNavigationComponent bottomNavigation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        
        // Managers ya disponibles: sessionManager, navigationManager, viewModel
        
        // Configurar navegación
        bottomNavigation = findViewById(R.id.bottomNavigation);
        setupNavigation();
        
        // Verificar sesión
        if (!isUserLoggedIn()) {
            navigationManager.navigateToLogin();
            return;
        }
    }
    
    private void setupNavigation() {
        bottomNavigation.setNavigationListener(/* ... */);
    }
}
```

### Para agregar un nuevo componente:

1. Crear clase que extiende de `LinearLayout` o `View`
2. Definir interface para listeners
3. Implementar método `initializeViews(View rootView)`
4. Exponer métodos públicos para configuración
5. Documentar uso y responsabilidades

## 📝 Recomendaciones

### Buenas Prácticas:
1. **Siempre usar managers** para sesión y navegación
2. **Validar inputs** con ValidationHelper
3. **Extender BaseActivity** para nuevas pantallas
4. **Crear componentes** para UI repetitiva
5. **Documentar** nuevos componentes y helpers

### Anti-Patrones a Evitar:
❌ No usar SharedPreferences directamente (usar SessionManager)
❌ No hacer navegación con Intents directos (usar NavigationManager)
❌ No duplicar validaciones (usar ValidationHelper)
❌ No repetir código de UI (crear componente reutilizable)

## 🔄 Migración desde Código Antiguo

### Paso 1: Cambiar imports
```java
// Antes
import androidx.appcompat.app.AppCompatActivity;

// Después
import com.example.snap.ui.base.BaseActivity;
```

### Paso 2: Cambiar clase base
```java
// Antes
public class MyActivity extends AppCompatActivity {

// Después
public class MyActivity extends BaseActivity {
```

### Paso 3: Reemplazar gestión de sesión
```java
// Antes
SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
String userId = prefs.getString("active_email", null);

// Después
String userId = getCurrentUser();
boolean isLoggedIn = isUserLoggedIn();
```

### Paso 4: Reemplazar navegación
```java
// Antes
Intent intent = new Intent(this, MainActivity.class);
intent.putExtra("USER_ID", userId);
startActivity(intent);

// Después
navigationManager.navigateToMain();
```

## 📦 Archivos Creados

### Managers
- `utils/SessionManager.java`
- `utils/NavigationManager.java`

### Helpers
- `utils/LanguageHelper.java`
- `utils/ValidationHelper.java`

### Base Classes
- `ui/base/BaseActivity.java`

### Componentes
- `ui/components/BottomNavigationComponent.java`
- `ui/components/TranslationInputComponent.java`
- `ui/components/TranslationOutputComponent.java`
- `ui/components/HistoryAdapter.java`

### Activities Refactorizadas
- `MainActivityRefactored.java`
- `StatisticsActivityRefactored.java`
- `LoginActivityRefactored.java`

## 🎓 Conclusión

Esta refactorización transforma el código de un monolito acoplado a una arquitectura modular y escalable. Los componentes reutilizables permiten desarrollo más rápido, menos bugs y código más mantenible.

**Próximos Pasos Sugeridos:**
1. Crear tests unitarios para managers y helpers
2. Agregar más componentes reutilizables (LoadingComponent, ErrorComponent)
3. Implementar ViewModel compartido entre pantallas
4. Considerar usar Dependency Injection (Dagger/Hilt)
