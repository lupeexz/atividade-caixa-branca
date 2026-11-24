# Teste de caixa branca e revisão de código-fonte

**Cenário 2 — Estoque por arquivo CSV**  
UX/UI e Testes de Software · FACENS

## 1. Introdução

A classe `EstoqueArquivo` consulta a quantidade inicial de um produto em `estoque.csv` e registra a saída em `movimentacoes.txt`. A revisão analisa os métodos `consultarQuantidade` e `registrarSaida`. O código em [original/](original/) é a transcrição, com a mesma numeração de linhas, da imagem fornecida na atividade do Canvas.

## 2. Execução

Requisito de execução dos testes: JDK 17 ou superior. No PowerShell, execute na raiz do repositório:

```powershell
powershell -ExecutionPolicy Bypass -File .\testes\executar.ps1
```

Se o JDK estiver fora do PATH, informe a pasta `bin` com `-JdkBin`. O script compila original e revisado separadamente porque os dois arquivos declaram a mesma classe. Cada combinação de caso e versão roda em um processo Java com diretório temporário próprio. As divergências esperadas do legado aparecem no relatório. O script termina com erro se a compilação falhar ou se a versão revisada divergir do contrato testado.

Para compilar a versão corrigida separadamente:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -encoding UTF-8 -d build revisado/EstoqueArquivo.java
```

A aplicação chamadora instancia `new EstoqueArquivo(Path.of("pasta-dos-arquivos"))` e chama os métodos públicos. O CSV de entrada usa UTF-8, sem cabeçalho, com uma linha `codigo;quantidade` por produto. Por exemplo, com `A;5` e sem histórico, `consultarQuantidade("A")` retorna 5. Uma chamada bem-sucedida a `registrarSaida("A", 2)` registra `A;2` no histórico e uma nova consulta retorna 3.

## 3. Análise estática

A aba **CAIXA BRANCA (ESTÁTICO)** da [planilha oficial preenchida](PLANO_DE_TESTE.xlsx) contém 15 verificações com localização, categoria, severidade, consequência e correção proposta. As demais abas conservam o conteúdo do modelo fornecido pela disciplina.

- **Crítica · AE12.** A consulta usa exclusivamente o saldo base. Como o registro de saídas nunca é descontado, pedidos sequenciais podem confirmar retiradas maiores que o saldo disponível.
- **Alta · AE08, AE09 e AE13.** Os dois blocos `catch` descartam a causa. Os arquivos ficam abertos, e `true` pode ser retornado com a escrita retida no buffer.
- **Alta · AE05, AE10 e AE11.** Quantidades zero ou negativas passam pelo único limite imposto, e códigos inválidos ou linhas malformadas comprometem o registro ou interrompem a operação.
- **Baixa a média · AE01, AE03, AE07 e AE14.** Contrato de arquivo implícito, caminhos literais, responsabilidades misturadas e codificação dependente do ambiente dificultam manutenção e diagnóstico.

## 4. Grafos do código original

Os nós representam blocos de comandos sequenciais, decisões, entrada e saída. Tracejado indica a transição para o `catch` por `IOException`. A entrada e a saída de cada método pertencem ao mesmo componente conectado. Os grafos são intraprocedurais. A consulta dentro de `registrarSaida` ocupa um único nó. Exceções não verificadas, como `ArrayIndexOutOfBoundsException` e `NumberFormatException`, são analisadas na matriz e ficam fora do cálculo. Nenhum dos métodos usa `switch`.

### 4.1 consultarQuantidade

![Grafo de consultarQuantidade](diagramas/consulta.svg)

[Fonte Mermaid](diagramas/consulta.mmd)

As arestas 3→9 e 4→9 representam, respectivamente, falha na abertura do leitor e falha em `readLine`. A leitura foi separada da decisão de fim de arquivo para distinguir exceção de EOF. O `break` direciona o nó 8 ao retorno sem outra leitura.

**N = 11, E = 14, P = 1.** V(G) = 14 − 11 + 2 × 1 = **5**. Checagem por desvios: 1 + 2 decisões explícitas (`while` e `if`) + 2 desvios excepcionais binários = 5.

### 4.2 registrarSaida

![Grafo de registrarSaida](diagramas/saida.svg)

[Fonte Mermaid](diagramas/saida.mmd)

As arestas 4→8 e 5→8 representam falha na abertura de `FileWriter` e na escrita. Os retornos verdadeiro e falso saem pelo mesmo nó terminal. A falha absorvida na consulta pode entregar disponibilidade zero ao nó 3. Uma exceção de conversão que escape da consulta encerra a chamada antes desse nó.

**N = 10, E = 12, P = 1.** V(G) = 12 − 10 + 2 × 1 = **4**. Checagem por desvios: 1 + 1 `if` + 2 desvios excepcionais binários = 4.

## 5. Caminhos básicos independentes

Os caminhos estão na ordem em que acrescentam arestas ao conjunto. Cada linha depois da primeira cita uma aresta que ainda não apareceu.

| Caminho | Nós | Condição | Aresta introduzida |
|---|---|---|---|
| Q1 | 1→2→3→4→5→10→11 | Arquivo vazio e EOF na primeira leitura | Caminho inicial |
| Q2 | 1→2→3→4→5→6→7→8→10→11 | Primeira linha corresponde e tem quantidade válida | 7→8 |
| Q3 | 1→2→3→4→5→6→7→4→5→10→11 | Uma linha diferente seguida de EOF | 7→4 |
| Q4 | 1→2→3→9→10→11 | Falha na abertura | 3→9 |
| Q5 | 1→2→3→4→9→10→11 | Abertura funciona e a primeira leitura falha | 4→9 |
| S1 | 1→2→3→9→10 | Quantidade solicitada excede o saldo | Caminho inicial |
| S2 | 1→2→3→4→5→6→7→10 | Saldo suficiente, abertura e chamada de escrita completam | 5→6 |
| S3 | 1→2→3→4→8→9→10 | Saldo suficiente, abertura do arquivo de saída falha | 4→8 |
| S4 | 1→2→3→4→5→8→9→10 | Abertura funciona e `write` lança IOException | 5→8 |

## 6. Matriz e resultados

Execução realizada em Windows com Eclipse Temurin OpenJDK 21.0.13. Foram **16 casos por versão**. O original teve **6 aprovações e 10 divergências**. O revisado teve **16 aprovações**. Os resultados foram comparados com o contrato desejado após a revisão. A evidência contém uma linha por caso e por versão, com esperado, observado e situação.

Os caminhos da matriz referem-se ao legado. Nos resultados compostos aparecem, conforme o caso, retorno, existência ou conteúdo do arquivo, último código registrado e saldo final. Uma posição vazia indica texto vazio. `true/true/null` em S06 significa retorno verdadeiro, arquivo criado e valor nulo em `ultimaMovimentacao`.

| ID | Método | Caminho legado | Entrada | Pré-condição | Esperado | Original observado e situação | Revisado observado e situação |
|---|---|---|---|---|---|---|---|
| [C01](evidencias/execucao.txt#L1) | `consultarQuantidade` | Q1 | A | CSV vazio | `0` | `0` · PASSOU | `0` · PASSOU |
| [C02](evidencias/execucao.txt#L2) | `consultarQuantidade` | Q2 | A | CSV `A;5` | `5` | `5` · PASSOU | `5` · PASSOU |
| [C03](evidencias/execucao.txt#L3) | `consultarQuantidade` | Q3 (2 iterações) | A | CSV `B;5` e `C;0` | `0` | `0` · PASSOU | `0` · PASSOU |
| [C04](evidencias/execucao.txt#L4) | `consultarQuantidade` | Q4 | A | CSV ausente | `UncheckedIOException` | `0` · FALHOU | `UncheckedIOException` · PASSOU |
| [C05](evidencias/execucao.txt#L5) | `consultarQuantidade` | Q4 ou Q5¹ | A | Diretório com nome `estoque.csv` | `UncheckedIOException` | `0` · FALHOU | `UncheckedIOException` · PASSOU |
| [C06](evidencias/execucao.txt#L6) | `consultarQuantidade` | Q2 até nó 8² | A | CSV `A` sem quantidade | `UncheckedIOException` | `ArrayIndexOutOfBoundsException` · FALHOU | `UncheckedIOException` · PASSOU |
| [C07](evidencias/execucao.txt#L7) | `consultarQuantidade` | Q2 até nó 8² | A | CSV `A;abc` | `UncheckedIOException` | `NumberFormatException` · FALHOU | `UncheckedIOException` · PASSOU |
| [C08](evidencias/execucao.txt#L8) | `consultarQuantidade` | Q2 | A | CSV `A;0` | `0` | `0` · PASSOU | `0` · PASSOU |
| [S01](evidencias/execucao.txt#L9) | `registrarSaida` | S1 / Q2 | A, 6 | CSV `A;5` | `false/false/` | `false/false/` · PASSOU | `false/false/` · PASSOU |
| [S02](evidencias/execucao.txt#L10) | `registrarSaida` | S2 / Q2 | A, 5 | CSV `A;5`, sem histórico | `true/A;5/A/0` | `true//A/5` · FALHOU | `true/A;5/A/0` · PASSOU |
| [S03](evidencias/execucao.txt#L11) | `registrarSaida` | S3 / Q2 | A, 1 | CSV `A;5`; `movimentacoes.txt` é diretório | `UncheckedIOException/` | `false/` · FALHOU | `UncheckedIOException/` · PASSOU |
| [S04](evidencias/execucao.txt#L12) | `registrarSaida` | S2 / Q2 | A, 0 | CSV `A;5`, sem histórico | `IllegalArgumentException/false/` | `true/true/A` · FALHOU | `IllegalArgumentException/false/` · PASSOU |
| [S05](evidencias/execucao.txt#L13) | `registrarSaida` | S2 / Q2 | A, −1 | CSV `A;5`, sem histórico | `IllegalArgumentException/false/` | `true/true/A` · FALHOU | `IllegalArgumentException/false/` · PASSOU |
| [S06](evidencias/execucao.txt#L14) | `registrarSaida` | S2 / Q3 | null, 0 | CSV `A;5`, sem histórico | `IllegalArgumentException/false/` | `true/true/null` · FALHOU | `IllegalArgumentException/false/` · PASSOU |
| [S07](evidencias/execucao.txt#L15) | `registrarSaida` | S2 duas vezes / Q2 | duas saídas de 4 para A | CSV `A;5`, mesma instância | `true/false/1` | `true/true/5` · FALHOU | `true/false/1` · PASSOU |
| [S08](evidencias/execucao.txt#L16) | `registrarSaida` | S1 / Q3 | B, 1 | CSV `A;5`, sem histórico | `false/false/` | `false/false/` · PASSOU | `false/false/` · PASSOU |

¹ A fase da falha em C05 depende do sistema operacional: o acesso ao diretório pode falhar já na abertura. A execução comprova o tratamento público de E/S, mas não distingue a aresta 3→9 da aresta 4→9.

² C06 e C07 alcançam o nó de conversão e terminam com exceção não verificada no legado. Eles não concluem o caminho Q2.

Para exercitar **Q5** e **S4** de forma determinística, uma versão de teste instrumentada poderia receber uma fábrica de leitores e escritores. Em Q5, a fábrica abre normalmente e o leitor simulado lança `IOException` no primeiro `readLine`. Em S4, a construção do escritor funciona e o simulado lança `IOException` em `write`. A simulação não foi executada. S03 demonstra falha real de acesso ao histórico; no revisado ela já é detectada ao carregar o saldo, antes da escrita.

## 7. Comparação antes e depois

| Aspecto | Original | Revisado |
|---|---|---|
| Saldo disponível | Lê exclusivamente o CSV | Calcula saldo inicial menos saídas registradas |
| Validação | Compara apenas o limite superior | Exige código válido e retirada positiva; valida registros e duplicidades |
| Persistência e estado | Pode devolver sucesso com diário vazio e mantém recursos abertos | Fecha a leitura, grava em temporário e confirma após a substituição atômica |
| Falha de E/S | Confunde erro de arquivo com zero ou falsa recusa | Propaga `UncheckedIOException` com contexto e causa; saldo insuficiente continua retornando `false` |
| Organização | Leitura, conversão, validação e escrita embutidas nos dois métodos | Métodos com contratos explícitos e operações de leitura e validação nomeadas |

Os valores V(G) de 5 e 4 se referem somente ao original. No revisado, a validação acrescenta decisões e o cálculo de saldo passa a chamar rotinas próprias. A ramificação direta da consulta diminui, mas parte da complexidade foi deslocada para `lerSaldo` e para a validação do formato. Na saída, a verificação de quantidade, a preservação do histórico e o tratamento da limpeza acrescentam fluxos necessários.

## 8. Limitações

A solução foi projetada para execução local com um único escritor por conjunto de arquivos. A sincronização coordena chamadas na mesma instância. Escritores em instâncias ou processos independentes exigiriam um mecanismo compartilhado de exclusão. O histórico é relido a cada operação e regravado a cada saída. Volume alto favoreceria armazenamento transacional. Se o sistema de arquivos não oferecer substituição atômica, a operação falha explicitamente. Registros legados inválidos ou incompatíveis com o CSV de referência precisam ser reconciliados antes do uso.

## 9. Conclusão e referências

A análise mostrou que a aprovação de uma saída dependia de um saldo estático e podia anteceder a gravação. A versão revisada desconta movimentos persistidos, valida os dados e evidencia falhas de arquivo. Os testes C04, S02 e S07 demonstram, respectivamente, a sinalização de falha de leitura, a retirada no limite com gravação e a recusa de uma segunda saída acima do saldo restante.

- FACENS, `UIUX_FUNCIONAIS__CAIXA_BRANCA_AULA_10_update.pdf`, especialmente páginas 9 a 18, para grafos, complexidade e independência de caminhos.
- Enunciado e código-base do cenário 2 disponibilizados no Canvas da disciplina.
