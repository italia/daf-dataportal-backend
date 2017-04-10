# dati-frontendserver
<br />
<a href="https://travis-ci.com/italia/dati-frontendserver">
<img title="Build Status Images" src="https://api.travis-ci.com/italia/dati-frontendserver.svg?token=sdc8mJz3EyP3LyxtXjxQ">
</a>

**FRONT END SERVER** 

Il front end server è un componente della architettura di 
https://developers.italia.it/it/datigov/

L'obiettivo del progetto é di create un layer intermedio tra le sorgenti dati Ckan, Dkan, Data Analytics Framework e servizi
esterni che offra una Restful interface ai client del progetto _dati aperti_.

Lo standard adottato sarà quello descritto in:
[OpenAPI-Specification](https://github.com/OAI/OpenAPI-Specification)
Abbiamo scelto di avere un approccio contract first, nel quale prima si definiscono i contratti di tipo Rest, in un file yaml/json.

- [file](conf/ftd_api.yaml) descrive, seguendo le specifiche [swagger 2.0](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md),
- le routes e il modello dati della risposta. Utilizzando, ad esempio [swagger-codegen](https://github.com/swagger-api/swagger-codegen) o online [swagger editor](http://editor.swagger.io/) e' possibile creare lo sceletro di un web server o api client a partire dal [file](conf/ftd_api.yaml) in vari linguaggi di programmazione che espone o consuma i servizi descritti.

Il progetto utilizza la libreria [api-first-hand](https://github.com/zalando/api-first-hand) che a partire dal file yaml genera un web server play con modello dati e alcuni test gia' implementati e la UI di swagger.
Zalando inoltre ha messo a disposizione le [linee guida per creare API](http://zalando.github.io/restful-api-guidelines/TOC.html), lettura interessante per approfondire l'argomento.

Al momento vengono esposte delle API di monitoriggio di dati.gov.it. I dati sono d'esempio per essere utilizzati anche in modalita' offline, rendendo il webserver una componente a se stante dalle sorgenti dati e dal front end in termini di sviluppo.

### Per lanciare il servizio ###

```
git clone git@github.com:italia/dati-frontendserver.git
sbt compile
sbt run
connect to:
http://localhost:9000
```

### Tecnologie ###
 - playframework 2.5.13
 - scala 2.11.8
 - sbt 0.13
 - [api-fitst-hand](https://github.com/zalando/api-first-hand)

### Processo di sviluppo generico ###
1. Scaricare il [file](conf/ftd_api.yaml) e aggiungere le routes e il modello dati di risposta (NB il modello dati dovrebbe essere definito avendo in mente una sorgente reale esistente (un database, un servizio esterno, ecc..).
2. Generare lo scheletro del  codice attraverso [swagger-codegen](https://github.com/swagger-api/swagger-codegen) o online [swagger editor](http://editor.swagger.io/)
3. Creare nella cartella [data/NOME_DATASET.json o .csv o ..](data/) i dati che andranno a riempire il modello dati generato attraverso il processo di build. NB File e non oggetti STUB per garantire di essere agnostici al linguaggio di programmazione utilizzato
4. Scrivere il codice che dai files va a riempire con i dati(nei files) il modello delle risposte descritto nello yaml.
5. Scrivere il codice che va a interfacciarsi con le sorgenti dati (databases, servizi ec... ) senza specificare URL di connessioni.
A breve un esempio di servizi esposti utilizzando nodejs

### dati-frontendserver: sviluppo ###
- [file](conf/ftd_api.yaml) descrive le routes e il modello dati delle risposte Rest verra' scomposto in piu' file.
- sbt compile rigenera le Action di routes e il modello dei dati
 e delle risposte ma non modifica il codice scritto
- le Action sono generate in [generated_controllers](app/generated_controllers/ftd_api.yaml.scala). Il codice tra i commenti
non viene rigenerato. Nell'esempio sottostante un metodo e il relativo popolamento che verra' spiegato nei dettagli di seguito.
```scala
val catalogDistrubutionFormat = catalogDistrubutionFormatAction { (catalogName: String) =>
// ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
     val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogFormat(catalogName)
     CatalogDistrubutionFormat200(distributions)
// ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
}
```
- la cartella [data](data/) contiene gli oggetti Stub che simulano dati di risposta, isolando l'applicativo dalle sorgenti dati
- nel path non committato sulla repository github che viene generato a compile time [target/scala-2.11/ftd_api/yaml] trovate il modello dati e delle risposte.
- per "riempire" con i dati in maniera generica il modello e le risposte utilizzo un pattern di dependency injection chiamato cake pattern
A riguardo potete leggere l'articolo [real world di](jonasboner.com/real-world-scala-dependency-injection-di/) oppure il [paper](http://lampwww.epfl.ch/~odersky/papers/ScalableComponent.pdf)
I vantaggi sono efficienza, modularita' e non dipendere da librerie esterne. Nel paragrafo successivo andro' nel dettaglio dell'implementazione.

### Implementazione ###
L'organizzazione dei packages potra' cambiare, al momento la maggior parte del codice e' contenuto nelle catelle

- [repositories](repositories/) 
- [services](services/).

La cartella repositories contiene il codice necessario a leggere i dati dalle sorgenti, per l'ambiente di test attuale dai
file nella cartella [data](data/) ed esporli attraverso i servizi dichiarati nella cartella [services](services/).
L'interfaccia Repository definisce i metodi da implementare mentre le classi MonitorRepositoryDev e MonitorRepositoryProd
l'implementazione dei metodi che leggono dalle sorgenti dati.

Al momento services/ ha solo funzione di proxy e dipende dall'injection di almeno una repository ma con i prossimi sviluppi
avra' differenti implementazione. Il vantaggio di quest'approccio risiede nella modularizzazione delle dipendenza
per la creazione di differenti ambienti test, sviluppo online/offline, staging, production indipendenti e intercambiabili.
Un esempio di codice d'esempio piu' esplicativo di molte parole:

```scala
trait TestEnvironment extends
  UserServiceComponent with
  UserRepositoryComponent with
  org.specs.mock.JMocker
{
  val userRepository = mock(classOf[UserRepository])
  val userService = mock(classOf[UserService])
}

// use it

class UserServiceSuite extends TestNGSuite with TestEnvironment {

  @Test { val groups=Array("unit") }
  def authenticateUser = {

    // create a fresh and clean (non-mock) UserService
    // (who's userRepository is still a mock)
    val userService = new UserService

    // record the mock invocation
    expect {
      val user = new User("test", "test")
      one(userRepository).authenticate(user) willReturn user
    }

    ... // test the authentication method
  }

  ...

```




### Test cases ###
TODO


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
