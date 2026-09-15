package br.com.iniflex.test;

import br.com.iniflex.model.Funcionario;
import br.com.iniflex.service.FuncionarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class RunTests {

    private static int totalTestes = 0;
    private static int testesAprovados = 0;
    private static int testesFalhos = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("🧪 EXECUÇÃO DA SUÍTE DE TESTES AUTOMATIZADOS (PROTHERA QA & SECURITY)");
        System.out.println("===============================================================================");

        FuncionarioService service = new FuncionarioService();

        // Teste 1: Carga Inicial (3.1)
        testar("3.1 - Carga inicial de 10 funcionários no banco SQL", () -> {
            service.carregarDadosIniciais();
            assertEquals(10, service.listarTodos().size(), "Quantidade inicial de funcionários deve ser 10");
        });

        // Teste 2: Remoção do João (3.2)
        testar("3.2 - Remoção do funcionário João", () -> {
            boolean removido = service.removerPorNome("João");
            assertTrue(removido, "Remoção de João deve retornar true");
            assertEquals(9, service.listarTodos().size(), "Quantidade após remoção deve ser 9");
            assertNull(service.listarTodos().stream().filter(f -> f.getNome().equalsIgnoreCase("João")).findFirst().orElse(null), "João não deve mais existir na lista");
        });

        // Teste 3: Aumento de 10% nos salários (3.4)
        testar("3.4 - Cálculo de precisão no aumento de 10% dos salários", () -> {
            Funcionario mariaAntes = service.listarTodos().stream().filter(f -> f.getNome().equals("Maria")).findFirst().orElse(null);
            assertNotNull(mariaAntes, "Maria deve existir");
            BigDecimal salarioAntigo = mariaAntes.getSalario();

            service.aplicarAumento(10.0);

            Funcionario mariaDepois = service.listarTodos().stream().filter(f -> f.getNome().equals("Maria")).findFirst().orElse(null);
            assertNotNull(mariaDepois, "Maria deve existir após aumento");
            BigDecimal esperado = salarioAntigo.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
            assertEquals(esperado, mariaDepois.getSalario(), "Salário da Maria com 10% deve bater exatamente");
        });

        // Teste 4: Agrupamento por Função (3.5)
        testar("3.5 - Agrupamento em Map por função", () -> {
            Map<String, List<Funcionario>> agrupados = service.agruparPorFuncao();
            assertTrue(agrupados.containsKey("Operador"), "Map deve conter a função Operador");
            assertTrue(agrupados.containsKey("Gerente"), "Map deve conter a função Gerente");
            assertEquals(2, agrupados.get("Gerente").size(), "Devem existir 2 Gerentes (Laura e Helena)");
        });

        // Teste 5: Filtro de Aniversariantes meses 10 e 12 (3.8)
        testar("3.8 - Filtro de aniversariantes dos meses 10 e 12", () -> {
            List<Funcionario> aniv = service.filtrarAniversariantes(10, 12);
            assertEquals(2, aniv.size(), "Devem ser 2 aniversariantes nos meses 10 e 12 (Maria e Miguel)");
        });

        // Teste 6: Determinação da Maior Idade (3.9)
        testar("3.9 - Cálculo do funcionário de maior idade", () -> {
            Funcionario maisVelho = service.obterMaisVelho();
            assertNotNull(maisVelho, "Mais velho não deve ser nulo");
            assertEquals("Caio", maisVelho.getNome(), "O funcionário mais velho deve ser o Caio");
        });

        // Teste 7: Ordenação Alfabética (3.10)
        testar("3.10 - Ordenação alfabética da lista", () -> {
            List<Funcionario> ordem = service.listarPorOrdemAlfabetica();
            assertEquals("Alice", ordem.get(0).getNome(), "Primeiro nome alfabético deve ser Alice");
            assertEquals("Miguel", ordem.get(ordem.size() - 1).getNome(), "Último nome alfabético deve ser Miguel");
        });

        // Teste 8: Módulo de Riscos & Compliance Prothera
        testar("PROTHERA - Módulo de Gerenciamento de Riscos & Compliance", () -> {
            Map<String, Object> risco = service.calcularAnaliseRiscoCompliance();
            assertNotNull(risco, "Relatório de risco não deve ser nulo");
            assertTrue(risco.containsKey("nivelRiscoGlobal"), "Deve conter o nível de risco global");
            Number percentual = (Number) risco.get("conformidadePisoPercentual");
            assertEquals(100L, percentual.longValue(), "100% de conformidade com o piso salarial");
        });

        // Teste 9: Validação de Segurança - Proteção contra SQL Injection em busca/remoção
        testar("SEGURANÇA - Proteção contra SQL Injection em PreparedStatement", () -> {
            String sqlInjectionPayload = "João' OR '1'='1";
            boolean removido = service.removerPorNome(sqlInjectionPayload);
            assertTrue(!removido, "Payload de SQL Injection não deve afetar a base de dados");
        });

        System.out.println("===============================================================================");
        System.out.printf("RESULTS: %d executados | %d APROVADOS | %d FALHAS%n", totalTestes, testesAprovados, testesFalhos);
        System.out.println("===============================================================================");

        if (testesFalhos > 0) {
            System.exit(1);
        }
    }

    private static void testar(String nomeTeste, Runnable teste) {
        totalTestes++;
        try {
            teste.run();
            testesAprovados++;
            System.out.println("✅ [PASSED] " + nomeTeste);
        } catch (AssertionError | Exception e) {
            testesFalhos++;
            System.err.println("❌ [FAILED] " + nomeTeste + " -> " + e.getMessage());
        }
    }

    private static void assertEquals(Object esperado, Object atual, String msg) {
        if (esperado == null && atual == null) return;
        if (esperado != null && esperado.equals(atual)) return;
        throw new AssertionError(msg + " | Esperado: " + esperado + ", Mas foi: " + atual);
    }

    private static void assertTrue(boolean condicao, String msg) {
        if (!condicao) throw new AssertionError(msg);
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError(msg);
    }

    private static void assertNull(Object obj, String msg) {
        if (obj != null) throw new AssertionError(msg);
    }
}
