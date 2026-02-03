je vais vous expliquez ce que je veux savoir sur un sujet: 

Je vais créer une application qui gère un authetifcation en utilisant cette table "create table users (
    id serial primary key,
    email varchar(50) unique not null,
    password_hash varchar(255) not null,
    created_at timestamp default current_timestamp
);" 
Nous aurrons deux applications :
1-Un application web avec une base de donnée postgresql; un backend avec java spring-boot et un frontend en vue.js.  

2-une application mobile qui sera synchronisé avec le backend spring boot. 

NB: l'inscription se feront via l'application web mais la connexion se fera via les deux applications. On utilise Firebase si il y a une connexion internet et si il n'y a pas de connexion internet on utilise une base de donnée locale postgresql sur docker.

# Ma question c'est comment faire pour que l'authentification fonctionne dans les deux applications en utilisant firebase quand il y a une connexion internet et la base de donnée locale postgresql quand il n'y a pas de connexion internet?     


# donc comment firebase communique avec cette base de donnée postgresql et comment il est structuré? comment ressemble la base de donnée sur Firebase qui est synchronisé sur la base postgresql local? 
   



   
