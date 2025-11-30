-- @id: update-person-name
-- @name: Mise à jour du nom d'une personne
-- @description: Met à jour le nom d'une personne dans la table PERSON.
-- @tags: person,update
-- @param: person_id|text|ID Personne|true
-- @param: name|text|Nom|true

UPDATE PERSON SET NAME = {{name}} WHERE PERSON_ID = {{person_id}};

