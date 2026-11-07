#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================================================
Disciplina : Sistemas Operacionais (Engenharia de Software)
Trabalho   : Gerenciamento de Memória em Sistemas Operacionais
Questão 4  : Garbage Collection em Python (Parte B)
Linguagem  : Python 3.8+ (testado em Python 3.13)

Executar   : python3 q4_garbage_collector.py
============================================================================

O CPython usa DOIS mecanismos complementares para liberar memória:

  1) CONTAGEM DE REFERÊNCIAS (reference counting)
     Todo objeto guarda quantas referências apontam para ele. Quando o
     contador chega a zero, o objeto é destruído NA HORA (determinístico).
     Limitação: não consegue liberar CICLOS (A -> B -> A), pois o contador
     de cada objeto do ciclo nunca chega a zero.

  2) COLETOR DE LIXO GERACIONAL (módulo gc)
     Roda de tempos em tempos e procura grupos de objetos que só são
     alcançáveis por eles mesmos (ciclos inalcançáveis). Os objetos são
     divididos em 3 gerações (0, 1 e 2): objetos novos entram na geração 0
     e, se sobreviverem a uma coleta, são promovidos para a seguinte. A
     geração 0 é coletada com frequência; a 2, raramente (a maioria dos
     objetos "morre jovem").

Este programa demonstra 4 cenários:
  Cenário 1 - Destruição automática por contagem de referências
  Cenário 2 - Referências circulares (a contagem sozinha não resolve)
  Cenário 3 - Coleta geracional automática x contagem de referências
  Cenário 4 - Coletas manuais por geração + estatísticas (gc.get_stats)
"""

import gc
import sys
import tracemalloc
import weakref


# --------------------------------------------------------------------------
# Classe pedida no enunciado
# --------------------------------------------------------------------------
class Objeto:
    """Objeto que ocupa memória e avisa quando é criado e destruído."""

    vivos = 0  # quantas instâncias estão vivas neste momento

    def __init__(self, nome, tamanho_kb=512, falar=True):
        self.nome = nome
        # 'dados' simula o uso de memória: um array grande de bytes
        self.dados = bytearray(tamanho_kb * 1024)
        # 'outro' será usado para montar referências circulares
        self.outro = None
        self.falar = falar
        Objeto.vivos += 1
        if self.falar:
            print(f"    [CRIADO]    {self.nome} ({tamanho_kb} KB)")

    def __del__(self):
        # Chamado pelo Python imediatamente antes de liberar o objeto.
        Objeto.vivos -= 1
        if self.falar:
            print(f"    [DESTRUÍDO] {self.nome}")


# --------------------------------------------------------------------------
# Funções auxiliares de apresentação
# --------------------------------------------------------------------------
def titulo(texto):
    print("\n" + "=" * 68)
    print(f" {texto}")
    print("=" * 68)


def memoria_kb():
    """Memória (em KB) atualmente alocada pelo Python (via tracemalloc)."""
    atual, _pico = tracemalloc.get_traced_memory()
    return atual / 1024


def situacao(rotulo):
    print(f"    >> {rotulo}: objetos vivos = {Objeto.vivos} | "
          f"memória alocada = {memoria_kb():,.0f} KB")


# --------------------------------------------------------------------------
# CENÁRIO 1: contagem de referências
# --------------------------------------------------------------------------
def cenario_1():
    titulo("CENÁRIO 1 - Destruição automática (contagem de referências)")

    print("\n(a) sys.getrefcount() e remoção de referências")
    a = Objeto("A")
    # Em versões mais antigas do Python, getrefcount devolve 1 a mais que o
    # real, pois o argumento da chamada cria uma referência temporária. Nas
    # mais recentes (ex.: 3.15) o valor já vem exato (1 neste caso).
    print(f"    Só a variável 'a' aponta para A  -> getrefcount = {sys.getrefcount(a)}")
    b = a
    print(f"    Com b = a                        -> getrefcount = {sys.getrefcount(a)}")
    lista = [a]
    print(f"    Com lista = [a]                  -> getrefcount = {sys.getrefcount(a)}")
    del b
    print(f"    Após del b                       -> getrefcount = {sys.getrefcount(a)}")
    del lista
    print(f"    Após del lista                   -> getrefcount = {sys.getrefcount(a)}")
    situacao("antes de 'del a'")
    print("    Executando: del a   (contador chega a 0)")
    del a  # contador vai a zero: __del__ roda imediatamente
    situacao("depois de 'del a'")

    print("\n(b) Reatribuir a variável também derruba a referência antiga")
    x = Objeto("X1")
    print("    Executando: x = Objeto('X2')")
    x = Objeto("X2")   # X1 perde a única referência e é destruído aqui
    situacao("depois da reatribuição")
    del x

    print("\n(c) Variável local some quando a função termina")

    def funcao_com_local():
        local = Objeto("LOCAL", tamanho_kb=256)
        situacao("dentro da função")
        # ao sair da função, 'local' deixa de existir

    funcao_com_local()
    situacao("depois da função")


# --------------------------------------------------------------------------
# CENÁRIO 2: referências circulares
# --------------------------------------------------------------------------
def cenario_2():
    titulo("CENÁRIO 2 - Referências circulares e gc.collect()")

    # Desligamos o coletor automático para o resultado ficar previsível:
    # assim vemos o que a contagem de referências sozinha consegue (ou não).
    gc.disable()
    print("\n  gc.disable() -> coletor cíclico automático desligado")

    print("\n(a) Objeto que referencia a si mesmo")
    o = Objeto("AUTO")
    o.outro = o                      # ciclo: o objeto aponta para si mesmo
    fraco_o = weakref.ref(o)         # referência fraca: não conta no refcount
    # Referências reais: a variável 'o' e o próprio objeto (o.outro = o) = 2.
    # Versões mais antigas do Python somam +1 pelo argumento da chamada; as
    # mais recentes não. Por isso a explicação é montada conforme o valor.
    total = sys.getrefcount(o)
    extra = total - 2
    nota = f" + {extra} do argumento da chamada" if extra > 0 else ""
    print(f"    getrefcount(AUTO) = {total} (variável 'o' + ele mesmo{nota})")
    print("    Executando: del o")
    del o
    print(f"    AUTO ainda existe na memória? {fraco_o() is not None}"
          "   <- contagem NÃO liberou (sobrou 1 referência: ele mesmo)")
    situacao("depois de del o")

    print("\n(b) Ciclo entre dois objetos (B1 <-> B2)")
    b1 = Objeto("B1")
    b2 = Objeto("B2")
    b1.outro = b2
    b2.outro = b1
    fraco_b1 = weakref.ref(b1)
    print("    Executando: del b1, b2")
    del b1, b2
    print(f"    B1 ainda existe na memória? {fraco_b1() is not None}")
    situacao("antes de gc.collect()")

    print("\n(c) Forçando a coleta com gc.collect()")
    encontrados = gc.collect()
    print(f"    gc.collect() devolveu {encontrados} (objetos inalcançáveis encontrados;\n"
          "    o valor inclui estruturas internas dos objetos e varia conforme a versão do Python)")
    print(f"    AUTO/B1 ainda existem? {fraco_o() is not None} / {fraco_b1() is not None}")
    situacao("depois de gc.collect()")

    gc.enable()
    print("\n  gc.enable() -> coletor automático religado")


# --------------------------------------------------------------------------
# CENÁRIO 3: coleta geracional automática x contagem de referências
# --------------------------------------------------------------------------
def cenario_3():
    titulo("CENÁRIO 3 - Coleta geracional automática x contagem de referências")

    print(f"\n  Limiares (gc.get_threshold)   : {gc.get_threshold()}")
    print("  (gen0 dispara quando alocações - desalocações > 1º valor)")
    gc.collect()  # começa de um estado limpo

    def coletas_por_geracao():
        return [g["collections"] for g in gc.get_stats()]

    antes = coletas_por_geracao()

    # (a) Objetos SEM ciclo: a contagem de referências resolve sozinha
    print("\n(a) 3.000 objetos sem ciclo, descartados um a um")
    for i in range(3000):
        tmp = Objeto(f"simples{i}", tamanho_kb=1, falar=False)
        del tmp
    situacao("depois do laço (sem ciclos)")
    print("    -> todos liberados NA HORA pela contagem de referências.")

    # (b) Objetos COM ciclo e o coletor automático ligado
    print("\n(b) 3.000 objetos com ciclo (cada um aponta para si mesmo)")
    for i in range(3000):
        tmp = Objeto(f"ciclo{i}", tamanho_kb=1, falar=False)
        tmp.outro = tmp
        del tmp          # a contagem não libera; sobra lixo cíclico
    situacao("logo depois do laço (com ciclos)")
    print("    -> sobraram objetos: o contador de referências não libera ciclos. O coletor\n"
          "       geracional dispara sozinho quando o limiar da geração 0 é ultrapassado\n"
          "       (ver contagem abaixo), mas os ciclos mais recentes ainda esperam coleta.")

    depois = coletas_por_geracao()
    print(f"\n    Coletas automáticas durante o cenário (gen0, gen1, gen2): "
          f"{[d - a for d, a in zip(depois, antes)]}")

    print("\n    Executando gc.collect() para recolher o que restou...")
    encontrados = gc.collect()
    print(f"    gc.collect() devolveu {encontrados}")
    situacao("depois de gc.collect()")

    print("\n  RESUMO DA DIFERENÇA")
    print("    Contagem de referências : imediata, objeto a objeto, não vê ciclos.")
    print("    Coletor geracional      : periódico (depende dos limiares), vê ciclos,")
    print("                              examina mais as gerações jovens que as velhas.")


# --------------------------------------------------------------------------
# CENÁRIO 4: gerações e estatísticas
# --------------------------------------------------------------------------
def cenario_4():
    titulo("CENÁRIO 4 - Coletas por geração e gc.get_stats()")

    gc.collect()
    print(f"\n  gc.get_count() inicial (gen0, gen1, gen2): {gc.get_count()}")

    # Cria objetos que VIVEM: ficam referenciados e, ao sobreviverem às
    # coletas, são promovidos para gerações mais velhas.
    sobreviventes = [Objeto(f"S{i}", tamanho_kb=1, falar=False) for i in range(50)]
    print(f"  Após criar 50 objetos duradouros : {gc.get_count()}")

    gc.collect(0)
    print(f"  Após gc.collect(0) (só gen0)     : {gc.get_count()}  "
          "<- sobreviventes promovidos para gen1")
    gc.collect(1)
    print(f"  Após gc.collect(1) (gen0 e gen1) : {gc.get_count()}  "
          "<- promovidos para gen2")
    gc.collect(2)
    print(f"  Após gc.collect(2) (coleta total): {gc.get_count()}")

    print("\n  Estatísticas acumuladas do coletor - gc.get_stats():")
    print("  (collections = nº de coletas | collected = objetos recolhidos |")
    print("   uncollectable = objetos que não puderam ser liberados)")
    for geracao, est in enumerate(gc.get_stats()):
        print(f"    geração {geracao}: collections={est['collections']:>4} | "
              f"collected={est['collected']:>6} | "
              f"uncollectable={est['uncollectable']}")

    del sobreviventes  # sem ciclos: a contagem de referências libera tudo


# --------------------------------------------------------------------------
# Programa principal
# --------------------------------------------------------------------------
def main():
    print(f"Python {sys.version.split()[0]} | gc habilitado: {gc.isenabled()}")
    tracemalloc.start()  # permite medir a memória alocada pelo Python

    cenario_1()
    cenario_2()
    cenario_3()
    cenario_4()

    titulo("FIM")
    situacao("estado final")
    tracemalloc.stop()


if __name__ == "__main__":
    main()
