# Instituto Tecnológico de Costa Rica

## Ingeniería en Computación

### Compiladores e Intérpretes

**Semestre:** II, 2026

**Profesor:** [Allan Rodriguez Davila](https://tecdigital.tec.ac.cr/dotlrn/community-member?user_id=451189)

**Proyecto:** #2 — Análisis Semántico y Generación de Código Intermedio

**Fecha de entrega:** 9 de noviembre de 2026

### Estudiantes

| Estudiante               | Carné     |
| ------------------------ | ---------- |
| Heldyis Agüero Espinoza | 2023296812 |
| Alice Arias Salazar      | 2023104639 |

---

# Índice

1. [Descripción del problema](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#descripci%C3%B3n-del-problema)
2. [Objetivos](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#objetivos)
3. [Estado del proyecto](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#estado-del-proyecto)
4. [Herramientas y librerías utilizadas](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#herramientas-y-librer%C3%ADas-utilizadas)
5. [Estructura del proyecto](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#estructura-del-proyecto)
6. [Requisitos](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#requisitos)
7. [Gramática del lenguaje](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#gram%C3%A1tica-del-lenguaje)
8. [Características del lenguaje](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#caracter%C3%ADsticas-del-lenguaje)
9. [Análisis léxico](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#an%C3%A1lisis-l%C3%A9xico)
10. [Análisis sintáctico](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#an%C3%A1lisis-sint%C3%A1ctico)
11. [Análisis semántico](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#an%C3%A1lisis-sem%C3%A1ntico)
12. [Tabla de símbolos](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#tabla-de-s%C3%ADmbolos)
13. [Manejo de errores](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#manejo-de-errores)
14. [Código intermedio](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#c%C3%B3digo-intermedio)
15. [Compilación del proyecto](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#compilaci%C3%B3n-del-proyecto)
16. [Ejecución del proyecto](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#ejecuci%C3%B3n-del-proyecto)
17. [Archivos de prueba](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#archivos-de-prueba)
18. [Flujo de ejecución](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#flujo-de-ejecuci%C3%B3n)
19. [Resultado esperado](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#resultado-esperado)
20. [Decisiones de diseño](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#decisiones-de-dise%C3%B1o)
21. [Limitaciones](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#limitaciones)
22. [Archivos importantes](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#archivos-importantes)
23. [Documentación y bitácora](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#documentaci%C3%B3n-y-bit%C3%A1cora)
24. [Conclusión](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#conclusi%C3%B3n)

---

# Descripción del problema

El proyecto continúa el desarrollo de un compilador para un lenguaje de programación imperativo ligero, diseñado para realizar configuraciones básicas en dispositivos y sistemas embebidos.

La propuesta surge debido a que los dispositivos electrónicos y chips actuales pueden configurarse mediante lenguajes de programación cada vez más ligeros y potentes. Por esta razón, se plantea el desarrollo de un lenguaje propio que permita representar instrucciones, declaraciones, expresiones y estructuras de control de una manera sencilla.

El Proyecto #1 desarrolló el análisis léxico y el análisis sintáctico. En este segundo proyecto se conservan esas etapas, se corrigen los alcances del primero y se agregan dos etapas nuevas:

* **Análisis semántico:** comprueba que el programa tenga sentido, verificando el  **tipado explícito y fuerte** , las declaraciones, las funciones, los arreglos y el resto de las reglas del lenguaje.
* **Generación de código intermedio:** traduce el programa a **código de tres direcciones** y lo escribe en un archivo.

El sistema recibe un archivo fuente con extensión `.cmm` y ejecuta cuatro fases en una sola corrida: análisis léxico, análisis sintáctico, análisis semántico y generación de código intermedio. Al final  **indica si el archivo puede o no ser generado por la gramática** , tomando en cuenta la gramática, la sintaxis y la semántica.

Además, el sistema debe detectar los errores léxicos, sintácticos y semánticos, indicar la línea y la columna donde ocurren y **continuar con el análisis** utilizando mecanismos de recuperación de errores, entre ellos la técnica obligatoria de  **Recuperación en Modo Pánico** .

---

# Objetivos

## Objetivo general

Desarrollar un analizador semántico y un generador de código intermedio para un lenguaje de programación imperativo definido mediante una gramática BNF, utilizando JFlex y CUP, preservando y corrigiendo los alcances del Proyecto #1.

## Objetivos específicos

* Preservar y corregir el análisis léxico y sintáctico del Proyecto #1.
* Implementar un analizador semántico utilizando  **Java CUP** , con tipado explícito y fuerte.
* Construir una tabla de símbolos con ámbitos anidados.
* Detectar y reportar errores léxicos, sintácticos y semánticos, indicando línea y columna.
* Utilizar la técnica de **Recuperación en Modo Pánico** y permitir que el análisis continúe después de encontrar errores.
* Implementar técnicas adicionales de recuperación de errores:  **a nivel de frase** , **producción de errores** y  **corrección global** .
* Indicar si el archivo fuente puede o no ser generado por la gramática.
* Escribir en un archivo el **código intermedio de tres direcciones** equivalente al programa fuente.
* Probar el funcionamiento del sistema mediante archivos `.cmm`.

---

# Estado del proyecto

* [X] Análisis léxico, con reporte de caracteres y lexemas no válidos
* [X] Análisis sintáctico con recuperación en **modo pánico** (obligatoria)
* [X] Análisis semántico completo (tipado fuerte), con un código de regla por cada tipo de error
* [X] Tabla de símbolos con ámbitos anidados, 12 columnas y reporte en archivo
* [X] Veredicto final: «SÍ / NO puede ser generado por la gramática»
* [X] Recuperación  **a nivel de frase** , **producción de errores** y **corrección global**
* [X] Función `principal` en cualquier posición del archivo (exactamente una)
* [X] Fase 4 en `Main`: guardar el `.tac` y mostrarlo
* [X] Código intermedio: aritmética, potencia, asignaciones, comparaciones y operaciones lógicas
* [ ] Código intermedio: `read` y `write`
* [ ] Código intermedio: `++` y `--`
* [ ] Código intermedio: funciones (etiquetas, `param`, `call` y `return`)
* [ ] Código intermedio: `if`, `elif`, `else`, `while`, `for` y `break`
* [ ] Código intermedio: arreglos

---

# Herramientas y librerías utilizadas

## Java JDK 21.0.12

Java es el lenguaje utilizado para desarrollar el proyecto.

En este proyecto se utiliza **JDK 21.0.12** para:

* Compilar los archivos `.java`.
* Ejecutar el programa.
* Procesar archivos de entrada y salida.
* Trabajar con las clases generadas por JFlex y CUP.

El proyecto requiere como mínimo  **Java 11** .

---

## JFlex 1.9.1

**JFlex** es utilizado para generar el analizador léxico.

El archivo:

```text
Lexer.flex
```

contiene las reglas que permiten reconocer los diferentes elementos del lenguaje.

A partir de este archivo, JFlex genera:

```text
Lexer.java
```

---

## Java CUP 11b

**Java CUP** se utiliza para generar el analizador sintáctico y, en este proyecto, también el semántico.

El archivo:

```text
Parser.cup
```

contiene tres cosas:

* La **gramática** del lenguaje.
* Las  **reglas semánticas** , que validan los tipos y las declaraciones (en el bloque `parser code`).
* Las llamadas al  **generador de código intermedio** .

A partir de este archivo se generan principalmente:

```text
parser.java
sym.java
```

El archivo `parser.java` contiene el analizador generado por CUP, mientras que `sym.java` contiene las constantes asociadas a los símbolos y tokens del lenguaje.

---

## Java CUP Runtime

El archivo:

```text
java-cup-11b.jar
```

también proporciona las clases necesarias para ejecutar el parser generado.

Entre ellas se encuentran:

```java
java_cup.runtime.Symbol
java_cup.runtime.Scanner
```

`Symbol` representa los símbolos utilizados durante el análisis y transporta el tipo de token, el lexema, la línea y la columna. `Scanner` permite entregarle al parser una lista de tokens en lugar de un archivo, lo que se aprovecha en la corrección global de errores.

---

## Librerías estándar de Java

Los archivos `Main.java`, `TablaSimbolos.java` y `Generador.java` utilizan diferentes librerías incluidas en Java.

### `java.io`

Se utiliza para trabajar con archivos, entradas, salidas, lectores y escritores.

```java
File
FileInputStream
FileOutputStream
InputStreamReader
OutputStream
PrintStream
PrintWriter
Reader
```

Estas clases permiten leer el archivo fuente, generar los archivos de salida y manejar la entrada y salida del programa.

### `java.lang.reflect`

Se utiliza para acceder dinámicamente a las constantes definidas en:

```text
sym.java
```

### `java.nio.charset`

Se utiliza para especificar la codificación de caracteres. El proyecto utiliza:

```text
UTF-8
```

para permitir el manejo correcto de los caracteres especiales del lenguaje.

### `java.nio.file`

Se utiliza para trabajar con archivos y rutas del sistema, por ejemplo para guardar la tabla de símbolos y el código intermedio.

```java
Files
Path
Paths
```

### `java.util`

Se utiliza para manejar estructuras de datos como:

```java
ArrayList
HashMap
LinkedHashMap
LinkedHashSet
List
Map
Set
```

Estas estructuras almacenan los tokens, los errores, los símbolos, los ámbitos y las pilas utilizadas por el generador de código.

### `java.util.regex`

Se utiliza para trabajar con expresiones regulares mediante:

```java
Matcher
Pattern
```

Estas clases permiten procesar el texto de los errores para mostrarlos con formato.

---

# Estructura del proyecto

La estructura actual del proyecto es la siguiente:

```text
PY2-Analisis-Semantico-y-Generacion-Codigo-Intermedio-Compiladores/
│
├── .vscode/
│
├── Documentacion/
│   └── Documentación.docx
│
├── Programa/
│   │
│   ├── bin/
│   │
│   ├── codigo_intermedio/
│   │   └── (archivos .tac generados)
│   │
│   ├── librerias/
│   │   ├── java-cup-11b.jar
│   │   └── jflex-full-1.9.1.jar
│   │
│   ├── pruebas/
│   │   ├── ejemplo1.cmm
│   │   ├── ejemplo2.cmm
│   │   ├── prueba_semantica.cmm
│   │   ├── prueba_valida_p2.cmm
│   │   ├── prueba_recuperacion.cmm
│   │   ├── prueba_correccion_global.cmm
│   │   ├── prueba_sin_principal.cmm
│   │   ├── prueba_generador.cmm
│   │   ├── prueba_tac.cmm
│   │   └── tokens.txt
│   │
│   ├── src/
│   │   ├── ejecutables/
│   │   │   ├── build.bat
│   │   │   └── run.bat
│   │   │
│   │   ├── Atrib.java
│   │   ├── Generador.java
│   │   ├── Main.java
│   │   └── TablaSimbolos.java
│   │
│   ├── tabla_simbolos/
│   │   └── (reportes de la tabla de símbolos)
│   │
│   ├── Lexer.flex
│   └── Parser.cup
│
├── info.txt
└── README.md
```

## Descripción de los principales archivos

### `Lexer.flex`

Contiene las reglas del analizador léxico.

Es utilizado por JFlex para generar automáticamente `Lexer.java`.

### `Parser.cup`

Contiene la gramática del lenguaje, las reglas semánticas y las llamadas al generador de código.

Es utilizado por CUP para generar `parser.java` y `sym.java`. Define **cuándo** se valida y **cuándo** se genera una instrucción.

### `Main.java`

Es el punto de entrada del programa.

Se encarga de coordinar las cuatro fases, procesar el archivo fuente, generar el archivo de tokens, mostrar los errores y el resultado del análisis, aplicar la corrección global de errores y guardar los reportes.

### `TablaSimbolos.java`

Contiene la tabla de símbolos, los ámbitos y los símbolos.

Es un almacén:  **guarda y busca** , pero no decide qué es un error. Esas decisiones se toman en `Parser.cup`.

### `Atrib.java`

Representa un valor durante el análisis: su  **tipo** , su **lugar** (el nombre donde queda su resultado: una variable, un literal o un temporal) y su **valor** cuando es una constante entera.

### `Generador.java`

Contiene el generador de código intermedio.

Conoce el **formato** de cada instrucción, lleva la cuenta de los temporales y las etiquetas, y guarda el código en un archivo `.tac`. Define **cómo** se escribe el código.

### `build.bat`

Automatiza el proceso de compilación del proyecto.

Se encarga de:

1. Verificar las librerías.
2. Crear la carpeta `bin`.
3. Generar el parser mediante CUP.
4. Generar el lexer mediante JFlex.
5. Compilar los archivos Java.

### `run.bat`

Automatiza la ejecución del proyecto.

Solicita el nombre del archivo `.cmm`, ejecuta el analizador y genera el archivo:

```text
pruebas/tokens.txt
```

### `pruebas/`

Contiene los archivos utilizados para comprobar el funcionamiento del analizador.

### `bin/`

Contiene los archivos generados y compilados necesarios para ejecutar el proyecto.

### `tabla_simbolos/`

Contiene los reportes de la tabla de símbolos que se generan en cada ejecución.

### `codigo_intermedio/`

Contiene los archivos `.tac` con el código intermedio, generados únicamente cuando el programa no tiene errores.

### `Documentacion/`

Contiene la documentación externa del proyecto.

### `info.txt`

Contiene los datos de la entrega.

---

# Requisitos

Para ejecutar el proyecto se necesita:

* Windows.
* Java JDK 21.0.12 (como mínimo Java 11).
* JFlex 1.9.1.
* Java CUP 11b.
* Java CUP Runtime.
* PowerShell o CMD.
* Visual Studio Code u otro editor compatible.
* Una terminal con codificación **UTF-8** (el lenguaje utiliza símbolos especiales). `build.bat` ejecuta `chcp 65001` automáticamente.
* Una terminal  **ancha** , de unos 170 caracteres, para que las tablas de errores y de símbolos se vean completas.

Las librerías de JFlex y CUP utilizadas por el proyecto se encuentran dentro de:

```text
Programa/librerias/
```

---

# Gramática del lenguaje

La gramática del lenguaje está definida en:

```text
Programa/Parser.cup
```

La gramática utiliza una estructura basada en BNF y define las reglas necesarias para reconocer programas escritos en el lenguaje.

El símbolo inicial de la gramática es:

```text
programa
```

La estructura general del programa está compuesta por declaraciones globales y unidades de programa:

```text
programa
    ::= lista_globales unidades_programa
```

Las unidades del programa son **funciones** y la función  **`principal`** , que pueden aparecer en  **cualquier orden** . Que exista **exactamente una** `principal` se comprueba al final del análisis: si falta, se reporta el error `PRG-1`; si se repite, se reporta como nombre repetido.

La estructura de una función y del método principal es:

```text
int doble є: int n :э ¿:
    return n + n»
:?

void principal є: :э ¿:
    ...
:?
```

Los símbolos utilizados para los diferentes elementos del lenguaje son definidos mediante tokens y posteriormente utilizados por las producciones de la gramática.

---

# Características del lenguaje

La gramática actual permite trabajar con diferentes elementos de un lenguaje imperativo.

## Símbolos especiales

| Símbolo              | Significado                                                                    |
| --------------------- | ------------------------------------------------------------------------------ |
| `¿:`y `:?`       | Abrir y cerrar un bloque                                                       |
| `є:`y `:э`      | Abrir y cerrar un paréntesis                                                  |
| `ʃ:`y `:ʅ`      | Abrir y cerrar un corchete (índices de arreglos y operandos de comparaciones) |
| `»`                | Fin de sentencia                                                               |
| `Ͱ`                | Asignación                                                                    |
| `λ`,`θ`y `Σ` | Y lógico, O lógico y negación                                               |
| `<code>`            | `</code>`                                                                    |

## Tipos de datos

Se incluyen los siguientes tipos:

```text
int
float
bool
char
string
void
```

## Valores booleanos

El lenguaje permite utilizar:

```text
true
false
```

## Declaración de variables

Las variables se declaran utilizando la palabra reservada:

```text
val
```

Por ejemplo:

```text
val int a Ͱ 10»
val float f»
val int notasʃ:3:ʅ Ͱ ¿:90, 80, 70:?»
```

## Operadores aritméticos

El lenguaje incluye operadores para:

* Incremento.
* Suma.
* Decremento.
* Resta.
* Multiplicación.
* División entera.
* División decimal.
* Módulo.
* Potencia.

Entre los símbolos utilizados se encuentran:

```text
++
+
--
-
*
//
/
mod
pot
```

## Operadores relacionales

Se incluyen:

```text
<=
>=
==
!=
<
>
```

## Operadores lógicos

Se incluyen:

```text
λ    (y lógico)
θ    (o lógico)
Σ    (negación)
```

## Estructuras de control

El lenguaje incluye:

```text
if
elif
else
while
for
```

## Funciones

La gramática permite declarar funciones con tipo de retorno `int`, `float`, `char` o `bool`. El tipo `void` se utiliza únicamente en la función `principal`.

También permite:

```text
return
break
```

## Entrada y salida

El lenguaje incluye operaciones para:

```text
read
write
```

La instrucción `print` del Proyecto #1 fue eliminada: la salida se realiza con `write` y la entrada con `read`.

## Arreglos

Se permiten arreglos de tipo:

```text
int
float
```

y el acceso mediante los delimitadores definidos para índices. Los índices empiezan en  **0** .

## Comentarios

El lenguaje permite comentarios de una línea utilizando:

```text
|
```

y comentarios multilínea utilizando:

```text
¡
...
!
```

---

# Análisis léxico

El análisis léxico es realizado mediante  **JFlex** .

El archivo utilizado para definir las reglas léxicas es:

```text
Lexer.flex
```

JFlex analiza el archivo fuente carácter por carácter y agrupa los caracteres en unidades llamadas  **tokens** .

Cada token contiene información relacionada con:

* Tipo.
* Lexema.
* Línea.
* Columna.

El lexer utiliza los mecanismos de JFlex para mantener la posición actual dentro del archivo fuente.

---

## Tokens reconocidos

Entre los tokens definidos se encuentran:

### Operadores aritméticos

```text
INCREMENT
PLUS
DECREMENT
MINUS
MULTIPLY
ENTIREDIV
FLOATDIV
MOD
POT
```

### Operadores relacionales

```text
LTE
GTE
EQUAL
NEQ
LT
GT
```

### Operadores lógicos

```text
AND
OR
NOT
```

### Delimitadores

```text
OPBLOCK
CLBLOCK
PARENOP
PARENCL
OPBRACKET
CLBRACKET
TERMINATOR
ASIGN
COMA
```

### Tipos y palabras reservadas

```text
VAL
PRINCIPAL
INT
FLOAT
BOOL
CHAR
STRING
VOID
TRUE
FALSE
```

### Estructuras de control

```text
IF
ELIF
ELSE
WHILE
FOR
RETURN
BREAK
```

### Entrada y salida

```text
PRINT
READ
WRITE
```

El token `PRINT` sigue definido en el lexer, pero ya no se utiliza en la gramática; por eso CUP muestra una advertencia («terminal declarado pero no usado») al generar el parser. Es una advertencia esperada.

### Literales e identificadores

```text
LITERAL_INT
LITERAL_FLOAT
LITERAL_STRING
LITERAL_CHAR
ID
```

---

# Reglas léxicas

El analizador también verifica reglas específicas para los diferentes tipos de lexemas.

Entre los errores detectados se encuentran:

* Números enteros con ceros iniciales.
* Números decimales con ceros iniciales.
* Números decimales que terminan de forma incorrecta.
* Números decimales sin los dígitos necesarios después del punto.
* Identificadores que comienzan con un número.
* Identificadores que comienzan con `_`.
* Caracteres vacíos.
* Caracteres con más de un símbolo.
* Caracteres sin cerrar.
* Comillas simples aisladas.
* Cadenas sin cerrar.
* Comentarios multilínea sin cerrar.
* Caracteres desconocidos.

Cuando ocurre un error léxico, el sistema registra información sobre el error, omite el carácter y continúa con el análisis.

---

# Análisis sintáctico

El análisis sintáctico es realizado utilizando  **Java CUP** .

El archivo principal de definición de la gramática es:

```text
Parser.cup
```

CUP utiliza los tokens generados por el analizador léxico para verificar si la secuencia recibida cumple con las producciones de la gramática.

Durante esta etapa se verifica, entre otras cosas:

* Declaraciones.
* Expresiones.
* Asignaciones.
* Funciones.
* Parámetros.
* Estructuras condicionales.
* Ciclos.
* Retornos.
* Operaciones de entrada y salida.
* Arreglos.
* Método principal.
* Estructura general del programa.

---

# Precedencia de expresiones

La gramática separa las expresiones en diferentes niveles para establecer el orden de evaluación.

Se utilizan producciones como:

```text
expresion_mod
expresion_multiplicacion
expresion_suma
expresion_unaria
expresion_potencia
valor_aritmetico
```

Esta organización permite diferenciar operaciones con distintos niveles de precedencia. De mayor a menor:

```text
1. pot
2. +  -
3. *  /  //
4. mod
```

Por ejemplo, `a + b * 2` se evalúa como `(a + b) * 2`, porque en este lenguaje la suma y la resta tienen mayor precedencia que la multiplicación.

También se contempla el uso de operaciones unarias y valores agrupados con paréntesis.

---

# Análisis semántico

El análisis semántico se realiza con  **Java CUP** : las reglas se escriben en el bloque `parser code` de `Parser.cup` y se invocan desde las acciones de la gramática.

El lenguaje es de  **tipado explícito y fuerte** : `int` y `float` no se mezclan y no existen conversiones implícitas.

Cada error semántico lleva un **código de regla** (por ejemplo `ARI-3`), que se agrupa por  **tipo de validación** .

| Validación                | Códigos                       | Qué comprueba                                                                                                                                 |
| -------------------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **Nombres**          | `TS-1`a `TS-7`             | No repetir nombres en un ámbito; declarar antes de usar; una función no se usa como variable; un arreglo no se usa sin índice.              |
| **Aritmética**      | `ARI-2`a `ARI-7`           | Operandos `int`o `float`; sin mezclar tipos;`//`solo con `int`;`/`solo con `float`;`pot`con exponente positivo.                  |
| **Comparación**     | `REL-1`a `REL-3`           | `<`,`>`,`<=`y `>=`con dos `int`o dos `float`;`==`y `!=`con `int`,`float`,`bool`o `char`; los `string`no se comparan. |
| **Lógica**          | `LOG-1`,`LOG-2`            | `λ`,`θ`y `Σ`requieren operandos `bool`.                                                                                             |
| **Asignación**      | `ASG-1`,`ASG-2`            | El valor debe tener el tipo de la variable, en la declaración y en la asignación.                                                            |
| **Arreglos**         | `ARR-1`a `ARR-8`           | Tamaño `int`y mayor que cero; valores del tipo del arreglo; no más valores que el tamaño; índice `int`, dentro de rango y no negativo. |
| **Funciones**        | `FUN-1`a `FUN-4`,`FUN-7` | Cantidad y tipo de argumentos; no retornar `string`; parámetros sin repetir; al menos un `return`.                                        |
| **Return**           | `RET-1`,`RET-2`            | `principal`(void) no retorna valor; el valor de un `return`tiene el tipo de la función.                                                   |
| **Control**          | `CON-1`a `CON-5`           | Las condiciones de `if`,`elif`,`while`y `for`son `bool`; tipos de la cabecera del `for`;`break`solo dentro de un ciclo.          |
| **Incremento**       | `INC-1`                      | `++`y `--`solo con `int`o `float`.                                                                                                     |
| **Entrada y salida** | `ES-1`,`ES-2`              | `read`acepta `int`,`float`o `char`;`write`acepta `int`,`float`o `string`.                                                      |
| **Programa**         | `PRG-1`                      | El programa debe tener una función `principal`.                                                                                             |

## Reglas del lenguaje que conviene recordar

* Las funciones devuelven `int`, `float`, `char` o `bool` (**no** `string`); `void` solo existe en `principal`.
* Las funciones deben declararse **antes** de ser llamadas. Se permite la recursión.
* Los arreglos son de `int` o `float`.
* Una condición debe ser de tipo `bool`: `if є: x :э` es válido cuando `x` es `bool`, y es un error cuando `x` es `int`.
* Los corchetes de las comparaciones son  **opcionales** : se aceptan comparaciones con y sin `ʃ: :ʅ`.
* Un error ya reportado  **no se repite** : un valor con error se marca internamente y no provoca errores en cadena.

---

# Tabla de símbolos

Cada bloque abre un  **ámbito** , que es una tabla hija de su ámbito padre. La tabla se muestra como un árbol de ámbitos y como una tabla con las siguientes columnas:

```text
Nº · NOMBRE · CATEGORÍA · TIPO · ÁMBITO · BYTES · DIMENSIÓN · DIRECCIÓN · VALOR INICIAL · ACCESO · LÍNEA · COL
```

Por ejemplo:

```text
global  (12 símbolos)
├── funcion suma  (2 símbolos)
├── funcion esPositivo  (1 símbolo)
│   ├── if  (0 símbolos)
│   └── else  (0 símbolos)
└── procedimiento principal  (56 símbolos)
    ├── while  (1 símbolo)
    └── for  (1 símbolo)
        └── cuerpo for  (0 símbolos)
```

* **Categorías:** variable, arreglo, parámetro, función y procedimiento.
* **Dirección:** `global+n` (zona estática) o `pila+n` (registro de activación).
* **Acceso:** `público` si se declara en el ámbito global; `privado` dentro de una función o bloque.
* **Tamaños:** `int`, `float` y `string` ocupan 4 bytes; `bool` y `char`, 1 byte.

El programa pregunta si se desea ver la tabla en pantalla y **siempre** la guarda en:

```text
Programa/tabla_simbolos/<nombre>_tabla_simbolos.txt
```

---

# Manejo de errores

El proyecto contempla errores en las etapas léxica, sintáctica y semántica. Los errores  **no detienen el análisis** : se reportan todos y el programa continúa.

## Errores léxicos

Son detectados directamente por las reglas de `Lexer.flex`.

El sistema los muestra en una tabla con:

* Número.
* Línea.
* Columna.
* Descripción.

## Errores sintácticos

Son detectados por CUP cuando la secuencia de tokens no coincide con las producciones de la gramática.

Cada error se muestra en un recuadro con la línea de código, una marca `^`, el token encontrado, los tokens esperados y, cuando el error lo corrigió el propio parser, la **técnica de recuperación** utilizada.

## Errores semánticos

Se muestran  **una línea por error** , con su número, línea, columna, tipo de validación y descripción, y un resumen al final por tipo de validación:

```text
  Nº    LÍNEA   COL  VALIDACIÓN      DESCRIPCIÓN
  ─────────────────────────────────────────────────────────────────────
  01        7    31  Funciones       'x22' ya fue declarado en este ámbito (línea 7)
  02       17    21  Aritmética      no se pueden mezclar 'int' y 'float' con el operador '+' (tipado fuerte)
  ─────────────────────────────────────────────────────────────────────
  Por validación: Funciones 1   Aritmética 1
```

---

## Técnicas de recuperación de errores

| Técnica                                 | Qué hace                                                                                                                                                                                                              | Dónde está                                                  |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| **Modo pánico** *(obligatoria)* | Ante un error descarta tokens hasta un punto seguro, como un `»`, y continúa.                                                                                                                                      | Reglas con `error`en `Parser.cup`                         |
| **Recuperación a nivel de frase** | **Inserta**el token que falta (`»`,`:э`o `:?`), reporta el error exacto y sigue sin descartar código.                                                                                                   | Reglas `fin_o_falta`,`cierra_paren`y `cierra_bloque_fn` |
| **Producción de errores**         | La gramática reconoce a propósito errores comunes para dar un mensaje preciso: variable sin tipo, arreglo de `bool`,`char`o `string`, parámetro sin tipo,`read`o `write`sin paréntesis.                  | Alternativas de error en `Parser.cup`                       |
| **Corrección global**             | Busca el**menor número de ediciones**(eliminar, insertar o reemplazar un token) que dejan el archivo con menos errores, probando cada edición con un nuevo análisis completo. Se muestra como una sugerencia. | `Main.java`                                                 |

La recuperación a nivel de frase solo se aplica donde no hay ambigüedad, por ejemplo antes de un bloque `¿:`. Dentro de una expresión o en bloques anidados el parser no puede saber si un cierre falta o si la construcción continúa, y CUP reportaría conflictos; en esos casos se utiliza el modo pánico.

## Veredicto final

Al terminar, el programa indica si el archivo puede o no ser generado por la gramática:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│   ARCHIVO CORRECTO                                                       │
│ El archivo SÍ puede ser generado por la gramática.                       │
│ No se encontraron errores léxicos, sintácticos ni semánticos.            │
└──────────────────────────────────────────────────────────────────────────┘
```

o, cuando hay errores:

```text
│   ARCHIVO CON ERRORES                                                    │
│ El archivo NO puede ser generado por la gramática.                       │
```

---

# Código intermedio

El código intermedio es  **código de tres direcciones** : cada instrucción menciona como máximo tres nombres, por ejemplo `t1 = a + b`.

Las expresiones largas se descomponen en pasos utilizando **temporales** (`t1`, `t2`…), y los saltos utilizan **etiquetas** (`L1`, `L2`…).

El código se guarda en:

```text
Programa/codigo_intermedio/<nombre>.tac
```

> **Si el archivo tiene errores no se genera ni se guarda código intermedio.**

## Formato

| Instrucción                | Significado                                                      | Estado        |
| --------------------------- | ---------------------------------------------------------------- | ------------- |
| `x = y`                   | Asignación                                                      | Implementado  |
| `t = a op b`              | Operación binaria:`+ - * / // mod pot < > <= >= == != and or` | Implementado  |
| `t = not a`               | Negación                                                        | Implementado  |
| `x = x + 1`/`x = x - 1` | `++`y `--`                                                   | En desarrollo |
| `read x`/`write x`      | Entrada y salida                                                 | En desarrollo |
| `L1:`                     | Etiqueta                                                         | En desarrollo |
| `goto L1`                 | Salto incondicional                                              | En desarrollo |
| `ifFalse t goto L1`       | Salta si `t`es falso                                           | En desarrollo |
| `nombre:`                 | Inicio de una función                                           | En desarrollo |
| `param x`                 | Argumento de la próxima llamada                                 | En desarrollo |
| `t = call f, n`           | Llamada a `f`con `n`argumentos; el resultado queda en `t`  | En desarrollo |
| `return`/`return x`     | Retorno, sin valor o con valor                                   | En desarrollo |
| `t = a[i]`/`a[i] = x`   | Lectura y escritura de un elemento de arreglo                    | En desarrollo |

## Ejemplo con lo implementado

Programa:

```text
void principal є: :э ¿:
    val int a Ͱ 1»
    val int b Ͱ 2»
    val int c Ͱ 0»
    c Ͱ a + b * 2»
    c Ͱ a mod b»
:?
```

Código intermedio:

```text
a = 1
b = 2
c = 0
t1 = a + b
t2 = t1 * 2
c = t2
t3 = a mod b
c = t3
```

## Ejemplo con funciones y ciclos

Este ejemplo muestra el resultado esperado cuando estén completos los puntos pendientes del código intermedio.

Programa:

```text
int doble є: int n :э ¿:
    return n + n»
:?

void principal є: :э ¿:
    val int i Ͱ 0»
    while є: ʃ:i:ʅ < ʃ:3:ʅ :э ¿:
        i++
    :?
    write є: doble є: i :э :э»
:?
```

Código intermedio:

```text
goto principal
doble:
t1 = n + n
return t1
principal:
i = 0
L1:
t2 = i < 3
ifFalse t2 goto L2
i = i + 1
goto L1
L2:
param i
t3 = call doble, 1
write t3
return
```

## Cómo se genera

1. El parser **valida** los tipos (en `Parser.cup`).
2. Solo si la operación no tiene error, le pide al `Generador` la instrucción.
3. El resultado de cada operación recibe un **lugar** (un temporal) que utiliza la siguiente instrucción.
4. `Main` guarda el texto en `Programa/codigo_intermedio/<nombre>.tac`.

## Convenciones

* **Parámetros:** quien llama escribe un `param` por argumento y luego el `call`; la función utiliza los nombres de sus parámetros.
* **`goto principal`:** antes de la primera función se salta a `principal`, de modo que las variables globales se inicializan primero y la ejecución empieza en `main`.
* **`return` implícito:** `principal`, la única función `void`, termina con un `return` automático.
* **Sin cortocircuito:** `and` y `or` evalúan siempre los dos operandos y guardan el resultado en un temporal.
* **Ciclos `for`:** el incremento se escribe después del cuerpo, aunque en el código fuente aparezca antes.

---

# Compilación del proyecto

Para compilar el proyecto se debe abrir una terminal en la carpeta:

```text
Programa
```

Por ejemplo:

```powershell
cd "C:\Users\arias\OneDrive\Desktop\PY2-Analisis-Semantico-y-Generacion-Codigo-Intermedio-Compiladores\Programa"
```

Luego se ejecuta el archivo:

```text
src\ejecutables\build.bat
```

mediante:

```powershell
.\src\ejecutables\build.bat
```

El proceso realiza automáticamente las siguientes acciones:

1. Verifica la existencia de JFlex.
2. Verifica la existencia de CUP.
3. Prepara la carpeta `bin`.
4. Genera `parser.java`.
5. Genera `sym.java`.
6. Genera `Lexer.java`.
7. Compila los archivos Java (`Main`, `TablaSimbolos`, `Atrib`, `Generador`, `Lexer`, `parser` y `sym`).
8. Almacena los archivos generados y compilados en `bin`.

El proceso debe terminar con el mensaje **«COMPILACION COMPLETADA»** y CUP debe informar  **`0 conflicts detected`** .

> Cada vez que se modifique `Parser.cup`, `Lexer.flex` o un archivo `.java` se debe volver a ejecutar `build.bat`. Si se agrega un archivo `.java` nuevo en `src/`, también se debe agregar a la lista del `javac` en `build.bat`.

### Compilación manual (Linux o macOS)

```bash
cd Programa
mkdir -p bin
java -cp librerias/java-cup-11b.jar java_cup.Main -destdir bin -parser parser -symbols sym Parser.cup
java -jar librerias/jflex-full-1.9.1.jar --encoding utf-8 -d bin Lexer.flex
javac -encoding UTF-8 -cp "librerias/java-cup-11b.jar:bin" -d bin src/*.java bin/Lexer.java bin/parser.java bin/sym.java
```

---

# Ejecución del proyecto

Una vez compilado correctamente, se puede ejecutar:

```powershell
.\src\ejecutables\run.bat
```

El programa solicita el nombre del archivo fuente.

Los archivos fuente utilizados para las pruebas se encuentran en:

```text
Programa/pruebas/
```

Por ejemplo:

```text
prueba_semantica.cmm
```

o:

```text
prueba_generador.cmm
```

El programa agrega automáticamente la extensión `.cmm` cuando es necesario.

---

# Ejecución directa

También es posible ejecutar el programa directamente desde la carpeta `Programa`.

La estructura general del comando es:

```powershell
java -cp "bin;librerias\java-cup-11b.jar" Main pruebas\archivo.cmm pruebas\tokens.txt
```

Por ejemplo:

```powershell
java -cp "bin;librerias\java-cup-11b.jar" Main pruebas\prueba_generador.cmm pruebas\tokens.txt
```

El primer argumento corresponde al archivo fuente y el segundo al archivo donde se almacenan los tokens.

Al terminar el análisis, el programa hace dos preguntas interactivas (`s` o `n`):

* **¿Desea ver el código intermedio?**
* **¿Desea ver la tabla de símbolos?**

---

# Archivos de prueba

Los archivos de prueba se encuentran en:

```text
Programa/pruebas/
```

La siguiente tabla indica cuántos errores debe reportar cada archivo:

| Archivo                          | Léxicos | Sintácticos | Semánticos | Qué comprueba                                                                      |
| -------------------------------- | :------: | :----------: | :---------: | ----------------------------------------------------------------------------------- |
| `ejemplo1.cmm`                 |    0    |      0      |     35     | Ejemplo del profesor, con los errores semánticos marcados                          |
| `ejemplo2.cmm`                 |    0    |      0      |     24     | Ejemplo del profesor, con los errores semánticos marcados                          |
| `prueba_semantica.cmm`         |    0    |      2      |     51     | Todas las reglas semánticas, con casos válidos e inválidos                       |
| `prueba_valida_p2.cmm`         |    0    |      0      |      0      | Programa grande y completamente válido                                             |
| `prueba_recuperacion.cmm`      |    1    |      11      |      2      | Recuperación a nivel de frase, producción de errores y posición de `principal` |
| `prueba_correccion_global.cmm` |    0    |      3      |      0      | Corrección global de errores                                                       |
| `prueba_sin_principal.cmm`     |    0    |      0      |      1      | Programa sin función `principal`(`PRG-1`)                                      |
| `prueba_generador.cmm`         |    0    |      0      |      0      | Programa válido para probar el código intermedio                                  |
| `prueba_tac.cmm`               |    0    |      0      |      0      | Programa corto para probar el código intermedio                                    |

En los archivos de prueba, el texto que aparece después de `|` es un comentario que indica el resultado esperado de cada línea (`OK` o `ERROR`).

## Comprobar que nada se rompió

Después de cualquier cambio se deben ejecutar los archivos anteriores y verificar que los conteos coincidan con la tabla y que CUP informe  **`0 conflicts detected`** .

---

# Archivo de tokens

Durante la ejecución se genera:

```text
Programa/pruebas/tokens.txt
```

Este archivo contiene los tokens reconocidos durante el análisis léxico, con su línea, columna y lexema.

El sistema identifica los tokens utilizando las constantes generadas en:

```text
sym.java
```

---

# Flujo de ejecución

El funcionamiento general del proyecto puede representarse de la siguiente manera:

```text
                  ARCHIVO .CMM
                       │
                       ▼
                ┌─────────────┐
                │    JFlex    │
                │    Lexer    │
                └──────┬──────┘
                       │
                       ▼
                TOKENS + LEXEMAS ─────────────► tokens.txt
                       │
                       ▼
                ┌─────────────┐
                │     CUP     │
                │    Parser   │
                └──────┬──────┘
                       │
          ┌────────────┴────────────┐
          │  Análisis sintáctico    │
          │  Análisis semántico     │──────────► tabla_simbolos/
          │  (tipado fuerte)        │
          └────────────┬────────────┘
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
        Sin errores           Con errores
             │                   │
             ▼                   ▼
    ┌────────────────┐    Recuperación de errores
    │   Generador    │    (pánico, frase, producción
    │ código 3 dir.  │     y corrección global)
    └───────┬────────┘            │
            │                     ▼
            ▼              "NO puede ser generado
   codigo_intermedio/        por la gramática"
   <nombre>.tac
   "SÍ puede ser generado
    por la gramática"
```

---

# Flujo del programa

El proceso completo se realiza de la siguiente manera:

### 1. Selección del archivo

El usuario proporciona un archivo fuente con extensión:

```text
.cmm
```

### 2. Análisis léxico

JFlex procesa el contenido del archivo y reconoce los diferentes elementos del lenguaje.

### 3. Generación de tokens

Cada elemento reconocido se convierte en un token y se registra su lexema y posición en:

```text
tokens.txt
```

### 4. Análisis sintáctico

Los tokens son procesados por el parser generado mediante CUP, que comprueba que la secuencia cumpla con las producciones definidas en `Parser.cup`.

### 5. Análisis semántico

Mientras el parser reconoce las construcciones, las acciones de la gramática construyen la tabla de símbolos y validan los tipos, las declaraciones, las funciones y los arreglos.

### 6. Manejo de errores

Si se encuentra un error, se reporta la información correspondiente y se continúa el análisis utilizando los mecanismos de recuperación implementados. Si quedaron errores sintácticos que el modo pánico tuvo que resolver, se muestra además una sugerencia de  **corrección global** .

### 7. Generación de código intermedio

Si el archivo no tiene errores, el generador escribe el código de tres direcciones en:

```text
Programa/codigo_intermedio/<nombre>.tac
```

### 8. Resultado

Finalmente se informa si el archivo puede o no ser generado por la gramática, se muestran los conteos de errores por fase y el tiempo de ejecución.

---

# Resultado esperado

El programa muestra una sección por cada fase:

```text
FASE 1 · ANÁLISIS LÉXICO
FASE 2 · ANÁLISIS SINTÁCTICO
FASE 3 · ANÁLISIS SEMÁNTICO
FASE 4 · CÓDIGO INTERMEDIO
RESULTADO FINAL
```

En el **análisis léxico** se indican los tokens reconocidos y los errores léxicos encontrados.

En el **análisis sintáctico** se determina si el programa cumple con la gramática. En caso de existir errores, se muestra información como:

```text
Línea
Columna
Se encontró
Se esperaba
Técnica de recuperación
```

En el **análisis semántico** se muestran la cantidad de ámbitos y de símbolos declarados, y los errores semánticos con su tipo de validación.

En la **generación de código intermedio** se indican la cantidad de instrucciones y el archivo donde se guardó el código, o un aviso de que no se generó código porque el archivo tiene errores.

En el **resultado final** se muestran los conteos de errores por fase, el tiempo de ejecución y el veredicto sobre si el archivo puede o no ser generado por la gramática.

Los archivos que se generan en cada ejecución son:

| Archivo                         | Ubicación                      | Contenido                             |
| ------------------------------- | ------------------------------- | ------------------------------------- |
| `tokens.txt`                  | `Programa/pruebas/`           | Tokens reconocidos                    |
| `<nombre>_tabla_simbolos.txt` | `Programa/tabla_simbolos/`    | Tabla de símbolos completa           |
| `<nombre>.tac`                | `Programa/codigo_intermedio/` | Código intermedio (solo sin errores) |

---

# Decisiones de diseño

| Decisión                                                                              | Motivo                                                                                                                                   |
| -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Las reglas semánticas viven en `Parser.cup`** , no en la tabla de símbolos | Un solo lugar para las reglas.`TablaSimbolos`solo guarda y busca.                                                                      |
| **El generador está en `Generador.java`**                                     | Separa el**cuándo**(el parser, que valida) del**cómo**(el generador, que escribe). Con un error de tipo no se genera nada. |
| **Códigos de regla en cada error**(`ARI-3`,`ARR-6`…)                       | Cada error se puede relacionar directamente con la documentación.                                                                       |
| **Una línea por error semántico**                                              | Es más fácil de leer y de comparar con el archivo fuente.                                                                              |
| **`write`acepta `int`,`float`y `string`**                                | Sigue los ejemplos del profesor.                                                                                                         |
| **`/`solo con `float`y `//`solo con `int`**                              | Tipado fuerte: cada división tiene su tipo.                                                                                             |
| **Corchetes opcionales en las comparaciones**                                    | Se aceptan comparaciones con y sin `ʃ: :ʅ`.                                                                                          |
| **`principal`en cualquier posición**                                          | El enunciado exige una sola `principal`, pero no dice que deba ser la última.                                                         |
| **El código intermedio se genera solo sin errores**                             | Un programa con errores no tiene un significado que traducir.                                                                            |
| **`and`y `or`sin cortocircuito**                                             | El enunciado no lo pide y el código intermedio queda más corto y más fácil de explicar.                                              |

---

# Limitaciones

* **No hay manejo de clases** (punto extra de clases).
* El código intermedio  **no se optimiza** .
* La recuperación a nivel de frase **no cubre** los paréntesis dentro de expresiones, los corchetes de los índices ni los bloques anidados, porque en esos casos CUP reporta conflictos.
* La **corrección global** es una sugerencia: prueba una edición por error y no inventa identificadores ni números.
* Las funciones deben estar declaradas **antes** de su primera llamada.
* Algunas instrucciones del código intermedio siguen en desarrollo (ver [Estado del proyecto](https://claude.ai/chat/f935065b-4f27-4a31-b468-9d40babbcf93#estado-del-proyecto)).

---

# Archivos importantes

| Archivo                         | Función                                                     |
| ------------------------------- | ------------------------------------------------------------ |
| `Lexer.flex`                  | Definición del analizador léxico                           |
| `Parser.cup`                  | Gramática, reglas semánticas y llamadas al generador       |
| `Main.java`                   | Punto de entrada, coordinación de las fases y recuperación |
| `TablaSimbolos.java`          | Tabla de símbolos, ámbitos y reporte                       |
| `Atrib.java`                  | Tipo, lugar y valor de cada expresión                       |
| `Generador.java`              | Generación y almacenamiento del código intermedio          |
| `build.bat`                   | Compilación automática                                     |
| `run.bat`                     | Ejecución automática                                       |
| `java-cup-11b.jar`            | CUP y runtime                                                |
| `jflex-full-1.9.1.jar`        | JFlex                                                        |
| `prueba_semantica.cmm`        | Prueba de todas las reglas semánticas                       |
| `prueba_recuperacion.cmm`     | Prueba de la recuperación de errores                        |
| `prueba_generador.cmm`        | Prueba del código intermedio                                |
| `tokens.txt`                  | Archivo generado con los tokens                              |
| `<nombre>_tabla_simbolos.txt` | Archivo generado con la tabla de símbolos                   |
| `<nombre>.tac`                | Archivo generado con el código intermedio                   |

---

# Documentación y bitácora

* **Documentación externa:** `Documentacion/Documentación.pdf`.
* **Documentación interna:** comentarios en cada parte del código con sus  **entradas, salidas, restricciones y objetivo** .
* **Bitácora de git** , con el commit de cada usuario y su comentario:

```powershell
git log --pretty=format:"%h | %an | %ad | %s" --date=short > bitacora.txt
```

# Conclusión

El proyecto completa las etapas centrales de un compilador para un lenguaje imperativo propio.

Sobre el análisis léxico y sintáctico del primer proyecto, se agregó un análisis semántico de tipado fuerte, una tabla de símbolos con ámbitos y un generador de código intermedio de tres direcciones. La separación entre `Lexer.flex`, `Parser.cup`, `Generador.java` y `Main.java` organiza el proyecto en componentes con una responsabilidad clara: el parser valida, el generador escribe y `Main` coordina.

Además, la implementación de varias técnicas de recuperación de errores permite continuar el análisis después de encontrar errores y entregar información útil sobre los problemas del código fuente, y el veredicto final indica si el archivo puede o no ser generado por la gramática.
