# Awesome Rocketeers

[Try the game here.](http://ttim.github.io/rockets/)
---
A two-player puzzle game.

Each player is given a board where he can launch rockets by connecting them to fire sources with segments of fuse. To win, a player must launch all rockets located on his board. Trick is, when a rocket is launched, it flies off to the other players board (if there is enough fuel). This way players can mess with each others game, thus making the game more fun and interesting.

Developed during [Clojure Cup](https://clojurecup.com) contest. See [description on ClojureCup website](https://clojurecup.com/#/apps/rockets) and [ClojureCup version](http://rockets.clojurecup.com/).

## Developer notes

[This excellent article](http://solovyov.net/en/2014/cljs-start/) by Alexander Solovyov was used to setup the project. We follow its author advice to use [Quiescent](https://github.com/levand/quiescent) frontend framework.

To start, install
* [Leiningen](http://leiningen.org/);
* [Cursive](https://cursiveclojure.com/) as an IDE for clojure (because it is based on [Intellij IDEA](http://www.jetbrains.com/idea/)).

To run on local machine run in the project root folder:

    lein figwheel dev

This will start server on http://localhost:3449 and watch for local file changes. Browser code will be reloaded automatically (so only need to start lein once).

For deployment start ./build.sh && ./deploy_server.sh in the root folder.
