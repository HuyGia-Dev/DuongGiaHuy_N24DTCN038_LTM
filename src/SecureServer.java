import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Secure TCP Server với giao diện Java Swing
 * - Sử dụng SSLServerSocket (bảo mật Socket)
 * - Multithread: mỗi client được xử lý trong 1 thread riêng
 * - Nhận chuỗi từ client, chuyển thành chữ in hoa, gửi trả lại
 */
public class SecureServer extends JFrame {

    private JTextArea logArea;
    private JTextField portField;
    private JButton startButton, stopButton;
    private JLabel statusLabel, clientCountLabel;
    private SSLServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private int clientCount = 0;
    private Thread serverThread;

    public SecureServer() {
        initUI();
    }

    private void initUI() {
        setTitle("🔒 Secure TCP Server (SSL/TLS)");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // === Panel điều khiển phía trên ===
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Cấu hình Server"));

        controlPanel.add(new JLabel("Port:"));
        portField = new JTextField("9999", 6);
        controlPanel.add(portField);

        startButton = new JButton("▶ Khởi động");
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        controlPanel.add(startButton);

        stopButton = new JButton("⏹ Dừng");
        stopButton.setBackground(new Color(231, 76, 60));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        controlPanel.add(stopButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // === Khu vực log ===
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));
        logArea.setCaretColor(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký Server"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // === Panel trạng thái phía dưới ===
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Trạng thái"));
        statusLabel = new JLabel("⚪ Chưa khởi động");
        clientCountLabel = new JLabel("👥 Clients đang kết nối: 0");
        statusPanel.add(statusLabel);
        statusPanel.add(clientCountLabel);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // === Sự kiện ===
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
    }

    /**
     * Khởi động server SSL trên port được chỉ định
     */
    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        portField.setEnabled(false);

        serverThread = new Thread(() -> {
            try {
                // Tạo SSLServerSocket với keystore
                SSLServerSocketFactory factory = createSSLServerSocketFactory();
                serverSocket = (SSLServerSocket) factory.createServerSocket(port);
                isRunning = true;

                updateStatus("🟢 Đang chạy trên port " + port + " (SSL/TLS)");
                appendLog("✅ Server SSL đã khởi động trên port " + port);
                appendLog("🔐 Giao thức bảo mật: TLS");
                appendLog("⏳ Đang chờ kết nối từ client...");

                while (isRunning) {
                    try {
                        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                        clientCount++;
                        updateClientCount();

                        String clientInfo = clientSocket.getInetAddress().getHostAddress()
                                + ":" + clientSocket.getPort();
                        appendLog("🔗 Client mới kết nối: " + clientInfo
                                + " (SSL Session: " + clientSocket.getSession().getProtocol() + ")");

                        // Multithread: tạo thread riêng cho mỗi client
                        new ClientHandler(clientSocket, clientInfo).start();

                    } catch (IOException e) {
                        if (isRunning) {
                            appendLog("❌ Lỗi chấp nhận kết nối: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                appendLog("❌ Lỗi khởi động server: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    portField.setEnabled(true);
                    updateStatus("🔴 Lỗi");
                });
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Dừng server
     */
    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            appendLog("⚠ Lỗi khi đóng server: " + e.getMessage());
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEnabled(true);
        updateStatus("🔴 Đã dừng");
        appendLog("🛑 Server đã dừng.");
    }

    /**
     * Tạo SSLServerSocketFactory từ keystore
     */
    private SSLServerSocketFactory createSSLServerSocketFactory() throws Exception {
        char[] password = "changeit".toCharArray();

        // Load keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("serverkeystore.jks")) {
            keyStore.load(fis, password);
        }

        // Khởi tạo KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        // Khởi tạo TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Tạo SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext.getServerSocketFactory();
    }

    /**
     * Thread xử lý từng client (Multithread)
     */
    private class ClientHandler extends Thread {
        private SSLSocket socket;
        private String clientInfo;

        public ClientHandler(SSLSocket socket, String clientInfo) {
            this.socket = socket;
            this.clientInfo = clientInfo;
            setDaemon(true);
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    appendLog("📩 [" + clientInfo + "] Nhận: \"" + inputLine + "\"");

                    // Chuyển chuỗi thành chữ IN HOA
                    String upperCase = inputLine.toUpperCase();

                    // Gửi kết quả trả về client
                    out.println(upperCase);
                    appendLog("📤 [" + clientInfo + "] Gửi:  \"" + upperCase + "\"");
                }
            } catch (IOException e) {
                appendLog("⚠ [" + clientInfo + "] Ngắt kết nối: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) { /* ignore */ }
                clientCount--;
                updateClientCount();
                appendLog("❌ [" + clientInfo + "] Đã ngắt kết nối.");
            }
        }
    }

    // === Các phương thức tiện ích cập nhật UI ===

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    private void updateClientCount() {
        SwingUtilities.invokeLater(() ->
            clientCountLabel.setText("👥 Clients đang kết nối: " + clientCount));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { /* fallback */ }
            new SecureServer().setVisible(true);
        });
    }
}
