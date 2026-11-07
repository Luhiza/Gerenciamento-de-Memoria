# Trabalho Prático 1 — Gerenciamento de Memória em Sistemas Operacionais

**Curso:** Engenharia de Software — **Disciplina:** Sistemas Operacionais
**Professor:** José Carlos Domingues Flores

**Grupo:**

| Nome | RA |
|------|----|
| Ana Luiza Ferreira Braga | 25061054-2 |
| Yuri Menegat | 25089796-2 |
| Gabriela de Oliveira do Carmo | 25178005-2 |
| Patricia de Paula Aliski | 26015671-2 |
| _(preencher)_ | _(preencher)_ |

## Estrutura do repositório

| Pasta | Conteúdo | Linguagem |
|-------|----------|-----------|
| `q1_alocacao/` | Alocação estática x dinâmica (array estático, `malloc`, endereços) | C |
| `q2_fragmentacao/` | Partições fixas, First-Fit e fragmentação interna | Python |
| `q3_fifo/` | Substituição de páginas FIFO | Java |
| `q4_garbage_collector/` | Contagem de referências, ciclos e módulo `gc` | Python |
| `q5_desempenho/` | Pilha x heap (1.000.000 de alocações, média de 10 repetições) | C |
| `bonus_lru/` | Substituição de páginas LRU comparada ao FIFO | Java |
| `capturas/` | Capturas de tela da execução de cada programa | — |

As respostas teóricas estão no PDF `Respostas_Teoricas_Gerenciamento_de_Memoria.pdf`.

## Requisitos

`gcc` (qualquer versão recente), `python3` (3.8 ou superior), JDK 8 ou superior (`javac` e `java`).
Os programas foram testados em Linux (gcc 13.3, Python 3.13, Java 21) e também no Windows
(TDM-GCC 4.9.2 do Dev-C++ e Python 3.15.2). As capturas de tela da pasta `capturas/` foram feitas no Windows.

## Como compilar e executar cada programa

Todos os comandos devem ser executados dentro da pasta de cada questão.

**Questão 1 (C)**
```bash
cd q1_alocacao
gcc -Wall -Wextra -std=c99 -o q1_alocacao q1_alocacao.c
./q1_alocacao
```

**Questão 2 (Python)**
```bash
cd q2_fragmentacao
python3 q2_fragmentacao.py
```

**Questão 3 (Java)** — pode receber a entrada pelo teclado ou por argumentos
```bash
cd q3_fifo
javac FifoPageReplacement.java
java FifoPageReplacement                                  # pergunta frames e sequência
java FifoPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2      # 3 frames + sequência
```

**Questão 4 (Python)**
```bash
cd q4_garbage_collector
python3 q4_garbage_collector.py
```

**Questão 5 (C)**
```bash
cd q5_desempenho
gcc -O2 -Wall -Wextra -std=c11 -o q5_desempenho q5_desempenho.c -lm
./q5_desempenho
```
Os tempos mudam de máquina para máquina; o que se mantém é a ordem (pilha mais rápida que heap).

**Bônus — LRU (Java)** — mesma entrada da Questão 3
```bash
cd bonus_lru
javac LruPageReplacement.java
java LruPageReplacement 3 7 0 1 2 0 3 0 4 2 3 0 3 2
```

**Rodar tudo de uma vez (Linux, macOS ou WSL):** `bash run_all.sh` (as saídas são gravadas na pasta `logs/`, criada na hora)

## Observações

- **Windows:** use MinGW-w64 (gcc) ou o WSL. Se os acentos aparecerem errados no
  terminal, execute `chcp 65001` antes de rodar os programas. O `run_all.sh` só
  funciona em ambiente tipo Unix (Git Bash ou WSL).
- **Q1:** os endereços impressos mudam a cada execução (o sistema operacional
  randomiza o espaço de endereços — ASLR). A distância entre os dois arrays
  continua grande, o que é o objetivo da demonstração.
- **Q4:** os números exatos de `gc.get_stats()` e `gc.collect()` dependem da versão do Python.
- **Capturas de tela:** estão em `capturas/` (`q1.png`, `q2_parte1.png` etc.), uma ou mais imagens por questão.
- **Windows com gcc antigo:** compile a Q1 com `-std=c99` e a Q5 com `-std=c11`, como nos comandos acima.
