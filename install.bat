@echo off

echo Instalando...

mvn install && ^
docker build -t calisto . && ^
docker save -o calisto.tar calisto