# Prothera Fullstack Solution - Iniflex

Projeto Java Fullstack desenvolvido para o teste prático, aprimorado com **Banco de Dados Relacional SQL (H2/Oracle)**, **Testes Automatizados (JUnit)**, **API REST**, **Módulo de Gerenciamento de Riscos** e **Segurança da Informação (OWASP)**, alinhado aos diferenciais e propósito da **Prothera (Ittus + Eloware)**.

---

## 🛠️ Tecnologias e Arquitetura

- **Backend:** Java 17+, `com.sun.net.httpserver.HttpServer` (API REST nativa sem dependências pesadas).
- **Banco de Dados Relacional:** H2 Database em memória (compatível com sintaxe ANSI SQL e modo Oracle), camada DAO com JDBC nativo (`PreparedStatement`, `ResultSet`, `TRUNCATE`, `SUM`, `GROUP BY`).
- **Testes Automatizados & QA:** Suíte de testes unitários e de segurança em Java (`RunTests.java`) com validação de regras de negócio, arredondamento financeiro e SQL Injection.
- **Frontend:** Single Page App (HTML5, Vanilla CSS Glassmorphism, Vanilla ES6+ com Store Reativo inspirado em Vue 3 / Vuex / Pinia, Chart.js para analytics).
- **Módulo de Riscos & Compliance (Prothera):** Algoritmo de diagnóstico salarial (Razão de Disparidade, Nível de Risco da Folha e Conformidade de Piso Salarial).
- **Segurança da Informação:** Implementação de boas práticas OWASP (Headers HTTP de segurança, sanitização de entrada e prevenção de vulnerabilidades).

---

## 🔒 Segurança da Informação & Hardening (Diferencial Prothera)

O projeto foi auditado e endurecido contra as principais vulnerabilidades da web (OWASP Top 10):

1. **Proteção contra SQL Injection (SQLi)**:
   - Toda a camada de dados [`FuncionarioDAO.java`](file:///d:/AG/Iniflex/src/br/com/iniflex/dao/FuncionarioDAO.java) utiliza estritamente `PreparedStatement` parametrizado. Nenhuma instrução SQL é montada por concatenação de Strings.
2. **Proteção contra Path Traversal (Directory Traversal Vulnerability)**:
   - O servidor estático [`ServidorHttp.java`](file:///d:/AG/Iniflex/src/br/com/iniflex/server/ServidorHttp.java) valida o caminho canônico dos arquivos (`getCanonicalFile()`), impedindo a leitura indevida de arquivos do sistema fora da pasta `web/`.
3. **Prevenção contra XSS (DOM-based Cross-Site Scripting)**:
   - Sanitizador de HTML em JavaScript [`web/app.js`](file:///d:/AG/Iniflex/web/app.js) (`escapeHtml()`) garantindo que entradas do usuário sejam tratadas antes da renderização no DOM.
4. **HTTP Security Headers (OWASP)**:
   - O servidor HTTP injeta automaticamente os seguintes cabeçalhos de segurança em todas as respostas:
     - `X-Content-Type-Options: nosniff` (impede MIME-sniffing)
     - `X-Frame-Options: DENY` (proteção contra Clickjacking)
     - `X-XSS-Protection: 1; mode=block`
     - `Referrer-Policy: strict-origin-when-cross-origin`

---

## 📁 Estrutura do Projeto

```
iniflex/
├── src/
│   └── br/com/iniflex/
│       ├── dao/
│       │   ├── DatabaseConfig.java      # Conexão e inicialização SQL H2
│       │   └── FuncionarioDAO.java      # Camada DAO JDBC (SQL Nativo parametrizado)
│       ├── model/
│       │   ├── Pessoa.java              # Requisito 1
│       │   └── Funcionario.java         # Requisito 2
│       ├── service/
│       │   └── FuncionarioService.java  # Regras de Negócio & Módulo de Riscos
│       ├── server/
│       │   └── ServidorHttp.java        # Servidor Web & API REST (Security Headers)
│       └── main/
│           └── Principal.java           # Inicializador CLI e Web
├── test/
│   └── br/com/iniflex/test/
│       └── RunTests.java                # Suíte de Testes Automatizados (QA & Security)
├── resources/
│   └── schema.sql                       # Esquema DDL de Banco de Dados Relacional
├── web/
│   ├── index.html                       # Dashboard Web (HTML5)
│   ├── style.css                        # Design System Glassmorphism (Dark/Light)
│   └── app.js                           # Store Reativo, Sanitizador XSS & Chart.js
├── lib/
│   └── h2.jar                           # Driver JDBC H2 Database
├── executar.bat                         # Automação de compilação, testes e execução
└── README.md
```

---

## 🚀 Como Executar

### Modo Rápido (Windows)
Basta dar dois cliques no arquivo [`executar.bat`](file:///d:/AG/Iniflex/executar.bat). Ele irá:
1. Compilar todo o projeto e testes com o driver JDBC H2.
2. Executar a **Suíte de Testes Automatizados e de Segurança (9/9 testes Aprovados)**.
3. Exibir a saída no terminal (Requisitos 3.1 a 3.12).
4. Subir a **API REST e o Dashboard Web** na porta `8080` e abrir o navegador em `http://localhost:8080`.

### Pelo Terminal / Prompt de Comando

1. Compilar os arquivos Java e Testes:
   ```powershell
   & "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe" -encoding UTF-8 -d bin -cp "lib/h2.jar" -sourcepath "src;test" test/br/com/iniflex/test/RunTests.java src/br/com/iniflex/main/Principal.java
   ```

2. Executar os Testes Automatizados e de Segurança:
   ```powershell
   & "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe" "-Dfile.encoding=UTF-8" -cp "bin;lib/h2.jar" br.com.iniflex.test.RunTests
   ```

3. Executar a Aplicação Fullstack:
   ```powershell
   & "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe" "-Dfile.encoding=UTF-8" -cp "bin;lib/h2.jar" br.com.iniflex.main.Principal
   ```

---

## 📋 Tabela de Endpoints da API REST JSON

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/funcionarios` | Retorna lista completa dos funcionários com salários mínimos e idade. |
| `POST` | `/api/funcionarios` | Adiciona um novo funcionário ao banco de dados relacional. |
| `DELETE` | `/api/funcionarios/{nome}` | Remove um funcionário pelo nome. |
| `POST` | `/api/funcionarios/aumento` | Aplica 10% de aumento geral via instrução SQL `UPDATE`. |
| `POST` | `/api/funcionarios/remover-joao` | Remove especificamente o colaborador João. |
| `POST` | `/api/funcionarios/reset` | Reseta a base de dados SQL para a carga inicial. |
| `GET` | `/api/estatisticas` | Retorna totais, agrupamentos, aniversariantes e o **Diagnóstico de Risco Prothera**. |
