# 🔍 Tests d'Intégration

Ce document décrit l'endpoint de test d'intégration disponible pour SQL Generator.

**Note** : La supervision (health check, métriques) est gérée au niveau supérieur du projet parent.

---

## 📊 Endpoint de Test d'Intégration

### Test d'Intégration Complet

**Endpoint** : `POST /api/admin/integration-test`

**Description** : Exécute une série de tests d'intégration pour valider le bon fonctionnement de l'application.

**Tests exécutés** :
1. ✅ **Chargement des templates** : Vérifie que les templates sont chargés au démarrage
2. ✅ **Parsing des métadonnées** : Valide le parsing des métadonnées SQL
3. ✅ **Génération d'un script SQL** : Teste la génération complète d'un script (fichier supprimé après test)
4. ✅ **Validation des schémas** : Vérifie la génération des schémas de formulaire
5. ✅ **Répertoire de sortie** : Vérifie l'accessibilité du répertoire

**Réponse** :
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "overallStatus": "PASSED",
  "testsCount": 5,
  "testsPassed": 5,
  "testsFailed": 0,
  "testsSkipped": 0,
  "tests": [
    {
      "name": "Chargement des templates",
      "description": "Vérifie que les templates sont chargés au démarrage",
      "status": "PASSED",
      "details": "2 template(s) chargé(s)"
    },
    {
      "name": "Parsing des métadonnées",
      "description": "Vérifie que les métadonnées SQL sont correctement parsées",
      "status": "PASSED",
      "details": "2/2 template(s) valide(s)"
    },
    {
      "name": "Génération d'un script SQL",
      "description": "Teste la génération d'un script SQL avec un template simple",
      "status": "PASSED",
      "details": "Script généré avec succès: update-person-name_unitaire_20250115103000.sql (245 bytes)",
      "note": "Fichier de test supprimé automatiquement"
    },
    {
      "name": "Validation des schémas",
      "description": "Vérifie que les schémas de formulaire sont générés correctement",
      "status": "PASSED",
      "details": "2/2 schéma(s) valide(s)"
    },
    {
      "name": "Répertoire de sortie",
      "description": "Vérifie que le répertoire de sortie est accessible",
      "status": "PASSED",
      "details": "Répertoire: ./svn_repo_mock/ (accessible: true)"
    }
  ]
}
```

**Codes de réponse** :
- `200 OK` : Tous les tests passent
- `500 Internal Server Error` : Un ou plusieurs tests ont échoué

---

## 🚀 Utilisation

### Test Manuel

```bash
# Test d'intégration
curl -X POST http://localhost:8080/api/admin/integration-test
```

### Intégration dans un Pipeline CI/CD

```yaml
# Exemple GitHub Actions
- name: Integration Tests
  run: |
    response=$(curl -X POST http://localhost:8080/api/admin/integration-test)
    echo "$response" | jq -r '.overallStatus' | grep -q "PASSED" || exit 1
```

---

## 📝 Checklist de Supervision

### Quotidien
- [ ] Exécuter `/api/admin/integration-test` → Status = "PASSED"

### Hebdomadaire
- [ ] Vérifier l'espace disque du répertoire de sortie

### Mensuel
- [ ] Analyser les logs pour détecter des erreurs récurrentes
- [ ] Vérifier la croissance du nombre de fichiers générés

---

## 🆘 Dépannage

### Test d'intégration échoue

1. Consulter le détail de chaque test dans la réponse
2. Vérifier les erreurs spécifiques mentionnées
3. Vérifier que les templates ont des métadonnées valides
4. Vérifier que le répertoire de sortie est accessible

---

**Pour toute question ou problème, consultez les logs de l'application ou le README.md**
