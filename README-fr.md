[English](README.md) Français

# Fanfix

Fanfix est un petit programme Java qui peut télécharger des histoires sur internet et les afficher hors ligne.

## Ceci est le programme serveur et command-line -- le client graphique s'appelle [Fanfix-swing](https://github.com/nikiroo/fanfix-swing/) et le client en mode TUI est [Fanfix-jexer](https://github.com/nikiroo/fanfix-jexer/)

## Synopsis

- ```fanfix``` --import [*URL*]
- ```fanfix``` --export [*id*] [*output_type*] [*target*]
- ```fanfix``` --convert [*URL*] [*output_type*] [*target*] (+info)
- ```fanfix``` --read [*id*] ([*chapter number*])
- ```fanfix``` --read-url [*URL*] ([*chapter number*])
- ```fanfix``` --search
- ```fanfix``` --search [*where*] [*keywords*] (page [*page*]) (item [*item*])
- ```fanfix``` --search-tag
- ```fanfix``` --search-tag [*index 1*]... (page [*page*]) (item [*item*])
- ```fanfix``` --list
- ```fanfix``` --server [*key*] [*port*]
- ```fanfix``` --stop-server [*key*] [*port*]
- ```fanfix``` --remote [*key*] [*host*] [*port*]
- ```fanfix``` --help

## Description

(Si vous voulez juste voir les derniers changements, vous pouvez regarder le [Changelog](changelog-fr.md) -- remarquez que le programme affiche le changelog si une version plus récente est détectée depuis la version 1.4.0.)

Le fonctionnement du programme est assez simple : il converti une URL venant d'un site supporté en un fichier .epub pour les histoires ou .cbz pour les comics (d'autres options d'enregistrement sont disponibles, comme du texte simple, du HTML...)

Pour vous aider à organiser vos histoires, il peut aussi servir de bibliothèque locale vous permettant :

- d'importer une histoire depuis son URL (ou depuis un fichier)
- d'exporter une histoire dans un des formats supportés vers un fichier
- d'afficher une histoire en mode texte
- via [fanfix-swing](https://github.com/nikiroo/fanfix-swing/): d'afficher une histoire en mode GUI **lui-même** ([fanfix-swing](https://github.com/nikiroo/fanfix-swing/)) ou **en appelant un programme natif pour lire le fichier** (potentiellement converti en HTML avant, pour que n'importe quel navigateur web puisse l'afficher)

### Sites supportés

Pour le moment, les sites suivants sont supportés :

- http://FimFiction.net/ : fanfictions dévouées à la série My Little Pony
- http://Fanfiction.net/ : fanfictions venant d'une multitude d'univers différents, depuis les shows télévisés aux livres en passant par les jeux-vidéos
- http://mangahub.io/ : un site répertoriant une quantité non négligeable de mangas (English)
- https://e621.net/ : un site Furry proposant des comics, y compris de MLP
- https://sofurry.com/ : même chose, mais orienté sur les histoires plutôt que les images
- https://e-hentai.org/ : support ajouté sur demande : n'hésitez pas à demander un site !
- http://mangas-lecture-en-ligne.fr/ : un site proposant beaucoup de mangas, en français

### Types de fichiers supportés

Nous supportons les types de fichiers suivants (aussi bien en entrée qu'en sortie) :

- epub : les fichiers .epub créés avec Fanfix (nous ne supportons pas les autres fichiers .epub, du moins pour le moment)
- text : les histoires enregistrées en texte (.txt), avec quelques règles spécifiques :
	- le titre doit être sur la première ligne
	- l'auteur (précédé de rien, ```Par ```, ```De ``` ou ```©```) doit être sur la deuxième ligne, optionnellement suivi de la date de publication entre parenthèses (i.e., ```Par Quelqu'un (3 octobre 1998)```)
	- les chapitres doivent être déclarés avec ```Chapitre x``` ou ```Chapitre x: NOM DU CHAPTITRE```, où ```x``` est le numéro du chapitre
	- une description de l'histoire doit être donnée en tant que chaptire 0
	- une image de couverture peut être présente avec le même nom de fichier que l'histoire, mais une extension .png, .jpeg ou .jpg
- info_text : fort proche du format texte, mais avec un fichier .info accompagnant l'histoire pour y enregistrer quelques metadata (le fichier de metadata est supposé être créé par Fanfix, ou être compatible avec)
- cbz : les fichiers .cbz (une collection d'images zipées), de préférence créés avec Fanfix (même si les autres .cbz sont aussi supportés, mais sans la majorité des metadata de Fanfix dans ce cas)
- html : les fichiers HTML que vous pouvez ouvrir avec n'importe quel navigateur ; remarquez que Fanfix créera un répertoire pour y mettre les fichiers nécessaires, dont un fichier ```index.html``` pour afficher le tout -- nous ne supportons en entrée que les fichiers HTML créés par Fanfix

### Plateformes supportées

Toute plateforme supportant Java 1.6 devrait suffire.

Le programme a été testé sur Linux (Debian, Slackware et Ubuntu), MacOS X et Windows pour le moment, mais n'hésitez pas à nous informer si vous l'essayez sur un autre système.

Si vous avez des difficultés pour le compiler avec une version supportée de Java (1.6+), contactez-nous.

## Options

Vous pouvez démarrer le programme de deux façons :

- ```java -jar fanfix.jar```
- ```fanfix``` (si vous avez utilisé *make install*)

Les arguments suivants sont supportés :

- ```--import [URL]```: importer une histoire dans la librairie
- ```--export [id] [output_type] [target]```: exporter l'histoire "id" vers le fichier donné
- ```--convert [URL] [output_type] [target] (+info)```: convertir l'histoire vers le fichier donné, et forcer l'ajout d'un fichier .info si +info est utilisé
- ```--read [id] ([chapter number])```: afficher l'histoire "id"
- ```--read-url [URL] ([chapter number])```: convertir l'histoire et la lire à la volée, sans la sauver
- ```--search```: liste les sites supportés (```where```)
- ```--search [where] [keywords] (page [page]) (item [item])```: lance une recherche et affiche les résultats de la page ```page``` (page 1 par défaut), et de l'item ```item``` spécifique si demandé
- ```--tag [where]```: liste tous les tags supportés par ce site web
- ```--tag [index 1]... (page [page]) (item [item])```: affine la recherche, tag par tag, et affiche si besoin les sous-tags, les histoires ou les infos précises de l'histoire demandée
- ```--list```: lister les histoires presentes dans la librairie et leurs IDs
- ```--server [key] [port]```: démarrer un serveur d'histoires sur ce port
- ```--stop-server [key] [port]```: arrêter le serveur distant sur ce port (key doit avoir la même valeur)
- ```--remote [key] [host] [port]```: contacter ce server au lieu de la librairie habituelle (key doit avoir la même valeur)
- ```--help```: afficher la liste des options disponibles
- ```--version```: retourne la version du programme

### Environnement

Certaines variables d'environnement sont reconnues par le programme :

- ```LANG=en```: forcer la langue du programme en anglais
- ```CONFIG_DIR=$HOME/.fanfix```: utilise ce répertoire pour les fichiers de configuration du programme (et copie les fichiers de configuration par défaut si besoin)
- ```NOUTF=1```: essaye d'utiliser des caractères non-unicode quand possible (cela peut avoir un impact sur les fichiers générés, pas uniquement sur les messages à l'utilisateur)
- ```DEBUG=1```: force l'option ```DEBUG=true``` du fichier de configuration (pour afficher plus d'information en cas d'erreur)

## Compilation

```./configure.sh && make```

Vous pouvez aussi importer les sources java dans, par exemple, [Eclipse](https://eclipse.org/), et faire un JAR exécutable depuis celui-ci.

Quelques tests unitaires sont disponibles :

```./configure.sh && make build test run-test```

Si vous faites tourner les tests unitaires, sachez que certains fichiers flags peuvent les impacter:

- ```test/VERBOSE```      : active le mode verbeux pour les erreurs
- ```test/OFFLINE```      : ne permet pas au programme de télécharger des données
- ```test/URLS```         : permet au programme de tester des URLs
- ```test/FORCE_REFRESH```: force le nettoyage du cache

Notez que le répertoire ```test/CACHE``` peut rester en place; il contient tous les fichiers téléchargés au moins une fois depuis le réseau par les tests unitaires (si vous autorisez les tests d'URLs, lancez les tests au moins une fois pour peupler le CACHE, puis activez le mode OFFLINE, ça marchera toujours).

Les fichiers de test seront:

- ```test/*.url```  : des URLs à télécharger en fichier texte (le contenu du fichier est l'URL)
- ```test/*.story```: des histoires en mode texte (le contenu du fichier est l'histoire)

### Librairies dépendantes (incluses)

Nécessaires :

- ```libs/nikiroo-utils-sources.jar```: quelques utilitaires partagés
- [```libs/unbescape-sources.jar```](https://github.com/unbescape/unbescape): une librairie sympathique pour convertir du texte depuis/vers beaucoup de formats ; utilisée ici pour la partie HTML
- [```libs/jsoup-sources.jar```](https://jsoup.org/): une libraririe pour parser du HTML
- [```libs/JSON-java-20190722-sources.jar```](https://github.com/stleary/JSON-java): une libraririe pour parser du JSON

Optionnelles :

- [```libs/jexer-sources.jar```](https://github.com/klamonte/jexer): une petite librairie qui offre des widgets en mode TUI
- [```pandoc```](http://pandoc.org/): pour générer les man pages depuis les fichiers README

Rien d'autre, si ce n'est Java 1.6+.

À noter : ```make libs``` exporte ces librairies dans le répertoire src/.

## Auteur

Fanfix a été écrit par Niki Roo <niki@nikiroo.be>

