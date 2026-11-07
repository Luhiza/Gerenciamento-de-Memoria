#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================================================
Disciplina : Sistemas Operacionais (Engenharia de Software)
Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
Questão 2  : Simulação de fragmentação de memória (Parte B)
Linguagem  : Python 3.8+

Executar   : python3 q2_fragmentacao.py
============================================================================

Simula um gerenciador de memória com PARTIÇÕES FIXAS e algoritmo FIRST-FIT.

  - Memória total: 1000 unidades, dividida em 5 partições fixas:
        100, 150, 200, 250 e 300 unidades (nessa ordem de endereços).
  - First-Fit: percorre as partições do início ao fim e usa a PRIMEIRA que
    estiver livre e for grande o bastante para o processo.
  - Fragmentação interna: espaço que sobra dentro da partição alocada
    (tamanho da partição - tamanho do processo). Esse espaço fica
    desperdiçado, pois a partição é indivisível.

Estrutura de dados:
  Cada partição é um dicionário; a memória é uma lista desses dicionários.
  Isso mantém a ordem de endereços (necessária para o First-Fit) e permite
  guardar o estado de cada partição de forma simples.
"""

# --------------------------------------------------------------------------
# Configuração da memória
# --------------------------------------------------------------------------
TAMANHOS_PARTICOES = [100, 150, 200, 250, 300]

# memoria: lista de partições. Cada partição guarda:
#   id       -> número da partição (1 a 5)
#   tamanho  -> tamanho fixo da partição
#   processo -> nome do processo que a ocupa (None = livre)
#   uso      -> tamanho do processo alocado (0 = livre)
memoria = [
    {"id": i + 1, "tamanho": t, "processo": None, "uso": 0}
    for i, t in enumerate(TAMANHOS_PARTICOES)
]


# --------------------------------------------------------------------------
# Funções auxiliares
# --------------------------------------------------------------------------
def fragmentacao_interna_total():
    """Soma o espaço desperdiçado dentro de todas as partições ocupadas."""
    return sum(p["tamanho"] - p["uso"] for p in memoria if p["processo"] is not None)


def _buscar_particao_do_processo(nome):
    """Devolve a partição ocupada pelo processo 'nome' (ou None)."""
    for p in memoria:
        if p["processo"] == nome:
            return p
    return None


# --------------------------------------------------------------------------
# Funções pedidas no enunciado
# --------------------------------------------------------------------------
def alocar_processo(nome, tamanho):
    """
    Aloca o processo na primeira partição livre que caiba (First-Fit).
    Exibe a fragmentação interna gerada. Retorna True se alocou, False se não.
    """
    # Tratamento de entradas inválidas
    if not isinstance(tamanho, int) or tamanho <= 0:
        print(f"  [ERRO] Tamanho inválido para o processo {nome}: {tamanho}")
        return False
    if _buscar_particao_do_processo(nome) is not None:
        print(f"  [ERRO] O processo {nome} já está alocado na memória.")
        return False

    # First-Fit: a ordem da lista já é a ordem de endereços
    for p in memoria:
        if p["processo"] is None and p["tamanho"] >= tamanho:
            p["processo"] = nome
            p["uso"] = tamanho
            desperdicio = p["tamanho"] - tamanho
            print(f"  [OK] {nome} ({tamanho}) -> partição {p['id']} "
                  f"({p['tamanho']}) | fragmentação interna: {desperdicio}")
            print(f"       Fragmentação interna total agora: "
                  f"{fragmentacao_interna_total()}")
            return True

    # Nenhuma partição livre comporta o processo
    maior = max(p["tamanho"] for p in memoria)
    if tamanho > maior:
        motivo = f"maior que a maior partição ({maior})"
    else:
        motivo = "todas as partições adequadas estão ocupadas"
    print(f"  [FALHA] {nome} ({tamanho}) não pôde ser alocado: {motivo}.")
    print(f"       Fragmentação interna total agora: "
          f"{fragmentacao_interna_total()}")
    return False


def liberar_processo(nome):
    """Libera a partição ocupada pelo processo. Retorna True se liberou."""
    p = _buscar_particao_do_processo(nome)
    if p is None:
        print(f"  [ERRO] O processo {nome} não está na memória.")
        return False
    print(f"  [OK] {nome} liberado da partição {p['id']} "
          f"(tamanho {p['tamanho']}).")
    p["processo"] = None
    p["uso"] = 0
    print(f"       Fragmentação interna total agora: "
          f"{fragmentacao_interna_total()}")
    return True


def exibir_memoria():
    """Mostra o estado atual de cada partição."""
    print("  +-----------+---------+----------+------+-------------+")
    print("  | Partição  | Tamanho | Processo | Uso  | Frag.interna|")
    print("  +-----------+---------+----------+------+-------------+")
    for p in memoria:
        if p["processo"] is None:
            print(f"  | {p['id']:^9} | {p['tamanho']:^7} | {'livre':^8} | "
                  f"{'-':^4} | {'-':^11} |")
        else:
            frag = p["tamanho"] - p["uso"]
            print(f"  | {p['id']:^9} | {p['tamanho']:^7} | {p['processo']:^8} | "
                  f"{p['uso']:^4} | {frag:^11} |")
    print("  +-----------+---------+----------+------+-------------+")
    livres = sum(p["tamanho"] for p in memoria if p["processo"] is None)
    print(f"  Memória total: {sum(TAMANHOS_PARTICOES)} | "
          f"em partições livres: {livres} | "
          f"fragmentação interna total: {fragmentacao_interna_total()}")


# --------------------------------------------------------------------------
# Teste com a sequência pedida no enunciado
# --------------------------------------------------------------------------
def main():
    print("=" * 62)
    print(" Simulação de partições fixas - First-Fit")
    print("=" * 62)
    print("\nEstado inicial:")
    exibir_memoria()

    # (descrição, função, argumentos)
    passos = [
        ("Alocar P1 com 90 unidades",           alocar_processo,  ("P1", 90)),
        ("Alocar P2 com 140 unidades",          alocar_processo,  ("P2", 140)),
        ("Alocar P3 com 180 unidades",          alocar_processo,  ("P3", 180)),
        ("Liberar P2",                          liberar_processo, ("P2",)),
        ("Alocar P4 com 100 unidades",          alocar_processo,  ("P4", 100)),
        ("Tentar alocar P5 com 350 (deve falhar)", alocar_processo, ("P5", 350)),
    ]

    for numero, (descricao, funcao, args) in enumerate(passos, start=1):
        print(f"\n--- Passo {numero}: {descricao} ---")
        funcao(*args)
        exibir_memoria()

    print("\n" + "=" * 62)
    print(f" Fragmentação interna total final: {fragmentacao_interna_total()} unidades")
    print("=" * 62)


if __name__ == "__main__":
    main()
