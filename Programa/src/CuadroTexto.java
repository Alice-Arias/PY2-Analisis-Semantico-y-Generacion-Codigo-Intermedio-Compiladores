import java.util.ArrayList;
import java.util.List;

/*
 * CUADRO DE TEXTO
 * Descripcion: Crea tablas con bordes y organiza el texto
 * para que se ajuste al ancho de cada columna.
 */
public class CuadroTexto {

    private final String[] titulos;
    private final int[] anchos;
    private final List<String[]> filas = new ArrayList<>();

    /*
     * Descripcion: Crea una tabla.
     * Entrada: Titulos y anchos de las columnas.
     * Salida: Una tabla vacia.
     * Restricciones: Cada titulo tiene su propio ancho.
     */
    public CuadroTexto(String[] titulos, int[] anchos) {
        this.titulos = titulos;
        this.anchos = anchos;
    }

    /*
     * Descripcion: Agrega una fila a la tabla.
     * Entrada: Datos de las celdas.
     * Salida: Ninguna.
     * Restricciones: Los datos se guardan en el orden recibido.
     */
    public void agregarFila(String... celdas) {
        filas.add(celdas);
    }

    /*
     * Descripcion: Dibuja la tabla completa.
     * Entrada: Texto que se coloca al inicio de cada linea.
     * Salida: Tabla con bordes y contenido.
     * Restricciones: Respeta los anchos de las columnas.
     */
    public String dibujar(String margen) {

        StringBuilder cuadro = new StringBuilder();

        cuadro.append(margen).append(borde('┌', '┬', '┐')).append("\n");
        cuadro.append(dibujarFila(margen, titulos));
        cuadro.append(margen).append(borde('╞', '╪', '╡').replace('─', '═')).append("\n");

        for (int i = 0; i < filas.size(); i++) {

            cuadro.append(dibujarFila(margen, filas.get(i)));

            boolean esUltimaFila = i == filas.size() - 1;

            if (!esUltimaFila) {
                cuadro.append(margen).append(borde('├', '┼', '┤')).append("\n");
            }
        }

        cuadro.append(margen).append(borde('└', '┴', '┘')).append("\n");

        return cuadro.toString();
    }

    /*
     * Descripcion: Crea una linea de los bordes de la tabla.
     * Entrada: Caracteres para los extremos y las uniones.
     * Salida: Una linea de borde.
     * Restricciones: Usa el ancho definido para cada columna.
     */
    private String borde(char izquierda, char medio, char derecha) {

        StringBuilder borde = new StringBuilder().append(izquierda);

        for (int i = 0; i < anchos.length; i++) {
            borde.append("─".repeat(anchos[i] + 2));
            borde.append(i < anchos.length - 1 ? medio : derecha);
        }

        return borde.toString();
    }

    /*
     * Descripcion: Dibuja una fila de la tabla.
     * Entrada: Margen y datos de las celdas.
     * Salida: Una fila con su contenido.
     * Restricciones: Si un texto es largo, ocupa varias lineas.
     */
    private String dibujarFila(String margen, String[] celdas) {

        List<List<String>> lineasPorCelda = new ArrayList<>();
        int alto = 1;

        for (int i = 0; i < anchos.length; i++) {

            String contenido = i < celdas.length && celdas[i] != null ? celdas[i] : "";
            List<String> lineas = partirEnLineas(contenido, anchos[i]);

            lineasPorCelda.add(lineas);
            alto = Math.max(alto, lineas.size());
        }

        StringBuilder fila = new StringBuilder();

        for (int linea = 0; linea < alto; linea++) {

            fila.append(margen).append("│");

            for (int i = 0; i < anchos.length; i++) {

                List<String> lineasCelda = lineasPorCelda.get(i);
                String contenido = linea < lineasCelda.size() ? lineasCelda.get(linea) : "";

                fila.append(" ").append(String.format("%-" + anchos[i] + "s", contenido)).append(" │");
            }

            fila.append("\n");
        }

        return fila.toString();
    }

    /*
     * Descripcion: Divide un texto en varias lineas.
     * Entrada: Texto y ancho maximo de cada linea.
     * Salida: Lista de lineas con el texto dividido.
     * Restricciones: No separa palabras, excepto si son mas largas que el ancho.
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
}