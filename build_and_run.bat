@echo off
echo ========================================
echo   BIEN DICH VA CHAY CHUONG TRINH
echo ========================================
echo.

echo [1/2] Bien dich...
javac -encoding UTF-8 src\SecureServer.java src\SecureClient.java -d out
if %errorlevel% neq 0 (
    echo LOI: Bien dich that bai!
    pause
    exit /b 1
)

echo Bien dich thanh cong!
echo.
echo [2/2] Huong dan chay:
echo.
echo   Mo 2 cua so CMD rieng biet:
echo.
echo   CMD 1 - Chay Server:
echo     cd %cd%
echo     java -cp out SecureServer
echo.
echo   CMD 2 - Chay Client:
echo     cd %cd%
echo     java -cp out SecureClient
echo.
echo   (Co the mo nhieu Client cung luc de test Multithread)
echo.
pause
