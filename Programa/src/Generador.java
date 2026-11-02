/*
 * Descripcion:
 * Genero el código intermedio (código de tres direcciones) del programa.
 * Cada instrucción tiene como máximo tres direcciones: "t1 = a + b".
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
 */
public class Generador {

    private final StringBuilder codigo = new StringBuilder();

    private int contadorTemporales = 0;

    private int contadorEtiquetas = 0;

    /* Un temporal nuevo: t1, t2, t3... */
    public String nuevoTemporal() {
        contadorTemporales++;
        return "t" + contadorTemporales;
    }

    /* Una etiqueta nueva para los saltos: L1, L2, L3... */
    public String nuevaEtiqueta() {
        contadorEtiquetas++;
        return "L" + contadorEtiquetas;
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

    /* Borra el código y reinicia los contadores */
    public void limpiar() {
        codigo.setLength(0);
        contadorTemporales = 0;
        contadorEtiquetas = 0;
    }

    /* Nombre de la carpeta donde se guarda el código intermedio */
    public static final String CARPETA = "codigo_intermedio";

    /*
     * Descripcion:
     * Ubico la carpeta codigo_intermedio dentro de la carpeta Programa
     * (al lado de bin y de tabla_simbolos).
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

        String temporal = nuevoTemporal();

        emitir(temporal + " = " + a.lugar + " " + op + " " + b.lugar);

        validado.lugar = temporal;

        return validado;
    }
    /* Operación de un solo operando: t = not e */
    public Atrib unaria(Atrib validado, String op, Atrib e) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal();

        emitir(temporal + " = " + op + " " + e.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /* Instrucción de lectura: read destino */
    public void leer(String destino) {
        emitir("read " + destino);
    }

    /* Instrucción de escritura: write valor */
    public void escribir(String valor) {
        emitir("write " + valor);
    }

    /* x++ y x--: se escriben como x = x + 1 y x = x - 1 */
    public void incremento(String id, String op) {
        emitir(id + " = " + id + (op.equals("++") ? " + 1" : " - 1"));
    }

    private boolean hayFuncion = false;

    /* Etiqueta con el nombre de la función. Antes de la primera función (si no es principal)
    se escribe un salto a principal: así las variables globales se inicializan primero
    y la ejecución empieza en main. */
    public void etiquetaFuncion(String nombre) {
        if (!hayFuncion && !nombre.equals("principal")) {
            emitir("goto principal");
        }
        hayFuncion = true;
        emitir(nombre + ":");
    }

    /* Llamada: un param por cada argumento y luego el call. Devuelve el temporal donde queda
    el resultado, o null si la función no devuelve valor. */
    public String llamar(String nombre, java.util.ArrayList args, boolean devuelve) {
        for (Object o : args) {
            emitir("param " + ((Atrib) o).lugar);
        }

        if (!devuelve) {
            emitir("call " + nombre + ", " + args.size());
            return null;

        }

        String temporal = nuevoTemporal();
        emitir(temporal + " = call " + nombre + ", " + args.size());
        return temporal;
    }
    
    /* return con valor (lugar) o sin valor (null) */
    public void retorno(String lugar) {
        emitir(lugar == null ? "return" : "return " + lugar);
    }

    /* ---------- if / elif / else ---------- */
    private static class Condicional {
        String falso; /* etiqueta a la que se salta si la condición es falsa */
        String fin; /* etiqueta del final de todo el if (solo existe si hay elif o else) */
    }

    private final java.util.ArrayList<Condicional> condicionales = new java.util.ArrayList<>();

    private Condicional ifActual() {
        return condicionales.isEmpty() ? null : condicionales.get(condicionales.size() - 1);
    }

    /* Empieza un if: se apila su "ficha" */
    public void iniciarIf() {
        condicionales.add(new Condicional());
    }

    /* Ya se escribió el código de la condición: salta si es falsa */
    public void condicionIf(String lugar) {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }
        
        c.falso = nuevaEtiqueta();
        emitir("ifFalse " + lugar + " goto " + c.falso);
    }
    
    /* Empieza un elif o un else: termina la rama anterior (salta al final) y ahí cae la condición falsa */
    public void otraRama() {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }

        if (c.fin == null) {
            c.fin = nuevaEtiqueta();
        }

        emitir("goto " + c.fin);

        if (c.falso != null) {
            emitir(c.falso + ":");
            c.falso = null;
        }
    }

    /* Termina todo el if: se escriben las etiquetas que quedaron pendientes */
    public void finIf() {
        if (condicionales.isEmpty()) {
            return;
        }

        Condicional c = condicionales.remove(condicionales.size() - 1);

        if (c.falso != null) {
            emitir(c.falso + ":");
        }

        if (c.fin != null) {
            emitir(c.fin + ":");
        }
    }

    /* ---------- while y for ---------- */
    private static class Ciclo {
        String inicio; /* donde se vuelve a evaluar la condición */
        String fin; /* donde se sale del ciclo (también lo usa break) */
        int marca = -1; /* for: dónde empieza en el código el incremento */
        String incremento = ""; /* for: el código del incremento, guardado para ponerlo al final */
    }

    private final java.util.ArrayList<Ciclo> ciclos = new java.util.ArrayList<>();

    private Ciclo nuevoCiclo() {
        Ciclo c = new Ciclo();
        c.inicio = nuevaEtiqueta();
        c.fin = nuevaEtiqueta();
        ciclos.add(c);
        return c;
    }

    private Ciclo cicloActual() {
        return ciclos.isEmpty() ? null : ciclos.get(ciclos.size() - 1);
    }

    /* while: la etiqueta de inicio va ANTES del código de la condición */
    public void iniciarWhile() {
        emitir(nuevoCiclo().inicio + ":");
    }

    /* while: ya está el código de la condición */
    public void saltoSiFalsoCiclo(String lugar) {
        Ciclo c = cicloActual();

        if (c != null) {
            emitir("ifFalse " + lugar + " goto " + c.fin);
        }
    }

    /* for: se apila el ciclo (su etiqueta de inicio se escribe después de la inicialización) */
    public void iniciarFor() {
        nuevoCiclo();
    }

    /* for: terminó la inicialización, aquí empieza la condición */
    public void inicioCondicionFor() {
        Ciclo c = cicloActual();

        if (c != null) {
            emitir(c.inicio + ":");
        }
    }

    /* for: terminó la condición, desde aquí se escribe el incremento */
    public void marcaIncremento() {
        Ciclo c = cicloActual();

        if (c != null) {
            c.marca = codigo.length();
        }
    }

    /* for: terminó la cabecera. El incremento ya está escrito, pero debe ir DESPUÉS del cuerpo: se corta del código y se guarda. Luego se escribe el salto de la condición. */
    public void cabeceraFor(String lugarCondicion) {
        Ciclo c = cicloActual();

        if (c == null) {
            return;
        }

        if (c.marca >= 0) {
            c.incremento = codigo.substring(c.marca);
            codigo.setLength(c.marca);
        }

        if (lugarCondicion != null) {
            emitir("ifFalse " + lugarCondicion + " goto " + c.fin);
        }
    }

    /* Termina un while o un for: incremento (si hay), vuelta al inicio y etiqueta de salida */
    public void finCiclo() {
        if (ciclos.isEmpty()) {
            return;
        }

        Ciclo c = ciclos.remove(ciclos.size() - 1);
        codigo.append(c.incremento);
        emitir("goto " + c.inicio);
        emitir(c.fin + ":");
    }
        /* break: salta a la salida del ciclo más interno */
    public void saltoBreak() {
        Ciclo c = cicloActual();

        if (c != null) {
            emitir("goto " + c.fin);
        }
    }
}

