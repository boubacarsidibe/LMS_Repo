@echo off
REM Ouvre un lien public temporaire vers la plateforme LMS (frontend, port 4200)
REM pour que d'autres personnes puissent se connecter et faire des tests manuels.
REM
REM Le lien change a chaque fois que ce script est relance : il s'affiche dans
REM un encadre juste apres "Your quick Tunnel has been created!". Copie-le et
REM partage-le a ce moment-la.
REM
REM Laisse cette fenetre ouverte tant que les tests durent. La fermer coupe
REM l'acces. Les conteneurs Docker (docker compose up) doivent deja tourner.

echo.
echo Verification que la plateforme tourne (docker compose)...
docker compose -f "%~dp0docker-compose.yml" ps --status running | findstr /C:"demo-frontend" >nul
if errorlevel 1 (
    echo.
    echo ATTENTION : le frontend ne semble pas demarre.
    echo Lance d'abord : docker compose up -d
    echo.
    pause
    exit /b 1
)

echo Plateforme active. Ouverture du tunnel...
echo.
set CLOUDFLARED="C:\Program Files (x86)\cloudflared\cloudflared.exe"
if not exist %CLOUDFLARED% set CLOUDFLARED=cloudflared
%CLOUDFLARED% tunnel --url http://localhost:4200
