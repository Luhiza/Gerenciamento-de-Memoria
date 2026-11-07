#!/usr/bin/env bash
# Compila e executa todos os programas, salvando as saídas na pasta logs/.
# Uso: bash run_all.sh      (Linux, macOS ou WSL; requer gcc, javac e python3)

set -e
cd "$(dirname "$0")"
mkdir -p logs

# Remove a mensagem informativa que algumas instalações do Java imprimem.
limpar() { grep -v "Picked up JAVA_TOOL_OPTIONS" || true; }

echo ">> Questão 1 (C)"
(cd q1_alocacao && gcc -Wall -Wextra -std=c99 -o q1_alocacao q1_alocacao.c && ./q1_alocacao) \
    | tee logs/q1_saida.txt

echo ">> Questão 2 (Python)"
(cd q2_fragmentacao && python3 q2_fragmentacao.py) | tee logs/q2_saida.txt

echo ">> Questão 3 (Java)"
(cd q3_fifo && javac FifoPageReplacement.java 2>&1 | limpar \
    && java FifoPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2 2>&1 | limpar) \
    | tee logs/q3_saida.txt
(cd q3_fifo && { echo "# Teste de entradas inválidas"; \
    echo '$ java FifoPageReplacement 0 1 2 3'; java FifoPageReplacement 0 1 2 3 2>&1 | limpar; \
    echo '$ java FifoPageReplacement abc 1 2'; java FifoPageReplacement abc 1 2 2>&1 | limpar; \
    echo '$ java FifoPageReplacement 3 1 -2 3'; java FifoPageReplacement 3 1 -2 3 2>&1 | limpar; } ) \
    > logs/q3_entradas_invalidas.txt

echo ">> Questão 4 (Python)"
(cd q4_garbage_collector && python3 q4_garbage_collector.py) | tee logs/q4_saida.txt

echo ">> Questão 5 (C)"
(cd q5_desempenho && gcc -O2 -Wall -Wextra -std=c11 -o q5_desempenho q5_desempenho.c -lm \
    && ./q5_desempenho) | tee logs/q5_saida.txt

echo ">> Bônus - LRU (Java)"
(cd bonus_lru && javac LruPageReplacement.java 2>&1 | limpar \
    && java LruPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2 2>&1 | limpar) \
    | tee logs/bonus_lru_saida.txt

echo
echo "Pronto! Saídas salvas em logs/."
