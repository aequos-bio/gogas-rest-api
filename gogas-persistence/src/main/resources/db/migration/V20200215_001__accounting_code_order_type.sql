ALTER TABLE tipologiaOrdine
    ADD codiceContabile varchar(50),
        fatturatoDaAequos bit not null default 0
GO

UPDATE tipologiaOrdine SET fatturatoDaAequos = 1
WHERE idOrdineAequos IS NOT NULL AND idOrdineAequos NOT IN (14, 15, 22, 24) --TODO: check ordini non fatturati da aequos