# dati-frontendserver
<br />
<a href="https://travis-ci.com/italia/dati-frontendserver">
<img title="Build Status Images" src="https://api.travis-ci.com/italia/dati-frontendserver.svg?token=sdc8mJz3EyP3LyxtXjxQ">
</a>

**FRONT END SERVER** 

Il front end server è un component della architettura di 
https://developers.italia.it/it/datigov/

L'obiettivo del progetto é di create un layer intermedio tra le sorgenti dati Ckan, Dkan, Data Analytics Framework
che offra una Restful interface ai client del progetto _dati aperti_.

Lo standard adottato sarà quello descritto in:
[OpenAPI-Specification](https://github.com/OAI/OpenAPI-Specification)

### Tecnologie ###
 - playframework 2.5.13 
 - scala 2.11.8 
 - sbt 0.13 

Per lanciare il server
```
git clone git@github.com:italia/dati-frontendserver.git
sbt compile
sbt run
http://localhost:9000
```

### Motivazione ###

Per front end server si intende una applicazione che espone come unica view una collezione di servizi Rest ful con Content Representation di tipo JSON organizzati secondo una filosofia di tipo Resource Oriented.
Questo approccio garantisce un'ampia possibilità di aggiungere delle nuove resources ed estendere il perimetro dei servizi offerti, mantenendo un approccio elastico e sostanzialmente neutro rispetto al contenuto degli stessi. 

I vantaggi che proponiamo sono:
- Avere un controllo completo del layer dei “dati” non avendo nessun dipendenza da un contenitore come  ad esempio un CMS
- Avere un front end assolutamente autonomo e sviluppato secondo standard quali HTML5, javascript con una estesa esperienza utente che prescinda dal backend.
- Fornire ai client un unico contratto di accesso ai dati, come un unico “schema” JSON da integrare, e soprattutto evitare delle dipendenze dirette da strutture dati di terze parti (CKAN, ect).
- Avere un controllo esclusivo sul layer dati e sulla struttura da esporre, garantendo quindi un approccio “essenziale” e mirato alle esigenze del client
- Rispondere in via centralizzata a requisiti non funzionali quali: sicurezza, anonimizzazione, logging, analisi,  etc. 

Compito del front end server sarà
- Esporre e documentare i contratti REST, ed il dialetto JSON usato
- Comunicare con i backend integrati
- Fornire un'architettura su cui integrare nuove sorgenti dati in maniera semplice attraverso interfacce applicative esposte
- Predisporre eventuali pipeline di trattamento dei dati 
- Gestire autenticazione ed autorizzazione sulle risorse
- Assicurare scalabilità alla piattaforma

 


