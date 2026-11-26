import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** Código-base intencionalmente problemático para revisão estática e estrutural. */
public class EstoqueArquivo {
    private String ultimaMovimentacao = "";

    public int consultarQuantidade(String codigo) {
        int quantidade = 0;
        try {
            BufferedReader leitor = new BufferedReader(new FileReader("estoque.csv"));
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String[] colunas = linha.split(";");
                if (colunas[0].equals(codigo)) {
                    quantidade = Integer.parseInt(colunas[1]);
                    break;
                }
            }
        } catch (IOException e) {
            // Erro de leitura ignorado.
        }
        return quantidade;
    }

    public boolean registrarSaida(String codigo, int quantidade) {
        int disponivel = consultarQuantidade(codigo);
        if (quantidade <= disponivel) {
            try {
                FileWriter escrita = new FileWriter("movimentacoes.txt", true);
                escrita.write(codigo + ";" + quantidade + "\n");
                ultimaMovimentacao = codigo;
                return true;
            } catch (IOException e) {
                // Falha ignorada.
            }
        }
        return false;
    }

    public String getUltimaMovimentacao() {
        return ultimaMovimentacao;
    }
}
