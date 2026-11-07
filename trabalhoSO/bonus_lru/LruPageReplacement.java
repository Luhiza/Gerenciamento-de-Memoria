/*
 * ============================================================================
 * Disciplina : Sistemas Operacionais (Engenharia de Software)
 * Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
 * Questão    : BÔNUS - Algoritmo de substituição de página LRU
 * Linguagem  : Java 8 ou superior
 *
 * Compilar : javac LruPageReplacement.java
 * Executar : java LruPageReplacement
 *              (o programa pergunta o número de frames e a sequência)
 *       ou : java LruPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2
 *              (primeiro argumento = nº de frames; demais = referências)
 * ============================================================================
 *
 * Ideia do LRU (Least Recently Used - "menos recentemente usada"):
 *   Quando falta espaço, sai a página que ficou mais tempo SEM ser usada.
 *   O raciocínio é a localidade temporal: quem foi usado há pouco tempo
 *   tende a ser usado de novo em breve; quem está parado há muito tempo,
 *   provavelmente não será necessário tão cedo.
 *
 *   Diferença para o FIFO: o FIFO olha só para QUANDO a página ENTROU; o LRU
 *   olha para QUANDO ela foi USADA pela última vez (hits também contam).
 *
 * Estrutura de dados usada no LRU:
 *   - List<Integer> frames          : páginas que estão nos frames (slots);
 *   - Map<Integer,Integer> ultimoUso: página -> "timestamp" do último acesso.
 *     O timestamp é um contador lógico que aumenta a cada referência
 *     (1, 2, 3, ...). A vítima é a página com MENOR timestamp.
 *   Consultar/atualizar o timestamp é O(1); escolher a vítima percorre os
 *   frames, custando O(f), onde f é o número de frames (pequeno na prática).
 *   (Alternativa O(1) em tudo: LinkedHashMap com accessOrder = true.)
 *
 * Este programa executa FIFO e LRU com a MESMA entrada e compara os dois.
 * Ao final mostra também: (a) um quadro variando o número de frames e
 * (b) a Anomalia de Belady, que afeta o FIFO mas nunca o LRU.
 */

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;

public class LruPageReplacement {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    /** Resultado resumido de uma simulação. */
    static class Resultado {
        final String algoritmo;
        final int total;
        final int faltas;

        Resultado(String algoritmo, int total, int faltas) {
            this.algoritmo = algoritmo;
            this.total = total;
            this.faltas = faltas;
        }

        int acertos()        { return total - faltas; }
        double taxaFaltas()  { return (double) faltas / total; }
    }

    public static void main(String[] args) {
        configurarSaidaUtf8();

        int numFrames;
        List<Integer> referencias;

        // ---------------------------------------------------------------
        // Leitura da entrada (mesmo formato da Questão 3)
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

        if (numFrames <= 0) {
            System.err.println("Erro: o número de frames deve ser maior que zero.");
            return;
        }
        if (referencias.isEmpty()) {
            System.err.println("Erro: a sequência de referências está vazia.");
            return;
        }

        // ---------------------------------------------------------------
        // Simulações detalhadas
        // ---------------------------------------------------------------
        System.out.println("=== Entrada | frames: " + numFrames
                + " | referências: " + referencias + " ===\n");

        System.out.println("--- FIFO ---");
        Resultado fifo = simularFifo(numFrames, referencias, true);

        System.out.println("\n--- LRU ---");
        Resultado lru = simularLru(numFrames, referencias, true);

        imprimirComparacao(fifo, lru);
        imprimirVariacaoDeFrames(referencias);
        demonstrarAnomaliaDeBelady();
        imprimirAnaliseCritica(fifo, lru);
    }

    // ===================================================================
    // FIFO
    // ===================================================================
    static Resultado simularFifo(int numFrames, List<Integer> referencias, boolean detalhar) {
        Queue<Integer> frames = new ArrayDeque<>(numFrames); // início = mais antiga
        int faltas = 0;

        for (int pagina : referencias) {
            String evento;
            if (frames.contains(pagina)) {
                evento = "Page Hit";                 // FIFO não muda a ordem no hit
            } else {
                faltas++;
                if (frames.size() == numFrames) {
                    int removida = frames.poll();    // sai a mais antiga
                    evento = "Page Fault (substituiu " + removida + ")";
                } else {
                    evento = "Page Fault";
                }
                frames.offer(pagina);
            }
            if (detalhar) {
                System.out.printf("Referência: %d | Frames: %s | %s | Total Faults: %d%n",
                        pagina, frames, evento, faltas);
            }
        }
        return new Resultado("FIFO", referencias.size(), faltas);
    }

    // ===================================================================
    // LRU
    // ===================================================================
    static Resultado simularLru(int numFrames, List<Integer> referencias, boolean detalhar) {
        List<Integer> frames = new ArrayList<>(numFrames);       // posições (slots) dos frames
        Map<Integer, Integer> ultimoUso = new HashMap<>();       // página -> timestamp do último uso
        int tempo = 0;                                           // relógio lógico
        int faltas = 0;

        for (int pagina : referencias) {
            tempo++;                                             // cada referência avança o relógio
            String evento;

            if (ultimoUso.containsKey(pagina)) {
                // PAGE HIT: a página já está na memória; só atualiza o último uso.
                evento = "Page Hit";
            } else {
                // PAGE FAULT
                faltas++;
                if (frames.size() < numFrames) {
                    frames.add(pagina);                          // ainda há frame livre
                    evento = "Page Fault";
                } else {
                    // Sem frame livre: procura a página com MENOR timestamp
                    // (a que está há mais tempo sem ser usada).
                    int vitima = -1;
                    int menorTempo = Integer.MAX_VALUE;
                    for (int p : frames) {
                        int t = ultimoUso.get(p);
                        if (t < menorTempo) {
                            menorTempo = t;
                            vitima = p;
                        }
                    }
                    frames.set(frames.indexOf(vitima), pagina);  // a nova ocupa o frame da vítima
                    ultimoUso.remove(vitima);
                    evento = "Page Fault (substituiu " + vitima + ")";
                }
            }
            ultimoUso.put(pagina, tempo);                        // registra o uso (hit ou fault)

            if (detalhar) {
                System.out.printf("Referência: %d | Frames: %s | %s | Total Faults: %d | Último uso: %s%n",
                        pagina, frames, evento, faltas, formatarTempos(frames, ultimoUso));
            }
        }
        return new Resultado("LRU", referencias.size(), faltas);
    }

    /** Monta o texto "7(t=1), 0(t=2)" com o último uso de cada página nos frames. */
    private static String formatarTempos(List<Integer> frames, Map<Integer, Integer> ultimoUso) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) sb.append(", ");
            int p = frames.get(i);
            sb.append(p).append("(t=").append(ultimoUso.get(p)).append(")");
        }
        return sb.toString();
    }

    // ===================================================================
    // Comparação e análises
    // ===================================================================
    static void imprimirComparacao(Resultado fifo, Resultado lru) {
        System.out.println("\n=================== COMPARAÇÃO FIFO x LRU ===================");
        System.out.printf(BR, "%-10s | %8s | %8s | %10s | %10s%n",
                "Algoritmo", "Faltas", "Acertos", "Taxa falta", "Taxa acerto");
        System.out.println("-----------+----------+----------+------------+------------");
        for (Resultado r : new Resultado[] {fifo, lru}) {
            System.out.printf(BR, "%-10s | %8d | %8d | %9.2f%% | %10.2f%%%n",
                    r.algoritmo, r.faltas, r.acertos(),
                    r.taxaFaltas() * 100, (1 - r.taxaFaltas()) * 100);
        }

        int dif = fifo.faltas - lru.faltas;
        System.out.println();
        if (dif > 0) {
            System.out.printf(BR, "Para esta entrada o LRU teve %d falta(s) a menos que o FIFO "
                    + "(redução de %.1f%%).%n", dif, 100.0 * dif / fifo.faltas);
        } else if (dif < 0) {
            System.out.printf(BR, "Para esta entrada o FIFO teve %d falta(s) a menos que o LRU.%n", -dif);
        } else {
            System.out.println("Para esta entrada os dois algoritmos tiveram o mesmo número de faltas.");
        }
    }

    /** Mostra faltas de FIFO e LRU variando o número de frames de 1 até (páginas distintas + 1). */
    static void imprimirVariacaoDeFrames(List<Integer> referencias) {
        int distintas = new HashSet<>(referencias).size();
        int maximo = Math.min(distintas + 1, 10);

        System.out.println("\n========== VARIANDO O NÚMERO DE FRAMES (mesma sequência) ==========");
        System.out.printf("%-8s | %-12s | %-12s | %s%n", "Frames", "FIFO faltas", "LRU faltas", "Menos faltas");
        System.out.println("---------+--------------+--------------+-------------");
        for (int f = 1; f <= maximo; f++) {
            int ff = simularFifo(f, referencias, false).faltas;
            int fl = simularLru(f, referencias, false).faltas;
            String melhor = ff == fl ? "empate" : (fl < ff ? "LRU" : "FIFO");
            System.out.printf("%-8d | %-12d | %-12d | %s%n", f, ff, fl, melhor);
        }
        System.out.println("(Com frames >= páginas distintas só restam as faltas iniciais, "
                + "e os dois empatam.)");
    }

    /** Anomalia de Belady: no FIFO, MAIS frames podem gerar MAIS faltas. */
    static void demonstrarAnomaliaDeBelady() {
        List<Integer> seq = Arrays.asList(1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5);

        System.out.println("\n================= ANOMALIA DE BELADY =================");
        System.out.println("Sequência clássica: " + seq);
        for (int f = 3; f <= 4; f++) {
            System.out.printf("  %d frames -> FIFO: %2d faltas | LRU: %2d faltas%n", f,
                    simularFifo(f, seq, false).faltas, simularLru(f, seq, false).faltas);
        }
        int f3 = simularFifo(3, seq, false).faltas;
        int f4 = simularFifo(4, seq, false).faltas;
        if (f4 > f3) {
            System.out.println("  -> No FIFO, passar de 3 para 4 frames AUMENTOU as faltas "
                    + "(" + f3 + " -> " + f4 + "): anomalia de Belady.");
        }
        System.out.println("  -> No LRU, mais frames nunca pioram o resultado.");
        System.out.println("  -> Atenção: com 3 frames o FIFO foi melhor que o LRU nesta sequência;");
        System.out.println("     nenhum dos dois vence em todas as entradas.");
    }

    static void imprimirAnaliseCritica(Resultado fifo, Resultado lru) {
        System.out.println("\n================= ANÁLISE CRÍTICA =================");
        System.out.println("1) Critério: o FIFO decide pela ordem de CHEGADA; o LRU, pelo USO recente.");
        System.out.println("   Por isso o FIFO pode expulsar uma página muito usada só por ser antiga,");
        System.out.println("   enquanto o LRU a mantém. Em programas com boa localidade temporal, o LRU");
        System.out.println("   costuma ter menos faltas (nesta entrada: FIFO " + fifo.faltas
                + " x LRU " + lru.faltas + ").");
        System.out.println("2) Anomalia de Belady: acontece no FIFO. O LRU é um algoritmo de pilha:");
        System.out.println("   o conjunto de páginas com N frames sempre está contido no de N+1 frames,");
        System.out.println("   então aumentar a memória nunca aumenta as faltas.");
        System.out.println("3) Custo: o FIFO só mexe na fila em page faults. O LRU precisa registrar");
        System.out.println("   o uso a CADA acesso (inclusive hits). Em hardware real isso é caro, então");
        System.out.println("   sistemas usam aproximações do LRU (bit de referência, algoritmo do relógio).");
        System.out.println("4) Limitações: o LRU não é sempre melhor. Em percursos sequenciais maiores que");
        System.out.println("   a memória (ex.: 1,2,3,4,1,2,3,4 com 3 frames) ele também falha em todos os");
        System.out.println("   acessos, igual ao FIFO. Nenhum dos dois conhece o futuro: o limite teórico");
        System.out.println("   é o algoritmo ótimo (Belady/OPT), que exige saber as próximas referências.");
    }

    // ===================================================================
    // Utilitários de entrada/saída
    // ===================================================================
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

    /** Garante que acentos saiam corretamente (UTF-8) independentemente da configuração do sistema. */
    private static void configurarSaidaUtf8() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 sempre existe na JVM; se falhar, segue com a saída padrão.
        }
    }
}
