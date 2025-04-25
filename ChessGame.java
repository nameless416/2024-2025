import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ChessGame extends JFrame {

    private Board board;
    private ChessBoardPanel chessBoardPanel;
    private Piece selectedPiece;
    private int currentPlayer = 0; // 0 = white, 1 = black
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private Piece enPassantVulnerable = null;
    private JLabel statusLabel;

    public ChessGame() {
        setTitle("Chess Game");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        board = new Board();
        chessBoardPanel = new ChessBoardPanel();
        add(chessBoardPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("White's turn", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * The panel that holds the 8x8 grid of chess squares.
     */
    private class ChessBoardPanel extends JPanel {
        private static final int ROWS = 8;
        private static final int COLS = 8;

        public ChessBoardPanel() {
            setLayout(new GridLayout(ROWS, COLS));
            initializeBoard();
        }

        private void initializeBoard() {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Square square = new Square(row, col);
                    square.addMouseListener(new SquareClickListener());
                    add(square);
                }
            }
        }

        /**
         * Updates the board by resetting each square’s piece.
         */
        public void updateBoard() {
            Component[] squares = getComponents();
            for (Component comp : squares) {
                if (comp instanceof Square) {
                    Square square = (Square) comp;
                    Piece piece = board.getPiece(square.getRow(), square.getCol());
                    square.setPiece(piece);
                    // Clear any temporary highlights.
                    square.setHighlighted(false);
                }
            }
            repaint();
        }

        /**
         * Clears highlights from all squares.
         */
        public void clearHighlights() {
            Component[] squares = getComponents();
            for (Component comp : squares) {
                if (comp instanceof Square) {
                    ((Square) comp).setHighlighted(false);
                }
            }
        }
    }

    /**
     * Represents an individual square on the chess board.
     */
    private class Square extends JPanel {
        private int row;
        private int col;
        private Piece piece;
        private JLabel label;

        public Square(int row, int col) {
            this.row = row;
            this.col = col;
            setLayout(new BorderLayout());
            setBackground((row + col) % 2 == 0 ? new Color(238, 238, 210) : new Color(118, 150, 86));
            label = new JLabel("", SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.PLAIN, 40));
            add(label);
            setBorder(BorderFactory.createLineBorder(getBackground()));
        }

        /**
         * Sets the piece on this square and updates the displayed symbol.
         */
        public void setPiece(Piece piece) {
            this.piece = piece;
            if (piece != null) {
                label.setText(piece.getSymbol());
                label.setForeground(piece.getColor() == 0 ? Color.WHITE : Color.BLACK);
            } else {
                label.setText("");
            }
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        /**
         * Highlights or unhighlights this square by changing its border.
         */
        public void setHighlighted(boolean highlighted) {
            if (highlighted) {
                setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
            } else {
                // Reset to a border matching the background.
                setBorder(BorderFactory.createLineBorder(getBackground()));
            }
        }
    }

    /**
     * Handles mouse clicks on squares.
     */
    private class SquareClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            Square square = (Square) e.getSource();
            // Clear any previous highlights.
            chessBoardPanel.clearHighlights();

            if (selectedPiece == null) {
                handlePieceSelection(square);
            } else {
                handlePieceMovement(square);
            }
        }

        /**
         * When no piece is selected, try to select a piece.
         */
        private void handlePieceSelection(Square square) {
            Piece piece = board.getPiece(square.getRow(), square.getCol());
            if (piece != null && piece.getColor() == currentPlayer) {
                selectedPiece = piece;
                highlightValidMoves();
            }
        }

        /**
         * When a piece is selected, try to move it to the clicked square.
         */
        private void handlePieceMovement(Square square) {
            int newRow = square.getRow();
            int newCol = square.getCol();

            if (board.isValidMove(selectedPiece, newRow, newCol)) {
                handleSpecialMoves(newRow, newCol);
                board.movePiece(selectedPiece, newRow, newCol);
                handlePostMoveLogic();
            }
            selectedPiece = null;
            chessBoardPanel.updateBoard();
        }

        /**
         * Checks and handles castling, en passant, and pawn promotion.
         */
        private void handleSpecialMoves(int newRow, int newCol) {
            // Castling: king moves two squares horizontally.
            if (selectedPiece instanceof King && Math.abs(newCol - selectedPiece.getCol()) == 2) {
                handleCastling(newCol);
            }

            // En passant: if a pawn moves diagonally into an empty square.
            if (selectedPiece instanceof Pawn && enPassantVulnerable != null
                    && newCol != selectedPiece.getCol() && board.getPiece(newRow, newCol) == null) {
                board.captureEnPassant(enPassantVulnerable);
            }

            // Pawn promotion: if a pawn reaches the end row.
            if (selectedPiece instanceof Pawn && (newRow == 0 || newRow == 7)) {
                promotePawn((Pawn) selectedPiece, newRow, newCol);
            }
        }

        /**
         * Handles switching the rook when castling.
         */
        private void handleCastling(int newCol) {
            int rookCol = newCol > selectedPiece.getCol() ? 7 : 0;
            int newRookCol = newCol > selectedPiece.getCol() ? 5 : 3;
            Piece rook = board.getPiece(selectedPiece.getRow(), rookCol);
            board.movePiece(rook, selectedPiece.getRow(), newRookCol);
        }

        /**
         * Prompts the user for a pawn promotion choice and updates the board.
         */
        private void promotePawn(Pawn pawn, int row, int col) {
            Object[] options = {"Queen", "Rook", "Bishop", "Knight"};
            int choice = JOptionPane.showOptionDialog(null, "Promote pawn to:", "Pawn Promotion",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            Piece newPiece = switch (choice) {
                case 1 -> new Rook(row, col, currentPlayer);
                case 2 -> new Bishop(row, col, currentPlayer);
                case 3 -> new Knight(row, col, currentPlayer);
                default -> new Queen(row, col, currentPlayer);
            };
            board.setPiece(row, col, newPiece);
        }

        /**
         * Highlights all legal moves for the selected piece by changing the square border.
         */
        private void highlightValidMoves() {
            // Retrieve the list of potential moves and filter them using full validation.
            List<Point> moves = new ArrayList<>();
            for (Point p : selectedPiece.getValidMoves(board.getGrid())) {
                if (board.isValidMove(selectedPiece, p.x, p.y)) {
                    moves.add(p);
                }
            }
            // Highlight the squares corresponding to the valid moves.
            Component[] components = chessBoardPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof Square) {
                    Square sq = (Square) comp;
                    for (Point move : moves) {
                        if (sq.getRow() == move.x && sq.getCol() == move.y) {
                            sq.setHighlighted(true);
                        }
                    }
                }
            }
        }

        /**
         * Updates state after a move is made.
         */
        private void handlePostMoveLogic() {
            // Update en passant vulnerability: only lasts one move.
            enPassantVulnerable = (selectedPiece instanceof Pawn
                    && Math.abs(selectedPiece.getRow() - selectedPiece.getPreviousRow()) == 2)
                    ? selectedPiece : null;

            // Update king moved status.
            if (selectedPiece instanceof King) {
                if (currentPlayer == 0) {
                    whiteKingMoved = true; 
                } else {
                    blackKingMoved = true;
                }
            }

            // Switch turns.
            currentPlayer = 1 - currentPlayer;
            statusLabel.setText((currentPlayer == 0 ? "White's" : "Black's") + " turn");

            // Check for check/checkmate.
            if (board.isCheck(currentPlayer)) {
                statusLabel.setText("Check!");
                if (board.isCheckmate(currentPlayer)) {
                    JOptionPane.showMessageDialog(null, "Checkmate! "
                            + (currentPlayer == 0 ? "Black" : "White") + " wins!");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * The Board class encapsulates the state of the chess board.
     */
    private class Board {
        private Piece[][] grid;

        public Board() {
            initializeBoard();
        }

        private void initializeBoard() {
            grid = new Piece[8][8];

            // Initialize white pieces.
            grid[0] = new Piece[]{
                new Rook(0, 0, 0), new Knight(0, 1, 0), new Bishop(0, 2, 0), new Queen(0, 3, 0),
                new King(0, 4, 0), new Bishop(0, 5, 0), new Knight(0, 6, 0), new Rook(0, 7, 0)
            };
            for (int col = 0; col < 8; col++) {
                grid[1][col] = new Pawn(1, col, 0);
            }

            // Initialize black pieces.
            grid[7] = new Piece[]{
                new Rook(7, 0, 1), new Knight(7, 1, 1), new Bishop(7, 2, 1), new Queen(7, 3, 1),
                new King(7, 4, 1), new Bishop(7, 5, 1), new Knight(7, 6, 1), new Rook(7, 7, 1)
            };
            for (int col = 0; col < 8; col++) {
                grid[6][col] = new Pawn(6, col, 1);
            }
        }

        public Piece getPiece(int row, int col) {
            return grid[row][col];
        }

        public void setPiece(int row, int col, Piece piece) {
            grid[row][col] = piece;
        }

        /**
         * Returns the board grid so that pieces can compute their valid moves.
         */
        public Piece[][] getGrid() {
            return grid;
        }

        /**
         * Checks whether a move is valid including boundary, collision, and
         * simulation to ensure the king is not left in check.
         */
        public boolean isValidMove(Piece piece, int newRow, int newCol) {
            // Check board boundaries.
            if (newRow < 0 || newRow >= 8 || newCol < 0 || newCol >= 8) {
                return false;
            }
            // Cannot capture a piece of the same color.
            if (grid[newRow][newCol] != null && grid[newRow][newCol].getColor() == piece.getColor()) {
                return false;
            }
            // Check if the piece's move pattern allows the move.
            if (!piece.isValidMove(newRow, newCol, grid)) {
                return false;
            }

            // Simulate the move to check whether it leaves the king in check.
            Piece original = grid[newRow][newCol];
            int oldRow = piece.getRow();
            int oldCol = piece.getCol();

            grid[oldRow][oldCol] = null;
            grid[newRow][newCol] = piece;
            piece.setPosition(newRow, newCol);

            boolean inCheck = isCheck(piece.getColor());

            // Undo the simulated move.
            grid[oldRow][oldCol] = piece;
            grid[newRow][newCol] = original;
            piece.setPosition(oldRow, oldCol);

            return !inCheck;
        }

        /**
         * Moves a piece and updates any special state (e.g., a rook’s moved status).
         */
        public void movePiece(Piece piece, int newRow, int newCol) {
            grid[piece.getRow()][piece.getCol()] = null;
            grid[newRow][newCol] = piece;
            piece.setPosition(newRow, newCol);
            // Mark the rook as moved if applicable.
            if (piece instanceof Rook) {
                ((Rook) piece).setMoved(true);
            }
        }

        /**
         * Removes the captured pawn from the board for en passant.
         */
        public void captureEnPassant(Piece pawn) {
            grid[pawn.getRow()][pawn.getCol()] = null;
        }

        /**
         * Checks if the king of the specified color is in check.
         */
        public boolean isCheck(int color) {
            Piece king = findKing(color);
            return isSquareUnderAttack(king.getRow(), king.getCol(), 1 - color);
        }

        /**
         * Checks if the given color is in checkmate.
         */
        public boolean isCheckmate(int color) {
            if (!isCheck(color)) {
                return false;
            }
            // For each piece of the given color, try every valid move.
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece piece = grid[row][col];
                    if (piece != null && piece.getColor() == color) {
                        List<Point> moves = piece.getValidMoves(grid);
                        for (Point move : moves) {
                            if (tryMove(piece, move.x, move.y)) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }

        /**
         * Tries a move and checks whether it leaves the king in check.
         */
        private boolean tryMove(Piece piece, int newRow, int newCol) {
            if (!isValidMove(piece, newRow, newCol)) {
                return false;
            }
            Piece original = grid[newRow][newCol];
            int oldRow = piece.getRow();
            int oldCol = piece.getCol();

            grid[oldRow][oldCol] = null;
            grid[newRow][newCol] = piece;
            piece.setPosition(newRow, newCol);

            boolean stillInCheck = isCheck(piece.getColor());

            grid[oldRow][oldCol] = piece;
            grid[newRow][newCol] = original;
            piece.setPosition(oldRow, oldCol);

            return !stillInCheck;
        }

        /**
         * Finds and returns the king piece for the given color.
         */
        private Piece findKing(int color) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece piece = grid[row][col];
                    if (piece instanceof King && piece.getColor() == color) {
                        return piece;
                    }
                }
            }
            return null;
        }

        /**
         * Determines whether a square is under attack by any piece of the attacking color.
         */
        public boolean isSquareUnderAttack(int targetRow, int targetCol, int attackingColor) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Piece piece = grid[row][col];
                    if (piece != null && piece.getColor() == attackingColor) {
                        if (piece.isValidMove(targetRow, targetCol, grid)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * The abstract Piece class representing a chess piece.
     */
    private abstract class Piece {
        protected int row;
        protected int col;
        protected int color;
        protected String symbol;
        protected int previousRow;
        protected int previousCol;

        public Piece(int row, int col, int color) {
            this.row = row;
            this.col = col;
            this.color = color;
            this.previousRow = row;
            this.previousCol = col;
        }

        public abstract boolean isValidMove(int newRow, int newCol, Piece[][] grid);

        public abstract List<Point> getValidMoves(Piece[][] grid);

        /**
         * Updates the piece's position and saves the previous position.
         */
        public void setPosition(int row, int col) {
            previousRow = this.row;
            previousCol = this.col;
            this.row = row;
            this.col = col;
        }

        public int getPreviousRow() {
            return previousRow;
        }

        public int getPreviousCol() {
            return previousCol;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getColor() {
            return color;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }

    /**
     * The Pawn class with move logic including regular moves, captures, and en passant.
     */
    private class Pawn extends Piece {

        public Pawn(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♙" : "♟";
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            int direction = color == 0 ? 1 : -1;
            int startRow = color == 0 ? 1 : 6;

            // Regular move forward.
            if (col == newCol && grid[newRow][newCol] == null) {
                if (newRow == row + direction) {
                    return true;
                }
                if (row == startRow && newRow == row + 2 * direction && grid[row + direction][col] == null) {
                    return true;
                }
            }

            // Capture move (including en passant).
            if (Math.abs(newCol - col) == 1 && newRow == row + direction) {
                if (grid[newRow][newCol] != null && grid[newRow][newCol].color != color) {
                    return true;
                }
                if (grid[row][newCol] instanceof Pawn && grid[row][newCol] == enPassantVulnerable) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            int direction = color == 0 ? 1 : -1;
            int startRow = color == 0 ? 1 : 6;

            // Forward moves.
            if (grid[row + direction][col] == null) {
                moves.add(new Point(row + direction, col));
                if (row == startRow && grid[row + 2 * direction][col] == null) {
                    moves.add(new Point(row + 2 * direction, col));
                }
            }

            // Diagonal captures.
            for (int dc = -1; dc <= 1; dc += 2) {
                if (col + dc >= 0 && col + dc < 8) {
                    if (grid[row + direction][col + dc] != null && grid[row + direction][col + dc].color != color) {
                        moves.add(new Point(row + direction, col + dc));
                    }
                    // Note: En passant capture square is the same as a diagonal move.
                    else if (grid[row][col + dc] instanceof Pawn && grid[row][col + dc] == enPassantVulnerable) {
                        moves.add(new Point(row + direction, col + dc));
                    }
                }
            }
            return moves;
        }
    }

    /**
     * The Rook class with straight-line movement.
     */
    private class Rook extends Piece {
        private boolean hasMoved = false;

        public Rook(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♖" : "♜";
        }

        public boolean hasMoved() {
            return hasMoved;
        }

        public void setMoved(boolean moved) {
            this.hasMoved = moved;
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            if (row != newRow && col != newCol) {
                return false;
            }
            int stepX = Integer.compare(newCol, col);
            int stepY = Integer.compare(newRow, row);
            int currentX = col + stepX;
            int currentY = row + stepY;
            while (currentX != newCol || currentY != newRow) {
                if (grid[currentY][currentX] != null) {
                    return false;
                }
                currentX += stepX;
                currentY += stepY;
            }
            return true;
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            for (int[] dir : directions) {
                int r = row + dir[0];
                int c = col + dir[1];
                while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == null) {
                        moves.add(new Point(r, c));
                    } else {
                        if (grid[r][c].color != color) {
                            moves.add(new Point(r, c));
                        }
                        break;
                    }
                    r += dir[0];
                    c += dir[1];
                }
            }
            return moves;
        }
    }

    /**
     * The Knight class with L-shaped moves.
     */
    private class Knight extends Piece {

        public Knight(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♘" : "♞";
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            int dx = Math.abs(newCol - col);
            int dy = Math.abs(newRow - row);
            return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            int[][] offsets = {
                {2, 1}, {1, 2}, {-1, 2}, {-2, 1},
                {-2, -1}, {-1, -2}, {1, -2}, {2, -1}
            };
            for (int[] offset : offsets) {
                int r = row + offset[0];
                int c = col + offset[1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == null || grid[r][c].color != color) {
                        moves.add(new Point(r, c));
                    }
                }
            }
            return moves;
        }
    }

    /**
     * The Bishop class with diagonal moves.
     */
    private class Bishop extends Piece {

        public Bishop(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♗" : "♝";
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            if (Math.abs(newRow - row) != Math.abs(newCol - col)) {
                return false;
            }
            int stepX = Integer.compare(newCol, col);
            int stepY = Integer.compare(newRow, row);
            int currentX = col + stepX;
            int currentY = row + stepY;
            while (currentX != newCol || currentY != newRow) {
                if (grid[currentY][currentX] != null) {
                    return false;
                }
                currentX += stepX;
                currentY += stepY;
            }
            return true;
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] dir : directions) {
                int r = row + dir[0];
                int c = col + dir[1];
                while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == null) {
                        moves.add(new Point(r, c));
                    } else {
                        if (grid[r][c].color != color) {
                            moves.add(new Point(r, c));
                        }
                        break;
                    }
                    r += dir[0];
                    c += dir[1];
                }
            }
            return moves;
        }
    }

    /**
     * The Queen class that combines the moves of the Rook and Bishop.
     */
    private class Queen extends Piece {

        public Queen(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♕" : "♛";
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            return new Rook(row, col, color).isValidMove(newRow, newCol, grid)
                    || new Bishop(row, col, color).isValidMove(newRow, newCol, grid);
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            moves.addAll(new Rook(row, col, color).getValidMoves(grid));
            moves.addAll(new Bishop(row, col, color).getValidMoves(grid));
            return moves;
        }
    }

    /**
     * The King class with normal moves and castling logic.
     */
    private class King extends Piece {

        public King(int row, int col, int color) {
            super(row, col, color);
            symbol = color == 0 ? "♔" : "♚";
        }

        @Override
        public boolean isValidMove(int newRow, int newCol, Piece[][] grid) {
            // Regular one-square move.
            if (Math.abs(newRow - row) <= 1 && Math.abs(newCol - col) <= 1) {
                return true;
            }
            // Castling: king moves two squares horizontally.
            if (row == newRow && Math.abs(newCol - col) == 2) {
                boolean kingside = newCol > col;
                int rookCol = kingside ? 7 : 0;
                Piece rook = grid[row][rookCol];
                if (rook instanceof Rook && !((Rook) rook).hasMoved()
                        && !(color == 0 ? whiteKingMoved : blackKingMoved)) {
                    // Check that the squares between the king and rook are empty.
                    int step = kingside ? 1 : -1;
                    for (int c = col + step; c != rookCol; c += step) {
                        if (grid[row][c] != null) {
                            return false;
                        }
                    }
                    // (Optional improvement: also check that the king does not pass through check.)
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Point> getValidMoves(Piece[][] grid) {
            List<Point> moves = new ArrayList<>();
            int[][] offsets = {
                {1, 0}, {1, 1}, {0, 1}, {-1, 1},
                {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
            };
            for (int[] offset : offsets) {
                int r = row + offset[0];
                int c = col + offset[1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == null || grid[r][c].color != color) {
                        moves.add(new Point(r, c));
                    }
                }
            }
            // Castling moves.
            if (!(color == 0 ? whiteKingMoved : blackKingMoved)) {
                // Kingside castling.
                if (grid[row][5] == null && grid[row][6] == null
                        && grid[row][7] instanceof Rook && !((Rook) grid[row][7]).hasMoved()) {
                    moves.add(new Point(row, col + 2));
                }
                // Queenside castling.
                if (grid[row][1] == null && grid[row][2] == null && grid[row][3] == null
                        && grid[row][0] instanceof Rook && !((Rook) grid[row][0]).hasMoved()) {
                    moves.add(new Point(row, col - 2));
                }
            }
            return moves;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGame());
    }
}
