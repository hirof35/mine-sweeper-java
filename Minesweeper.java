package minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Minesweeper extends JFrame {
    private int gridRows, gridCols, mineCount;
    private int openedCells = 0;
    private int secondsPassed = 0;
    
    private JButton[][] buttons;
    private boolean[][] mines;
    private JLabel timerLabel;
    private JLabel bestScoreLabel;
    private JButton resetButton; // インスタンス変数化
    private JPanel boardPanel;   // レイアウト管理用
    private javax.swing.Timer gameTimer;
    private boolean gameStarted = false;
    private String currentDifficulty = "Beginner";

    public Minesweeper() {
        setTitle("Java Minesweeper Pro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        setupMenuBar();
        setupInfoPanel();
        
        // 初回起動
        initGameParams(9, 9, 10);
        buildBoard();
        
        setVisible(true);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("ゲーム設定");
        
        addMenuItem(gameMenu, "初級 (9x9)", () -> changeDifficulty(9, 9, 10, "Beginner"));
        addMenuItem(gameMenu, "中級 (16x16)", () -> changeDifficulty(16, 16, 40, "Intermediate"));
        addMenuItem(gameMenu, "上級 (16x30)", () -> changeDifficulty(16, 30, 99, "Expert"));
        
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    private void addMenuItem(JMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private void setupInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(1, 3));
        timerLabel = new JLabel("Time: 000", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        
        resetButton = new JButton("🙂");
        resetButton.setFont(new Font("SansSerif", Font.BOLD, 20));
        resetButton.addActionListener(e -> resetGame());

        bestScoreLabel = new JLabel("Best: ---", SwingConstants.CENTER);
        
        infoPanel.add(timerLabel);
        infoPanel.add(resetButton);
        infoPanel.add(bestScoreLabel);
        add(infoPanel, BorderLayout.NORTH);
    }

    private void changeDifficulty(int r, int c, int m, String diff) {
        currentDifficulty = diff;
        initGameParams(r, c, m);
        buildBoard();
    }

    private void initGameParams(int r, int c, int m) {
        this.gridRows = r;
        this.gridCols = c;
        this.mineCount = m;
    }

    // ボードの物理的な構築（難易度変更時のみ実行）
    private void buildBoard() {
        if (boardPanel != null) remove(boardPanel);
        
        boardPanel = new JPanel(new GridLayout(gridRows, gridCols));
        buttons = new JButton[gridRows][gridCols];
        mines = new boolean[gridRows][gridCols];

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(35, 35));
                btn.setFont(new Font("SansSerif", Font.BOLD, 14));
                btn.setMargin(new Insets(0, 0, 0, 0));
                
                int row = r, col = c;
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) resetButton.setText("😮");
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        resetButton.setText("🙂");
                        if (SwingUtilities.isRightMouseButton(e)) toggleFlag(row, col);
                        else if (SwingUtilities.isLeftMouseButton(e)) clickCell(row, col);
                    }
                });
                buttons[r][c] = btn;
                boardPanel.add(btn);
            }
        }
        add(boardPanel, BorderLayout.CENTER);
        resetGame();
        pack();
        setLocationRelativeTo(null);
    }

    private void resetGame() {
        stopTimer();
        secondsPassed = 0;
        openedCells = 0;
        gameStarted = false;
        timerLabel.setText("Time: 000");
        resetButton.setText("🙂");
        loadBestScore();

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                mines[r][c] = false;
                buttons[r][c].setEnabled(true);
                buttons[r][c].setText("");
                buttons[r][c].setBackground(null);
            }
        }
    }

    private void initializeMines(int firstR, int firstC) {
        Random rand = new Random();
        int placed = 0;
        while (placed < mineCount) {
            int r = rand.nextInt(gridRows);
            int c = rand.nextInt(gridCols);
            // 初手(firstR, firstC)とその周囲8マスには配置しない
            if (!mines[r][c] && (Math.abs(r - firstR) > 1 || Math.abs(c - firstC) > 1)) {
                mines[r][c] = true;
                placed++;
            }
        }
    }

    private void clickCell(int r, int c) {
        if (!buttons[r][c].isEnabled() || buttons[r][c].getText().equals("🚩")) return;

        if (!gameStarted) {
            initializeMines(r, c); // 初回クリック時に地雷を配置
            startTimer();
            gameStarted = true;
        }

        if (mines[r][c]) {
            gameOver();
        } else {
            revealCell(r, c);
            checkWin();
        }
    }

    private void revealCell(int r, int c) {
        if (r < 0 || r >= gridRows || c < 0 || c >= gridCols || !buttons[r][c].isEnabled()) return;

        buttons[r][c].setEnabled(false);
        buttons[r][c].setBackground(Color.LIGHT_GRAY);
        openedCells++;

        int count = countSurroundingMines(r, c);
        if (count > 0) {
            buttons[r][c].setText(String.valueOf(count));
            setNumberColor(buttons[r][c], count);
        } else {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    revealCell(r + dr, c + dc);
                }
            }
        }
    }

    private int countSurroundingMines(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < gridRows && nc >= 0 && nc < gridCols && mines[nr][nc]) count++;
            }
        }
        return count;
    }

    private void toggleFlag(int r, int c) {
        if (!gameStarted && !gameStarted) return; // 開始前はフラグ不可
        if (!buttons[r][c].isEnabled() && !buttons[r][c].getText().equals("🚩")) return;

        if (buttons[r][c].getText().equals("🚩")) {
            buttons[r][c].setText("");
            buttons[r][c].setForeground(null);
        } else {
            buttons[r][c].setText("🚩");
            buttons[r][c].setForeground(Color.RED);
        }
    }

    private void gameOver() {
        stopTimer();
        resetButton.setText("😵");
        List<JButton> mineList = new ArrayList<>();
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (mines[r][c]) mineList.add(buttons[r][c]);
                buttons[r][c].setEnabled(false);
            }
        }
        // 地雷を順番に表示する演出
        javax.swing.Timer explodeTimer = new javax.swing.Timer(30, null);
        explodeTimer.addActionListener(new ActionListener() {
            int i = 0;
            public void actionPerformed(ActionEvent e) {
                if (i < mineList.size()) {
                    JButton b = mineList.get(i);
                    b.setText("💣");
                    b.setBackground(Color.RED);
                    i++;
                } else {
                    explodeTimer.stop();
                    JOptionPane.showMessageDialog(Minesweeper.this, "Mission Failed...");
                }
            }
        });
        explodeTimer.start();
    }

    private void checkWin() {
        if (openedCells == (gridRows * gridCols) - mineCount) {
            stopTimer();
            resetButton.setText("😎");
            saveBestScore();
            JOptionPane.showMessageDialog(this, "Mission Clear!");
            for (JButton[] row : buttons) for (JButton btn : row) btn.setEnabled(false);
        }
    }

    private void startTimer() {
        gameTimer = new javax.swing.Timer(1000, e -> {
            secondsPassed++;
            if (secondsPassed < 1000) {
                timerLabel.setText(String.format("Time: %03d", secondsPassed));
            }
        });
        gameTimer.start();
    }

    private void stopTimer() { if (gameTimer != null) gameTimer.stop(); }

    private void loadBestScore() {
        Preferences prefs = Preferences.userNodeForPackage(Minesweeper.class);
        int best = prefs.getInt(currentDifficulty + "_best", 999);
        bestScoreLabel.setText("Best: " + (best == 999 ? "---" : String.format("%03d", best)));
    }

    private void saveBestScore() {
        Preferences prefs = Preferences.userNodeForPackage(Minesweeper.class);
        int currentBest = prefs.getInt(currentDifficulty + "_best", 999);
        if (secondsPassed < currentBest) {
            prefs.putInt(currentDifficulty + "_best", secondsPassed);
            loadBestScore();
        }
    }

    private void setNumberColor(JButton btn, int count) {
        Color[] colors = {Color.BLUE, new Color(0, 128, 0), Color.RED, new Color(0, 0, 128), new Color(128, 0, 0), new Color(0, 128, 128), Color.BLACK, Color.GRAY};
        btn.setForeground(count <= colors.length ? colors[count - 1] : Color.BLACK);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Minesweeper::new);
    }
}
