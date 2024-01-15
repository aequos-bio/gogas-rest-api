INSERT INTO notificationPrefs
SELECT idUtente, NULL, 1, 1, 60, 1, 60, 1, 1
FROM utenti
WHERE idUtente NOT IN (SELECT idUtente FROM notificationPrefs);