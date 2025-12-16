# Commandes pour gérer le port 8080 (Windows)

## 🔍 Lister le processus sur le port 8080

### Méthode 1 : PowerShell (recommandée)
```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object LocalAddress, LocalPort, State, OwningProcess
```

### Méthode 2 : Avec le nom du processus
```powershell
$port = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port) {
    Get-Process -Id $port.OwningProcess | Select-Object Id, ProcessName, Path
}
```

### Méthode 3 : netstat (ligne de commande classique)
```cmd
netstat -ano | findstr :8080
```

---

## 🛑 Tuer le processus sur le port 8080

### Méthode 1 : PowerShell (une seule commande)
```powershell
$port = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port) {
    Stop-Process -Id $port.OwningProcess -Force
    Write-Host "Processus tué avec succès"
} else {
    Write-Host "Aucun processus trouvé sur le port 8080"
}
```

### Méthode 2 : PowerShell (en deux étapes)
```powershell
# 1. Trouver le PID
$pid = (Get-NetTCPConnection -LocalPort 8080).OwningProcess

# 2. Tuer le processus
Stop-Process -Id $pid -Force
```

### Méthode 3 : Avec netstat + taskkill
```cmd
# 1. Trouver le PID
netstat -ano | findstr :8080

# 2. Tuer le processus (remplacer <PID> par le numéro trouvé)
taskkill /PID <PID> /F
```

### Méthode 4 : Tuer tous les processus Java (si c'est Spring Boot)
```powershell
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
```

---

## 🔄 Script PowerShell complet (lister + tuer)

```powershell
# Lister et tuer le processus sur le port 8080
$connection = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

if ($connection) {
    $pid = $connection.OwningProcess
    $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
    
    if ($process) {
        Write-Host "Processus trouvé :"
        Write-Host "  PID: $($process.Id)"
        Write-Host "  Nom: $($process.ProcessName)"
        Write-Host "  Chemin: $($process.Path)"
        
        # Tuer le processus
        Stop-Process -Id $pid -Force
        Write-Host "`nProcessus tué avec succès !" -ForegroundColor Green
    } else {
        Write-Host "Processus introuvable" -ForegroundColor Yellow
    }
} else {
    Write-Host "Aucun processus n'écoute sur le port 8080" -ForegroundColor Yellow
}
```

---

## 📝 Exemples d'utilisation

### Vérifier si le port est libre
```powershell
$port = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port) {
    Write-Host "Port 8080 occupé par PID: $($port.OwningProcess)"
} else {
    Write-Host "Port 8080 libre"
}
```

### Tuer tous les processus Spring Boot
```powershell
Get-Process | Where-Object {$_.ProcessName -eq "java" -and $_.Path -like "*spring-boot*"} | Stop-Process -Force
```

---

## ⚠️ Notes importantes

- `/F` dans `taskkill` = Force (tue immédiatement)
- `-Force` dans `Stop-Process` = Force (tue immédiatement)
- Si le processus refuse de se terminer, utiliser `-Force`
- Vérifier avec `Get-NetTCPConnection` après pour confirmer que le port est libre

