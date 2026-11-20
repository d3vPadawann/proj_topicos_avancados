@echo off
setlocal
chcp 65001 > nul
title FinFamily v2.0 - UCSAL (II Unidade)
echo ============================================================================
echo   FinFamily: Sistema de Gestao Financeira e Controle de Investimentos Familiar
echo   Disciplina: Topicos Avancados de Programacao - Prof. Msc. Elton Figueiredo
echo ============================================================================
echo.

rem ---------------------------------------------------------------------------
rem 1. Localiza o Java 21+ (usa JAVA_HOME se estiver definido; senao, o java do PATH)
rem ---------------------------------------------------------------------------
set "JAVA_EXE="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    ) else (
        echo [ERRO] A variavel JAVA_HOME aponta para uma pasta sem Java:
        echo        %JAVA_HOME%
        echo        Corrija o JAVA_HOME ou remova a variavel e tente de novo.
        goto falha
    )
)
if not defined JAVA_EXE (
    for %%J in (java.exe) do set "JAVA_EXE=%%~$PATH:J"
)
if not defined JAVA_EXE (
    echo [ERRO] Java nao encontrado. Instale o JDK 21 ^(https://adoptium.net^) e tente de novo.
    goto falha
)

"%JAVA_EXE%" -version 2>&1 | findstr /r /c:"version \"2[1-9]" /c:"version \"[3-9][0-9]" > nul
if errorlevel 1 (
    echo [ERRO] O FinFamily precisa do Java 21 ou superior. Versao encontrada:
    "%JAVA_EXE%" -version
    goto falha
)
echo [OK] Java 21+ encontrado: %JAVA_EXE%

rem ---------------------------------------------------------------------------
rem 2. Verifica se a porta 8085 ja esta em uso (outro FinFamily aberto?)
rem ---------------------------------------------------------------------------
netstat -ano | findstr /r /c:":8085 .*LISTENING" > nul
if not errorlevel 1 (
    echo [AVISO] A porta 8085 ja esta em uso. Provavelmente o FinFamily ja esta aberto.
    echo         Feche a outra janela do servidor ^(Ctrl+C^) ou acesse http://localhost:8085
    goto falha
)

if exist "%~dp0backend\config\ia.properties" (
    echo [OK] Configuracao da IA encontrada em backend\config\ia.properties
) else (
    echo [INFO] Sem backend\config\ia.properties: o assistente usara a contingencia local.
    echo        Para usar o Gemini, copie ia.properties.exemplo e coloque a sua chave.
)

rem ---------------------------------------------------------------------------
rem 3. Inicia o servidor Spring Boot pelo Maven Wrapper (nao precisa ter Maven instalado)
rem    O servidor ja entrega o front-end React compilado na mesma porta.
rem ---------------------------------------------------------------------------
echo.
echo Iniciando o servidor na porta 8085 em uma nova janela...
echo (Na primeira vez o Maven e as dependencias sao baixados; pode levar alguns minutos.)
start "FinFamily - Servidor (porta 8085)" /D "%~dp0backend" cmd /k .\mvnw.cmd spring-boot:run

rem ---------------------------------------------------------------------------
rem 4. Aguarda o servidor responder e abre o navegador
rem ---------------------------------------------------------------------------
set /a TENTATIVAS=0
:aguardar
set /a TENTATIVAS+=1
curl -s -o nul http://localhost:8085/api/health
if not errorlevel 1 goto pronto
if %TENTATIVAS% GEQ 150 goto demorou
ping -n 3 127.0.0.1 > nul
goto aguardar

:pronto
echo.
echo [OK] FinFamily no ar!
start "" http://localhost:8085
echo.
echo - Aplicacao (front-end + API): http://localhost:8085
echo - Console do banco H2:         http://localhost:8085/h2-console  (JDBC URL: jdbc:h2:file:./data/finfamily_db)
echo - Usuarios de demonstracao:    senha 123456 (veja o README)
echo.
echo Para encerrar, feche a janela "FinFamily - Servidor (porta 8085)" ou use Ctrl+C nela.
pause
exit /b 0

:demorou
echo.
echo [AVISO] O servidor ainda nao respondeu depois de 5 minutos.
echo         Veja a janela "FinFamily - Servidor (porta 8085)" para conferir se houve erro.
pause
exit /b 1

:falha
echo.
pause
exit /b 1
