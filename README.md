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


 


