CREATE UNIQUE INDEX IDX_ordini_unique ON ordini (
	idDateOrdine,
	idUtente,
	idProdotto,
	riepilogoUtente
);

DROP INDEX ordini.IX_ordini_1;