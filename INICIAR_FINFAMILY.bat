@echo off
chcp 65001 > nul
title FinFamily v2.0 - UCSAL (II Unidade)
echo ============================================================================
echo   FinFamily: Sistema de Gestao Financeira e Controle de Investimentos Familiar
echo   Disciplina: Topicos Avancados de Programacao - Prof. Msc. Elton Figueiredo
echo ============================================================================
echo.

set "JAVA_HOME=G:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.4.1\plugins\maven\lib\maven3\bin\mvn.cmd"

echo [1/2] Iniciando Servidor Back-End (Java 21 + Spring Boot 3) na porta 8085...
start "FinFamily Back-End (Spring Boot - Porta 8085)" cmd /k "cd /d "%~dp0backend" && set "JAVA_HOME=G:\Program Files\Java\jdk-21" && set "PATH=G:\Program Files\Java\jdk-21\bin;%PATH%" && "%MVN_CMD%" spring-boot:run"

echo [2/2] Iniciando Servidor Front-End (React + Vite) na porta 5173...
start "FinFamily Front-End (React Vite - Porta 5173)" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo Aguardando inicializacao dos servidores (8 segundos)...
timeout /t 8 /nobreak > nul

echo Abrindo o FinFamily no navegador padrao...
start http://localhost:5173
start http://localhost:8085

echo.
echo Sistema iniciado com sucesso!
echo - Front-End Dev (Vite): http://localhost:5173
echo - Aplicacao Integrada + API REST (Spring Boot): http://localhost:8085
echo - Console do Banco H2: http://localhost:8085/h2-console (JDBC URL: jdbc:h2:file:./data/finfamily_db)
pause
