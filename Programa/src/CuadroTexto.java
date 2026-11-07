import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================
 *                       CUADRO DE TEXTO
 * ============================================================
 * Descripcion: dibuja una tabla con bordes. Si un texto no cabe en su
 * columna, continúa en la línea siguiente de la misma fila.
 */
public class CuadroTexto {

    private final String[] titulos;
    private final int[] anchos;
    private final List<String[]> filas = new ArrayList<>();

    public CuadroTexto(String[] titulos, int[] anchos) {
        this.titulos = titulos;
        this.anchos = anchos;
    }

    public void agregarFila(String... celdas) {
        filas.add(celdas);
    }

    /* El cuadro completo; cada linea empieza con el margen indicado */
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

    /* Linea de borde: ┌───┬───┐ */
    private String borde(char izquierda, char medio, char derecha) {

        StringBuilder borde = new StringBuilder().append(izquierda);

        for (int i = 0; i < anchos.length; i++) {
            borde.append("─".repeat(anchos[i] + 2));
            borde.append(i < anchos.length - 1 ? medio : derecha);
        }

        return borde.toString();
    }

    /* Una fila; si alguna celda es larga, la fila ocupa varias lineas */
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
     * Corta un texto en lineas de maximo "ancho" caracteres sin partir palabras.
     * Si una palabra sola es mas larga que el ancho, esa si se parte.
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
