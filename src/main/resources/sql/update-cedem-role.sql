-- @id: update-cedem-role
-- @name: Mise à jour du rôle CEDEM avec logs
-- @description: Met à jour le rôle dans CEDEM et PEPERROL avec logs avant/après et commit conditionnel.
-- @tags: cedem,peperrol,update
-- @param: dem_iide|text|ID Demande (CEDEM)|true
-- @param: num_person|text|Numéro de personne|true

DECLARE
  v_dem_iide VARCHAR2(50) := {{dem_iide}};
  v_num_person VARCHAR2(50) := {{num_person}};
  v_prl_iide_before VARCHAR2(50);
  v_prl_iide_after VARCHAR2(50);
BEGIN
  -- Log AVANT : Récupération de la valeur actuelle
  SELECT dem_r_pcl INTO v_prl_iide_before FROM cedem WHERE dem_iide = v_dem_iide;
  DBMS_OUTPUT.PUT_LINE('=== LOG AVANT ===');
  DBMS_OUTPUT.PUT_LINE('dem_iide: ' || v_dem_iide);
  DBMS_OUTPUT.PUT_LINE('dem_r_pcl (avant): ' || NVL(v_prl_iide_before, 'NULL'));
  
  -- UPDATE 1 : Mise à jour de CEDEM
  UPDATE cedem
  SET dem_r_pcl = (SELECT prl_iide FROM peperrol WHERE prl_r_per_jourol = v_num_person AND prl_r_rol_possed = 12)
  WHERE dem_iide = v_dem_iide;
  
  -- UPDATE 2 : Mise à jour de PEPERROL
  UPDATE peperrol
  SET prlddatfin = NULL
  WHERE prl_iide = (SELECT prl_iide FROM peperrol WHERE prl_r_per_jourol = v_num_person AND prl_r_rol_possed = 12);
  
  -- Log APRÈS : Vérification de la nouvelle valeur
  SELECT dem_r_pcl INTO v_prl_iide_after FROM cedem WHERE dem_iide = v_dem_iide;
  DBMS_OUTPUT.PUT_LINE('=== LOG APRÈS ===');
  DBMS_OUTPUT.PUT_LINE('dem_r_pcl (après): ' || NVL(v_prl_iide_after, 'NULL'));
  
  -- Commit conditionnel (seulement si tout s'est bien passé)
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('=== SUCCÈS ===');
  DBMS_OUTPUT.PUT_LINE('Transaction réussie - COMMIT effectué');
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('=== ERREUR ===');
    DBMS_OUTPUT.PUT_LINE('Code erreur: ' || SQLCODE);
    DBMS_OUTPUT.PUT_LINE('Message: ' || SQLERRM);
    DBMS_OUTPUT.PUT_LINE('ROLLBACK effectué - Transaction annulée');
    RAISE;
END;

