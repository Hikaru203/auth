@echo off
REM ============================================================
REM Generate RSA keypair for JWT RS256 signing
REM Requires OpenSSL to be installed and on PATH
REM ============================================================

SET KEYS_DIR=src\main\resources\keys

if not exist "%KEYS_DIR%" mkdir "%KEYS_DIR%"

echo Generating RSA private key...
openssl genrsa -out "%KEYS_DIR%\private_raw.pem" 2048

echo Converting to PKCS8 format...
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt ^
    -in "%KEYS_DIR%\private_raw.pem" ^
    -out "%KEYS_DIR%\private.pem"

echo Extracting public key...
openssl rsa -in "%KEYS_DIR%\private_raw.pem" -pubout -out "%KEYS_DIR%\public.pem"

echo Cleaning up raw key...
del "%KEYS_DIR%\private_raw.pem"

echo.
echo Keys generated:
echo   Private key: %KEYS_DIR%\private.pem
echo   Public key:  %KEYS_DIR%\public.pem
echo.
echo IMPORTANT: Add keys/ to .gitignore to avoid committing private keys!
