@echo off
echo ========================================
echo   TAO CHUNG CHI SSL CHO BAI TAP
echo ========================================
echo.

REM Xoa file cu (neu co)
del /f /q serverkeystore.jks 2>nul
del /f /q clienttruststore.jks 2>nul
del /f /q server.cer 2>nul

echo [1/3] Tao Keystore cho Server...
keytool -genkeypair -alias serverkey -keyalg RSA -keysize 2048 -validity 365 ^
    -keystore serverkeystore.jks -storepass changeit -keypass changeit ^
    -dname "CN=localhost, OU=PTIT, O=PTIT, L=HN, ST=HN, C=VN"

echo.
echo [2/3] Xuat certificate tu Server Keystore...
keytool -exportcert -alias serverkey -keystore serverkeystore.jks ^
    -storepass changeit -file server.cer

echo.
echo [3/3] Tao Truststore cho Client (import certificate cua Server)...
keytool -importcert -alias serverkey -file server.cer ^
    -keystore clienttruststore.jks -storepass changeit -noprompt

echo.
echo ========================================
echo   HOAN THANH! Da tao:
echo   - serverkeystore.jks (cho Server)
echo   - clienttruststore.jks (cho Client)
echo ========================================
echo.

REM Xoa file certificate tam
del /f /q server.cer 2>nul

pause
