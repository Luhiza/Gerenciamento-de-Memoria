/*
 * ============================================================================
 * Disciplina : Sistemas Operacionais (Engenharia de Software)
 * Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
 * Questão 3  : Algoritmo de substituição de página FIFO (Parte B)
 * Linguagem  : Java 8 ou superior
 *
 * Compilar : javac FifoPageReplacement.java
 * Executar : java FifoPageReplacement
 *              (o programa pergunta o número de frames e a sequência)
 *       ou : java FifoPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2
 *              (primeiro argumento = nº de frames; demais = referências)
 * ============================================================================
 *
 * Ideia do FIFO (First-In, First-Out):
 *   Os frames (quadros) funcionam como uma fila. A página que está há mais
 *   tempo na memória (a primeira a ter entrado) é a escolhida para sair
 *   quando precisamos de espaço. Acessos a páginas que já estão na memória
 *   (page hits) NÃO alteram a ordem da fila.
 *
 * Estrutura de dados:
 *   Queue<Integer> (implementada com ArrayDeque):
 *     - offer() insere no fim da fila  -> página que acabou de chegar;
 *     - poll()  remove do início       -> página mais antiga (vítima).
 */

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;

public class FifoPageReplacement {

    /** Garante que acentos saiam corretamente (UTF-8) independentemente da configuração do sistema. */
    private static void configurarSaidaUtf8() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 sempre existe na JVM; se falhar, segue com a saída padrão.
        }
    }

    public static void main(String[] args) {
        configurarSaidaUtf8();
        int numFrames;
        List<Integer> referencias;

        // ---------------------------------------------------------------
        // Leitura da entrada (por argumentos de linha de comando ou teclado)
        // ---------------------------------------------------------------
        try {
            if (args.length >= 2) {
                numFrames = Integer.parseInt(args[0].trim());
                referencias = lerReferencias(
                        String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            } else {
                Scanner teclado = new Scanner(System.in);
                System.out.print("Número de frames: ");
                numFrames = Integer.parseInt(teclado.nextLine().trim());
                System.out.print("Sequência de referências (ex: 7, 0, 1, 2, 0, 3): ");
                referencias = lerReferencias(teclado.nextLine());
            }
        } catch (NumberFormatException | NoSuchElementException e) {
            System.err.println("Erro: entrada inválida. Informe o número de frames "
                    + "(inteiro) e uma sequência de páginas (inteiros não negativos).");
            return;
        }

        // Validações básicas
        if (numFrames <= 0) {
            System.err.println("Erro: o número de frames deve ser maior que zero.");
            return;
        }
        if (referencias.isEmpty()) {
            System.err.println("Erro: a sequência de referências está vazia.");
            return;
        }

        simularFifo(numFrames, referencias);
    }

    /**
     * Converte uma linha como "7, 0, 1, 2" (ou "7 0 1 2") em uma lista de inteiros.
     * Lança NumberFormatException se algum item não for um inteiro não negativo.
     */
    static List<Integer> lerReferencias(String linha) {
        List<Integer> lista = new ArrayList<>();
        String limpa = linha.trim();
        if (limpa.isEmpty()) {
            return lista;
        }
        for (String token : limpa.split("[,;\\s]+")) {
            int pagina = Integer.parseInt(token);
            if (pagina < 0) {
                throw new NumberFormatException("página negativa: " + pagina);
            }
            lista.add(pagina);
        }
        return lista;
    }

    /**
     * Simula o FIFO, imprimindo o estado dos frames após cada referência e
     * as estatísticas finais.
     */
    static void simularFifo(int numFrames, List<Integer> referencias) {
        // Fila com as páginas que estão nos frames (início = mais antiga)
        Queue<Integer> frames = new ArrayDeque<>(numFrames);
        int faltas = 0;

        System.out.println("=== Simulação FIFO | frames: " + numFrames
                + " | referências: " + referencias + " ===\n");

        for (int pagina : referencias) {
            String evento;

            if (frames.contains(pagina)) {
                // PAGE HIT: a página já está na memória, nada muda na fila.
                evento = "Page Hit";
            } else {
                // PAGE FAULT: a página precisa ser carregada.
                faltas++;
                if (frames.size() == numFrames) {
                    // Sem frame livre: sai a página mais antiga (início da fila).
                    int removida = frames.poll();
                    evento = "Page Fault (substituiu " + removida + ")";
                } else {
                    evento = "Page Fault";
                }
                frames.offer(pagina); // a nova página entra no fim da fila
            }

            System.out.printf("Referência: %d | Frames: %s | %s | Total Faults: %d%n",
                    pagina, frames, evento, faltas);
        }

        // ---------------------------------------------------------------
        // Estatísticas finais
        // ---------------------------------------------------------------
        int total = referencias.size();
        int acertos = total - faltas;
        double taxaFaltas = (double) faltas / total;
        Locale br = Locale.forLanguageTag("pt-BR");

        System.out.println("\n--- Estatísticas ---");
        System.out.println("Total de referências : " + total);
        System.out.println("Page faults          : " + faltas);
        System.out.println("Page hits            : " + acertos);
        System.out.println(String.format(br,
                "Taxa de faltas       : %d/%d = %.4f (%.2f%%)",
                faltas, total, taxaFaltas, taxaFaltas * 100));
        System.out.println(String.format(br,
                "Taxa de acertos      : %.2f%%", (1 - taxaFaltas) * 100));
    }
}
