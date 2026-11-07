/*
 * Descripcion:
 * Genero el código intermedio código de tres direcciones del programa.
 * Cada instrucción tiene como máximo tres direcciones
 * 
 * Entrada:
 * Las instrucciones que le pide el parser (Parser.cup), una por una, mientras
 * analiza el archivo fuente.
 *
 * Salida:
 * El texto del código intermedio, que Main guarda en un archivo.
 *
 * Restricciones:
 * Solo tiene sentido guardar el código cuando el programa no tiene errores.
 * Este archivo no valida tipos: eso lo hace el parser antes de pedir el código.
 *
 * Objetivo:
 * Que el Parser.cup quede solo con la gramática y las reglas semánticas, y que
 * todo lo del código intermedio viva en un solo lugar.
 *
 * Formato de las etiquetas:
 * funcion_estructura_numero_parte
 * Las funciones usan tipo_nombre: y terminan con tipo_nombre_final.
 */
public class Generador {

    private final StringBuilder codigo = new StringBuilder();

    private int contadorTemporales = 0;

    private int contadorEtiquetas = 0;

    private String funcionActual = "global";

    private String tipoFuncionActual = "";

    private static final String TIPO_PRINCIPAL = "void";

    /* Partes del nombre de las etiquetas. Cada etiqueta y cada salto hacia ella usan la
    misma constante, así nunca puede quedar un goto hacia una etiqueta que no existe. */
    private static final String PARTE_INICIALIZACION = "_inicializacion";
    private static final String PARTE_CONDICION = "_condicion";
    private static final String PARTE_INCREMENTO = "_inc";
    private static final String PARTE_BLOQUE = "_bloque";
    private static final String PARTE_SIGUIENTE = "_sig";
    private static final String PARTE_FINAL = "_final";

    /* Un temporal nuevo con el tipo del dato que guarda int1, float2, bool3
    El numero es uno solo para todos los tipos, asi nunca se repite un nombre. */
    public String nuevoTemporal(String tipo) {
        contadorTemporales++;
        return tipo + contadorTemporales;
    }

    /* Prefijo de una estructura nueva funExc_for_2 */
    private String prefijo(String estructura) {
        contadorEtiquetas++;
        return funcionActual + "_" + estructura + "_" + contadorEtiquetas;
    }

    private void etiqueta(String nombre) {
        emitir("");
        emitir(nombre + ":");
    }

    /* Instrucción de una asignación: destino = lugar del valor */
    public void asignacion(String destino, Atrib valor) {
        emitir(destino + " = " + valor.lugar);
    }

    /* Agrega una instrucción al código */
    public void emitir(String instruccion) {
        codigo.append(instruccion).append("\n");
    }

    /* El código generado hasta ahora */
    public String obtenerCodigo() {
        return codigo.toString();
    }

    /* Borra el código y reinicia todo */
    public void limpiar() {
        codigo.setLength(0);
        contadorTemporales = 0;
        contadorEtiquetas = 0;
        funcionActual = "global";
        tipoFuncionActual = "";
        hayFuncion = false;
        condicionales.clear();
        ciclos.clear();
        ultimaCarga = null;
        marcaAsignacion = 0;
    }

    /* Carpeta donde se guarda el código intermedio, dentro de Programa */
    public static final String CARPETA = "salida" + java.io.File.separator + "codigo_intermedio";

    /*
     * Descripcion:
     * Ubico la carpeta salida/codigo_intermedio dentro de la carpeta Programa
     * (al lado de bin).
     *
     * Restricciones:
     * Se calcula a partir de la ubicación de los .class, así funciona
     * sin importar desde qué carpeta se ejecute el programa.
     */
    public static java.io.File carpeta() {
        try {
            java.io.File bin = new java.io.File(Generador.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());

            return new java.io.File(bin.getParentFile(), CARPETA);

        } catch (Exception e) {
            return new java.io.File(CARPETA);
        }
    }

    /*
     * Descripcion:
     * Guardo el código intermedio en un archivo .tac.
     *
     * Entrada: el archivo fuente analizado (de él sale el nombre).
     * Salida : el archivo creado, por ejemplo codigo_intermedio/prueba.tac
     */
    public java.io.File guardar(java.io.File fuente) throws java.io.IOException {

        java.io.File carpeta = carpeta();

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        String base = fuente.getName();
        int punto = base.lastIndexOf('.');

        if (punto > 0) {
            base = base.substring(0, punto);
        }

        java.io.File salida = new java.io.File(carpeta, base + ".tac");

        java.nio.file.Files.writeString(salida.toPath(), obtenerCodigo(),
                java.nio.charset.StandardCharsets.UTF_8);

        return salida;
    }

    /*
     * Descripcion:
     * Genero la instrucción de una operación binaria: t = a op b.
     *
     * Entrada:
     * El resultado que ya validó el parser (con su tipo), los dos operandos y el operador.
     *
     * Salida:
     * El mismo resultado, con el temporal nuevo como lugar. Si el tipo es "error" no
     * genera nada y lo devuelve igual.
     */
    public Atrib binaria(Atrib validado, Atrib a, String op, Atrib b) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal(validado.tipo);

        emitir(temporal + " = " + a.lugar + " " + op + " " + b.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /* Operación de un solo operando: t = not e */
    public Atrib unaria(Atrib validado, String op, Atrib e) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal(validado.tipo);

        emitir(temporal + " = " + op + " " + e.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /* Lee una posición de un arreglo en un temporal: t = a[i].
    Así las operaciones solo reciben direcciones simples (t = t1 + 1, no t = a[i] + 1). */
    public String cargarElemento(String tipo, String elemento) {
        String temporal = nuevoTemporal(tipo);
        emitir(temporal + " = " + elemento);
        return temporal;
    }


    private String ultimaCarga = null;
    private String valorUltimaCarga = null;
    private int inicioUltimaCarga = -1;
    private int finUltimaCarga = -1;

    /* Dónde empieza el valor de la última asignación (justo después del =) */
    private int marcaAsignacion = 0;

    /* Copia un literal o una variable a un temporal: t1 = 5, t2 = x.
    Conserva el tipo y el valor constante. Si el dato tiene error, no escribe nada. */
    public Atrib cargar(Atrib dato) {
        if (dato == null || dato.tipo.equals("error")) {
            return dato;
        }

        String temporal = nuevoTemporal(dato.tipo);
        inicioUltimaCarga = codigo.length();
        emitir(temporal + " = " + dato.lugar);
        finUltimaCarga = codigo.length();
        ultimaCarga = temporal;
        valorUltimaCarga = dato.lugar;

        Atrib cargado = new Atrib(dato.tipo, temporal);
        cargado.valor = dato.valor;
        return cargado;
    }

    public Atrib sinCarga(Atrib dato) {
        boolean esLaUltima = dato != null && dato.lugar.equals(ultimaCarga)
                && codigo.length() == finUltimaCarga;

        if (!esLaUltima) {
            return dato;
        }

        codigo.setLength(inicioUltimaCarga);
        contadorTemporales--;
        ultimaCarga = null;

        Atrib directo = new Atrib(dato.tipo, valorUltimaCarga);
        directo.valor = dato.valor;
        return directo;
    }

    /* Se llama al leer el = de una asignación: ahí empieza el código del valor */
    public void marcarAsignacion() {
        marcaAsignacion = codigo.length();
    }

    /* Declaración sin valor: data#int x */
    public void declarar(String tipo, String nombre) {
        emitir("data#" + tipo + " " + nombre);
    }

    /* Declaración con valor: el data# debe ir ANTES del código del valor,
    pero el parser lo pide al final de la regla. Se inserta en la marca del =. */
    public void declararAntesDelValor(String tipo, String nombre) {
        codigo.insert(marcaAsignacion, "DeclVal#" + tipo + " " + nombre + "\n");
        ultimaCarga = null;
    }

    /* Parámetro de una función, al inicio de su cuerpo: param_int x */
    public void parametro(String tipo, String nombre) {
        emitir("param_" + tipo + " " + nombre);
    }

    /* Instrucción de lectura: read destino */
    public void leer(String destino) {
        emitir("read " + destino);
    }

    /* Instrucción de escritura: write valor */
    public void escribir(String valor) {
        emitir("write " + valor);
    }

    /* x++ y x--: int1 = x, int2 = int1 + 1, x = int2 */
    public void incremento(String tipo, String id, String op) {
        String valor = nuevoTemporal(tipo);
        emitir(valor + " = " + id);

        String resultado = nuevoTemporal(tipo);
        emitir(resultado + " = " + valor + (op.equals("++") ? " + 1" : " - 1"));

        emitir(id + " = " + resultado);
    }

    /* ---------- funciones ---------- */

    private boolean hayFuncion = false;

    /* Etiqueta de inicio de la función: int_funExc:
    Antes de la primera función (si no es principal) se escribe un salto a principal:
    así las variables globales se inicializan primero y la ejecución empieza en main. */
    public void etiquetaFuncion(String tipo, String nombre) {
        if (!hayFuncion && !nombre.equals("principal")) {
            emitir("goto " + TIPO_PRINCIPAL + "_principal");
        }

        hayFuncion = true;
        funcionActual = nombre;
        tipoFuncionActual = tipo;

        if (codigo.length() > 0) {
            emitir("");
        }
        emitir(tipo + "_" + nombre + ":");
    }

    /* Fin de la función: int_funExc_final */
    public void finFuncion() {
        emitir(tipoFuncionActual + "_" + funcionActual + PARTE_FINAL);
        funcionActual = "global";
        tipoFuncionActual = "";
    }

    /* Llamada: un param por cada argumento y luego el call. Devuelve el temporal donde queda
    el resultado (con el tipo que devuelve la función), o null si no devuelve valor. */
    public String llamar(String nombre, String tipo, java.util.ArrayList args, boolean devuelve) {
        for (Object o : args) {
            emitir("param " + ((Atrib) o).lugar);
        }

        if (!devuelve) {
            emitir("call " + nombre + ", " + args.size());
            return null;
        }

        String temporal = nuevoTemporal(tipo);
        emitir(temporal + " = call " + nombre + ", " + args.size());
        return temporal;
    }

    /* return con valor (lugar) o sin valor (null) */
    public void retorno(String lugar) {
        emitir(lugar == null ? "return" : "return " + lugar);
    }

    /* ---------- if / elif / else ----------
       funExc_if_3_bloque1, funExc_if_3_sig1, ..., funExc_if_3_final */
    private static class Condicional {
        String base;      /* prefijo: funExc_if_3 */
        int rama = 0;     /* número de la rama con condición (if = 1, primer elif = 2...) */
        String siguiente; /* etiqueta pendiente a la que se salta si la condición es falsa */
    }

    private final java.util.ArrayList<Condicional> condicionales = new java.util.ArrayList<>();

    private Condicional ifActual() {
        return condicionales.isEmpty() ? null : condicionales.get(condicionales.size() - 1);
    }

    /* Empieza un if: se apila su "ficha" */
    public void iniciarIf() {
        Condicional c = new Condicional();
        c.base = prefijo("if");
        condicionales.add(c);
    }

    /* Ya se escribió el código de la condición (del if o de un elif) */
    public void condicionIf(String lugar) {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }

        c.rama++;
        String bloque = c.base + PARTE_BLOQUE + c.rama;
        c.siguiente = c.base + PARTE_SIGUIENTE + c.rama;

        emitir("if " + lugar + " goto " + bloque);
        emitir("goto " + c.siguiente);
        etiqueta(bloque);
    }

    /* Empieza un elif o un else: la rama anterior salta al final y aquí cae la condición falsa */
    public void otraRama() {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }

        emitir("goto " + c.base + PARTE_FINAL);

        if (c.siguiente != null) {
            etiqueta(c.siguiente);
            c.siguiente = null;
        }
    }

    /* Termina todo el if: se escriben las etiquetas pendientes */
    public void finIf() {
        if (condicionales.isEmpty()) {
            return;
        }

        Condicional c = condicionales.remove(condicionales.size() - 1);

        if (c.siguiente != null) {
            etiqueta(c.siguiente);
        }

        etiqueta(c.base + PARTE_FINAL);
    }

    /* ---------- while y for ----------
       for:   funExc_for_2_inicializacion / _condicion / _inc / _bloque / _final
       while: funExc_while_4_condicion / _bloque / _final */
    private static class Ciclo {
        String base;    /* prefijo: funExc_for_2 */
        boolean esFor;
        int marca = -1; /* for: dónde empieza en el código el incremento */
    }

    private final java.util.ArrayList<Ciclo> ciclos = new java.util.ArrayList<>();

    private Ciclo nuevoCiclo(String estructura) {
        Ciclo c = new Ciclo();
        c.base = prefijo(estructura);
        c.esFor = estructura.equals("for");
        ciclos.add(c);
        return c;
    }

    private Ciclo cicloActual() {
        return ciclos.isEmpty() ? null : ciclos.get(ciclos.size() - 1);
    }

    /* while: la etiqueta _condicion va ANTES del código de la condición */
    public void iniciarWhile() {
        etiqueta(nuevoCiclo("while").base + PARTE_CONDICION);
    }

    /* while: ya está el código de la condición */
    public void saltoSiFalsoCiclo(String lugar) {
        Ciclo c = cicloActual();

        if (c == null) {
            return;
        }

        emitir("if " + lugar + " goto " + c.base + PARTE_BLOQUE);
        emitir("goto " + c.base + PARTE_FINAL);
        etiqueta(c.base + PARTE_BLOQUE);
    }

    /* for: se apila el ciclo y se escribe _inicializacion (antes de la inicialización) */
    public void iniciarFor() {
        etiqueta(nuevoCiclo("for").base + PARTE_INICIALIZACION);
    }

    /* for: terminó la inicialización, aquí empieza la condición */
    public void inicioCondicionFor() {
        Ciclo c = cicloActual();

        if (c != null) {
            etiqueta(c.base + PARTE_CONDICION);
        }
    }

    /* for: terminó la condición, desde aquí se escribe el incremento */
    public void marcaIncremento() {
        Ciclo c = cicloActual();

        if (c != null) {
            c.marca = codigo.length();
        }
    }

    /* for: terminó la cabecera. Los saltos de la condición deben ir ANTES del incremento,
    pero el lugar de la condición llega hasta ahora: se corta el incremento, se escriben
    los saltos y se vuelve a poner el incremento debajo de su etiqueta _inc. */
    public void cabeceraFor(String lugarCondicion) {
        Ciclo c = cicloActual();

        if (c == null) {
            return;
        }

        String incremento = "";

        if (c.marca >= 0) {
            incremento = codigo.substring(c.marca);
            codigo.setLength(c.marca);
        }

        if (lugarCondicion != null) {
            emitir("if " + lugarCondicion + " goto " + c.base + PARTE_BLOQUE);
            emitir("goto " + c.base + PARTE_FINAL);
        } else {
            emitir("goto " + c.base + PARTE_BLOQUE);
        }

        etiqueta(c.base + PARTE_INCREMENTO);
        codigo.append(incremento);
        emitir("goto " + c.base + PARTE_CONDICION);

        etiqueta(c.base + PARTE_BLOQUE);
    }

    /* Termina un while o un for: el for vuelve a _inc, el while a _condicion */
    public void finCiclo() {
        if (ciclos.isEmpty()) {
            return;
        }

        Ciclo c = ciclos.remove(ciclos.size() - 1);

        emitir("goto " + c.base + (c.esFor ? PARTE_INCREMENTO : PARTE_CONDICION));
        etiqueta(c.base + PARTE_FINAL);
    }

    /* break: salta a la salida del ciclo más interno */
    public void saltoBreak() {
        Ciclo c = cicloActual();

        if (c != null) {
            emitir("goto " + c.base + PARTE_FINAL);
        }
    }
}