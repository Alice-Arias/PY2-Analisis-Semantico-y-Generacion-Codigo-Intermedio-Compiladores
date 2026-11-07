import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descripcion: Genera un reporte de la tabla de símbolos.
 * Entrada: Tabla de símbolos y nombre del archivo fuente.
 * Salida: Reporte con ámbitos, símbolos, funciones y errores.
 * Restricciones: No modifica la tabla de símbolos.
 */
public class ReporteTablaSimbolos {

    private static final String LINEA_DOBLE = "═".repeat(140);

    private static final Pattern PATRON_ERROR = Pattern.compile(
            "Error \\S+ en línea (\\d+), columna (\\d+): (.*)");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TablaSimbolos tabla;
    private final StringBuilder texto = new StringBuilder();

    /**
     * Descripcion: Guarda la tabla de símbolos.
     * Entrada: Tabla de símbolos.
     * Salida: Objeto inicializado.
     * Restricciones: La tabla no puede ser null.
     */
    ReporteTablaSimbolos(TablaSimbolos tabla) {
        this.tabla = tabla;
    }

    /**
     * Descripcion: Genera todas las partes del reporte.
     * Entrada: Nombre del archivo fuente.
     * Salida: Texto completo del reporte.
     * Restricciones: Utiliza los datos de la tabla recibida.
     */
    String generar(String nombreFuente) {

        agregarEncabezado(nombreFuente);
        agregarConvenciones();
        agregarArbol();
        agregarSimbolosPorAmbito();
        agregarFunciones();
        agregarErrores();

        texto.append("\n").append(LINEA_DOBLE).append("\n");

        return texto.toString();
    }

    /**
     * Descripcion: Agrega los datos generales del reporte.
     * Entrada: Nombre del archivo fuente.
     * Salida: Encabezado con fecha y cantidades.
     * Restricciones: Utiliza la fecha y los datos actuales.
     */
    private void agregarEncabezado(String nombreFuente) {

        texto.append(LINEA_DOBLE).append("\n");
        texto.append("  TABLA DE SÍMBOLOS\n");
        texto.append(LINEA_DOBLE).append("\n\n");

        texto.append("  Archivo fuente : ").append(nombreFuente).append("\n");
        texto.append("  Generado       : ").append(LocalDateTime.now().format(FORMATO_FECHA)).append("\n");
        texto.append("  Ámbitos        : ").append(tabla.getAmbitos().size()).append("\n");
        texto.append("  Símbolos       : ").append(tabla.cantidadSimbolos()).append("\n");
        texto.append("  Errores        : ").append(tabla.getErrores().size()).append("\n\n");
    }

    /**
     * Descripcion: Muestra las convenciones del reporte.
     * Entrada: Ninguna.
     * Salida: Información sobre tamaños, direcciones y accesos.
     * Restricciones: Utiliza los tamaños definidos por el lenguaje.
     */
    private void agregarConvenciones() {

        texto.append("  CONVENCIONES\n");
        texto.append("  ─────────────\n");
        texto.append("  Tamaños    : int 4 B · float 4 B · bool 1 B · char 1 B · string 4 B (referencia a la cadena)\n");
        texto.append("  Dirección  : global+n = desplazamiento en la zona estática · pila+n = desplazamiento en el registro de activación\n");
        texto.append("  Acceso     : público = declarado en el ámbito global · privado = declarado dentro de una función o bloque\n");
        texto.append("  Parámetros : el lenguaje solo tiene paso por valor\n\n");
    }

    // ---------- 1. Árbol de ámbitos ----------

    /**
     * Descripcion: Agrega el árbol de ámbitos al reporte.
     * Entrada: Ámbito global.
     * Salida: Árbol con todos los ámbitos.
     * Restricciones: Comienza desde el ámbito global.
     */
    private void agregarArbol() {
        agregarTitulo("1. ÁRBOL DE ÁMBITOS (tablas encadenadas)");
        texto.append(dibujarArbol(tabla.ambitoGlobal())).append("\n");
    }

    /**
     * Descripcion: Crea el árbol de ámbitos.
     * Entrada: Ámbito global.
     * Salida: Árbol en forma de texto.
     * Restricciones: Incluye los ámbitos hijos.
     */
    static String dibujarArbol(Ambito global) {
        StringBuilder arbol = new StringBuilder();
        dibujarRama(arbol, global, "", true);
        return arbol.toString();
    }

    /**
     * Descripcion: Dibuja un ámbito y sus hijos.
     * Entrada: Árbol, ámbito, sangría y posición del hijo.
     * Salida: Rama del árbol en forma de texto.
     * Restricciones: Mantiene el orden de los ámbitos.
     */
    private static void dibujarRama(
            StringBuilder arbol,
            Ambito ambito,
            String sangria,
            boolean esUltimoHijo) {

        arbol.append("  ");

        if (!ambito.esGlobal()) {
            arbol.append(sangria).append(esUltimoHijo ? "└── " : "├── ");
        }

        arbol.append(ambito.nombre)
             .append("  (").append(textoCantidadSimbolos(ambito)).append(")\n");

        String sangriaHijos = ambito.esGlobal()
                ? ""
                : sangria + (esUltimoHijo ? "    " : "│   ");

        for (int indice = 0; indice < ambito.hijos.size(); indice++) {
            dibujarRama(
                    arbol,
                    ambito.hijos.get(indice),
                    sangriaHijos,
                    indice == ambito.hijos.size() - 1);
        }
    }

    /**
     * Descripcion: Obtiene la cantidad de símbolos de un ámbito.
     * Entrada: Ámbito que se desea consultar.
     * Salida: Cantidad de símbolos en texto.
     * Restricciones: Usa el singular o plural según la cantidad.
     */
    private static String textoCantidadSimbolos(Ambito ambito) {
        return ambito.cantidad()
                + (ambito.cantidad() == 1 ? " símbolo" : " símbolos");
    }

    // ---------- 2. Símbolos por ámbito ----------

    /**
     * Descripcion: Agrega los símbolos de cada ámbito.
     * Entrada: Lista de ámbitos.
     * Salida: Tablas con los símbolos encontrados.
     * Restricciones: Muestra cuando un ámbito no tiene símbolos.
     */
    private void agregarSimbolosPorAmbito() {

        agregarTitulo("2. SÍMBOLOS POR ÁMBITO");

        for (Ambito ambito : tabla.getAmbitos()) {
            agregarDatosDelAmbito(ambito);

            if (ambito.cantidad() == 0) {
                texto.append("  (sin declaraciones)\n\n");
            } else {
                texto.append(cuadroDeSimbolos(ambito).dibujar("  ")).append("\n");
            }
        }
    }

    /**
     * Descripcion: Agrega la información de un ámbito.
     * Entrada: Ámbito que se desea mostrar.
     * Salida: Nombre, nivel, padre y ámbitos hijos.
     * Restricciones: El ámbito global no tiene padre.
     */
    private void agregarDatosDelAmbito(Ambito ambito) {

        texto.append("  ÁMBITO: ").append(ambito.ruta()).append("\n");
        texto.append("  Nivel ").append(ambito.nivel)
             .append(ambito.esGlobal() ? " (global)" : " (local)")
             .append(" · padre: ").append(ambito.esGlobal() ? "—" : ambito.padre.nombre)
             .append(" · tablas hijas: ").append(ambito.nombresHijos()).append("\n");
    }

    /**
     * Descripcion: Crea la tabla de símbolos de un ámbito.
     * Entrada: Ámbito que contiene los símbolos.
     * Salida: Tabla con los datos de cada símbolo.
     * Restricciones: Muestra los símbolos en el orden registrado.
     */
    private static CuadroTexto cuadroDeSimbolos(Ambito ambito) {

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"Nº", "NOMBRE", "CATEGORÍA", "TIPO", "BYTES", "DIMENSIÓN",
                              "DIRECCIÓN", "VALOR INICIAL", "ACCESO", "LÍN:COL"},
                new int[] {3, 16, 13, 7, 6, 12, 11, 30, 8, 8});

        int numero = 1;

        for (Simbolo simbolo : ambito.simbolos()) {
            cuadro.agregarFila(
                    String.valueOf(numero++),
                    simbolo.nombre,
                    simbolo.categoria,
                    simbolo.tipo,
                    textoBytes(simbolo),
                    simbolo.textoDimensionCorta(),
                    simbolo.direccion(),
                    textoValorInicial(simbolo),
                    simbolo.acceso,
                    simbolo.linea + ":" + simbolo.columna);
        }

        return cuadro;
    }

    /**
     * Descripcion: Obtiene el tamaño de un símbolo.
     * Entrada: Símbolo que se desea consultar.
     * Salida: Cantidad de bytes en texto.
     * Restricciones: Las funciones no tienen tamaño asignado.
     */
    private static String textoBytes(Simbolo simbolo) {

        if (simbolo.esFuncion()) {
            return "—";
        }

        return simbolo.bytes > 0 ? String.valueOf(simbolo.bytes) : "?";
    }

    /**
     * Descripcion: Obtiene el valor inicial de un símbolo.
     * Entrada: Símbolo que se desea consultar.
     * Salida: Valor inicial o un guion.
     * Restricciones: Las funciones y los valores null muestran un guion.
     */
    private static String textoValorInicial(Simbolo simbolo) {
        return simbolo.esFuncion() || simbolo.valorInicial == null
                ? "—" : simbolo.valorInicial;
    }

    // ---------- 3. Funciones y procedimientos ----------

    /**
     * Descripcion: Agrega las funciones y procedimientos al reporte.
     * Entrada: Símbolos del ámbito global.
     * Salida: Tabla con los datos de las funciones.
     * Restricciones: Solo muestra los símbolos que son funciones.
     */
    private void agregarFunciones() {

        agregarTitulo("3. FUNCIONES Y PROCEDIMIENTOS (estructura de parámetros)");

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"NOMBRE", "CATEGORÍA", "RETORNO", "Nº PARÁM.", "PARÁMETROS FORMALES",
                              "PASO", "REG. ACTIV.", "TABLA HIJA", "LÍN:COL"},
                new int[] {14, 13, 8, 9, 30, 9, 11, 24, 8});

        boolean hayFunciones = false;

        for (Simbolo simbolo : tabla.ambitoGlobal().simbolos()) {
            if (simbolo.esFuncion()) {
                agregarFilaDeFuncion(cuadro, simbolo);
                hayFunciones = true;
            }
        }

        texto.append(hayFunciones ? cuadro.dibujar("  ") : "  (no hay funciones)\n")
             .append("\n");
    }

    /**
     * Descripcion: Agrega una función a la tabla.
     * Entrada: Tabla y símbolo de la función.
     * Salida: Fila con los datos de la función.
     * Restricciones: Muestra cuando la función no tiene parámetros.
     */
    private static void agregarFilaDeFuncion(CuadroTexto cuadro, Simbolo funcion) {

        boolean sinParametros = funcion.parametros.isEmpty();

        cuadro.agregarFila(
                funcion.nombre,
                funcion.categoria,
                funcion.tipo,
                String.valueOf(funcion.parametros.size()),
                sinParametros ? "(ninguno)" : funcion.listaParametros(),
                sinParametros ? "—" : "por valor",
                funcion.bytes + " B",
                funcion.tablaHija == null ? "—" : funcion.tablaHija.nombre,
                funcion.linea + ":" + funcion.columna);
    }

    // ---------- 4. Errores ----------

    /**
     * Descripcion: Agrega los errores encontrados al reporte.
     * Entrada: Lista de errores semánticos.
     * Salida: Tabla con los errores encontrados.
     * Restricciones: Muestra un mensaje cuando no hay errores.
     */
    private void agregarErrores() {

        agregarTitulo("4. ERRORES SEMÁNTICOS DETECTADOS CON LA TABLA");

        List<String> errores = tabla.getErrores();

        if (errores.isEmpty()) {
            texto.append("  No se encontraron errores.\n");
            return;
        }

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"Nº", "LÍNEA", "COL", "DESCRIPCIÓN"},
                new int[] {3, 6, 5, 80});

        int numero = 1;

        for (String error : errores) {
            agregarFilaDeError(cuadro, numero++, error);
        }

        texto.append(cuadro.dibujar("  "));
    }

    /**
     * Descripcion: Separa los datos de un error.
     * Entrada: Tabla, número y texto del error.
     * Salida: Fila con número, línea, columna y descripción.
     * Restricciones: Si el error no coincide con el formato, muestra el texto completo.
     */
    private static void agregarFilaDeError(CuadroTexto cuadro, int numero, String error) {

        Matcher partes = PATRON_ERROR.matcher(error);

        if (partes.matches()) {
            cuadro.agregarFila(
                    String.valueOf(numero),
                    partes.group(1),
                    partes.group(2),
                    partes.group(3));
        } else {
            cuadro.agregarFila(
                    String.valueOf(numero), "-", "-", error);
        }
    }

    /**
     * Descripcion: Agrega un título al reporte.
     * Entrada: Texto del título.
     * Salida: Título con una línea debajo.
     * Restricciones: La línea tiene el mismo largo que el título.
     */
    private void agregarTitulo(String titulo) {
        texto.append("  ").append(titulo).append("\n");
        texto.append("  ").append("─".repeat(titulo.length())).append("\n\n");
    }
}