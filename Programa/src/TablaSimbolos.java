import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Descripcion: Administra la tabla de símbolos y los ámbitos.
 * Entrada: Declaraciones y consultas realizadas por el parser.
 * Salida: Ámbitos, símbolos y errores semánticos.
 * Restricciones: Esta clase guarda y busca símbolos, pero las reglas
 * semánticas se revisan en Parser.cup.
 */
public class TablaSimbolos {

    // ============================================================
    // TAMAÑOS DE LOS TIPOS
    // ============================================================

    /**
     * Descripcion: Guarda el tamaño de cada tipo de dato.
     * Entrada: Nombre de cada tipo.
     * Salida: Tamaño correspondiente en bytes.
     * Restricciones: Los tipos tienen un tamaño definido.
     */
    private static final Map<String, Integer> BYTES_POR_TIPO = new HashMap<>();

    static {
        BYTES_POR_TIPO.put("int", 4);
        BYTES_POR_TIPO.put("float", 4);
        BYTES_POR_TIPO.put("bool", 1);
        BYTES_POR_TIPO.put("char", 1);
        BYTES_POR_TIPO.put("string", 4);
        BYTES_POR_TIPO.put("void", 0);
    }

    /**
     * Descripcion: Obtiene el tamaño de un tipo de dato.
     * Entrada: Nombre del tipo.
     * Salida: Cantidad de bytes.
     * Restricciones: Si el tipo no existe, devuelve 0.
     */
    public static int bytesDelTipo(String tipo) {
        return BYTES_POR_TIPO.getOrDefault(tipo, 0);
    }

    // ============================================================
    // DATOS DE LA TABLA
    // ============================================================

    /**
     * Descripcion: Guarda el ámbito en el que se está trabajando.
     * Entrada: Ámbito actual.
     * Salida: Referencia al ámbito activo.
     * Restricciones: Solo puede haber un ámbito actual.
     */
    private Ambito ambitoActual;

    /**
     * Descripcion: Guarda todos los ámbitos creados.
     * Entrada: Ámbitos registrados.
     * Salida: Lista de ámbitos en orden de creación.
     * Restricciones: Incluye el ámbito global.
     */
    private final List<Ambito> ambitos = new ArrayList<>();

    /**
     * Descripcion: Guarda los errores semánticos encontrados.
     * Entrada: Mensajes de error.
     * Salida: Lista de errores.
     * Restricciones: No revisa las reglas semánticas.
     */
    private final List<String> errores = new ArrayList<>();

    /**
     * Descripcion: Guarda los datos del próximo ámbito.
     * Entrada: Nombre del ámbito, función y parámetros pendientes.
     * Salida: Información lista para abrir el siguiente ámbito.
     * Restricciones: Se utiliza porque algunos datos se leen antes de abrir el ámbito.
     */
    private String nombreProximoAmbito = "bloque";
    private Simbolo funcionPendiente = null;
    private final List<Simbolo> parametrosPendientes = new ArrayList<>();

    /**
     * Descripcion: Crea la tabla de símbolos.
     * Entrada: Ninguna.
     * Salida: Tabla con el ámbito global creado.
     * Restricciones: El ámbito global es el primero de la lista.
     */
    public TablaSimbolos() {
        ambitoActual = new Ambito(null, "global", true);
        ambitos.add(ambitoActual);
    }

    // ============================================================
    // ÁMBITOS
    // ============================================================

    /**
     * Descripcion: Establece el nombre del próximo ámbito.
     * Entrada: Nombre del ámbito.
     * Salida: Nombre guardado para el siguiente ámbito.
     * Restricciones: Se aplica al próximo ámbito que se abra.
     */
    public void siguienteAmbito(String nombre) {
        nombreProximoAmbito = nombre;
    }

    /**
     * Descripcion: Abre un nuevo ámbito.
     * Entrada: Nombre guardado para el próximo ámbito.
     * Salida: Nuevo ámbito dentro del actual.
     * Restricciones: Utiliza el nombre establecido anteriormente.
     */
    public void abrirAmbito() {
        abrirAmbito(nombreProximoAmbito);
    }

    /**
     * Descripcion: Crea un ámbito dentro del ámbito actual.
     * Entrada: Nombre del nuevo ámbito.
     * Salida: Nuevo ámbito agregado a la lista.
     * Restricciones: Si pertenece a una función, también la enlaza con su ámbito.
     */
    public void abrirAmbito(String nombre) {

        boolean esCuerpoDeFuncion = funcionPendiente != null;

        ambitoActual = new Ambito(ambitoActual, nombre, esCuerpoDeFuncion);
        ambitos.add(ambitoActual);

        if (esCuerpoDeFuncion) {
            enlazarConSuFuncion();
        }

        nombreProximoAmbito = "bloque";
        declararParametrosPendientes();
    }

    /**
     * Descripcion: Relaciona una función con su ámbito.
     * Entrada: Función pendiente y ámbito actual.
     * Salida: Ambos quedan relacionados.
     * Restricciones: Se utiliza al abrir el cuerpo de una función.
     */
    private void enlazarConSuFuncion() {
        ambitoActual.funcionDuena = funcionPendiente;
        funcionPendiente.tablaHija = ambitoActual;
        funcionPendiente = null;
    }

    /**
     * Descripcion: Declara los parámetros pendientes.
     * Entrada: Lista de parámetros de una función.
     * Salida: Parámetros agregados al ámbito actual.
     * Restricciones: La lista se limpia después de declararlos.
     */
    private void declararParametrosPendientes() {
        for (Simbolo parametro : parametrosPendientes) {
            declarar(parametro);
        }
        parametrosPendientes.clear();
    }

    /**
     * Descripcion: Cierra el ámbito actual y regresa al anterior.
     * Entrada: Ámbito que se encuentra abierto.
     * Salida: Ámbito padre como nuevo ámbito actual.
     * Restricciones: No permite cerrar el ámbito global.
     * Si pertenece a una función, guarda el tamaño de su registro.
     */
    public void cerrarAmbito() {

        if (ambitoActual.esGlobal()) {
            return;
        }

        Simbolo funcion = ambitoActual.funcionDuena;

        if (funcion != null) {
            funcion.bytes = ambitoActual.marco.siguienteDireccion;
        }

        ambitoActual = ambitoActual.padre;
    }

    // ============================================================
    // DECLARACIONES
    // ============================================================

    /**
     * Descripcion: Agrega un símbolo al ámbito actual.
     * Entrada: Símbolo que se desea declarar.
     * Salida: null si se agrega o el símbolo existente si está repetido.
     * Restricciones: No permite nombres repetidos en el mismo ámbito.
     */
    public Simbolo declarar(Simbolo simbolo) {

        if (simbolo == null || simbolo.nombre == null) {
            return null;
        }

        Simbolo existente = ambitoActual.buscarSoloAqui(simbolo.nombre);

        if (existente != null) {
            return existente;
        }

        simbolo.ambito = ambitoActual;
        simbolo.acceso = ambitoActual.esGlobal() ? "público" : "privado";

        // Las funciones no ocupan espacio en memoria de datos.
        if (!simbolo.esFuncion()) {
            reservarDireccion(simbolo);
        }

        ambitoActual.agregar(simbolo);
        return null;
    }

    /**
     * Descripcion: Asigna una dirección de memoria al símbolo.
     * Entrada: Símbolo que se desea ubicar.
     * Salida: Símbolo con dirección y segmento asignados.
     * Restricciones: Utiliza la siguiente dirección libre del ámbito.
     */
    private void reservarDireccion(Simbolo simbolo) {

        Ambito marco = ambitoActual.marco;

        simbolo.desplazamiento = marco.siguienteDireccion;
        simbolo.segmento = marco.esGlobal() ? "global" : "pila";

        if (simbolo.bytes > 0) {
            marco.siguienteDireccion += simbolo.bytes;
        }
    }

    /**
     * Descripcion: Declara una variable en el ámbito actual.
     * Entrada: Nombre, tipo, valor inicial, línea y columna.
     * Salida: Símbolo declarado o símbolo repetido.
     * Restricciones: El valor inicial conserva su signo negativo.
     */
    public Simbolo declararVariable(String nombre, String tipo, String valorInicial, int linea, int columna) {

        Simbolo variable = new Simbolo(nombre, tipo, Simbolo.VARIABLE, linea, columna);
        variable.valorInicial = unirSignoNegativo(valorInicial);

        return declarar(variable);
    }

    /**
     * Descripcion: Declara un arreglo en el ámbito actual.
     * Entrada: Nombre, tipo, tamaño, cantidad de valores y ubicación.
     * Salida: Símbolo del arreglo declarado.
     * Restricciones: Si no se conoce la cantidad de elementos, se guarda -1.
     */
    public Simbolo declararArreglo(String nombre, String tipo, String tamano, int cantidadValores,
                                   String valorInicial, int linea, int columna) {

        Simbolo arreglo = new Simbolo(nombre, tipo, Simbolo.ARREGLO, linea, columna);

        arreglo.valorInicial = unirSignoNegativo(valorInicial);
        arreglo.elementos = cantidadDeElementos(tamano, cantidadValores);
        arreglo.bytes = arreglo.elementos > 0 ? arreglo.elementos * bytesDelTipo(tipo) : 0;

        return declarar(arreglo);
    }

    /**
     * Descripcion: Calcula la cantidad de elementos de un arreglo.
     * Entrada: Tamaño escrito y cantidad de valores.
     * Salida: Cantidad de elementos o -1 si se desconoce.
     * Restricciones: Si el tamaño no es un número válido, devuelve -1.
     */
    private static int cantidadDeElementos(String tamano, int cantidadValores) {

        if (tamano == null) {
            return cantidadValores > 0 ? cantidadValores : -1;
        }

        try {
            return Integer.parseInt(tamano.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Descripcion: Declara una función o procedimiento.
     * Entrada: Nombre, tipo de retorno, parámetros y ubicación.
     * Salida: Símbolo de la función o símbolo repetido.
     * Restricciones: Si el tipo es void, se declara como procedimiento.
     * Sus parámetros quedan pendientes hasta abrir su ámbito.
     */
    public Simbolo declararFuncion(String nombre, String tipo, List<Simbolo> parametros, int linea, int columna) {

        boolean esProcedimiento = "void".equals(tipo);
        String categoria = esProcedimiento ? Simbolo.PROCEDIMIENTO : Simbolo.FUNCION;

        Simbolo funcion = new Simbolo(nombre, tipo, categoria, linea, columna);

        if (parametros != null) {
            funcion.parametros.addAll(parametros);
            parametrosPendientes.addAll(parametros);
        }

        Simbolo existente = declarar(funcion);

        siguienteAmbito((esProcedimiento ? "procedimiento " : "funcion ") + nombre);
        funcionPendiente = funcion;

        return existente;
    }

    /**
     * Descripcion: Une el signo negativo con el número correspondiente.
     * Entrada: Texto con el valor o expresión.
     * Salida: Texto con los signos negativos unidos.
     * Restricciones: Si el valor es null, devuelve null.
     */
    private static String unirSignoNegativo(String valor) {

        if (valor == null) {
            return null;
        }

        return valor.replaceAll("(^|[-+*/,Ͱ]\\s|mod\\s|pot\\s|є:\\s|ʃ:\\s|¿:\\s)-\\s(?=\\d)", "$1-");
    }

    // ============================================================
    // USOS Y ERRORES
    // ============================================================

    /**
     * Descripcion: Busca un símbolo desde el ámbito actual.
     * Entrada: Nombre del símbolo.
     * Salida: Símbolo encontrado o null si no existe.
     * Restricciones: Busca también en los ámbitos que contienen al actual.
     */
    public Simbolo buscar(String nombre) {
        return nombre == null ? null : ambitoActual.buscar(nombre);
    }

    /**
     * Descripcion: Guarda un error semántico.
     * Entrada: Línea, columna y mensaje del error.
     * Salida: Error agregado a la lista.
     * Restricciones: No revisa el error, solo guarda su mensaje.
     */
    public void errorSemantico(int linea, int columna, String mensaje) {
        errores.add("Error semántico en línea " + linea + ", columna " + columna + ": " + mensaje);
    }

    /**
     * Descripcion: Obtiene los errores semánticos.
     * Entrada: Ninguna.
     * Salida: Lista de errores guardados.
     * Restricciones: Devuelve los errores registrados.
     */
    public List<String> getErrores() {
        return errores;
    }

    // ============================================================
    // CONSULTAS
    // ============================================================

    /**
     * Descripcion: Obtiene todos los ámbitos creados.
     * Entrada: Ninguna.
     * Salida: Lista de ámbitos.
     * Restricciones: Mantiene el orden en que fueron creados.
     */
    public List<Ambito> getAmbitos() {
        return ambitos;
    }

    /**
     * Descripcion: Obtiene el ámbito global.
     * Entrada: Ninguna.
     * Salida: Primer ámbito de la lista.
     * Restricciones: El ámbito global siempre está en la posición cero.
     */
    public Ambito ambitoGlobal() {
        return ambitos.get(0);
    }

    /**
     * Descripcion: Cuenta todos los símbolos registrados.
     * Entrada: Lista de ámbitos.
     * Salida: Cantidad total de símbolos.
     * Restricciones: Suma los símbolos de todos los ámbitos.
     */
    public int cantidadSimbolos() {

        int total = 0;

        for (Ambito ambito : ambitos) {
            total += ambito.cantidad();
        }

        return total;
    }

    /**
     * Descripcion: Genera el árbol de ámbitos.
     * Entrada: Ámbito global.
     * Salida: Árbol de ámbitos en forma de texto.
     * Restricciones: Utiliza el formato de ramas del reporte.
     */
    public String arbolDeAmbitos() {
        return ReporteTablaSimbolos.dibujarArbol(ambitoGlobal());
    }

    // ============================================================
    // REPORTE EN ARCHIVO
    // ============================================================

    /**
     * Descripcion: Define la carpeta donde se guarda el reporte.
     * Entrada: Separador de carpetas del sistema.
     * Salida: Ruta de la carpeta salida/tabla_simbolos.
     * Restricciones: Se guarda dentro de la carpeta Programa.
     */
    public static final String CARPETA_REPORTE = "salida" + File.separator + "tabla_simbolos";

    /**
     * Descripcion: Busca la carpeta donde se guardará el reporte.
     * Entrada: Ubicación de los archivos compilados.
     * Salida: Carpeta salida/tabla_simbolos.
     * Restricciones: Si no encuentra la ubicación, utiliza la carpeta actual.
     */
    public static File carpetaReporte() {
        try {
            File carpetaBin = new File(TablaSimbolos.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());

            return new File(carpetaBin.getParentFile(), CARPETA_REPORTE);

        } catch (Exception e) {
            // Si no encuentra la carpeta, utiliza la ruta actual.
            return new File(CARPETA_REPORTE);
        }
    }

    /**
     * Descripcion: Genera y guarda el reporte de la tabla de símbolos.
     * Entrada: Archivo fuente analizado.
     * Salida: Archivo de texto con el reporte.
     * Restricciones: El nombre del reporte se obtiene del archivo fuente.
     */
    public File guardarReporte(File fuente) throws IOException {
        return guardarReporte(fuente, new ReporteTablaSimbolos(this).generar(fuente.getName()));
    }

    /**
     * Descripcion: Guarda un texto en un archivo.
     * Entrada: Archivo fuente y contenido del reporte.
     * Salida: Archivo de texto creado.
     * Restricciones: Utiliza UTF-8 y guarda el archivo en la carpeta del reporte.
     */
    public File guardarReporte(File fuente, String contenido) throws IOException {

        File carpeta = carpetaReporte();

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        File reporte = new File(carpeta, nombreSinExtension(fuente) + "_tabla_simbolos.txt");

        Files.writeString(reporte.toPath(), contenido, StandardCharsets.UTF_8);

        return reporte;
    }

    /**
     * Descripcion: Obtiene el nombre de un archivo sin su extensión.
     * Entrada: Archivo que se desea consultar.
     * Salida: Nombre del archivo sin extensión.
     * Restricciones: Si no tiene extensión, devuelve el nombre completo.
     */
    private static String nombreSinExtension(File archivo) {

        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');

        return punto > 0 ? nombre.substring(0, punto) : nombre;
    }
}