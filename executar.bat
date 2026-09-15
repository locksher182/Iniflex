@echo off
chcp 65001 > nul
echo ===============================================================================
echo PROTHERA FULLSTACK SOLUTION - INIFLEX (JAVA + SQL H2 + JUNIT + WEB DASHBOARD)
echo ===============================================================================
echo.
echo [1/3] Compilando arquivos Java e Suite de Testes...
if not exist bin mkdir bin
"C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe" -encoding UTF-8 -d bin -cp "lib/h2.jar" -sourcepath "src;test" test/br/com/iniflex/test/RunTests.java src/br/com/iniflex/main/Principal.java
if %errorlevel% neq 0 (
    echo Erro ao compilar os arquivos Java.
    pause
    exit /b %errorlevel%
)

echo [2/3] Executando Suite de Testes Automatizados (Prothera QA)...
echo.
"C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe" "-Dfile.encoding=UTF-8" -cp "bin;lib/h2.jar" br.com.iniflex.test.RunTests
if %errorlevel% neq 0 (
    echo Testes falharam. Interrompendo execucao.
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] Iniciando relatorio CLI e Servidor Web Fullstack HTTP (Porta 8080)...
echo.
"C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\java.exe" "-Dfile.encoding=UTF-8" -cp "bin;lib/h2.jar" br.com.iniflex.main.Principal
echo.
pause
