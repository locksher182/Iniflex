package br.com.iniflex.dao;

import br.com.iniflex.model.Funcionario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuncionarioDAO {

    public FuncionarioDAO() {
        DatabaseConfig.inicializarBancoDeDados();
    }

    public void salvar(Funcionario f) {
        String sql = "INSERT INTO funcionarios (nome, data_nascimento, salario, funcao) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, f.getNome());
            stmt.setDate(2, Date.valueOf(f.getDataNascimento()));
            stmt.setBigDecimal(3, f.getSalario());
            stmt.setString(4, f.getFuncao());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao salvar funcionário via SQL: " + e.getMessage());
        }
    }

    public void salvarTodos(List<Funcionario> funcionarios) {
        limparTabela();
        for (Funcionario f : funcionarios) {
            salvar(f);
        }
    }

    public List<Funcionario> listarTodos() {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT nome, data_nascimento, salario, funcao FROM funcionarios ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String nome = rs.getString("nome");
                LocalDate dataNasc = rs.getDate("data_nascimento").toLocalDate();
                BigDecimal salario = rs.getBigDecimal("salario");
                String funcao = rs.getString("funcao");

                lista.add(new Funcionario(nome, dataNasc, salario, funcao));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar funcionários via SQL: " + e.getMessage());
        }
        return lista;
    }

    public boolean removerPorNome(String nome) {
        String sql = "DELETE FROM funcionarios WHERE LOWER(nome) = LOWER(?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao remover funcionário via SQL: " + e.getMessage());
            return false;
        }
    }

    public void aplicarAumentoSQL(BigDecimal fatorMultiplicador) {
        String sql = "UPDATE funcionarios SET salario = ROUND(salario * ?, 2)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, fatorMultiplicador);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao aplicar aumento via SQL: " + e.getMessage());
        }
    }

    public BigDecimal calcularTotalSalariosSQL() {
        String sql = "SELECT SUM(salario) AS total FROM funcionarios";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao calcular total via SQL: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public void limparTabela() {
        String sql = "TRUNCATE TABLE funcionarios";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            // Se truncate falhar, executa DELETE
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM funcionarios");
            } catch (SQLException ignored) {}
        }
    }
}
