/*
 * ============================================================================
 * Disciplina : Sistemas Operacionais (Engenharia de Software)
 * Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
 * Questão 1  : Alocação estática vs. dinâmica (Parte B)
 * Linguagem  : C (padrão C99 ou superior)
 *
 * Compilar : gcc -Wall -Wextra -std=c99 -o q1_alocacao q1_alocacao.c
 * Executar : ./q1_alocacao
 * ============================================================================
 *
 * O que o programa faz:
 *   1. Declara um array ESTÁTICO de 5 inteiros e preenche com 1..5.
 *   2. Aloca DINAMICAMENTE (malloc) um array de 10 inteiros.
 *   3. Preenche o array dinâmico com 10..19.
 *   4. Imprime os endereços de memória dos dois arrays.
 *   5. Calcula a diferença entre os endereços, mostrando que os dois estão
 *      em regiões diferentes da memória do processo.
 *   6. Libera a memória alocada dinamicamente (free).
 *
 * Observação importante sobre "estático":
 *   Em C, a palavra "static" dentro de uma função faz a variável ser guardada
 *   na área de dados do programa (segmento .data/.bss), que existe durante
 *   toda a execução. É diferente de uma variável local comum (automática),
 *   que fica na PILHA (stack). Aqui usamos "static" de propósito, porque é a
 *   alocação estática "de verdade": o endereço é fixado em tempo de
 *   compilação/carga e não muda durante a execução.
 *
 *   Já o malloc devolve memória do HEAP (área dinâmica), gerenciada em tempo
 *   de execução. Por isso os endereços ficam bem distantes um do outro.
 */

#include <stdio.h>    /* printf, fprintf */
#include <stdlib.h>   /* malloc, free, EXIT_FAILURE */
#include <stdint.h>   /* uintptr_t: inteiro grande o bastante para guardar um endereço */

#define TAM_ESTATICO  5
#define TAM_DINAMICO 10

int main(void)
{
    /* ------------------------------------------------------------------ */
    /* 1. Array estático de 5 inteiros (fica na área de dados do programa) */
    /* ------------------------------------------------------------------ */
    static int vetor_estatico[TAM_ESTATICO];

    for (int i = 0; i < TAM_ESTATICO; i++) {
        vetor_estatico[i] = i + 1;            /* valores de 1 a 5 */
    }

    /* ------------------------------------------------------------------ */
    /* 2. Array dinâmico de 10 inteiros (fica no heap)                     */
    /* ------------------------------------------------------------------ */
    int *vetor_dinamico = (int *)malloc(TAM_DINAMICO * sizeof(int));

    /* Tratamento de erro: malloc devolve NULL quando não há memória
     * disponível. Usar o ponteiro sem checar isso causaria falha de
     * segmentação, então verificamos ANTES de qualquer uso. */
    if (vetor_dinamico == NULL) {
        fprintf(stderr, "Erro: falha na alocação dinâmica de %lu bytes.\n",
                (unsigned long)(TAM_DINAMICO * sizeof(int)));
        return EXIT_FAILURE;
    }

    /* ------------------------------------------------------------------ */
    /* 3. Preenche o array dinâmico com valores de 10 a 19                 */
    /* ------------------------------------------------------------------ */
    for (int i = 0; i < TAM_DINAMICO; i++) {
        vetor_dinamico[i] = 10 + i;
    }

    /* ------------------------------------------------------------------ */
    /* 4. Exibe valores e endereços de cada elemento                       */
    /* ------------------------------------------------------------------ */
    printf("==================================================\n");
    printf(" Alocação estática x dinâmica\n");
    printf("==================================================\n\n");

    printf("[ARRAY ESTÁTICO] (%d inteiros, %lu bytes)\n",
           TAM_ESTATICO, (unsigned long)sizeof(vetor_estatico));
    for (int i = 0; i < TAM_ESTATICO; i++) {
        printf("  vetor_estatico[%d] = %2d   endereço: %p\n",
               i, vetor_estatico[i], (void *)&vetor_estatico[i]);
    }

    printf("\n[ARRAY DINÂMICO] (%d inteiros, %lu bytes)\n",
           TAM_DINAMICO, (unsigned long)(TAM_DINAMICO * sizeof(int)));
    for (int i = 0; i < TAM_DINAMICO; i++) {
        printf("  vetor_dinamico[%d] = %2d   endereço: %p\n",
               i, vetor_dinamico[i], (void *)&vetor_dinamico[i]);
    }

    /* ------------------------------------------------------------------ */
    /* 5. Diferença entre os endereços base dos dois arrays                */
    /* ------------------------------------------------------------------ */
    /* Subtrair ponteiros de objetos diferentes é comportamento indefinido
     * em C. Por isso convertemos para uintptr_t (um inteiro) antes de
     * subtrair: assim a conta é feita sobre os números dos endereços. */
    uintptr_t end_estatico = (uintptr_t)vetor_estatico;
    uintptr_t end_dinamico = (uintptr_t)vetor_dinamico;

    uintptr_t diferenca = (end_dinamico > end_estatico)
                              ? end_dinamico - end_estatico
                              : end_estatico - end_dinamico;

    printf("\n--------------------------------------------------\n");
    printf(" Comparação dos endereços base\n");
    printf("--------------------------------------------------\n");
    printf("  Base do array estático : %p  (área de dados do programa)\n",
           (void *)vetor_estatico);
    printf("  Base do array dinâmico : %p  (heap)\n",
           (void *)vetor_dinamico);
    /* Imprimimos como double (%.0f) em vez de %llu: compiladores antigos do
     * Windows (como o do Dev-C++) não entendem %llu no printf. */
    printf("  Diferença              : %.0f bytes (%.2f MiB)\n",
           (double)diferenca, (double)diferenca / (1024.0 * 1024.0));
    printf("\n  Os endereços estão distantes: os arrays pertencem a regiões\n");
    printf("  diferentes da memória do processo (dados estáticos x heap).\n");

    /* ------------------------------------------------------------------ */
    /* 6. Libera a memória dinâmica                                        */
    /* ------------------------------------------------------------------ */
    free(vetor_dinamico);
    vetor_dinamico = NULL;   /* evita ponteiro "pendurado" (dangling pointer) */

    printf("\nMemória dinâmica liberada com sucesso (free).\n");
    /* O array estático não precisa (nem pode) ser liberado: ele existe
     * até o fim do programa e é recuperado pelo sistema operacional. */

    return EXIT_SUCCESS;
}
