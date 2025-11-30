-- @id: activate-contrats
-- @name: Activation de contrats en masse
-- @description: Active plusieurs contrats en utilisant une clause IN. Supporte les fichiers avec lotissement automatique si > 999 valeurs.
-- @tags: contrat,update
-- @param-file: contrat_iide|text|Fichier contenant les IDs contrats (1 par ligne)|true

UPDATE contrat SET contrat.isactive = 1 WHERE contrat.contrat_iide IN ({{contrat_iide}});

