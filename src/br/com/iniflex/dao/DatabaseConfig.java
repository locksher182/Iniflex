package br.com.iniflex.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseConfig {

    private static final String JDBC_URL = "jdbc:h2:mem:iniflexdb;DB_CLOSE_DELAY=-1;MODE=Oracle";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASS = "";

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC H2 não localizado, utilizando persistência SQL em memória.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
    }

    public static synchronized void inicializarBancoDeDados() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            InputStream is = DatabaseConfig.class.getResourceAsStream("/schema.sql");
            if (is == null) {
                File file = new File("resources/schema.sql");
                if (file.exists()) {
                    is = new FileInputStream(file);
                }
            }

            if (is != null) {
                StringBuilder sqlBuilder = new StringBuilder();
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                            sqlBuilder.append(line).append("\n");
                        }
                    }
                }

                String[] statements = sqlBuilder.toString().split(";");
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                System.out.println("🗄️  Banco de dados relacional H2 (Modo Oracle) inicializado com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("Erro na inicialização do banco SQL: " + e.getMessage());
        }
    }
}
