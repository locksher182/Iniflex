package br.com.iniflex.service;

import br.com.iniflex.dao.FuncionarioDAO;
import br.com.iniflex.model.Funcionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FuncionarioService {

    private final FuncionarioDAO dao;
    private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1212.00");

    public FuncionarioService() {
        this.dao = new FuncionarioDAO();
        carregarDadosIniciais();
    }

    public synchronized void carregarDadosIniciais() {
        List<Funcionario> iniciais = new ArrayList<>();
        // 3.1 - Inserir todos os funcionários
        iniciais.add(new Funcionario("Maria", LocalDate.of(2000, 10, 18), new BigDecimal("2009.44"), "Operador"));
        iniciais.add(new Funcionario("João", LocalDate.of(1990, 5, 12), new BigDecimal("2284.38"), "Operador"));
        iniciais.add(new Funcionario("Caio", LocalDate.of(1961, 5, 2), new BigDecimal("9836.14"), "Coordenador"));
        iniciais.add(new Funcionario("Miguel", LocalDate.of(1988, 10, 14), new BigDecimal("19119.88"), "Diretor"));
        iniciais.add(new Funcionario("Alice", LocalDate.of(1995, 1, 5), new BigDecimal("2234.68"), "Recepcionista"));
        iniciais.add(new Funcionario("Heitor", LocalDate.of(1999, 11, 19), new BigDecimal("1582.72"), "Operador"));
        iniciais.add(new Funcionario("Arthur", LocalDate.of(1993, 3, 31), new BigDecimal("4071.84"), "Contador"));
        iniciais.add(new Funcionario("Laura", LocalDate.of(1994, 7, 8), new BigDecimal("3017.45"), "Gerente"));
        iniciais.add(new Funcionario("Heloísa", LocalDate.of(2003, 5, 24), new BigDecimal("1606.85"), "Eletricista"));
        iniciais.add(new Funcionario("Helena", LocalDate.of(1996, 9, 2), new BigDecimal("2799.93"), "Gerente"));

        dao.salvarTodos(iniciais);
    }

    public synchronized List<Funcionario> listarTodos() {
        return dao.listarTodos();
    }

    public synchronized boolean removerPorNome(String nome) {
        return dao.removerPorNome(nome);
    }

    public synchronized void aplicarAumento(double percentual) {
        BigDecimal fator = BigDecimal.valueOf(1 + (percentual / 100.0));
        dao.aplicarAumentoSQL(fator);
    }

    public synchronized Map<String, List<Funcionario>> agruparPorFuncao() {
        return listarTodos().stream().collect(Collectors.groupingBy(Funcionario::getFuncao));
    }

    public synchronized List<Funcionario> filtrarAniversariantes(int... meses) {
        List<Integer> mesesList = new ArrayList<>();
        for (int m : meses) {
            mesesList.add(m);
        }
        return listarTodos().stream()
                .filter(f -> mesesList.contains(f.getDataNascimento().getMonthValue()))
                .collect(Collectors.toList());
    }

    public synchronized Funcionario obterMaisVelho() {
        return listarTodos().stream()
                .min(Comparator.comparing(Funcionario::getDataNascimento))
                .orElse(null);
    }

    public synchronized int calcularIdade(Funcionario f) {
        if (f == null) return 0;
        return Period.between(f.getDataNascimento(), LocalDate.now()).getYears();
    }

    public synchronized List<Funcionario> listarPorOrdemAlfabetica() {
        return listarTodos().stream()
                .sorted(Comparator.comparing(Funcionario::getNome))
                .collect(Collectors.toList());
    }

    public synchronized BigDecimal calcularTotalSalarios() {
        return dao.calcularTotalSalariosSQL();
    }

    public synchronized BigDecimal calcularSalariosMinimos(Funcionario f) {
        if (f == null || f.getSalario() == null) return BigDecimal.ZERO;
        return f.getSalario().divide(SALARIO_MINIMO, 2, RoundingMode.HALF_UP);
    }

    public synchronized boolean adicionar(Funcionario f) {
        if (f != null && f.getNome() != null && !f.getNome().trim().isEmpty()) {
            dao.salvar(f);
            return true;
        }
        return false;
    }

    // =========================================================================
    // MÓDULO PROTHERA: GERENCIAMENTO DE RISCOS & COMPLIANCE SALARIAL
    // =========================================================================
    public synchronized Map<String, Object> calcularAnaliseRiscoCompliance() {
        List<Funcionario> lista = listarTodos();
        Map<String, Object> relatorio = new HashMap<>();

        if (lista.isEmpty()) {
            relatorio.put("nivelRiscoGlobal", "BAIXO");
            relatorio.put("disparidadeSalarialRazao", "1.00");
            relatorio.put("concentracaoRisco", Collections.emptyMap());
            relatorio.put("conformidadePisoPercentual", 100);
            return relatorio;
        }

        BigDecimal minSalario = lista.stream().map(Funcionario::getSalario).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxSalario = lista.stream().map(Funcionario::getSalario).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal totalFolha = calcularTotalSalarios();

        // 1. Razão de Disparidade (Maior Salário / Menor Salário)
        BigDecimal razaoDisparidade = minSalario.compareTo(BigDecimal.ZERO) > 0
                ? maxSalario.divide(minSalario, 2, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        // 2. Nível de Risco Financeiro de Folha
        String nivelRisco;
        if (totalFolha.compareTo(new BigDecimal("60000.00")) > 0) {
            nivelRisco = "ALTO - Requer Auditoria Financeira";
        } else if (totalFolha.compareTo(new BigDecimal("30000.00")) > 0) {
            nivelRisco = "MÉDIO - Folha Controlada";
        } else {
            nivelRisco = "BAIXO - Dentro do Orçamento";
        }

        // 3. Percentual de Concentração de Custo por Função (Compliance)
        Map<String, BigDecimal> concentracaoPorFuncao = new HashMap<>();
        Map<String, List<Funcionario>> porFuncao = agruparPorFuncao();
        porFuncao.forEach((funcao, membros) -> {
            BigDecimal custoFuncao = membros.stream().map(Funcionario::getSalario).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percentual = totalFolha.compareTo(BigDecimal.ZERO) > 0
                    ? custoFuncao.multiply(new BigDecimal("100")).divide(totalFolha, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            concentracaoPorFuncao.put(funcao, percentual);
        });

        // 4. Conformidade Piso Salarial (Verifica se todos ganham >= R$ 1.212,00)
        long conformes = lista.stream().filter(f -> f.getSalario().compareTo(SALARIO_MINIMO) >= 0).count();
        double percentualConformidade = (double) conformes / lista.size() * 100.0;

        relatorio.put("nivelRiscoGlobal", nivelRisco);
        relatorio.put("disparidadeSalarialRazao", razaoDisparidade.toString());
        relatorio.put("maiorSalario", maxSalario);
        relatorio.put("menorSalario", minSalario);
        relatorio.put("concentracaoPorFuncao", concentracaoPorFuncao);
        relatorio.put("conformidadePisoPercentual", Math.round(percentualConformidade));

        return relatorio;
    }
}
