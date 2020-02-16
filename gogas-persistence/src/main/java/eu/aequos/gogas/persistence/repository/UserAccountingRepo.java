package eu.aequos.gogas.persistence.repository;

import eu.aequos.gogas.persistence.utils.UserTotalProjection;
import eu.aequos.gogas.persistence.utils.UserTransactionFull;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

//TODO: check if we can merge with other repos
@Repository
public interface UserAccountingRepo extends JpaRepository<UserTransactionFull, String> {

    @Query(nativeQuery = true, value = "select userId, sum(total) as total " +
            "  from ( " +
            "         SELECT d.idDateOrdini AS idOrdine, " +
            "                CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END AS userId, " +
            "                ROUND(SUM(CASE WHEN t.totaleCalcolato = 1 THEN o.qtaRitirataKg * o.prezzoKg ELSE o.prezzoKg END), 2) + COALESCE(s.importo, 0) AS total " +
            "         FROM dateOrdini AS d " +
            "                INNER JOIN tipologiaOrdine AS t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
            "                INNER JOIN ordini AS o ON d.idDateOrdini = o.idDateOrdine " +
            "                INNER JOIN utenti AS u ON o.idUtente = u.idUtente " +
            "                LEFT OUTER JOIN speseTrasporto AS s ON u.idUtente = s.idUtente AND d.idDateOrdini = s.idDateOrdini " +
            "         WHERE o.riepilogoUtente = 1 " +
            "           AND o.contabilizzato = 1 " +
            "         GROUP BY d.idDateOrdini, CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END, s.importo " +
            "       ) as ordertotals " +
            "group by userId;")
    List<UserTotalProjection> findOrderTotals();

    @Query(nativeQuery = true, value = "select CASE WHEN m.idReferente IS NOT NULL THEN m.idReferente ELSE m.idUtente END as userId, " +
            "sum(CASE WHEN c.segno = '-' THEN m.importo * - 1 ELSE m.importo END - COALESCE (s.importo, 0) ) as total " +
            "from movimenti m " +
            "inner join causale c on c.codiceCausale=m.causale " +
            "LEFT OUTER JOIN speseTrasporto AS s ON m.idUtente = s.idUtente AND m.idDateOrdini = s.idDateOrdini " +
            "where m.confermato=1 " +
            "group by CASE WHEN m.idReferente IS NOT NULL THEN m.idReferente ELSE m.idUtente END")
    List<UserTotalProjection> findTransactionTotals();

    @Query(nativeQuery = true, value = "SELECT d.idDateOrdini AS id, " +
        "CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END AS user_id, " +
        "d.dataConsegna AS date, " +
        "'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3) AS description, " +
        "ROUND(SUM(CASE WHEN t.totaleCalcolato = 1 THEN o.qtaRitirataKg * o.prezzoKg ELSE o.prezzoKg END), 2) + COALESCE (s.importo, 0) AS amount, " +
        "'' AS reason, " +
        "'-' AS sign, " +
        "o.contabilizzato AS recorded, " +
        "'O' AS type, " +
        "NULL as friend " +
        "FROM dateOrdini AS d " +
        "INNER JOIN tipologiaOrdine AS t ON d.idTipologiaOrdine = t.idTipologiaOrdine " +
        "INNER JOIN ordini AS o ON d.idDateOrdini = o.idDateOrdine " +
        "INNER JOIN utenti AS u ON o.idUtente = u.idUtente " +
        "LEFT OUTER JOIN speseTrasporto AS s ON u.idUtente = s.idUtente AND d.idDateOrdini = s.idDateOrdini " +
        "WHERE o.riepilogoUtente = 1 AND o.contabilizzato=1 AND (o.idUtente=:userId OR o.idReferenteAmico=:refId)" +
        "GROUP BY CASE WHEN o.idReferenteAmico IS NOT NULL THEN o.idReferenteAmico ELSE o.idUtente END, " +
        "d.dataConsegna, d.idDateOrdini, s.importo, 'Totale ordine ' + t.tipoOrdine + ' in consegna ' + CONVERT(VARCHAR, d.dataConsegna, 3), o.contabilizzato")
    List<UserTransactionFull> getUserRecordedOrders(@Param("userId") String userId, @Param("refId") String refId);


    


}
