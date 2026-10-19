package fixtures.game;

class GameTest {
    private Game game;

    @org.junit.jupiter.api.Test
    void startsGame() { game = new Game(new Board(), new Player(), new Player()); }
}