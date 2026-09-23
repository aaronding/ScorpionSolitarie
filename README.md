This is a Solitaire game I created in 2006 using Java Swing, brought up to date in 2026.

Download `Scorpion.jar` from the [latest release](https://github.com/aaronding/ScorpionSolitaire/releases/latest) and run it with Java 11 or newer:

```sh
java -jar Scorpion.jar
```

Every deal can be won: the game shuffles until its solver finds a way. Three levels (build down in any suit, the same color, or the same suit), each with its own undo rules. Give up and the computer finishes the game for you.

To build it from source (downloads two small libraries into `lib/` the first time):

```sh
./build.sh
```

To run the tests (needs Java 17 or newer):

```sh
./test.sh
```

Card art: [SVG Playing Cards](https://www.tekeye.uk/playing_cards/svg-playing-cards) by Tek Eye, public domain. Uses [FlatLaf](https://www.formdev.com/flatlaf/) and [JSVG](https://github.com/weisJ/jsvg).

![Alt text](https://me.webthinking.io/res/scorpion2.jpg "Scorpion Solitaire")
