import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java_cup.runtime.Symbol;

/*
 * Descripcion:
 * Programa principal del compilador. Ejecuta las cuatro fases (lexica, sintactica,
 * semantica y codigo intermedio) y muestra los resultados.
 *
 * Entrada:
 * El archivo .cmm a analizar y, si se quiere, el archivo donde guardar los tokens.
 *
 * Salida:
 * Resultados y errores en la consola. Tokens, tabla de simbolos y codigo
 * intermedio guardados en la carpeta salida.
 *
 * Restricciones:
 * El archivo .cmm debe estar en UTF-8.
 */
public class Main {

    /* ============================================================
       CONFIGURACION
       ============================================================ */

    /* Ancho de los titulos, cuadros y textos de la consola */
    private static final int ANCHO = 76;

    /* Errores de la forma "Error léxico en línea 3, columna 5: detalle" (sirve para léxicos y semánticos) */
    private static final Pattern PATRON_ERROR = Pattern.compile("Error \\S+ en línea (\\d+), columna (\\d+): (.*)");

    /* Los errores semánticos empiezan con su validación: "[Aritmética] detalle" (grupo 1 = validación, grupo 2 = detalle) */
    private static final Pattern PATRON_VALIDACION = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");

    /* Las etiquetas de if, while y for tienen "$if$N$", "$while$N$" o "$for$N$" en el nombre */
    private static final Pattern PATRON_ETIQUETA_ESTRUCTURA = Pattern.compile("\\$(if|while|for)\\$\\d+\\$");

    /* Si el parser se rinde, su resultado cuenta como si tuviera muchisimos errores */
    private static final int PENALIZACION_INTERRUMPIDO = 1_000_000;

    /* Cuantas ediciones prueba como maximo la correccion global */
    private static final int MAXIMO_EDICIONES = 6;


    /* ============================================================
       COLORES
       ============================================================ */

    private static final String NEGRITA = "1";
    private static final String SIN_COLOR = "0";
    private static final String GRIS = "90";
    private static final String BLANCO = "1;37";
    private static final String ROJO = "1;31";
    private static final String ROJO_SUAVE = "31";
    private static final String VERDE = "1;32";
    private static final String VERDE_SUAVE = "32";
    private static final String AMARILLO = "1;33";
    private static final String AMARILLO_SUAVE = "33";
    private static final String CIAN = "1;36";
    private static final String CIAN_SUAVE = "36";
    private static final String MAGENTA_SUAVE = "35";
    private static final String AZUL = "34;1";

    /* Los colores se apagan definiendo la variable de entorno NO_COLOR (por ejemplo: set NO_COLOR=1) */
    private static final boolean USAR_COLORES = detectarColores();

    /*
     * Descripcion: Revisa si la consola puede mostrar colores.
     * Entrada: Ninguna.
     * Salida: true si se pueden usar colores.
     * Restricciones: Se apagan si existe la variable NO_COLOR.
     */
    private static boolean detectarColores() {

        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        String sistema = System.getProperty("os.name", "").toLowerCase();

        if (sistema.contains("win")) {
            activarColoresEnWindows();
            return true;
        }

        String terminal = System.getenv("TERM");
        return terminal != null && !terminal.equals("dumb");
    }

    /*
     * Descripcion: Prepara la consola de Windows para los colores.
     * Entrada: Ninguna.
     * Salida: Ninguna.
     * Restricciones: Si falla, solo no se ven colores.
     */
    private static void activarColoresEnWindows() {
        try {
            new ProcessBuilder("cmd", "/c", "rem").inheritIO().start().waitFor();
        } catch (Exception e) {
            /* Si falla, simplemente no se ven colores */
        }
    }

    /*
     * Descripcion: Le pone color a un texto.
     * Entrada: Color y texto.
     * Salida: El texto con color.
     * Restricciones: Si no hay colores, devuelve el texto igual.
     */
    private static String pintar(String color, String texto) {
        return USAR_COLORES ? "\u001B[" + color + "m" + texto + "\u001B[0m" : texto;
    }


    /* ============================================================
       NOMBRES DE LOS TOKENS
       ============================================================ */

    /* Nombre interno del token -> como se escribe en el lenguaje */
    private static final Map<String, String> TEXTO_DEL_TOKEN = new HashMap<>();

    static {
        TEXTO_DEL_TOKEN.put("PLUS", "+");
        TEXTO_DEL_TOKEN.put("MINUS", "-");
        TEXTO_DEL_TOKEN.put("MULTIPLY", "*");
        TEXTO_DEL_TOKEN.put("ENTIREDIV", "//");
        TEXTO_DEL_TOKEN.put("FLOATDIV", "/");
        TEXTO_DEL_TOKEN.put("MOD", "mod");
        TEXTO_DEL_TOKEN.put("POT", "pot");
        TEXTO_DEL_TOKEN.put("INCREMENT", "++");
        TEXTO_DEL_TOKEN.put("DECREMENT", "--");
        TEXTO_DEL_TOKEN.put("LTE", "<=");
        TEXTO_DEL_TOKEN.put("GTE", ">=");
        TEXTO_DEL_TOKEN.put("EQUAL", "==");
        TEXTO_DEL_TOKEN.put("NEQ", "!=");
        TEXTO_DEL_TOKEN.put("LT", "<");
        TEXTO_DEL_TOKEN.put("GT", ">");
        TEXTO_DEL_TOKEN.put("AND", "λ");
        TEXTO_DEL_TOKEN.put("OR", "θ");
        TEXTO_DEL_TOKEN.put("NOT", "Σ");
        TEXTO_DEL_TOKEN.put("OPBLOCK", "¿:");
        TEXTO_DEL_TOKEN.put("CLBLOCK", ":?");
        TEXTO_DEL_TOKEN.put("PARENOP", "є:");
        TEXTO_DEL_TOKEN.put("PARENCL", ":э");
        TEXTO_DEL_TOKEN.put("OPBRACKET", "ʃ:");
        TEXTO_DEL_TOKEN.put("CLBRACKET", ":ʅ");
        TEXTO_DEL_TOKEN.put("TERMINATOR", "»");
        TEXTO_DEL_TOKEN.put("ASIGN", "Ͱ");
        TEXTO_DEL_TOKEN.put("COMA", ",");
        TEXTO_DEL_TOKEN.put("ID", "identificador");
        TEXTO_DEL_TOKEN.put("LITERAL_INT", "número entero");
        TEXTO_DEL_TOKEN.put("LITERAL_FLOAT", "número decimal");
        TEXTO_DEL_TOKEN.put("LITERAL_CHAR", "carácter");
        TEXTO_DEL_TOKEN.put("LITERAL_STRING", "cadena de texto");
        TEXTO_DEL_TOKEN.put("EOF", "fin de archivo");
    }

    /* Numero del token -> nombre interno (PLUS, ID...). Se arma una sola vez leyendo la clase sym */
    private static final Map<Integer, String> NOMBRE_DEL_TOKEN = cargarNombresDeTokens();

    /*
     * Descripcion: Arma la lista de nombres de los tokens.
     * Entrada: Ninguna (lee la clase sym).
     * Salida: Numero de cada token y su nombre.
     * Restricciones: Se hace una sola vez.
     */
    private static Map<Integer, String> cargarNombresDeTokens() {

        Map<Integer, String> nombres = new HashMap<>();

        for (Field campo : sym.class.getFields()) {

            boolean esConstante = campo.getType() == int.class && Modifier.isStatic(campo.getModifiers());

            if (esConstante) {
                try {
                    nombres.put(campo.getInt(null), campo.getName());
                } catch (IllegalAccessException e) {
                    /* Un campo que no se puede leer no es un token */
                }
            }
        }

        return nombres;
    }

    /*
     * Descripcion: Busca el nombre de un token.
     * Entrada: Numero del token.
     * Salida: Nombre del token, por ejemplo PLUS.
     * Restricciones: Si no existe, devuelve DESCONOCIDO.
     */
    private static String nombreToken(int numero) {
        return NOMBRE_DEL_TOKEN.getOrDefault(numero, "DESCONOCIDO");
    }

    /*
     * Descripcion: Dice como se escribe un token en el lenguaje.
     * Entrada: Nombre del token.
     * Salida: El simbolo, por ejemplo "+" para PLUS.
     * Restricciones: Si no esta en la lista, lo devuelve en minuscula.
     */
    private static String textoLegible(String nombreToken) {
        return TEXTO_DEL_TOKEN.getOrDefault(nombreToken, nombreToken.toLowerCase());
    }


    /* ============================================================
       PROGRAMA PRINCIPAL
       ============================================================ */

    /*
     * Descripcion: Ejecuta el compilador completo.
     * Entrada: Archivo .cmm y, opcional, archivo de tokens.
     * Salida: Resultados en consola y archivos en salida.
     * Restricciones: Si no recibe archivo, muestra como usarlo.
     */
    public static void main(String[] args) throws Exception {

        long inicio = System.nanoTime();

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));

        if (args.length < 1) {
            mostrarTitulo("ANALIZADOR · USO");
            System.out.println();
            System.out.println("  java Main <archivo_fuente> [archivo_tokens]");
            System.out.println();
            return;
        }

        File archivoFuente = new File(args[0]);
        File archivoTokens = args.length > 1 ? new File(args[1]) : archivoTokensPorDefecto(archivoFuente);

        mostrarTitulo("ANALIZADOR LÉXICO, SINTÁCTICO Y SEMÁNTICO");
        System.out.println();
        mostrarDato("Archivo fuente", rutaCorta(archivoFuente));
        mostrarDato("Archivo de tokens", rutaCorta(archivoTokens));

        ResultadoLexico lexico = faseLexica(archivoFuente, archivoTokens);

        ParserConErrores analizador = faseSintactica(archivoFuente);

        TablaSimbolos tabla = analizador.tabla;
        File reporte = faseSemantica(tabla, archivoFuente);

        int erroresTotales = lexico.errores.size() + analizador.errores.size() + tabla.getErrores().size();

        /* Si el parser se rindio, el resultado tampoco es valido aunque no haya errores contados */
        boolean archivoCorrecto = erroresTotales == 0 && !analizador.interrumpido;

        String codigoIntermedio = faseCodigoIntermedio(analizador, archivoFuente, archivoCorrecto);

        mostrarResultadoFinal(lexico, analizador, tabla, erroresTotales, archivoCorrecto, inicio);

        /* Las preguntas van despues del resultado: asi el veredicto se ve primero
           y el tiempo no cuenta lo que se tarda la persona en responder */
        ofrecerCodigoIntermedio(codigoIntermedio);

        ofrecerTablaSimbolos(tabla, reporte);

        System.out.println();
    }


    /* ============================================================
       FASES
       ============================================================ */

    /* Lo que deja el analisis lexico. */
    private static class ResultadoLexico {
        int tokens;
        List<String> errores;
    }

    /*
     * Descripcion: Fase 1: lee los tokens y muestra los errores lexicos.
     * Entrada: Archivo fuente y archivo de tokens.
     * Salida: Cantidad de tokens y errores lexicos.
     * Restricciones: Guarda los tokens en un archivo.
     */
    private static ResultadoLexico faseLexica(File fuente, File archivoTokens) throws Exception {

        mostrarSeccion("FASE 1 · ANÁLISIS LÉXICO");

        ResultadoLexico resultado = analisisLexico(fuente, archivoTokens);

        mostrarConteo("Tokens reconocidos", resultado.tokens);
        mostrarConteoErrores("Errores léxicos", resultado.errores.size());

        if (resultado.errores.isEmpty()) {
            mostrarEstado(true, "Análisis léxico correcto.");
        } else {
            mostrarErroresLexicos(resultado.errores);
            mostrarEstado(false, "El archivo contiene errores léxicos.");
        }

        return resultado;
    }

    /*
     * Descripcion: Fase 2: revisa la estructura y muestra los errores sintacticos.
     * Entrada: Archivo fuente.
     * Salida: El parser ya ejecutado.
     * Restricciones: El parser tambien llena la tabla y genera el codigo.
     */
    private static ParserConErrores faseSintactica(File fuente) throws Exception {

        mostrarSeccion("FASE 2 · ANÁLISIS SINTÁCTICO");

        ParserConErrores analizador = analisisSintactico(fuente);

        List<String> lineasFuente = leerLineas(fuente);

        int numero = 1;
        for (ErrorSintactico error : analizador.errores) {
            mostrarErrorSintactico(numero++, error, lineasFuente);
        }

        if (!analizador.errores.isEmpty()) {
            mostrarCorreccionGlobal(analizador);
            mostrarEstado(false, "El archivo contiene errores sintácticos.");
        } else if (analizador.interrumpido) {
            mostrarEstado(false, "El análisis se interrumpió de forma inesperada antes de terminar.");
        } else {
            mostrarEstado(true, "Análisis sintáctico correcto.");
        }

        return analizador;
    }

    /*
     * Descripcion: Fase 3: guarda la tabla de simbolos y muestra los errores semanticos.
     * Entrada: Tabla de simbolos y archivo fuente.
     * Salida: Archivo donde quedo la tabla.
     * Restricciones: La tabla ya la lleno el parser.
     */
    private static File faseSemantica(TablaSimbolos tabla, File fuente) throws Exception {

        mostrarSeccion("FASE 3 · ANÁLISIS SEMÁNTICO");

        File reporte = tabla.guardarReporte(fuente, tablaSimbolosParaArchivo(tabla));

        List<String> errores = tabla.getErrores();

        mostrarConteo("Ámbitos", tabla.getAmbitos().size());
        mostrarConteo("Símbolos declarados", tabla.cantidadSimbolos());
        mostrarConteoErrores("Errores semánticos", errores.size());

        if (errores.isEmpty()) {
            mostrarEstado(true, "Análisis semántico correcto.");
        } else {
            mostrarErroresSemanticos(errores);
            mostrarEstado(false, "El archivo contiene errores semánticos.");
        }

        return reporte;
    }

    /*
     * Descripcion: Fase 4: guarda el codigo intermedio.
     * Entrada: Parser, archivo fuente y si no hay errores.
     * Salida: El codigo generado, o null si no se genero.
     * Restricciones: Si hay errores, no se genera.
     */
    private static String faseCodigoIntermedio(ParserConErrores analizador, File fuente, boolean archivoCorrecto)
            throws Exception {

        mostrarSeccion("FASE 4 · CÓDIGO INTERMEDIO");

        if (!archivoCorrecto) {
            mostrarEstado(false, "No se generó código intermedio: el archivo tiene errores.");
            return null;
        }

        String codigo = analizador.gen.obtenerCodigo();
        File archivoTac = analizador.gen.guardar(fuente);

        mostrarConteo("Instrucciones", contarInstrucciones(codigo));
        mostrarDato("Archivo", archivoTac.getName());
        mostrarDato("Carpeta", carpetaCorta(archivoTac));

        mostrarEstado(true, "Código intermedio generado.");

        return codigo;
    }

    /*
     * Descripcion: Pregunta si se quiere ver el codigo intermedio.
     * Entrada: El codigo, o null si no se genero.
     * Salida: El codigo en consola, si se pide.
     * Restricciones: Si no hay codigo, no pregunta.
     */
    private static void ofrecerCodigoIntermedio(String codigo) {

        if (codigo == null) {
            return;
        }

        System.out.println();

        if (preguntarSiNo("¿Desea ver el código intermedio? (s/n): ")) {
            mostrarCodigoIntermedio(codigo);
        }
    }

    /*
     * Descripcion: Muestra el resumen de todas las fases.
     * Entrada: Resultados de cada fase y hora de inicio.
     * Salida: Conteo de errores, tiempo y veredicto.
     * Restricciones: Ninguna.
     */
    private static void mostrarResultadoFinal(ResultadoLexico lexico, ParserConErrores analizador,
                                              TablaSimbolos tabla, int erroresTotales,
                                              boolean archivoCorrecto, long inicio) {

        mostrarSeccion("RESULTADO FINAL");

        mostrarConteo("Tokens reconocidos", lexico.tokens);
        mostrarConteoErrores("Errores léxicos", lexico.errores.size());
        mostrarConteoErrores("Errores sintácticos", analizador.errores.size());
        mostrarConteoErrores("Errores semánticos", tabla.getErrores().size());

        System.out.println("  " + "─".repeat(24) + " : " + "─".repeat(5));

        mostrarConteoErrores("Errores totales", erroresTotales);

        System.out.println(String.format("  %-24s : ", "Tiempo de ejecución")
                + pintar(CIAN, String.format("%5s", tiempoTranscurrido(inicio))));
        System.out.println();

        if (archivoCorrecto) {
            mostrarCajaResultado(true, "  ARCHIVO CORRECTO",
                    "El archivo SÍ puede ser generado por la gramática.",
                    "No se encontraron errores léxicos, sintácticos ni semánticos.");
        } else {
            mostrarCajaResultado(false, "  ARCHIVO CON ERRORES",
                    "El archivo NO puede ser generado por la gramática.",
                    "Corrige los errores indicados y vuelve a ejecutar.");
        }
    }

    /*
     * Descripcion: Calcula cuanto tardo el programa.
     * Entrada: Hora de inicio.
     * Salida: Texto como "850 ms" o "1.250 s".
     * Restricciones: Ninguna.
     */
    private static String tiempoTranscurrido(long inicio) {

        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        if (milisegundos < 1000) {
            return milisegundos + " ms";
        }

        return (milisegundos / 1000) + "." + String.format("%03d", milisegundos % 1000) + " s";
    }

    /*
     * Descripcion: Muestra donde quedo la tabla y pregunta si se quiere ver.
     * Entrada: Tabla de simbolos y archivo del reporte.
     * Salida: La tabla en consola, si se pide.
     * Restricciones: Ninguna.
     */
    private static void ofrecerTablaSimbolos(TablaSimbolos tabla, File reporte) {

        System.out.println();
        mostrarCuadroReporte(reporte);
        System.out.println();

        if (preguntarSiNo("¿Desea ver la tabla de símbolos? (s/n): ")) {
            mostrarSeccion("TABLA DE SÍMBOLOS");
            mostrarTablaSimbolos(tabla);
        } else {
            System.out.println();
            System.out.println(pintar(GRIS, "  Puede revisarla después en la ruta indicada."));
        }
    }


    /* ============================================================
       ELEMENTOS DE LA CONSOLA
       ============================================================ */

    /*
     * Descripcion: Muestra un titulo dentro de un marco.
     * Entrada: Texto del titulo.
     * Salida: Titulo en consola.
     * Restricciones: El texto debe caber en el ancho.
     */
    private static void mostrarTitulo(String texto) {

        int libre = ANCHO - 2 - texto.length();
        int izquierda = Math.max(0, libre / 2);
        int derecha = Math.max(0, libre - izquierda);

        System.out.println();
        System.out.println(pintar(CIAN, "╔" + "═".repeat(ANCHO - 2) + "╗"));
        System.out.println(pintar(CIAN, "║" + " ".repeat(izquierda) + texto + " ".repeat(derecha) + "║"));
        System.out.println(pintar(CIAN, "╚" + "═".repeat(ANCHO - 2) + "╝"));
    }

    /*
     * Descripcion: Muestra la linea que separa cada fase.
     * Entrada: Nombre de la seccion.
     * Salida: Linea de titulo en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarSeccion(String texto) {

        String cabecera = "━━ " + texto + " ";

        System.out.println();
        System.out.println(pintar(CIAN, cabecera + "━".repeat(Math.max(3, ANCHO - cabecera.length()))));
        System.out.println();
    }

    /*
     * Descripcion: Muestra un cuadro verde o rojo con mensajes.
     * Entrada: Si es correcto y las lineas de texto.
     * Salida: Cuadro en consola.
     * Restricciones: Cada linea debe caber en el cuadro.
     */
    private static void mostrarCajaResultado(boolean correcto, String... lineas) {

        String color = correcto ? VERDE : ROJO;

        System.out.println(pintar(color, "┌" + "─".repeat(ANCHO - 2) + "┐"));

        for (String linea : lineas) {
            System.out.println(pintar(color, String.format("│ %-" + (ANCHO - 4) + "s │", linea)));
        }

        System.out.println(pintar(color, "└" + "─".repeat(ANCHO - 2) + "┘"));
    }

    /*
     * Descripcion: Muestra una etiqueta con su valor.
     * Entrada: Etiqueta y valor.
     * Salida: Linea en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarDato(String etiqueta, Object valor) {
        System.out.printf("  %-24s : %s%n", etiqueta, valor);
    }

    /*
     * Descripcion: Muestra un numero con su etiqueta.
     * Entrada: Etiqueta y numero.
     * Salida: Linea en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarConteo(String etiqueta, int valor) {
        System.out.println(String.format("  %-24s : ", etiqueta) + pintar(CIAN, String.format("%5d", valor)));
    }

    /*
     * Descripcion: Muestra una cantidad de errores.
     * Entrada: Etiqueta y cantidad.
     * Salida: Linea en consola.
     * Restricciones: Rojo si hay errores, verde si no.
     */
    private static void mostrarConteoErrores(String etiqueta, int valor) {
        String color = valor > 0 ? ROJO : VERDE;
        System.out.println(String.format("  %-24s : ", etiqueta) + pintar(color, String.format("%5d", valor)));
    }

    /*
     * Descripcion: Muestra si una fase salio bien o mal.
     * Entrada: Si es correcto y el mensaje.
     * Salida: Mensaje con ✔ o ✖.
     * Restricciones: Ninguna.
     */
    private static void mostrarEstado(boolean correcto, String mensaje) {
        System.out.println();
        System.out.println(pintar(correcto ? VERDE : ROJO, "  " + (correcto ? "✔ " : "✖ ") + mensaje));
    }

    /*
     * Descripcion: Muestra una fila dentro de un cuadro.
     * Entrada: Texto sin color, texto con color y color del borde.
     * Salida: Fila en consola.
     * Restricciones: El texto sin color sirve para medir el espacio.
     */
    private static void filaCuadro(String textoPlano, String textoConColor, String colorBorde) {

        int relleno = Math.max(0, ANCHO - 4 - textoPlano.length());

        System.out.println(pintar(colorBorde, "│") + " " + textoConColor
                + " ".repeat(relleno) + " " + pintar(colorBorde, "│"));
    }

    /*
     * Descripcion: Corta un texto largo en varias lineas.
     * Entrada: Texto y ancho maximo.
     * Salida: Lista de lineas.
     * Restricciones: No parte palabras, salvo si una sola no cabe.
     */
    private static List<String> partirEnLineas(String texto, int ancho) {

        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (String palabra : texto.split(" ")) {

            while (palabra.length() > ancho) {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                lineas.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }

            if (actual.length() > 0 && actual.length() + 1 + palabra.length() > ancho) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }

            if (actual.length() > 0) {
                actual.append(' ');
            }
            actual.append(palabra);
        }

        if (actual.length() > 0 || lineas.isEmpty()) {
            lineas.add(actual.toString());
        }

        return lineas;
    }

    /*
     * Descripcion: Acorta la ruta de un archivo.
     * Entrada: Archivo.
     * Salida: Ruta desde la carpeta actual.
     * Restricciones: Si no se puede, devuelve solo el nombre.
     */
    private static String rutaCorta(File archivo) {
        try {
            Path base = Paths.get("").toAbsolutePath().normalize();
            Path ruta = archivo.getCanonicalFile().toPath();

            return ruta.startsWith(base) ? base.relativize(ruta).toString() : ruta.toString();

        } catch (Exception e) {
            return archivo.getName();
        }
    }

    /* Carpeta donde se guardan los tokens, dentro de Programa */
    private static final String CARPETA_TOKENS = "salida" + File.separator + "tokens";

    /*
     * Descripcion: Arma la ruta del archivo de tokens.
     * Entrada: Archivo fuente.
     * Salida: salida/tokens/<nombre>_tokens.txt.
     * Restricciones: Se usa si no se indico otro archivo.
     */
    private static File archivoTokensPorDefecto(File fuente) {

        String nombre = fuente.getName();
        int punto = nombre.lastIndexOf('.');

        if (punto > 0) {
            nombre = nombre.substring(0, punto);
        }

        return new File(carpetaPrograma(), CARPETA_TOKENS + File.separator + nombre + "_tokens.txt");
    }

    /*
     * Descripcion: Busca la carpeta Programa.
     * Entrada: Ninguna.
     * Salida: Carpeta que contiene a bin.
     * Restricciones: Si no se puede, usa la carpeta actual.
     */
    private static File carpetaPrograma() {
        try {
            File carpetaBin = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return carpetaBin.getParentFile();
        } catch (Exception e) {
            return new File(".");
        }
    }

    /*
     * Descripcion: Da las dos ultimas carpetas de un archivo.
     * Entrada: Archivo.
     * Salida: Texto como "salida/codigo_intermedio".
     * Restricciones: Ninguna.
     */
    private static String carpetaCorta(File archivo) {

        File carpeta = archivo.getParentFile();

        if (carpeta == null) {
            return "";
        }

        File carpetaSuperior = carpeta.getParentFile();

        return (carpetaSuperior == null ? "" : carpetaSuperior.getName() + File.separator) + carpeta.getName();
    }

    /* El teclado se abre una sola vez: dos Scanner sobre System.in se quitan la entrada */
    private static Scanner teclado = null;

    /*
     * Descripcion: Hace una pregunta de si o no.
     * Entrada: Texto de la pregunta.
     * Salida: true si responde s, false si responde n.
     * Restricciones: Repite hasta recibir una respuesta valida.
     */
    private static boolean preguntarSiNo(String pregunta) {

        if (teclado == null) {
            teclado = new Scanner(System.in, StandardCharsets.UTF_8);
        }

        while (true) {

            System.out.print(pintar(AMARILLO, "  " + pregunta));
            System.out.flush();

            if (!teclado.hasNextLine()) {
                System.out.println();
                return false;
            }

            String respuesta = teclado.nextLine().trim().toLowerCase();

            if (respuesta.equals("s") || respuesta.equals("si") || respuesta.equals("sí")) {
                return true;
            }

            if (respuesta.equals("n") || respuesta.equals("no")) {
                return false;
            }

            System.out.println(pintar(ROJO_SUAVE, "  Respuesta no válida. Escriba s o n."));
        }
    }

    /*
     * Descripcion: Apaga la consola mientras trabaja el Lexer.
     * Entrada: Ninguna.
     * Salida: La consola original, para volver a encenderla.
     * Restricciones: Hay que restaurarla al terminar.
     */
    private static PrintStream silenciarConsola() {
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        return original;
    }


    /* ============================================================
       FASE 1: ANALISIS LEXICO
       ============================================================ */

    /*
     * Descripcion: Lee todos los tokens y los guarda en un archivo.
     * Entrada: Archivo fuente y archivo de tokens.
     * Salida: Cantidad de tokens y errores lexicos.
     * Restricciones: Crea la carpeta de tokens si no existe.
     */
    private static ResultadoLexico analisisLexico(File fuente, File archivoTokens) throws Exception {

        File carpetaTokens = archivoTokens.getAbsoluteFile().getParentFile();

        if (carpetaTokens != null) {
            carpetaTokens.mkdirs();
        }

        PrintStream consola = silenciarConsola();

        try (Reader lector = new InputStreamReader(new FileInputStream(fuente), StandardCharsets.UTF_8);
             PrintWriter salida = new PrintWriter(archivoTokens, "UTF-8")) {

            Lexer lexer = new Lexer(lector);

            salida.printf("%-8s %-8s %-18s %s%n", "LINEA", "COLUMNA", "TOKEN", "LEXEMA");
            salida.println("-".repeat(60));

            ResultadoLexico resultado = new ResultadoLexico();

            Symbol token = lexer.next_token();

            while (token.sym != sym.EOF) {
                salida.printf("%-8d %-8d %-18s %s%n", token.left, token.right, nombreToken(token.sym), token.value);
                resultado.tokens++;
                token = lexer.next_token();
            }

            resultado.errores = lexer.getErrores();

            if (!resultado.errores.isEmpty()) {
                salida.println();
                salida.println("ERRORES LEXICOS (" + resultado.errores.size() + ")");
                salida.println("-".repeat(60));

                for (String error : resultado.errores) {
                    salida.println(error);
                }
            }

            return resultado;

        } finally {
            System.setOut(consola);
        }
    }

    /* Las partes de un error: linea, columna y detalle. */
    private static class PartesError {
        String linea = "-";
        String columna = "-";
        String detalle;
    }

    /*
     * Descripcion: Separa un error en linea, columna y detalle.
     * Entrada: Texto del error.
     * Salida: Las partes del error.
     * Restricciones: Si no tiene el formato esperado, todo queda en el detalle.
     */
    private static PartesError separarError(String error) {

        PartesError partes = new PartesError();

        String texto = error == null ? "" : error.trim().replace("\r", " ").replace("\n", " ");
        partes.detalle = texto;

        Matcher coincidencia = PATRON_ERROR.matcher(texto);

        if (coincidencia.matches()) {
            partes.linea = coincidencia.group(1);
            partes.columna = coincidencia.group(2);
            partes.detalle = coincidencia.group(3);
        }

        return partes;
    }

    /*
     * Descripcion: Muestra la tabla de errores lexicos.
     * Entrada: Lista de errores.
     * Salida: Tabla en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarErroresLexicos(List<String> errores) {

        System.out.println();
        System.out.println(pintar(CIAN, String.format("  %-4s %6s %5s   %s", "Nº", "LÍNEA", "COL", "DESCRIPCIÓN")));
        System.out.println(pintar(GRIS, "  " + "─".repeat(4) + " " + "─".repeat(6) + " " + "─".repeat(5)
                + "   " + "─".repeat(ANCHO - 24)));

        int numero = 1;

        for (String error : errores) {

            PartesError partes = separarError(error);
            String textoNumero = String.format("%02d", numero++);

            String prefijoPlano = String.format("  %-4s %6s %5s   ", textoNumero, partes.linea, partes.columna);

            String prefijoConColor = "  "
                    + pintar(GRIS, String.format("%-4s", textoNumero)) + " "
                    + pintar(AMARILLO_SUAVE, String.format("%6s", partes.linea)) + " "
                    + pintar(AMARILLO_SUAVE, String.format("%5s", partes.columna)) + "   ";

            List<String> lineas = partirEnLineas(partes.detalle, ANCHO - prefijoPlano.length());

            for (int i = 0; i < lineas.size(); i++) {
                String prefijo = i == 0 ? prefijoConColor : " ".repeat(prefijoPlano.length());
                System.out.println(prefijo + lineas.get(i));
            }
        }
    }


    /* ============================================================
       FASE 2: ANALISIS SINTACTICO
       ============================================================ */

    /*
     * Descripcion: Ejecuta el parser sobre el archivo.
     * Entrada: Archivo fuente.
     * Salida: El parser ya ejecutado.
     * Restricciones: Si el parser falla, queda marcado como interrumpido.
     */
    private static ParserConErrores analisisSintactico(File fuente) throws Exception {

        PrintStream consola = silenciarConsola();

        try (Reader lector = new InputStreamReader(new FileInputStream(fuente), StandardCharsets.UTF_8)) {

            ParserConErrores analizador = new ParserConErrores(new Lexer(lector));

            try {
                analizador.parse();
            } catch (Exception e) {
                analizador.interrumpido = true;
            }

            return analizador;

        } finally {
            System.setOut(consola);
        }
    }

    /*
     * Descripcion: Lee todas las lineas del archivo fuente.
     * Entrada: Archivo fuente.
     * Salida: Lista de lineas.
     * Restricciones: Si no se puede leer, devuelve una lista vacia.
     */
    private static List<String> leerLineas(File fuente) {
        try {
            return Files.readAllLines(fuente.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /* Los datos de un error sintactico. */
    private static class ErrorSintactico {
        int linea;
        int columna;
        boolean alFinalDelArchivo;
        String token;
        String lexema;
        List<String> esperados = new ArrayList<>();

        /* Si el parser lo corrigio por su cuenta: con que tecnica y que dice (null en un error normal) */
        String tecnica;
        String mensaje;

        /* Para la correccion global: posicion del token en el flujo y numeros de los tokens esperados */
        int posicionEnFlujo = -1;
        List<Integer> numerosEsperados = new ArrayList<>();

        /*
         * Descripcion: Dice si el error se recupero con modo panico.
         * Entrada: Ninguna.
         * Salida: true si el error no tiene mensaje propio.
         * Restricciones: Ninguna.
         */
        boolean esDeModoPanico() {
            return mensaje == null;
        }
    }

    /*
     * Descripcion: Muestra un error sintactico en un cuadro rojo.
     * Entrada: Numero del error, el error y las lineas del archivo.
     * Salida: Cuadro en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarErrorSintactico(int numero, ErrorSintactico error, List<String> lineasFuente) {

        int anchoInterno = ANCHO - 4;

        String ubicacion = error.linea > 0
                ? "línea " + error.linea + ", columna " + error.columna
                : "final del archivo";

        String titulo = " ERROR " + String.format("%02d", numero) + " · " + ubicacion + " ";

        System.out.println(pintar(ROJO_SUAVE, "┌─") + pintar(ROJO, titulo)
                + pintar(ROJO_SUAVE, "─".repeat(Math.max(1, ANCHO - 3 - titulo.length())) + "┐"));

        filaCuadro("", "", ROJO_SUAVE);

        if (error.linea >= 1 && error.linea <= lineasFuente.size()) {
            mostrarLineaConError(error, lineasFuente.get(error.linea - 1), anchoInterno);
            filaCuadro("", "", ROJO_SUAVE);
        }

        if (!error.esDeModoPanico()) {

            /* El parser lo corrigio por su cuenta: se muestra con que tecnica */
            mostrarDetalleError("Técnica", error.tecnica, CIAN_SUAVE);

            if (!error.esperados.isEmpty()) {
                mostrarDetalleError("Se esperaba", String.join(", ", error.esperados), VERDE_SUAVE);
            }

            mostrarDetalleError("Detalle", error.mensaje, AMARILLO);

        } else {

            mostrarLoEncontrado(error);

            if (!error.esperados.isEmpty()) {
                mostrarDetalleError("Se esperaba", resumirEsperados(error.esperados), VERDE_SUAVE);
            }

            if (error.alFinalDelArchivo) {
                mostrarDetalleError("Sugerencia", "Puede faltar un cierre de bloque o un terminador.", CIAN_SUAVE);
            }
        }

        filaCuadro("", "", ROJO_SUAVE);
        System.out.println(pintar(ROJO_SUAVE, "└" + "─".repeat(ANCHO - 2) + "┘"));
        System.out.println();
    }

    /*
     * Descripcion: Muestra la linea del error y marca el token con ^^^.
     * Entrada: Error, linea del codigo y ancho disponible.
     * Salida: Dos filas en el cuadro.
     * Restricciones: Si la linea es larga, muestra solo la zona del error.
     */
    private static void mostrarLineaConError(ErrorSintactico error, String lineaFuente, int anchoInterno) {

        int anchoNumero = Math.max(3, String.valueOf(error.linea).length());

        String texto = lineaFuente.replace("\t", " ").replaceAll("\\s+$", "");

        int espacioDisponible = anchoInterno - anchoNumero - 3;
        int posicionError = Math.max(0, error.columna - 1);
        int inicioVisible = 0;

        /* Si la linea es muy larga, se muestra solo la zona del error */
        if (texto.length() > espacioDisponible) {
            inicioVisible = Math.max(0, Math.min(posicionError - espacioDisponible / 3,
                    texto.length() - espacioDisponible));
            texto = texto.substring(inicioVisible, Math.min(texto.length(), inicioVisible + espacioDisponible));
        }

        String numeroLinea = String.format("%" + anchoNumero + "d", error.linea);

        filaCuadro(numeroLinea + " │ " + texto, pintar(GRIS, numeroLinea + " │ ") + texto, ROJO_SUAVE);

        if (error.alFinalDelArchivo || error.columna <= 0) {
            return;
        }

        String lexema = error.lexema == null ? "" : error.lexema;

        int columnaVisible = Math.max(0, posicionError - inicioVisible);
        int largoMarca = Math.max(1, Math.min(Math.max(1, lexema.length()), espacioDisponible - columnaVisible));

        String guia = " ".repeat(anchoNumero) + " │ ";
        String marca = "^".repeat(largoMarca);

        filaCuadro(guia + " ".repeat(columnaVisible) + marca,
                pintar(GRIS, guia) + " ".repeat(columnaVisible) + pintar(ROJO, marca),
                ROJO_SUAVE);
    }

    /*
     * Descripcion: Muestra el token que se encontro.
     * Entrada: Error.
     * Salida: Fila "Se encontro" en el cuadro.
     * Restricciones: Corta los textos de mas de 40 caracteres.
     */
    private static void mostrarLoEncontrado(ErrorSintactico error) {

        if (error.alFinalDelArchivo) {
            mostrarDetalleError("Se encontró", "fin de archivo", AMARILLO);
            return;
        }

        String lexema = error.lexema == null ? "" : error.lexema;

        if (lexema.length() > 40) {
            lexema = lexema.substring(0, 39) + "…";
        }

        String encontrado = "'" + lexema + "'";
        String token = "(" + error.token + ")";
        String prefijo = String.format("%-11s : ", "Se encontró");

        filaCuadro(prefijo + encontrado + "  " + token,
                pintar(NEGRITA, prefijo) + pintar(AMARILLO, encontrado) + "  " + pintar(GRIS, token),
                ROJO_SUAVE);
    }

    /*
     * Descripcion: Resume la lista de tokens esperados.
     * Entrada: Lista de tokens esperados.
     * Salida: Texto con maximo 6 opciones.
     * Restricciones: Si hay mas, termina en ", …".
     */
    private static String resumirEsperados(List<String> esperados) {

        List<String> lista = new ArrayList<>(esperados);
        boolean hayMas = lista.remove("…");

        if (lista.size() > 6) {
            lista = new ArrayList<>(lista.subList(0, 6));
            hayMas = true;
        }

        return String.join(", ", lista) + (hayMas ? ", …" : "");
    }

    /*
     * Descripcion: Muestra un dato dentro del cuadro de error.
     * Entrada: Etiqueta, valor y color.
     * Salida: Una o varias filas en el cuadro.
     * Restricciones: Si el valor es largo, sigue en la linea de abajo.
     */
    private static void mostrarDetalleError(String etiqueta, String valor, String color) {

        String prefijo = String.format("%-11s : ", etiqueta);
        List<String> lineas = partirEnLineas(valor, ANCHO - 4 - prefijo.length());

        for (int i = 0; i < lineas.size(); i++) {

            String inicio = i == 0 ? prefijo : " ".repeat(prefijo.length());
            String inicioConColor = i == 0 ? pintar(NEGRITA, prefijo) : inicio;

            filaCuadro(inicio + lineas.get(i), inicioConColor + pintar(color, lineas.get(i)), ROJO_SUAVE);
        }
    }


    /* ============================================================
       CORRECCION GLOBAL
       ============================================================ */

    /* Los tres tipos de edicion de la correccion global. */
    private enum TipoEdicion { INSERTAR, REEMPLAZAR, ELIMINAR }

    /* Un cambio de un token y como queda el archivo despues. */
    private static class Edicion {
        TipoEdicion tipo;
        String tokenOriginal;      /* el token que se elimina, se reemplaza o antes del cual se inserta */
        String tokenNuevo;         /* el token que se inserta o que reemplaza (null al eliminar) */
        int linea;
        int columna;
        List<Symbol> tokensResultantes;
        int puntaje;               /* errores que quedan despues de la edicion */
    }

    /*
     * Descripcion: Le da al parser tokens de una lista en vez de un archivo.
     * Entrada: Lista de tokens.
     * Salida: Un token cada vez que el parser lo pide.
     * Restricciones: Se usa en la correccion global.
     */
    private static class ListaDeTokens implements java_cup.runtime.Scanner {

        private final List<Symbol> tokens;
        private int posicion = 0;

        ListaDeTokens(List<Symbol> tokens) {
            this.tokens = tokens;
        }

        /*
         * Descripcion: Entrega el siguiente token de la lista.
         * Entrada: Ninguna.
         * Salida: El siguiente token.
         * Restricciones: Al final repite el ultimo (fin de archivo).
         */
        @Override
        public Symbol next_token() {
            Symbol token = tokens.get(Math.min(posicion, tokens.size() - 1));
            posicion++;
            return new Symbol(token.sym, token.left, token.right, token.value);
        }
    }

    /*
     * Descripcion: Busca las menores ediciones que quitan errores sintacticos.
     * Entrada: El parser que ya analizo el archivo.
     * Salida: Sugerencia en consola.
     * Restricciones: Solo si hubo modo panico. Maximo 6 ediciones. No cambia el archivo.
     */
    private static void mostrarCorreccionGlobal(ParserConErrores analisisOriginal) {

        boolean hayErrorDeModoPanico = false;

        for (ErrorSintactico error : analisisOriginal.errores) {
            if (error.esDeModoPanico()) {
                hayErrorDeModoPanico = true;
            }
        }

        if (!hayErrorDeModoPanico) {
            return;
        }

        List<Symbol> tokens = tokensConFinDeArchivo(analisisOriginal.flujo);
        ParserConErrores analisisActual = analisisOriginal;
        int puntajeInicial = puntaje(analisisOriginal);
        List<Edicion> aplicadas = new ArrayList<>();

        for (int ronda = 0; ronda < MAXIMO_EDICIONES && puntaje(analisisActual) > 0; ronda++) {

            ErrorSintactico objetivo = primerErrorDeModoPanico(analisisActual);

            if (objetivo == null) {
                break;
            }

            Edicion mejor = null;

            for (Edicion candidata : edicionesPosibles(tokens, objetivo)) {

                candidata.puntaje = puntaje(analizarTokens(candidata.tokensResultantes));

                boolean mejora = candidata.puntaje < puntaje(analisisActual);

                if (mejora && (mejor == null || candidata.puntaje < mejor.puntaje)) {
                    mejor = candidata;
                }
            }

            if (mejor == null) {
                break;
            }

            aplicadas.add(mejor);
            tokens = mejor.tokensResultantes;
            analisisActual = analizarTokens(tokens);
        }

        if (!aplicadas.isEmpty()) {
            mostrarCuadroCorreccion(aplicadas, puntajeInicial, puntaje(analisisActual));
        }
    }

    /*
     * Descripcion: Busca el primer error de modo panico.
     * Entrada: Un analisis.
     * Salida: El error, o null si no hay.
     * Restricciones: Ninguna.
     */
    private static ErrorSintactico primerErrorDeModoPanico(ParserConErrores analisis) {

        for (ErrorSintactico error : analisis.errores) {
            if (error.esDeModoPanico() && error.posicionEnFlujo >= 0) {
                return error;
            }
        }

        return null;
    }

    /*
     * Descripcion: Muestra las ediciones sugeridas.
     * Entrada: Ediciones y errores antes y despues.
     * Salida: Cuadro cian en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarCuadroCorreccion(List<Edicion> aplicadas, int puntajeInicial, int puntajeFinal) {

        System.out.println(pintar(CIAN_SUAVE, "┌─ CORRECCIÓN GLOBAL (sugerencia) "
                + "─".repeat(Math.max(1, ANCHO - 35)) + "┐"));

        filaCuadro("", "", CIAN_SUAVE);

        List<String> parrafos = new ArrayList<>();

        parrafos.add("Se buscó el menor número de ediciones (insertar, reemplazar o eliminar un token) "
                + "que deja el archivo con menos errores sintácticos. Cada edición se probó "
                + "volviendo a analizar el archivo completo.");
        parrafos.add("");

        int puntajeAnterior = puntajeInicial;
        int numero = 1;

        for (Edicion edicion : aplicadas) {

            parrafos.add(numero++ + ". " + describirEdicion(edicion)
                    + " (línea " + edicion.linea + ", columna " + edicion.columna + "): errores sintácticos "
                    + textoPuntaje(puntajeAnterior) + " → " + textoPuntaje(edicion.puntaje));

            puntajeAnterior = edicion.puntaje;
        }

        parrafos.add("");
        parrafos.add(puntajeFinal == 0
                ? "Con " + aplicadas.size() + " edición(es) el archivo queda sin errores sintácticos."
                : "Con " + aplicadas.size() + " edición(es) los errores sintácticos pasan de "
                        + textoPuntaje(puntajeInicial) + " a " + textoPuntaje(puntajeFinal) + ".");

        for (String parrafo : parrafos) {

            if (parrafo.isEmpty()) {
                filaCuadro("", "", CIAN_SUAVE);
                continue;
            }

            for (String linea : partirEnLineas(parrafo, ANCHO - 4)) {
                filaCuadro(linea, linea, CIAN_SUAVE);
            }
        }

        filaCuadro("", "", CIAN_SUAVE);
        System.out.println(pintar(CIAN_SUAVE, "└" + "─".repeat(ANCHO - 2) + "┘"));
        System.out.println();
    }

    /*
     * Descripcion: Explica una edicion en palabras.
     * Entrada: Edicion.
     * Salida: Texto como "Insertar '»' antes de 'x'".
     * Restricciones: Ninguna.
     */
    private static String describirEdicion(Edicion edicion) {
        switch (edicion.tipo) {
            case INSERTAR:
                return "Insertar '" + edicion.tokenNuevo + "' antes de '" + edicion.tokenOriginal + "'";
            case REEMPLAZAR:
                return "Reemplazar '" + edicion.tokenOriginal + "' por '" + edicion.tokenNuevo + "'";
            default:
                return "Eliminar '" + edicion.tokenOriginal + "'";
        }
    }

    /*
     * Descripcion: Mide que tan malo es un analisis.
     * Entrada: Un analisis.
     * Salida: Cantidad de errores.
     * Restricciones: Si el parser se rindio, suma una penalizacion muy grande.
     */
    private static int puntaje(ParserConErrores analisis) {
        return analisis.interrumpido
                ? PENALIZACION_INTERRUMPIDO + analisis.errores.size()
                : analisis.errores.size();
    }

    /*
     * Descripcion: Convierte un puntaje en texto.
     * Entrada: Puntaje.
     * Salida: Texto con la cantidad de errores.
     * Restricciones: Agrega "(se interrumpe)" si el parser se rindio.
     */
    private static String textoPuntaje(int puntaje) {
        return puntaje >= PENALIZACION_INTERRUMPIDO
                ? (puntaje - PENALIZACION_INTERRUMPIDO) + " (se interrumpe)"
                : String.valueOf(puntaje);
    }

    /*
     * Descripcion: Analiza una lista de tokens con un parser nuevo.
     * Entrada: Lista de tokens.
     * Salida: El analisis.
     * Restricciones: No imprime nada.
     */
    private static ParserConErrores analizarTokens(List<Symbol> tokens) {

        PrintStream consola = silenciarConsola();

        ParserConErrores analisis = new ParserConErrores(new ListaDeTokens(tokens));

        try {
            analisis.parse();
        } catch (Exception e) {
            analisis.interrumpido = true;
        } finally {
            System.setOut(consola);
        }

        return analisis;
    }

    /*
     * Descripcion: Copia la lista de tokens.
     * Entrada: Lista original.
     * Salida: Copia que termina con fin de archivo.
     * Restricciones: Ninguna.
     */
    private static List<Symbol> tokensConFinDeArchivo(List<Symbol> original) {

        List<Symbol> copia = new ArrayList<>(original);

        boolean terminaEnEof = !copia.isEmpty() && copia.get(copia.size() - 1).sym == sym.EOF;

        if (!terminaEnEof) {
            Symbol ultimo = copia.isEmpty() ? null : copia.get(copia.size() - 1);
            int linea = ultimo == null ? 1 : ultimo.left;
            int columna = ultimo == null ? 1 : ultimo.right;
            copia.add(new Symbol(sym.EOF, linea, columna, null));
        }

        return copia;
    }

    /*
     * Descripcion: Dice si un token se puede usar en una sugerencia.
     * Entrada: Numero del token.
     * Salida: true si es un simbolo o palabra del lenguaje.
     * Restricciones: No inventa identificadores, numeros ni textos.
     */
    private static boolean esTokenDeEstructura(int numero) {

        String nombre = nombreToken(numero);

        return numero != sym.error
                && !nombre.equals("ID")
                && !nombre.startsWith("LITERAL_")
                && !nombre.equals("EOF")
                && !nombre.equals("DESCONOCIDO");
    }

    /*
     * Descripcion: Dice como se muestra un token en una sugerencia.
     * Entrada: Token.
     * Salida: Su texto.
     * Restricciones: Ninguna.
     */
    private static String textoDelToken(Symbol token) {

        boolean tieneValor = token.sym != sym.EOF && token.value != null && !String.valueOf(token.value).isEmpty();

        if (tieneValor) {
            return String.valueOf(token.value);
        }

        return token.sym == sym.EOF ? "fin de archivo" : textoLegible(nombreToken(token.sym));
    }

    /*
     * Descripcion: Crea una edicion.
     * Entrada: Tipo, token, token nuevo y tokens resultantes.
     * Salida: La edicion.
     * Restricciones: Ninguna.
     */
    private static Edicion nuevaEdicion(TipoEdicion tipo, Symbol token, String tokenNuevo, List<Symbol> tokens) {

        Edicion edicion = new Edicion();
        edicion.tipo = tipo;
        edicion.tokenOriginal = textoDelToken(token);
        edicion.tokenNuevo = tokenNuevo;
        edicion.linea = token.left;
        edicion.columna = token.right;
        edicion.tokensResultantes = tokens;
        return edicion;
    }

    /*
     * Descripcion: Lista las ediciones que se prueban en un error.
     * Entrada: Tokens y el error.
     * Salida: Ediciones: eliminar, insertar y reemplazar.
     * Restricciones: Si dos arreglan lo mismo, gana la primera.
     */
    private static List<Edicion> edicionesPosibles(List<Symbol> tokens, ErrorSintactico error) {

        List<Edicion> ediciones = new ArrayList<>();

        int posicion = error.posicionEnFlujo;

        if (posicion < 0 || posicion >= tokens.size()) {
            return ediciones;
        }

        Symbol tokenDelError = tokens.get(posicion);

        /* Eliminar el token del error, o el anterior (por ejemplo, un operador de mas) */
        for (int i = posicion; i >= Math.max(0, posicion - 1); i--) {

            Symbol token = tokens.get(i);

            if (token.sym == sym.EOF) {
                continue;
            }

            List<Symbol> sinElToken = new ArrayList<>(tokens);
            sinElToken.remove(i);

            ediciones.add(nuevaEdicion(TipoEdicion.ELIMINAR, token, null, sinElToken));
        }

        Set<Integer> esperados = new LinkedHashSet<>(error.numerosEsperados);

        /* Insertar un token esperado justo antes del token del error */
        for (int numero : esperados) {

            if (!esTokenDeEstructura(numero)) {
                continue;
            }

            String texto = textoLegible(nombreToken(numero));

            List<Symbol> conElToken = new ArrayList<>(tokens);
            conElToken.add(posicion, new Symbol(numero, tokenDelError.left, tokenDelError.right, texto));

            ediciones.add(nuevaEdicion(TipoEdicion.INSERTAR, tokenDelError, texto, conElToken));
        }

        /* Reemplazar el token del error por uno esperado */
        if (tokenDelError.sym != sym.EOF) {

            for (int numero : esperados) {

                if (!esTokenDeEstructura(numero)) {
                    continue;
                }

                String texto = textoLegible(nombreToken(numero));

                List<Symbol> conReemplazo = new ArrayList<>(tokens);
                conReemplazo.set(posicion, new Symbol(numero, tokenDelError.left, tokenDelError.right, texto));

                ediciones.add(nuevaEdicion(TipoEdicion.REEMPLAZAR, tokenDelError, texto, conReemplazo));
            }
        }

        return ediciones;
    }


    /* ============================================================
       FASE 3: ERRORES SEMANTICOS
       ============================================================ */

    /* Ancho maximo de la lista de errores semanticos (se ajusta a la descripcion mas larga) */
    private static final int ANCHO_MAXIMO_SEMANTICOS = 170;

    /* Largo del inicio de cada linea: Nº, LÍNEA, COL y VALIDACIÓN */
    private static final int ANCHO_PREFIJO_SEMANTICO = 37;

    /* Un error semantico listo para mostrar. */
    private static class FilaSemantica {
        String linea;
        String columna;
        String validacion;
        String detalle;
    }

    /*
     * Descripcion: Muestra los errores semanticos, uno por linea.
     * Entrada: Lista de errores.
     * Salida: Tabla en consola y resumen por tipo.
     * Restricciones: Ninguna.
     */
    private static void mostrarErroresSemanticos(List<String> errores) {

        List<FilaSemantica> filas = new ArrayList<>();
        int detalleMasLargo = 0;

        for (String error : errores) {

            PartesError partes = separarError(error);

            FilaSemantica fila = new FilaSemantica();
            fila.linea = partes.linea;
            fila.columna = partes.columna;
            fila.validacion = "Semántico";
            fila.detalle = partes.detalle;

            /* El error empieza con su tipo de validacion: [Aritmética] detalle */
            Matcher validacion = PATRON_VALIDACION.matcher(partes.detalle);

            if (validacion.matches()) {
                fila.validacion = validacion.group(1);
                fila.detalle = validacion.group(2);
            }

            filas.add(fila);
            detalleMasLargo = Math.max(detalleMasLargo, fila.detalle.length());
        }

        int ancho = Math.min(ANCHO_MAXIMO_SEMANTICOS, Math.max(100, ANCHO_PREFIJO_SEMANTICO + detalleMasLargo));

        System.out.println();
        System.out.println(pintar(CIAN, String.format("  %-4s %6s %5s  %-15s %s",
                "Nº", "LÍNEA", "COL", "VALIDACIÓN", "DESCRIPCIÓN")));
        System.out.println(pintar(GRIS, "  " + "─".repeat(ancho - 2)));

        /* Tipo de validacion -> cantidad de errores (en el orden en que aparecen) */
        Map<String, Integer> cantidadPorValidacion = new LinkedHashMap<>();

        int numero = 1;

        for (FilaSemantica fila : filas) {

            cantidadPorValidacion.merge(fila.validacion, 1, Integer::sum);

            String prefijoConColor = "  "
                    + pintar(GRIS, String.format("%-4s", String.format("%02d", numero++))) + " "
                    + pintar(AMARILLO_SUAVE, String.format("%6s", fila.linea)) + " "
                    + pintar(AMARILLO_SUAVE, String.format("%5s", fila.columna)) + "  "
                    + pintar(CIAN_SUAVE, String.format("%-15s", fila.validacion)) + " ";

            List<String> lineas = partirEnLineas(fila.detalle, ancho - ANCHO_PREFIJO_SEMANTICO);

            for (int i = 0; i < lineas.size(); i++) {
                String prefijo = i == 0 ? prefijoConColor : " ".repeat(ANCHO_PREFIJO_SEMANTICO);
                System.out.println(prefijo + lineas.get(i));
            }
        }

        System.out.println(pintar(GRIS, "  " + "─".repeat(ancho - 2)));

        mostrarResumenPorValidacion(cantidadPorValidacion, ancho);
    }

    /*
     * Descripcion: Muestra cuantos errores hay de cada tipo.
     * Entrada: Cantidad por tipo y ancho.
     * Salida: Linea "Por validacion" en consola.
     * Restricciones: Si no cabe, sigue en otra linea.
     */
    private static void mostrarResumenPorValidacion(Map<String, Integer> cantidadPorValidacion, int ancho) {

        String inicio = "  Por validación: ";

        int largoLinea = inicio.length();
        StringBuilder linea = new StringBuilder("  " + pintar(NEGRITA, "Por validación: "));

        for (Map.Entry<String, Integer> entrada : cantidadPorValidacion.entrySet()) {

            String elemento = entrada.getKey() + " " + entrada.getValue();

            if (largoLinea + elemento.length() + 5 > ancho) {
                System.out.println(linea);
                linea = new StringBuilder("    ");
                largoLinea = 4;
            }

            linea.append(pintar(CIAN_SUAVE, entrada.getKey())).append(" ")
                 .append(pintar(AMARILLO, String.valueOf(entrada.getValue()))).append("   ");

            largoLinea += elemento.length() + 3;
        }

        System.out.println(linea);
    }


    /* ============================================================
       FASE 4: CODIGO INTERMEDIO EN CONSOLA
       Colores (solo tres, mas gris):
         amarillo : inicio y fin de cada funcion, y la funcion de un call
         cian     : etiquetas de if, while y for, y el destino de cada salto
         verde    : declaraciones (declaracion#, declaracionPrevia#, param_)
         rojo     : operadores (+ - * // / mod pot < > <= >= == != and or not)
         gris     : numeros de linea y separadores
         negrita  : palabras de control (goto, if, call, return, param, read, write)
       ============================================================ */

    private static final String SANGRIA_ETIQUETA = "  ";
    private static final String SANGRIA_INSTRUCCION = "    ";

    /*
     * Descripcion: Dice si una linea es una etiqueta.
     * Entrada: Linea de codigo.
     * Salida: true si termina en ":" y no tiene espacios.
     * Restricciones: Ninguna.
     */
    private static boolean esEtiqueta(String linea) {
        return linea.endsWith(":") && !linea.contains(" ");
    }

    /*
     * Descripcion: Dice si es una etiqueta de if, while o for.
     * Entrada: Linea de codigo.
     * Salida: true si tiene $if$, $while$ o $for$.
     * Restricciones: Ninguna.
     */
    private static boolean esEtiquetaDeEstructura(String linea) {
        return esEtiqueta(linea) && PATRON_ETIQUETA_ESTRUCTURA.matcher(linea).find();
    }

    /*
     * Descripcion: Dice si es la etiqueta de inicio de una funcion.
     * Entrada: Linea de codigo.
     * Salida: true si es etiqueta y no es de estructura.
     * Restricciones: Ninguna.
     */
    private static boolean esInicioDeFuncion(String linea) {
        return esEtiqueta(linea) && !PATRON_ETIQUETA_ESTRUCTURA.matcher(linea).find();
    }

    /*
     * Descripcion: Dice si es el fin de una funcion.
     * Entrada: Linea de codigo.
     * Salida: true si termina en $final y no lleva ":".
     * Restricciones: Ninguna.
     */
    private static boolean esFinDeFuncion(String linea) {
        return linea.endsWith("$final") && !linea.contains(" ") && !linea.endsWith(":");
    }

    /*
     * Descripcion: Dice si una linea es una declaracion.
     * Entrada: Linea de codigo.
     * Salida: true si empieza con declaracion#, declaracionPrevia# o param_.
     * Restricciones: Ninguna.
     */
    private static boolean esDeclaracion(String linea) {
        return linea.startsWith("declaracion#") || linea.startsWith("declaracionPrevia#") || linea.startsWith("param_");
    }

    /*
     * Descripcion: Cuenta las instrucciones del codigo intermedio.
     * Entrada: Codigo intermedio.
     * Salida: Cantidad de instrucciones.
     * Restricciones: No cuenta lineas vacias, etiquetas ni fines de funcion.
     */
    private static int contarInstrucciones(String codigo) {

        int cantidad = 0;

        for (String linea : codigo.split("\n")) {
            if (!linea.isEmpty() && !esEtiqueta(linea) && !esFinDeFuncion(linea)) {
                cantidad++;
            }
        }

        return cantidad;
    }

    /*
     * Descripcion: Muestra el codigo intermedio con colores.
     * Entrada: Codigo intermedio.
     * Salida: Codigo numerado en consola.
     * Restricciones: Los numeros son los mismos del .tac.
     */
    private static void mostrarCodigoIntermedio(String codigo) {

        mostrarSeccion("CÓDIGO INTERMEDIO");

        if (codigo.isEmpty()) {
            System.out.println(pintar(GRIS, "  (el generador todavía no produjo instrucciones)"));
            System.out.println();
            return;
        }

        System.out.println("  " + pintar(AMARILLO, "funciones") + pintar(GRIS, "  ·  ")
                + pintar(CIAN, "etiquetas y saltos") + pintar(GRIS, "  ·  ")
                + pintar(VERDE_SUAVE, "declaraciones") + pintar(GRIS, "  ·  ")
                + pintar(ROJO_SUAVE, "operadores"));
        System.out.println();

        for (String linea : formatearCodigoIntermedio(codigo)) {
            System.out.println(linea);
        }

        System.out.println();
    }

    /*
     * Descripcion: Arma el codigo con numeros, sangria y separadores.
     * Entrada: Codigo intermedio.
     * Salida: Lista de lineas listas para mostrar.
     * Restricciones: Ninguna.
     */
    private static List<String> formatearCodigoIntermedio(String codigo) {

        List<String> resultado = new ArrayList<>();

        if (codigo.isEmpty()) {
            return resultado;
        }

        String[] lineas = codigo.split("\n");
        int anchoNumero = String.valueOf(lineas.length).length();
        String margen = "  " + " ".repeat(anchoNumero);

        boolean dentroDeFuncion = false;

        for (int i = 0; i < lineas.length; i++) {

            String linea = lineas[i];
            String texto;

            if (esInicioDeFuncion(linea)) {
                resultado.add(pintar(GRIS, margen + " ├" + "─".repeat(50)));
                texto = pintar(AMARILLO, linea);
                dentroDeFuncion = true;

            } else if (esEtiquetaDeEstructura(linea)) {
                texto = SANGRIA_ETIQUETA + pintar(CIAN, linea);

            } else if (esFinDeFuncion(linea)) {
                texto = pintar(AMARILLO_SUAVE, linea);
                dentroDeFuncion = false;

            } else if (linea.isEmpty()) {
                texto = "";

            } else {
                texto = (dentroDeFuncion ? SANGRIA_INSTRUCCION : "") + colorearInstruccion(linea);
            }

            String numero = pintar(GRIS, String.format("  %" + anchoNumero + "d │", i + 1));

            resultado.add(texto.isEmpty() ? numero : numero + " " + texto);
        }

        return resultado;
    }

    /* Los codigos ANSI de color: ESC[36m, ESC[0m... */
    private static final Pattern PATRON_COLOR = Pattern.compile("\u001B\\[[0-9;]*m");

    /*
     * Descripcion: Quita los colores de un texto.
     * Entrada: Texto con colores.
     * Salida: Texto sin colores.
     * Restricciones: Se usa antes de guardar en un archivo.
     */
    private static String quitarColores(String texto) {
        return PATRON_COLOR.matcher(texto).replaceAll("");
    }

    /*
     * Descripcion: Le pone color a una instruccion.
     * Entrada: Linea de codigo.
     * Salida: La linea con colores.
     * Restricciones: El color depende del tipo de instruccion.
     */
    private static String colorearInstruccion(String linea) {

        /* declaracion# int x,  declaracionPrevia# int x  y  param_int x */
        if (esDeclaracion(linea)) {
            return pintar(VERDE_SUAVE, linea);
        }

        /* goto destino */
        if (linea.startsWith("goto ")) {
            return pintar(NEGRITA, "goto") + " " + pintar(CIAN_SUAVE, linea.substring("goto ".length()));
        }

        /* if condicion goto destino */
        if (linea.startsWith("if ") && linea.contains(" goto ")) {

            int posicionGoto = linea.indexOf(" goto ");
            String condicion = linea.substring("if ".length(), posicionGoto);
            String destino = linea.substring(posicionGoto + " goto ".length());

            return pintar(NEGRITA, "if") + " " + condicion + " "
                    + pintar(NEGRITA, "goto") + " " + pintar(CIAN_SUAVE, destino);
        }

        /* temporal = call funcion, n   y   call funcion, n */
        if (linea.startsWith("call ") || linea.contains(" = call ")) {

            int posicionCall = linea.indexOf("call ");
            String antes = linea.substring(0, posicionCall);
            String resto = linea.substring(posicionCall + "call ".length());

            int coma = resto.indexOf(',');
            String funcion = coma >= 0 ? resto.substring(0, coma) : resto;
            String despues = coma >= 0 ? resto.substring(coma) : "";

            return antes + pintar(NEGRITA, "call") + " " + pintar(AMARILLO_SUAVE, funcion) + despues;
        }

        /* return, param, read y write */
        for (String palabra : new String[] {"return", "param", "read", "write"}) {
            if (linea.equals(palabra) || linea.startsWith(palabra + " ")) {
                return pintar(NEGRITA, palabra) + linea.substring(palabra.length());
            }
        }

        /* Asignaciones y operaciones: solo el operador lleva color */
        return colorearOperador(linea);
    }

    private static final Set<String> OPERADORES = new LinkedHashSet<>(java.util.Arrays.asList(
            "+", "-", "*", "//", "/", "mod", "pot",
            "<", ">", "<=", ">=", "==", "!=",
            "and", "or", "not"));

    /*
     * Descripcion: Pinta de rojo el operador de una operacion.
     * Entrada: Linea de codigo.
     * Salida: La linea con el operador en rojo.
     * Restricciones: No toca textos, caracteres ni asignaciones simples.
     */
    private static String colorearOperador(String linea) {

        int posicionIgual = linea.indexOf(" = ");

        if (posicionIgual < 0) {
            return linea;
        }

        String destino = linea.substring(0, posicionIgual);
        String valor = linea.substring(posicionIgual + " = ".length());

        /* Un texto o un caracter puede tener espacios adentro: no se toca */
        if (valor.startsWith("\"") || valor.startsWith("'")) {
            return linea;
        }

        String[] partes = valor.split(" ");

        if (partes.length == 3 && OPERADORES.contains(partes[1])) {
            return destino + " = " + partes[0] + " " + pintar(ROJO_SUAVE, partes[1]) + " " + partes[2];
        }

        if (partes.length == 2 && OPERADORES.contains(partes[0])) {
            return destino + " = " + pintar(ROJO_SUAVE, partes[0]) + " " + partes[1];
        }

        return linea;
    }


    /* ============================================================
       TABLA DE SIMBOLOS EN CONSOLA
       ============================================================ */

    /*
     * Descripcion: Muestra donde quedo guardada la tabla de simbolos.
     * Entrada: Archivo del reporte.
     * Salida: Cuadro verde en consola.
     * Restricciones: Ninguna.
     */
    private static void mostrarCuadroReporte(File reporte) {

        String titulo = " TABLA DE SÍMBOLOS GENERADA ";
        String mensaje = "✔ La tabla de símbolos se guardó correctamente.";

        System.out.println(pintar(VERDE, "┌─" + titulo
                + "─".repeat(Math.max(1, ANCHO - 3 - titulo.length())) + "┐"));

        filaCuadro("", "", VERDE);
        filaCuadro(mensaje, pintar(VERDE, mensaje), VERDE);
        filaCuadro("", "", VERDE);

        mostrarDatoEnCuadro("Archivo", reporte.getName(), BLANCO, VERDE);
        mostrarDatoEnCuadro("Carpeta", carpetaCorta(reporte), CIAN_SUAVE, VERDE);
        mostrarDatoEnCuadro("Ruta", reporte.getAbsolutePath(), GRIS, VERDE);

        filaCuadro("", "", VERDE);
        System.out.println(pintar(VERDE, "└" + "─".repeat(ANCHO - 2) + "┘"));
    }

    /*
     * Descripcion: Muestra un dato dentro de un cuadro.
     * Entrada: Etiqueta, valor, color y color del borde.
     * Salida: Una o varias filas.
     * Restricciones: Las rutas largas se cortan despues de una barra.
     */
    private static void mostrarDatoEnCuadro(String etiqueta, String valor, String color, String colorBorde) {

        String prefijo = String.format("%-8s : ", etiqueta);
        int ancho = ANCHO - 4 - prefijo.length();

        List<String> partes = new ArrayList<>();
        String resto = valor;

        while (resto.length() > ancho) {

            int ultimaBarra = Math.max(resto.lastIndexOf(File.separatorChar, ancho - 1),
                                       resto.lastIndexOf('/', ancho - 1));

            int corte = ultimaBarra > 0 ? ultimaBarra + 1 : ancho;

            partes.add(resto.substring(0, corte));
            resto = resto.substring(corte);
        }

        partes.add(resto);

        for (int i = 0; i < partes.size(); i++) {

            String inicio = i == 0 ? prefijo : " ".repeat(prefijo.length());
            String inicioConColor = i == 0 ? pintar(NEGRITA, inicio) : inicio;

            filaCuadro(inicio + partes.get(i), inicioConColor + pintar(color, partes.get(i)), colorBorde);
        }
    }

    /* Columnas de la tabla de simbolos en consola y su ancho */
    private static final String[] TITULOS_TABLA = {
        "Nº", "NOMBRE", "CATEGORÍA", "TIPO", "ÁMBITO", "BYTES",
        "DIMENSIÓN", "DIRECCIÓN", "VALOR INICIAL", "ACCESO", "LÍNEA", "COL"};

    private static final int[] ANCHOS_TABLA = {3, 18, 13, 7, 24, 5, 10, 11, 22, 8, 5, 3};

    /*
     * Descripcion: Muestra la tabla de simbolos en consola.
     * Entrada: Tabla de simbolos.
     * Salida: Arbol de ambitos y tabla.
     * Restricciones: Es ancha: conviene una terminal grande.
     */
    private static void mostrarTablaSimbolos(TablaSimbolos tabla) {

        for (String linea : formatearTablaSimbolos(tabla)) {
            System.out.println(linea);
        }
    }

    /*
     * Descripcion: Prepara la tabla para guardarla en un archivo.
     * Entrada: Tabla de simbolos.
     * Salida: La tabla como texto, sin colores.
     * Restricciones: Queda igual que en consola.
     */
    private static String tablaSimbolosParaArchivo(TablaSimbolos tabla) {

        StringBuilder texto = new StringBuilder();

        for (String linea : formatearTablaSimbolos(tabla)) {
            texto.append(quitarColores(linea)).append("\n");
        }

        return texto.toString();
    }

    /*
     * Descripcion: Arma la tabla de simbolos linea por linea.
     * Entrada: Tabla de simbolos.
     * Salida: Lista de lineas.
     * Restricciones: La usan la consola y el archivo.
     */
    private static List<String> formatearTablaSimbolos(TablaSimbolos tabla) {

        List<String> lineas = new ArrayList<>();

        lineas.add(pintar(NEGRITA, "  Árbol de ámbitos"));
        lineas.add("");

        for (String linea : tabla.arbolDeAmbitos().split("\n")) {
            lineas.add(pintar(CIAN_SUAVE, linea));
        }

        lineas.add("");

        lineas.add("  " + pintar(NEGRITA, "Categorías: ")
                + pintar(colorCategoria("variable"), "variable") + "  "
                + pintar(colorCategoria("arreglo"), "arreglo") + "  "
                + pintar(colorCategoria("parámetro"), "parámetro") + "  "
                + pintar(colorCategoria("función"), "función") + "  "
                + pintar(colorCategoria("procedimiento"), "procedimiento"));

        lineas.add(pintar(GRIS, "  Dirección:   global+n = zona estática                 ----   pila+n = registro de activación"));
        lineas.add(pintar(GRIS, "  Acceso:      público = declarado en el ámbito global ------- privado = dentro de una función o bloque"));
        lineas.add("");

        lineas.add(pintar(GRIS, "  " + bordeTabla('┌', '┬', '┐')));

        String[] coloresTitulos = new String[TITULOS_TABLA.length];
        java.util.Arrays.fill(coloresTitulos, CIAN);
        filaTabla(lineas, TITULOS_TABLA, coloresTitulos);

        lineas.add(pintar(GRIS, "  " + bordeTabla('├', '┼', '┤')));

        int numero = 1;
        boolean esPrimerAmbito = true;

        for (Ambito ambito : tabla.getAmbitos()) {

            /* Un ambito sin simbolos no tiene filas: solo aparece en el arbol */
            if (ambito.cantidad() == 0) {
                continue;
            }

            if (!esPrimerAmbito) {
                lineas.add(pintar(GRIS, "  " + bordeTabla('├', '┼', '┤')));
            }
            esPrimerAmbito = false;

            String nombreAmbito = nombreCortoAmbito(ambito);

            for (Simbolo simbolo : ambito.simbolos()) {
                filaTabla(lineas, celdasDelSimbolo(numero++, simbolo, nombreAmbito), coloresDelSimbolo(simbolo));
            }
        }

        lineas.add(pintar(GRIS, "  " + bordeTabla('└', '┴', '┘')));
        lineas.add("");

        return lineas;
    }

    /*
     * Descripcion: Arma las celdas de una fila de la tabla.
     * Entrada: Numero, simbolo y nombre del ambito.
     * Salida: Textos de cada columna.
     * Restricciones: Ninguna.
     */
    private static String[] celdasDelSimbolo(int numero, Simbolo simbolo, String nombreAmbito) {

        boolean esFuncion = simbolo.esFuncion();

        return new String[] {
            String.valueOf(numero),
            simbolo.nombre,
            simbolo.categoria,
            simbolo.tipo,
            nombreAmbito,
            esFuncion ? "—" : (simbolo.bytes > 0 ? String.valueOf(simbolo.bytes) : "?"),
            esFuncion ? "—" : simbolo.textoDimensionCorta(),
            simbolo.direccion(),
            esFuncion || simbolo.valorInicial == null ? "—" : simbolo.valorInicial,
            simbolo.acceso,
            String.valueOf(simbolo.linea),
            String.valueOf(simbolo.columna)
        };
    }

    /*
     * Descripcion: Elige el color de cada celda de una fila.
     * Entrada: Simbolo.
     * Salida: Color de cada columna.
     * Restricciones: Ninguna.
     */
    private static String[] coloresDelSimbolo(Simbolo simbolo) {
        return new String[] {
            SIN_COLOR,
            NEGRITA,
            colorCategoria(simbolo.categoria),
            SIN_COLOR,
            CIAN_SUAVE,
            SIN_COLOR,
            "arreglo".equals(simbolo.categoria) ? AMARILLO_SUAVE : GRIS,
            SIN_COLOR,
            SIN_COLOR,
            "público".equals(simbolo.acceso) ? VERDE_SUAVE : GRIS,
            SIN_COLOR,
            SIN_COLOR
        };
    }

    /*
     * Descripcion: Acorta el nombre de un ambito.
     * Entrada: Ambito.
     * Salida: Texto como "funcion f1 › if".
     * Restricciones: El global se queda como "global".
     */
    private static String nombreCortoAmbito(Ambito ambito) {

        String ruta = ambito.ruta();

        if (ambito.nivel > 0 && ruta.startsWith("global › ")) {
            ruta = ruta.substring("global › ".length());
        }

        return ruta.replace("funcion ", "función ");
    }

    /*
     * Descripcion: Arma una linea de borde de la tabla.
     * Entrada: Caracteres de izquierda, medio y derecha.
     * Salida: Linea como ┌───┬───┐.
     * Restricciones: Ninguna.
     */
    private static String bordeTabla(char izquierda, char medio, char derecha) {

        StringBuilder borde = new StringBuilder().append(izquierda);

        for (int i = 0; i < ANCHOS_TABLA.length; i++) {
            borde.append("─".repeat(ANCHOS_TABLA[i] + 2));
            borde.append(i < ANCHOS_TABLA.length - 1 ? medio : derecha);
        }

        return borde.toString();
    }

    /*
     * Descripcion: Agrega una fila a la tabla.
     * Entrada: Lista de salida, celdas y colores.
     * Salida: Una o varias lineas agregadas.
     * Restricciones: Si una celda es larga, la fila ocupa varias lineas.
     */
    private static void filaTabla(List<String> salida, String[] celdas, String[] colores) {

        List<List<String>> lineasPorCelda = new ArrayList<>();
        int alto = 1;

        for (int i = 0; i < ANCHOS_TABLA.length; i++) {
            List<String> lineas = partirEnLineas(celdas[i], ANCHOS_TABLA[i]);
            lineasPorCelda.add(lineas);
            alto = Math.max(alto, lineas.size());
        }

        for (int fila = 0; fila < alto; fila++) {

            StringBuilder linea = new StringBuilder("  " + pintar(GRIS, "│"));

            for (int i = 0; i < ANCHOS_TABLA.length; i++) {

                List<String> lineasCelda = lineasPorCelda.get(i);
                String contenido = fila < lineasCelda.size() ? lineasCelda.get(fila) : "";
                String texto = String.format("%-" + ANCHOS_TABLA[i] + "s", contenido);

                linea.append(" ")
                     .append(SIN_COLOR.equals(colores[i]) ? texto : pintar(colores[i], texto))
                     .append(" ")
                     .append(pintar(GRIS, "│"));
            }

            salida.add(linea.toString());
        }
    }

    /*
     * Descripcion: Elige el color de una categoria.
     * Entrada: Categoria.
     * Salida: Color.
     * Restricciones: Ninguna.
     */
    private static String colorCategoria(String categoria) {
        switch (categoria) {
            case "variable":      return VERDE_SUAVE;
            case "arreglo":       return AMARILLO_SUAVE;
            case "parámetro":     return MAGENTA_SUAVE;
            case "función":       return AZUL;
            case "procedimiento": return CIAN;
            default:              return SIN_COLOR;
        }
    }


    /* ============================================================
       PARSER CON CONTROL DE ERRORES
       ============================================================ */

    /*
     * Descripcion: Parser que guarda los errores en vez de imprimirlos.
     * Entrada: Tokens del Lexer o de una lista.
     * Salida: Errores sintacticos y si el analisis se interrumpio.
     * Restricciones: Main muestra todos los errores con el mismo formato.
     */
    private static class ParserConErrores extends parser {

        private final List<ErrorSintactico> errores = new ArrayList<>();

        /* true si el parser no pudo recuperarse de un error y se detuvo */
        private boolean interrumpido = false;

        /* Ultimo token que no es fin de archivo, para ubicar errores al final */
        private Symbol ultimoTokenReal = null;

        /* Todos los tokens que el parser leyo, en orden (la correccion global los vuelve a analizar) */
        private final List<Symbol> flujo = new ArrayList<>();

        /*
         * Descripcion: Crea el parser.
         * Entrada: De donde salen los tokens.
         * Salida: El parser listo.
         * Restricciones: Ninguna.
         */
        ParserConErrores(java_cup.runtime.Scanner lexer) {
            super(lexer);
        }

        /*
         * Descripcion: Busca la posicion de un token.
         * Entrada: Token.
         * Salida: Su posicion, o -1 si no esta.
         * Restricciones: Ninguna.
         */
        private int posicionEnFlujo(Symbol token) {
            for (int i = flujo.size() - 1; i >= 0; i--) {
                if (flujo.get(i) == token) {
                    return i;
                }
            }
            return -1;
        }

        /*
         * Descripcion: Dice cuantos tokens correctos hacen falta para seguir.
         * Entrada: Ninguna.
         * Salida: 1.
         * Restricciones: Ninguna.
         */
        @Override
        protected int error_sync_size() {
            return 1;
        }

        /*
         * Descripcion: Pide el siguiente token y lo guarda.
         * Entrada: Ninguna.
         * Salida: El token leido.
         * Restricciones: Recuerda el ultimo token real.
         */
        @Override
        public Symbol scan() throws Exception {

            Symbol token = super.scan();

            if (token != null) {
                flujo.add(token);

                if (token.sym != sym.EOF) {
                    ultimoTokenReal = token;
                }
            }

            return token;
        }

        /*
         * Descripcion: Guarda un error de modo panico.
         * Entrada: Token donde fallo.
         * Salida: Error agregado a la lista.
         * Restricciones: Ninguna.
         */
        @Override
        public void syntax_error(Symbol token) {

            ErrorSintactico error = new ErrorSintactico();

            if (token != null) {

                error.alFinalDelArchivo = token.sym == sym.EOF;
                error.token = nombreToken(token.sym);
                error.lexema = token.value == null ? null : String.valueOf(token.value);

                /* El Lexer entrega linea y columna desde 1 */
                if (token.left > 0) {
                    error.linea = token.left;
                    error.columna = token.right;
                } else if (error.alFinalDelArchivo && ultimoTokenReal != null) {
                    error.linea = ultimoTokenReal.left;
                    error.columna = 0;
                }
            }

            error.esperados = textosEsperados();
            error.posicionEnFlujo = posicionEnFlujo(token);

            try {
                for (int numero : expected_token_ids()) {
                    error.numerosEsperados.add(numero);
                }
            } catch (Exception e) {
                /* Sin tokens esperados */
            }

            errores.add(error);
        }

        /*
         * Descripcion: Evita que CUP imprima mensajes.
         * Entrada: Mensaje e informacion.
         * Salida: Ninguna.
         * Restricciones: Main muestra los errores.
         */
        @Override
        public void report_error(String message, Object info) {
        }

        /*
         * Descripcion: Marca que el parser se rindio.
         * Entrada: Mensaje e informacion.
         * Salida: Ninguna.
         * Restricciones: Ninguna.
         */
        @Override
        public void report_fatal_error(String message, Object info) {
            interrumpido = true;
        }

        /*
         * Descripcion: Guarda un error comun que la gramatica reconoce.
         * Entrada: Linea, columna y mensaje.
         * Salida: Error agregado a la lista.
         * Restricciones: Ninguna.
         */
        @Override
        public void errorProduccion(int linea, int columna, String mensaje) {

            ErrorSintactico error = new ErrorSintactico();

            error.linea = linea;
            error.columna = columna;
            error.token = "ERROR";
            error.tecnica = "Producción de error";
            error.mensaje = Character.toUpperCase(mensaje.charAt(0)) + mensaje.substring(1) + ".";

            errores.add(error);
        }

        /*
         * Descripcion: Guarda un simbolo que faltaba y que el parser agrego.
         * Entrada: Simbolo, linea y columna.
         * Salida: Error agregado a la lista.
         * Restricciones: Ninguna.
         */
        @Override
        public void tokenFaltante(String token, int linea, int columna) {

            ErrorSintactico error = new ErrorSintactico();

            error.linea = linea;
            error.columna = columna;
            error.tecnica = "Recuperación a nivel de frase";
            error.esperados.add(token);

            String queFalta;

            switch (token) {
                case ":э":
                    error.token = "PARENCL";
                    queFalta = "Falta el paréntesis de cierre :э.";
                    break;
                case ":?":
                    error.token = "CLBLOCK";
                    queFalta = "Falta el cierre de bloque :? de la función.";
                    break;
                default:
                    error.token = "TERMINATOR";
                    queFalta = "Falta el terminador » al final de la sentencia.";
            }

            error.mensaje = queFalta + " Se insertó y se siguió leyendo.";

            errores.add(error);
        }

        /*
         * Descripcion: Lista los tokens que se esperaban.
         * Entrada: Ninguna.
         * Salida: Lista de textos.
         * Restricciones: Maximo 10.
         */
        private List<String> textosEsperados() {

            Set<String> textos = new LinkedHashSet<>();

            try {
                for (int numero : expected_token_ids()) {
                    if (numero != sym.error) {
                        textos.add(textoLegible(nombreToken(numero)));
                    }
                }
            } catch (Exception e) {
                return new ArrayList<>();
            }

            List<String> lista = new ArrayList<>(textos);

            if (lista.size() > 10) {
                lista = new ArrayList<>(lista.subList(0, 10));
                lista.add("…");
            }

            return lista;
        }
    }
}