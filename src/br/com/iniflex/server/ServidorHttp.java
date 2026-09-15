package br.com.iniflex.server;

import br.com.iniflex.model.Funcionario;
import br.com.iniflex.service.FuncionarioService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ServidorHttp {

    private final int porta;
    private final FuncionarioService service;
    private HttpServer server;

    public ServidorHttp(int porta, FuncionarioService service) {
        this.porta = porta;
        this.service = service;
    }

    public void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress(porta), 0);

        // Endpoints API REST
        server.createContext("/api/funcionarios", new FuncionariosHandler());
        server.createContext("/api/funcionarios/aumento", new AumentoHandler());
        server.createContext("/api/funcionarios/remover-joao", new RemoverJoaoHandler());
        server.createContext("/api/funcionarios/reset", new ResetHandler());
        server.createContext("/api/estatisticas", new EstatisticasHandler());

        // Arquivos estáticos (Web Frontend)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("===============================================================================");
        System.out.println("🌐 SERVIDOR WEB INIFLEX INICIADO COM SUCESSO!");
        System.out.println("🔒 Headers de Segurança HTTP & Proteção contra Path Traversal Ativos");
        System.out.println("👉 Acesse no navegador: http://localhost:" + porta);
        System.out.println("===============================================================================");
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
        }
    }

    // Handler para /api/funcionarios (GET, POST, DELETE)
    private class FuncionariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                responder(exchange, 204, "");
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                List<Funcionario> lista = service.listarTodos();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    json.append(funcionarioToJson(lista.get(i)));
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                responderJson(exchange, 200, json.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = lerCorpoRequisicao(exchange);
                Map<String, String> params = parseJsonSimple(body);

                try {
                    String nome = params.get("nome");
                    LocalDate dataNasc = LocalDate.parse(params.get("dataNascimento"));
                    BigDecimal salario = new BigDecimal(params.get("salario"));
                    String funcao = params.get("funcao");

                    Funcionario f = new Funcionario(nome, dataNasc, salario, funcao);
                    service.adicionar(f);

                    responderJson(exchange, 201, "{\"sucesso\":true,\"mensagem\":\"Funcionário adicionado com sucesso!\"}");
                } catch (Exception e) {
                    responderJson(exchange, 400, "{\"sucesso\":false,\"mensagem\":\"Dados inválidos: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                String path = exchange.getRequestURI().getPath();
                String[] partes = path.split("/");
                if (partes.length >= 4) {
                    String nome = URLDecoder.decode(partes[3], "UTF-8");
                    boolean removido = service.removerPorNome(nome);
                    if (removido) {
                        responderJson(exchange, 200, "{\"sucesso\":true,\"mensagem\":\"Funcionário removido com sucesso!\"}");
                    } else {
                        responderJson(exchange, 404, "{\"sucesso\":false,\"mensagem\":\"Funcionário não encontrado.\"}");
                    }
                } else {
                    responderJson(exchange, 400, "{\"sucesso\":false,\"mensagem\":\"Nome não fornecido.\"}");
                }
            } else {
                responder(exchange, 405, "Método não permitido");
            }
        }
    }

    // Handler para /api/funcionarios/aumento (POST)
    private class AumentoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 204, "");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                service.aplicarAumento(10.0);
                responderJson(exchange, 200, "{\"sucesso\":true,\"mensagem\":\"Aumento de 10% aplicado a todos os funcionários!\"}");
            } else {
                responder(exchange, 405, "Método não permitido");
            }
        }
    }

    // Handler para /api/funcionarios/remover-joao (POST/DELETE)
    private class RemoverJoaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 204, "");
                return;
            }
            boolean removido = service.removerPorNome("João");
            if (removido) {
                responderJson(exchange, 200, "{\"sucesso\":true,\"mensagem\":\"Funcionário João removido da lista!\"}");
            } else {
                responderJson(exchange, 200, "{\"sucesso\":false,\"mensagem\":\"João não estava mais na lista.\"}");
            }
        }
    }

    // Handler para /api/funcionarios/reset (POST)
    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 204, "");
                return;
            }
            service.carregarDadosIniciais();
            responderJson(exchange, 200, "{\"sucesso\":true,\"mensagem\":\"Dados dos funcionários restaurados ao estado inicial!\"}");
        }
    }

    // Handler para /api/estatisticas (GET)
    private class EstatisticasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 204, "");
                return;
            }

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            DecimalFormat curFmt = new DecimalFormat("#,##0.00", symbols);

            BigDecimal totalSalarios = service.calcularTotalSalarios();
            Funcionario maisVelho = service.obterMaisVelho();
            int idadeMaisVelho = service.calcularIdade(maisVelho);

            Map<String, List<Funcionario>> porFuncao = service.agruparPorFuncao();
            List<Funcionario> aniversariantes = service.filtrarAniversariantes(10, 12);
            Map<String, Object> risco = service.calcularAnaliseRiscoCompliance();

            StringBuilder json = new StringBuilder("{");
            json.append("\"totalSalarios\":").append(totalSalarios).append(",");
            json.append("\"totalSalariosFormatado\":\"R$ ").append(curFmt.format(totalSalarios)).append("\",");
            json.append("\"quantidadeFuncionarios\":").append(service.listarTodos().size()).append(",");

            if (maisVelho != null) {
                json.append("\"funcionarioMaisVelho\":{");
                json.append("\"nome\":\"").append(escapeJson(maisVelho.getNome())).append("\",");
                json.append("\"idade\":").append(idadeMaisVelho);
                json.append("},");
            } else {
                json.append("\"funcionarioMaisVelho\":null,");
            }

            // Módulo Prothera Risco & Compliance
            json.append("\"protheraRisco\":{");
            json.append("\"nivelRiscoGlobal\":\"").append(escapeJson(String.valueOf(risco.get("nivelRiscoGlobal")))).append("\",");
            json.append("\"disparidadeSalarialRazao\":\"").append(escapeJson(String.valueOf(risco.get("disparidadeSalarialRazao")))).append("\",");
            json.append("\"conformidadePisoPercentual\":").append(risco.get("conformidadePisoPercentual")).append(",");
            json.append("\"concentracaoPorFuncao\":{");
            
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> conc = (Map<String, BigDecimal>) risco.get("concentracaoPorFuncao");
            if (conc != null) {
                int cCount = 0;
                for (Map.Entry<String, BigDecimal> entry : conc.entrySet()) {
                    json.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
                    if (++cCount < conc.size()) json.append(",");
                }
            }
            json.append("}},");

            // Agrupamento por Função
            json.append("\"porFuncao\":{");
            int fCount = 0;
            for (Map.Entry<String, List<Funcionario>> entry : porFuncao.entrySet()) {
                json.append("\"").append(escapeJson(entry.getKey())).append("\":[");
                List<Funcionario> subList = entry.getValue();
                for (int i = 0; i < subList.size(); i++) {
                    json.append(funcionarioToJson(subList.get(i)));
                    if (i < subList.size() - 1) json.append(",");
                }
                json.append("]");
                if (++fCount < porFuncao.size()) json.append(",");
            }
            json.append("},");

            // Aniversariantes
            json.append("\"aniversariantesMeses10e12\":[");
            for (int i = 0; i < aniversariantes.size(); i++) {
                json.append(funcionarioToJson(aniversariantes.get(i)));
                if (i < aniversariantes.size() - 1) json.append(",");
            }
            json.append("]");

            json.append("}");

            responderJson(exchange, 200, json.toString());
        }
    }

    // Handler de Arquivos Estáticos com Proteção contra Path Traversal (Directory Traversal Vulnerability)
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            adicionarSecurityHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File webDir = new File("web").getCanonicalFile();
            File file = new File(webDir, path.startsWith("/") ? path.substring(1) : path).getCanonicalFile();

            // Proteção contra Path Traversal: impede o acesso a pastas fora de 'web/'
            if (!file.getPath().startsWith(webDir.getPath())) {
                responder(exchange, 403, "403 - Acesso Negado (Proteção Path Traversal)");
                return;
            }

            if (!file.exists() || file.isDirectory()) {
                responder(exchange, 404, "404 - Arquivo não encontrado");
                return;
            }

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file); OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        private String getContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html";
            if (filename.endsWith(".css")) return "text/css";
            if (filename.endsWith(".js")) return "text/javascript";
            if (filename.endsWith(".json")) return "application/json";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "text/plain";
        }
    }

    // Utilitários auxiliares
    private String funcionarioToJson(Funcionario f) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat curFmt = new DecimalFormat("#,##0.00", symbols);

        BigDecimal salMin = service.calcularSalariosMinimos(f);
        int idade = service.calcularIdade(f);

        return String.format(Locale.US,
                "{\"nome\":\"%s\",\"dataNascimento\":\"%s\",\"dataNascimentoFormatada\":\"%s\",\"salario\":%.2f,\"salarioFormatado\":\"R$ %s\",\"funcao\":\"%s\",\"idade\":%d,\"salariosMinimos\":%.2f}",
                escapeJson(f.getNome()),
                f.getDataNascimento(),
                f.getDataNascimento().format(fmt),
                f.getSalario(),
                curFmt.format(f.getSalario()),
                escapeJson(f.getFuncao()),
                idade,
                salMin
        );
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private Map<String, String> parseJsonSimple(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);

        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim().replace("\"", "");
                map.put(k, v);
            }
        }
        return map;
    }

    private String lerCorpoRequisicao(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody(); Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    // Injeção de HTTP Security Headers (Segurança da Informação)
    private void adicionarSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        // Security Headers OWASP Best Practices
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    private void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void responder(HttpExchange exchange, int status, String resposta) throws IOException {
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
