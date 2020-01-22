ALTER TABLE ordiniFornitore ADD externalCode varchar(50)
GO

UPDATE f SET f.externalCode = p.idEsterno
FROM ordiniFornitore f
INNER JOIN prodotti p ON f.idProdotto = p.idProdotto