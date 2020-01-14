# gogas-rest-api
GoGas REST APIs

### Configurazione del database
Le definizioni delle connessioni ai tenant sono salvate in un database master.
Per il primo avviamento dell'applicazione é necessario creare questo database ed inserire le properties url, username, password e driverClassName nel file **application.properties** sotto la chiave **spring.datasource**

Esempio:
```
spring.datasource.url=jdbc:sqlserver://localhost\\SQLEXPRESS:1433;database=masterdb
spring.datasource.username=master
spring.datasource.password=master
spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

Nel database deve essere presente una tablella denominata **tenants** con la struttura definita da questo DDL:


```
CREATE TABLE tenants (
	tenant_id varchar(30) NOT NULL PRIMARY KEY,
	username varchar(30) NOT NULL,
	password varchar(30) NOT NULL,
	url varchar(256) NOT NULL
);

```

### Esecuzione e compilazione del frontend React
Il software del frontend è contemuto nella cartella **gogas-service/frontend**
Prima di poter essere utilizzato è necessario eseguire nella cartella il comando per scaricare tutte le dipendenze
```
npm install
```
a questo punto è possibile avviare un server di sviluppo con il supporto per l'hot reloading:
```
npm start
```
il client sarà quindi disponibile all'indirizzo **localhost:3000**, le chiamate al backend verranno proxate sull'indirzzo **localhost:8081**.

Per generare la versione compilata pronta per essere distribuita è sufficiente eseguire il comando
```
npm run build
```
questo genererà i file compilati nella cartella del progetto **gogas-service/src/main/resources**


