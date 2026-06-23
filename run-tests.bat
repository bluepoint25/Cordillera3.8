@echo off
echo ========== EJECUTANDO TESTS ==========

echo.
echo [1/6] MS-login
cd MS-login
call mvn test
cd ..

echo.
echo [2/6] MS-data
cd MS-data
call mvn test
cd ..

echo.
echo [3/6] MS-kpi
cd MS-kpi
call mvn test
cd ..

echo.
echo [4/6] MS-reportes
cd MS-reportes
call mvn test
cd ..

echo.
echo [5/6] MS-reporte-mail
cd MS-reporte-mail
call mvn test
cd ..

echo.
echo [6/6] API-gateway
cd api-gateway
call mvn test
cd ..

echo.
echo ========== TESTS COMPLETADOS ==========
pause
