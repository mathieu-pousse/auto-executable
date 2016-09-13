# No magic

Depuis l'avènement de spring-boot, on entend parler du fait qu'il est désormais possible de compiler un jar qui contient tout le necessaire pour s'executer. Cela inclus l'application, ses dépendances et le serveur. On a tous utilisé cela en developpement, mais jusque là je compilais un war et le deployais dans une infrastructure existante. Mais actuellement, je me trouve confronté à un client qui n'a aucune expérience dans l'exploitation d'un serveur comme tomcat. Je pourrais faire ma première mise en production d'un jar, mais pas avant d'avoir compris tout ce qui se cache derrière la magie qui l'entoure !

# Mon serveur

Avant tout, notre jar a besoin de contenir 1oo% des éléments que nous avons définit dans notre fichier de dépendence. Cela inclus les dépendances avec le scope ̀`compile`, mais aussi celle du `runtime`. La principal,  dont on sous-estime l'impact, c'est `javax.servlet`. Celle-là, est responsable de connecter une socket à votre servlet. En pratique, c'est tomcat, jetty, undertow, ... qui implémentent cette API.

Pour que notre jar soit auto-suffisant, il faut donc que celui-ci contienne un de ces serveurs. Par défaut spring embarque un tomcat. Mais le fait de cacher le serveur dans un jar permet-il de le configurer toujours aussi finement en cas de besoin?

Bien sur, c'est la marque de fabrique de spring. Une configuration par défaut qui fonctionne dans 9o% des cas, mais pour les 1o% restant, laisser le système ouvert et permettre de tout paramétrer. En ce qui concerne tomcat, cela se passe dans la classe `org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory`. Elle va permettre de redéfinir tout ce qui se trouve dans le fichier `context.xml` de manière programatique. Cela se passe également par le fait de pouvoir changer de connecteur et utiliser le non-blocking-io si vous en ressentez le besoin:

https://github.com/spring-projects/spring-boot/blob/master/spring-boot-samples/spring-boot-sample-tomcat-multi-connectors/src/main/java/sample/tomcat/multiconnector/SampleTomcatTwoConnectorsApplication.java

# Mes dépendances

Il faut maintenant que notre jar contienne notre application mais aussi toutes les dépendances. Pour se faire, il va falloir repackager tous nos jars en un seul. Il existe une technique appelé uber-jar. L'idée c'est de prendre tous les jars qui composent les dependances de notre application, toutes les décompresser dans un même répertoire, puis créer une nouvelle archive qui contiendra l'ensemble des classes. Le plugin de maven qui permet de faire cela facilement c'est `maven-shade-plugin`. Il propose en plus un certain nombre d'opérations qui peuvent transformer les classes, les renomer pour éviter les collisions, en exclure, en concaténer, en ajouter...

Vous trouverez [https://github.com/mathieu-pousse/auto-executable/](https://github.com/mathieu-pousse/auto-executable/) un mini projet qui en fait la démonstration. C'est une application avec une classe `app.Bootstrap` et une dépendance vers apache commons.

Après l'éxecution de la commande `mvn clean install`, on trouve dans le répertoire target: 

    - original-auto-executable-1.0-SNAPSHOT.jar contient uniquement la classe de notre application
    - auto-executable-1.0-SNAPSHOT.jar l'uber jar qui contient bien notre application et les classes d'apache commons

    $ jar -tf target/auto-executable-1.0-SNAPSHOT.jar | grep -v class | grep -v META 
    app/ <-- our application
    org/ <-- apache common classes start here
    org/apache/
    org/apache/commons/
    org/apache/commons/lang3/
    org/apache/commons/lang3/builder/
    org/apache/commons/lang3/concurrent/
    org/apache/commons/lang3/event/
    org/apache/commons/lang3/exception/
    org/apache/commons/lang3/math/
    org/apache/commons/lang3/mutable/
    org/apache/commons/lang3/reflect/
    org/apache/commons/lang3/text/
    org/apache/commons/lang3/text/translate/
    org/apache/commons/lang3/time/
    org/apache/commons/lang3/tuple/

Ce plugin propose donc (relativement) simplement de repackager notre application en une seule archive auto suffisante. Plus de problème de tester sur un serveur différent de celui de la production.

# Mon service systemd

Systemd est le successeur du System V et de ses fichiers que l'on trouvait dans `/etc/init.d`. C'est le système de service qui est désormais embarqué par défaut dans la plus part des distributions Linux. C'est une refonte globale du système de gestion des services. Il s'appuie sur des fichiers de description des services. Donc pour notre application spring-boot, il 'suffit' d'en créer un pour notre besoin.

```
cat > /lib/systemd/system/da-application.service << EOF
[Unit]
Description=Da Application
After=syslog.target

[Service]
User=da-user
ExecStart=/opt/da-application.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF
```

C'est simple et efficace ! 

Une petite `Description`, le tag `After` permet de configurer les dépendances necessaires pour notre service. La section `[Service]` spécifie l'utilisateur à utiliser pour lancer le service, le chemin de l'executable, et un code d'erreur "normal" (143 c'est le code renvoyé en cas de SIGTERM par l'application et qui n'est pas une erreur mais une fin normal).

Ensuite, il suffit de controler le service avec systemctl pour lancer le service.

    $ # Enable at startup
    $ systemctl enable da-application.service
    $ # Start da application
    $ systemctl start da-application.service

Oui mais voila, si vous executez un jar fraichement compilé, voilà ce qui se passe: 

    $ ./da-application.jar
    invalid file (bad magic number): Exec format error

En effet, un jar est une archive, un zip, pas un fichier executable. 

    $ file da-application.jar
    da-application.jar: Java archive data (JAR)

Mais alors, comment faire pour pouvoir executer un jar comme un programe? La solution réside dans une astuce d'organisation d'un fichier zip. La norme autorise à ce qu'un fichier zip soit préfixé par des données. Au moment de l'extraction, cette zone sera simplement ignorée. Mais cela nous permet par exemple de préfixer notre fichier zip avec un programme qui se charge d'executer notre programme.



