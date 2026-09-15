package br.com.iniflex.main;

import br.com.iniflex.model.Funcionario;
import br.com.iniflex.server.ServidorHttp;
import br.com.iniflex.service.FuncionarioService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Principal {

    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public static void main(String[] args) {
        FuncionarioService service = new FuncionarioService();

        // Modo CLI: Executa todos os passos solicitados no teste prático (3.1 a 3.12)
        executarModoCLI(service);

        // Inicia o Servidor HTTP para o Dashboard Web Frontend (Requisito Fullstack)
        try {
            int porta = 8080;
            ServidorHttp servidor = new ServidorHttp(porta, service);
            servidor.iniciar();

            abrirNavegador("http://localhost:" + porta);
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o servidor Web HTTP: " + e.getMessage());
        }
    }

    private static void executarModoCLI(FuncionarioService service) {
        // 3.1 - Inserir todos os funcionários, na mesma ordem e informações da tabela (já inseridos pelo service)

        // 3.2 - Remover o funcionário "João" da lista
        service.removerPorNome("João");

        // 3.3 - Imprimir todos os funcionários com todas suas informações (data dd/MM/yyyy e valor #,##0.00)
        System.out.println("===============================================================================");
        System.out.println("3.3 - LISTA DE TODOS OS FUNCIONÁRIOS (SEM O JOÃO)");
        System.out.println("===============================================================================");
        service.listarTodos().forEach(System.out::println);
        System.out.println();

        // 3.4 - Os funcionários receberam 10% de aumento de salário, atualizar a lista
        service.aplicarAumento(10.0);

        // 3.5 & 3.6 - Agrupar os funcionários por função em um MAP e imprimir
        System.out.println("===============================================================================");
        System.out.println("3.6 - FUNCIONÁRIOS AGRUPADOS POR FUNÇÃO (COM 10% DE AUMENTO)");
        System.out.println("===============================================================================");
        Map<String, List<Funcionario>> funcionariosPorFuncao = service.agruparPorFuncao();
        funcionariosPorFuncao.forEach((funcao, lista) -> {
            System.out.println("Função: " + funcao);
            lista.forEach(f -> System.out.println("   -> " + f));
        });
        System.out.println();

        // 3.8 - Imprimir os funcionários que fazem aniversário no mês 10 e 12
        System.out.println("===============================================================================");
        System.out.println("3.8 - FUNCIONÁRIOS QUE FAZEM ANIVERSÁRIO NO MÊS 10 E 12");
        System.out.println("===============================================================================");
        service.filtrarAniversariantes(10, 12).forEach(System.out::println);
        System.out.println();

        // 3.9 - Imprimir o funcionário com a maior idade, exibir os atributos: nome e idade
        System.out.println("===============================================================================");
        System.out.println("3.9 - FUNCIONÁRIO COM A MAIOR IDADE");
        System.out.println("===============================================================================");
        Funcionario maisVelho = service.obterMaisVelho();
        if (maisVelho != null) {
            int idade = service.calcularIdade(maisVelho);
            System.out.println("Nome: " + maisVelho.getNome() + " | Idade: " + idade + " anos");
        }
        System.out.println();

        // 3.10 - Imprimir a lista de funcionários por ordem alfabética
        System.out.println("===============================================================================");
        System.out.println("3.10 - FUNCIONÁRIOS POR ORDEM ALFABÉTICA");
        System.out.println("===============================================================================");
        service.listarPorOrdemAlfabetica().forEach(System.out::println);
        System.out.println();

        // 3.11 - Imprimir o total dos salários dos funcionários
        System.out.println("===============================================================================");
        System.out.println("3.11 - TOTAL DOS SALÁRIOS DOS FUNCIONÁRIOS");
        System.out.println("===============================================================================");
        BigDecimal totalSalarios = service.calcularTotalSalarios();
        System.out.println("Total: R$ " + CURRENCY_FORMAT.format(totalSalarios));
        System.out.println();

        // 3.12 - Imprimir quantos salários mínimos ganha cada funcionário (salário mínimo: R$ 1.212,00)
        System.out.println("===============================================================================");
        System.out.println("3.12 - QUANTIDADE DE SALÁRIOS MÍNIMOS POR FUNCIONÁRIO (SALÁRIO MÍNIMO: R$ 1.212,00)");
        System.out.println("===============================================================================");
        service.listarTodos().forEach(f -> {
            BigDecimal salariosMinimos = service.calcularSalariosMinimos(f);
            System.out.printf("Nome: %-10s | Salário: R$ %10s | Ganha: %s salários mínimos%n",
                    f.getNome(),
                    CURRENCY_FORMAT.format(f.getSalario()),
                    CURRENCY_FORMAT.format(salariosMinimos));
        });
        System.out.println("===============================================================================");
    }

    private static void abrirNavegador(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception ignored) {
            // Se falhar ao abrir automaticamente, o usuário tem a URL impressa no terminal
        }
    }
}
