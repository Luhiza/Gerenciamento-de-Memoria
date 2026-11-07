/*
 * ============================================================================
 * Disciplina : Sistemas Operacionais (Engenharia de Software)
 * Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
 * Questão 5  : Comparação de desempenho de alocação - pilha x heap (Parte B)
 * Linguagem  : C (gcc)
 *
 * Compilar : gcc -O2 -Wall -Wextra -std=c11 -o q5_desempenho q5_desempenho.c -lm
 * Executar : ./q5_desempenho
 * ============================================================================
 *
 * O que é medido (cada teste executa 1.000.000 de operações):
 *
 *   Teste 1 - PILHA : chama uma função 1.000.000 de vezes; a cada chamada
 *                     uma variável local (struct de 64 bytes) é criada na
 *                     pilha e destruída quando a função retorna.
 *
 *   Teste 2 - HEAP  : 1.000.000 de vezes faz malloc(64) -> usa -> free().
 *                     Cada objeto é liberado logo depois de usado.
 *
 *   Teste 3 - HEAP em lote (variação): aloca 1.000.000 de objetos de 64
 *                     bytes e MANTÉM todos vivos; só depois libera todos.
 *                     Mostra o custo quando o heap precisa crescer.
 *
 * Cada teste é repetido N_REPETICOES vezes (mais 1 execução de aquecimento
 * descartada) e usamos a MÉDIA. As repetições alternam os testes para que
 * ruídos do sistema afetem todos de forma parecida.
 *
 * Cuidados para a comparação ser justa:
 *   - O mesmo tamanho (64 bytes) e o mesmo trabalho (escrever e ler o
 *     primeiro e o último campo) é feito nos três testes.
 *   - 'volatile' impede que o compilador elimine as alocações, pois sem ele
 *     um compilador com otimização poderia perceber que o objeto nunca é
 *     "usado de verdade" e removê-lo (o teste mediria o nada).
 *   - A função da pilha é marcada noinline para a chamada acontecer de fato.
 */

#define _POSIX_C_SOURCE 200809L   /* habilita clock_gettime no Linux/macOS */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#if defined(_WIN32)
  #include <windows.h>
  /* No Windows usamos o contador de alta resolução. */
  static double agora_s(void)
  {
      LARGE_INTEGER freq, cont;
      QueryPerformanceFrequency(&freq);
      QueryPerformanceCounter(&cont);
      return (double)cont.QuadPart / (double)freq.QuadPart;
  }
#else
  #include <time.h>
  /* Relógio monotônico: não é afetado por ajustes da hora do sistema. */
  static double agora_s(void)
  {
      struct timespec ts;
      clock_gettime(CLOCK_MONOTONIC, &ts);
      return (double)ts.tv_sec + (double)ts.tv_nsec / 1e9;
  }
#endif

#define N_OPERACOES  1000000   /* operações por teste */
#define N_REPETICOES 10        /* repetições para calcular a média */
#define N_CAMPOS     16        /* 16 ints = 64 bytes por objeto */

typedef struct {
    int campos[N_CAMPOS];
} Objeto;

/* Variável global volatile: "consome" os resultados para o compilador não
 * descartar o trabalho feito em cada iteração. */
static volatile int sumidouro;

/* Escreve no primeiro e no último campo e lê os dois de volta. É o mesmo
 * trabalho (pequeno e igual) nos três testes, para que a diferença de tempo
 * venha da ALOCAÇÃO e não do uso do objeto. */
static void usar_objeto(volatile Objeto *o, int semente)
{
    o->campos[0] = semente;
    o->campos[N_CAMPOS - 1] = semente + 1;
    sumidouro = o->campos[0] + o->campos[N_CAMPOS - 1];
}

/* ------------------------------------------------------------------ */
/* Teste 1: alocação na PILHA                                          */
/* ------------------------------------------------------------------ */
__attribute__((noinline))
static void funcao_com_variavel_local(int i)
{
    volatile Objeto local;      /* criada na pilha ao entrar na função  */
    usar_objeto(&local, i);
}                               /* destruída ao sair (só mexe no SP)    */

static void teste_pilha(void)
{
    for (int i = 0; i < N_OPERACOES; i++) {
        funcao_com_variavel_local(i);
    }
}

/* ------------------------------------------------------------------ */
/* Teste 2: alocação no HEAP (malloc/free imediatos)                   */
/* ------------------------------------------------------------------ */
static void teste_heap(void)
{
    for (int i = 0; i < N_OPERACOES; i++) {
        Objeto *p = (Objeto *)malloc(sizeof(Objeto));
        if (p == NULL) {                       /* tratamento de erro */
            fprintf(stderr, "Erro: malloc falhou na iteração %d\n", i);
            exit(EXIT_FAILURE);
        }
        usar_objeto(p, i);
        free(p);
    }
}

/* ------------------------------------------------------------------ */
/* Teste 3: alocação no HEAP em lote (todos vivos ao mesmo tempo)      */
/* ------------------------------------------------------------------ */
static void teste_heap_em_lote(void)
{
    Objeto **vetor = (Objeto **)malloc(N_OPERACOES * sizeof(Objeto *));
    if (vetor == NULL) {
        fprintf(stderr, "Erro: malloc do vetor de ponteiros falhou\n");
        exit(EXIT_FAILURE);
    }
    for (int i = 0; i < N_OPERACOES; i++) {
        vetor[i] = (Objeto *)malloc(sizeof(Objeto));
        if (vetor[i] == NULL) {
            fprintf(stderr, "Erro: malloc falhou na iteração %d\n", i);
            exit(EXIT_FAILURE);
        }
        usar_objeto(vetor[i], i);
    }
    for (int i = 0; i < N_OPERACOES; i++) {
        free(vetor[i]);
    }
    free(vetor);
}

/* ------------------------------------------------------------------ */
/* Infraestrutura de medição                                           */
/* ------------------------------------------------------------------ */
typedef void (*TesteFn)(void);

static double cronometrar(TesteFn teste)
{
    double inicio = agora_s();
    teste();
    return agora_s() - inicio;
}

typedef struct {
    double media, desvio, minimo, maximo;   /* em segundos */
} Estatisticas;

static Estatisticas calcular(const double *t, int n)
{
    Estatisticas e = {0.0, 0.0, t[0], t[0]};
    for (int i = 0; i < n; i++) {
        e.media += t[i];
        if (t[i] < e.minimo) e.minimo = t[i];
        if (t[i] > e.maximo) e.maximo = t[i];
    }
    e.media /= n;
    for (int i = 0; i < n; i++) {
        e.desvio += (t[i] - e.media) * (t[i] - e.media);
    }
    e.desvio = sqrt(e.desvio / n);
    return e;
}

static void linha(const char *nome, Estatisticas e)
{
    printf("  %-28s | %8.3f | %8.3f | %8.3f | %8.3f | %7.1f\n",
           nome, e.media * 1000, e.desvio * 1000,
           e.minimo * 1000, e.maximo * 1000,
           e.media / N_OPERACOES * 1e9);
}

int main(void)
{
    double t_pilha[N_REPETICOES], t_heap[N_REPETICOES], t_lote[N_REPETICOES];

    printf("==============================================================\n");
    printf(" Comparação de desempenho: pilha (stack) x heap\n");
    printf("==============================================================\n");
    printf(" Operações por teste : %d\n", N_OPERACOES);
    printf(" Repetições          : %d (+1 de aquecimento descartada)\n", N_REPETICOES);
    printf(" Tamanho do objeto   : %lu bytes\n\n", (unsigned long)sizeof(Objeto));

    /* Aquecimento: a primeira execução paga custos que não queremos medir
     * (páginas ainda não mapeadas, caches frios, expansão inicial do heap). */
    cronometrar(teste_pilha);
    cronometrar(teste_heap);
    cronometrar(teste_heap_em_lote);

    for (int r = 0; r < N_REPETICOES; r++) {
        t_pilha[r] = cronometrar(teste_pilha);
        t_heap[r]  = cronometrar(teste_heap);
        t_lote[r]  = cronometrar(teste_heap_em_lote);
        printf("  Repetição %2d: pilha %8.3f ms | heap %8.3f ms | heap em lote %8.3f ms\n",
               r + 1, t_pilha[r] * 1000, t_heap[r] * 1000, t_lote[r] * 1000);
    }

    Estatisticas ep = calcular(t_pilha, N_REPETICOES);
    Estatisticas eh = calcular(t_heap,  N_REPETICOES);
    Estatisticas el = calcular(t_lote,  N_REPETICOES);

    printf("\n  %-28s | %8s | %8s | %8s | %8s | %7s\n",
           "Teste", "média ms", "desvio", "mín ms", "máx ms", "ns/op");
    printf("  -----------------------------+----------+----------+----------+----------+--------\n");
    linha("1) Pilha (var. local)", ep);
    linha("2) Heap (malloc+free)",     eh);
    linha("3) Heap em lote",           el);

    /* Diferença percentual: quanto o heap é mais lento que a pilha.
     *   diferença % = (tempo_heap - tempo_pilha) / tempo_pilha * 100      */
    double dif_heap = (eh.media - ep.media) / ep.media * 100.0;
    double dif_lote = (el.media - ep.media) / ep.media * 100.0;

    printf("\n  Diferença de desempenho em relação à pilha:\n");
    printf("    Heap (malloc+free) : %.1f%% mais lento (%.1fx o tempo da pilha)\n",
           dif_heap, eh.media / ep.media);
    printf("    Heap em lote       : %.1f%% mais lento (%.1fx o tempo da pilha)\n",
           dif_lote, el.media / ep.media);

    /*
     * ----------------------------- INTERPRETAÇÃO -----------------------------
     * Por que a pilha é mais rápida?
     *   - Alocar na pilha é só deslocar o ponteiro de pilha (SP): uma
     *     instrução ao entrar na função e outra ao sair. Não há busca por um
     *     bloco livre, não há metadados e não há chamada ao alocador.
     *   - A pilha segue a disciplina LIFO e fica "quente" na cache da CPU:
     *     o mesmo trecho de memória é reaproveitado a cada chamada.
     *
     * Por que o heap é mais lento?
     *   - malloc precisa procurar um bloco adequado (listas/bins), atualizar
     *     estruturas internas e guardar metadados; free faz o caminho
     *     inverso (e pode juntar blocos vizinhos).
     *   - Quando o heap precisa crescer, o alocador pede mais memória ao
     *     sistema operacional (brk/mmap), o que envolve chamadas de sistema
     *     e page faults - por isso o Teste 3 é bem mais caro que o Teste 2.
     *   - No Teste 2 o malloc/free imediato reaproveita sempre o mesmo bloco
     *     (cache por thread do alocador), então ele é o "melhor caso" do
     *     heap. O Teste 3 mostra o custo mais realista.
     *
     * Possíveis variações nos resultados:
     *   - Nível de otimização do compilador (-O0, -O2...), versão do gcc e
     *     da biblioteca C (glibc, musl, MSVCRT) - cada alocador é diferente.
     *   - Outros processos competindo pela CPU, frequência dinâmica da CPU,
     *     tamanho das caches, máquina virtual/WSL e sistema operacional.
     *   - Por isso repetimos o teste e usamos média e desvio padrão: o valor
     *     absoluto muda de máquina para máquina, mas a ORDEM (pilha mais
     *     rápida que heap) se mantém.
     * -------------------------------------------------------------------------
     */
    return EXIT_SUCCESS;
}
