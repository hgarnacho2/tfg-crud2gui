package fixtures.game;

class Game {
    private final Board board;
    private final Player firstPlayer;
    private final Player secondPlayer;

    Game(Board board, Player firstPlayer, Player secondPlayer) {
        this.board = board;
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
    }
}

class GameLauncher {
    private Game lastGame;
    public static void main(String[] args) {
        Game game = new Game(new Board(), new Player(), new Player());
    }
}

class Board { }
class Player { }