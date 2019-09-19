# Fanfix

# Version WIP

- new: maintenant compatible Android (voir [companion project](https://gitlab.com/Rayman22/fanfix-android))
- new: recherche d'histoires (pas encore toutes les sources)
- new: support d'un proxy
- fix: support des CBZ contenant du texte
- fix: correction de DEBUG=0
- fix: correction des histoires importées qui n'arrivent pas immédiatement à l'affichage
- gui: correction pour le focus 
- gui: fix pour la couleur d'arrière plan
- gui: fix pour la navigation au clavier (haut et bas)
- gui: configuration beaucoup plus facile
- MangaLEL: site web changé
- search: supporte MangaLEL
- search: supporte Fanfiction.net
- FimFictionAPI: correction d'une NPE
- remote: changement du chiffrement because Google
- remote: incompatible avec 2.x
- remote: moins bonnes perfs mais meilleure utilisation de la mémoire
- remote: le log inclus maintenant la date des évènements
- remote: le mot de passe se configure maintenant dans le fichier de configuration

# Version 2.0.3

SoFurry: correction pour les histoires disponibles uniquement aux utilisateurs inscrits sur le site

# Version 2.0.2

- i18n: changer la langue dans les options fonctionne aussi quand $LANG existe
- gui: traduction en français
- gui: ReDownloader ne supprime plus le livre original
- fix: corrections pour le visionneur interne
- fix: quelques corrections pour les traductions

# Version 2.0.1

- core: un changement de titre/source/author n'était pas toujours visible en runtime
- gui: ne recharger les histoires que quand nécessaire

# Version 2.0.0

- new: les sources peuvent contenir "/" (et utiliseront des sous-répertoires en fonction)
- gui: nouvelle page pour voir les propriétés d'une histoire
- gui: renommer les histoires, changer l'auteur
- gui: permet de lister les auteurs ou les sources en mode "tout" ou "listing"
- gui: lecteur intégré pour les histoires (texte et images)
- tui: fonctionne maintenant assez bien que pour être déclaré stable
- cli: permet maintenant de changer la source, le titre ou l'auteur
- remote: fix de setSourceCover (ce n'était pas vu par le client)
- remote: on peut maintenant importer un fichier local
- remote: meilleures perfs
- remote: incompatible avec 1.x
- fix: deadlock dans certains cas rares (nikiroo-utils)
- fix: le résumé n'était pas visibe dans certains cas
- fix: update de nikiroo-utils, meilleures perfs pour le remote
- fix: eHentai content warning

# Version 1.8.1

- e621: les images étaient rangées à l'envers pour les recherches (/post/)
- e621: correction pour /post/search/
- remote: correction de certains problèmes de timeout
- remote: amélioration des perfs
- fix: permettre les erreurs I/O pour les CBZ (ignore l'image)
- fix: corriger le répertoire des covers par défaut

# Version 1.8.0

- FimfictionAPI: les noms des chapitres sont maintenant triés correctement
- e621: supporte aussi les recherches (/post/)
- remote: la cover est maintenant envoyée au client pour les imports
- MangaLel: support pour MangaLel

# Version 1.7.1

- GUI: fichiers tmp supprimés trop vite en mode GUI
- fix: une histoire sans cover pouvait planter le programme
- ePub: erreur d'import pour un EPUB local sans cover

# Version 1.7.0

- new: utilisation de jsoup pour parser le HTML (pas encore partout)
- update: mise à jour de nikiroo-utils
- android: compatibilité Android
- MangaFox: fix après une mise-à-jour du site
- MangaFox: l'ordre des tomes n'était pas toujours bon
- ePub: correction pour la compatibilité avec certains lecteurs ePub
- remote: correction pour l'utilisation du cache
- fix: TYPE= était parfois mauvais dans l'info-file
- fix: les guillemets n'étaient pas toujours bien ordonnés
- fix: amélioration du lanceur externe (lecteur natif)
- test: plus de tests unitaires
- doc: changelog disponible en français
- doc: man pages (en, fr)
- doc: SysV init script

# Version 1.6.3

- fix: corrections de bugs
- remote: notification de l'état de progression des actions
- remote: possibilité d'envoyer des histoires volumineuses
- remote: détection de l'état du serveur
- remote: import and change source on server
- CBZ: meilleur support de certains CBZ (si SUMMARY ou URL est présent dans le CBZ)
- Library: correction pour les pages de couvertures qui n'étaient pas toujours effacées quand l'histoire l'était
- fix: correction pour certains cas où les images ne pouvaient pas être sauvées (quand on demande un jpeg mais que l'image n'est pas supportée, nous essayons maintenant ensuite en png)
- remote: correction pour certaines images de couvertures qui n'étaient pas trouvées (nikiroo-utils)
- remote: correction pour les images de couvertures qui n'étaient pas transmises

## Version 1.6.2

- GUI: amélioration des barres de progression
- GUI: meilleures performances pour l'ouverture d'une histoire si le type de l'histoire est déjà le type demandé pour l'ouverture (CBZ -> CBZ ou HTML -> HTML par exemple)

## Version 1.6.1

- GUI: nouvelle option (désactivée par défaut) pour afficher un élément par source (type) sur la page de démarrage au lieu de tous les éléments triés par source (type)
- fix: correction de la source (type) qui était remis à zéro après un re-téléchargement
- GUI: affichage du nombre d'images présentes au lieu du nombre de mots pour les histoires en images

## Version 1.6.0

- TUI: un nouveau TUI (mode texte mais avec des fenêtres et des menus en texte) -- cette option n'est pas compilée par défaut (configure.sh)
- remote: un serveur pour offrir les histoires téléchargées sur le réseau
- remote: une Library qui reçoit les histoires depuis un serveur distant
- update: mise à jour de nikiroo-utils
- FimFiction: support for the new API
- new: mise à jour du cache (effacer le cache actuel serait une bonne idée)
- GUI: correction pour le déplacement d'une histoire qui n'est pas encore dans le cache

## Version 1.5.3

- FimFiction: correction pour les tags dans les metadata et la gestion des chapitres pour certaines histoires

## Version 1.5.2

- FimFiction: correction pour les tags dans les metadata

## Version 1.5.1

- FimFiction: mise à jour pour supporter FimFiction 4
- eHentai: correction pour quelques metadata qui n'étaient pas reprises

## Version 1.5.0

- eHentai: nouveau site supporté sur demande (n'hésitez pas !) : e-hentai.org
- Library: amélioration des performances quand on récupère une histoire (la page de couverture n'est plus chargée quand cela n'est pas nécessaire)
- Library: correction pour les pages de couvertures qui n'étaient pas toujours effacées quand l'histoire l'était
- GUI: amélioration des performances pour l'affichage des histoires (la page de couverture est re-dimensionnée en cache)
- GUI: on peut maintenant éditer la source d'une histoire ("Déplacer vers...")

## Version 1.4.2

- GUI: nouveau menu Options pour configurer le programme (très minimaliste pour le moment)
- new: gestion de la progression des actions plus fluide et avec plus de détails
- fix: meilleur support des couvertures pour les fichiers en cache

## Version 1.4.1

- fix: correction de UpdateChecker qui affichait les nouveautés de TOUTES les versions du programme au lieu de se limiter aux versions plus récentes
- fix: correction de la gestion de certains sauts de ligne pour le support HTML (entre autres, FanFiction.net)
- GUI: les barres de progrès fonctionnent maintenant correctement
- update: mise à jour de nikiroo-utils pour récupérer toutes les étapes dans les barres de progrès
- ( --Fin des nouveautés de la version 1.4.1-- )

## Version 1.4.0

- new: sauvegarde du nombre de mots et de la date de création des histoires dans les fichiers mêmes
- GUI: nouvelle option pour afficher le nombre de mots plutôt que le nom de l'auteur sous le nom de l'histoire
- CBZ: la première page n'est plus doublée sur les sites n'offrant pas de page de couverture
- GUI: recherche de mise à jour (le programme cherche maintenant si une mise à jour est disponible pour en informer l'utilisateur)

## Version 1.3.1

- GUI: on peut maintenant trier les histoires par auteur

## Version 1.3.0

- YiffStar: le site YiffStar (SoFurry.com) est maintenant supporté
- new: support des sites avec login/password
- GUI: les URLs copiées (ctrl+C) sont maintenant directement proposées par défaut quand on importe une histoire
- GUI: la version est maintenant visible (elle peut aussi être récupérée avec --version)

## Version 1.2.4

- GUI: nouvelle option re-télécharger
- GUI: les histoires sont maintenant triées (et ne changeront plus d'ordre après chaque re-téléchargement)
- fix: corrections sur l'utilisation des guillemets
- fix: corrections sur la détection des chapitres
- new: de nouveaux tests unitaires

## Version 1.2.3

- HTML: les fichiers originaux (info_text) sont maintenant rajoutés quand on sauve
- HTML: support d'un nouveau type de fichiers à l'import: HTML (si fait par Fanfix)

## Version 1.2.2

- GUI: nouvelle option "Sauver sous..."
- GUI: corrections (rafraîchissement des icônes)
- fix: correction de la gestion du caractère TAB dans les messages utilisateurs
- GUI: LocalReader supporte maintenant "--read"
- ePub: corrections sur le CSS

## Version 1.2.1

- GUI: de nouvelles fonctions ont été ajoutées dans le menu
- GUI: popup avec un clic droit sur les histoires
- GUI: corrections, particulièrement pour LocalLibrary
- GUI: nouvelle icône (un rond vert) pour dénoter qu'une histoire est "cachée" (dans le LocalReader)

## Version 1.2.0

- GUI: système de notification de la progression des actions
- ePub: changements sur le CSS
- new: de nouveaux tests unitaires
- GUI: de nouvelles fonctions ont été ajoutées dans le menu (supprimer, rafraîchir, un bouton exporter qui ne fonctionne pas encore)

## Version 1.1.0

- CLI: nouveau système de notification de la progression des actions
- e621: correction pour les "pending pools" qui ne fonctionnaient pas avant
- new: système de tests unitaires ajouté (pas encore de tests propres à Fanfix)

## Version 1.0.0

- GUI: état acceptable pour une 1.0.0 (l'export n'est encore disponible qu'en CLI)
- fix: bugs fixés
- GUI: (forte) amélioration
- new: niveau fonctionnel acceptable pour une 1.0.0

## Version 0.9.5

- fix: bugs fixés
- new: compatibilité avec WIN32 (testé sur Windows 10)

## Version 0.9.4

- fix: (beaucoup de) bugs fixés
- new: amélioration des performances
- new: moins de fichiers cache utilisés
- GUI: amélioration (pas encore parfait, mais utilisable)

## Version 0.9.3

- fix: (beaucoup de) bugs fixés
- GUI: première implémentation graphique (laide et buggée)

## Version 0.9.2

- new: version minimum de la JVM : Java 1.6 (tous les JAR binaires ont été compilés en Java 1.6)
- fix: bugs fixés

## Version 0.9.1

- version initiale

