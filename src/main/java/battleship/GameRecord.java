package battleship;

public record GameRecord(String result, int moves, int hits, int misses, int repeated, int outside) {
}