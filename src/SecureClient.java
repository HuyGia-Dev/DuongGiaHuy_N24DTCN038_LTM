import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Secure TCP Client với giao diện Java Swing
 * - Sử dụng SSLSocket (bảo mật Socket)
 * - Gửi chuỗi đến server, nhận kết quả chữ in hoa
 */
public class SecureClient extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField, hostField, portField;
    private JButton connectButton, disconnectButton, sendButton;
    private JLabel statusLabel;

    private SSLSocket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean isConnected = false;

    public SecureClient() {
        initUI();
    }

    private void initUI() {
        setTitle("🔒 Secure TCP Client (SSL/TLS)");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // === Panel kết nối phía trên ===
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        connectPanel.setBorder(BorderFactory.createTitledBorder("Kết nối đến Server"));

        connectPanel.add(new JLabel("Host:"));
        hostField = new JTextField("localhost", 10);
        connectPanel.add(hostField);

        connectPanel.add(new JLabel("Port:"));
        portField = new JTextField("9999", 5);
        connectPanel.add(portField);

        connectButton = new JButton("🔗 Kết nối");
        connectButton.setBackground(new Color(46, 204, 113));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectPanel.add(connectButton);

        disconnectButton = new JButton("❌ Ngắt");
        disconnectButton.setBackground(new Color(231, 76, 60));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setEnabled(false);
        connectPanel.add(disconnectButton);

        mainPanel.add(connectPanel, BorderLayout.NORTH);

        // === Khu vực hiển thị kết quả ===
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(0, 200, 255));
        chatArea.setCaretColor(Color.CYAN);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Kết quả giao tiếp"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // === Panel nhập liệu phía dưới ===
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Nhập chuỗi gửi đến Server"));

        inputField = new JTextField();
        inputField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputField.setEnabled(false);
        inputPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("📤 Gửi");
        sendButton.setBackground(new Color(52, 152, 219));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setEnabled(false);
        inputPanel.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        // Trạng thái
        statusLabel = new JLabel("⚪ Chưa kết nối");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // === Sự kiện ===
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage()); // Nhấn Enter để gửi
    }

    /**
     * Kết nối đến server qua SSL/TLS
     */
    private void connect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                SSLSocketFactory factory = createSSLSocketFactory();
                socket = (SSLSocket) factory.createSocket(host, port);
                socket.startHandshake(); // Bắt đầu SSL handshake

                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                isConnected = true;

                SwingUtilities.invokeLater(() -> {
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    hostField.setEnabled(false);
                    portField.setEnabled(false);
                    statusLabel.setText("🟢 Đã kết nối đến " + host + ":" + port + " (SSL/TLS)");
                    inputField.requestFocus();
                });

                appendChat("✅ Đã kết nối bảo mật đến " + host + ":" + port);
                appendChat("🔐 Giao thức: " + socket.getSession().getProtocol());
                appendChat("📝 Nhập chuỗi và nhấn Gửi để chuyển đổi thành chữ IN HOA\n");

                // Lắng nghe phản hồi từ server
                String response;
                while (isConnected && (response = in.readLine()) != null) {
                    appendChat("📥 Kết quả từ Server: \"" + response + "\"");
                }
            } catch (Exception e) {
                appendChat("❌ Lỗi kết nối: " + e.getMessage());
                SwingUtilities.invokeLater(() -> resetUI());
            }
        }).start();
    }

    /**
     * Ngắt kết nối
     */
    private void disconnect() {
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) { /* ignore */ }

        appendChat("🛑 Đã ngắt kết nối.\n");
        resetUI();
    }

    /**
     * Gửi chuỗi đến server
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || !isConnected) return;

        appendChat("📤 Gửi đến Server: \"" + message + "\"");
        out.println(message);
        inputField.setText("");
        inputField.requestFocus();
    }

    /**
     * Tạo SSLSocketFactory từ truststore
     */
    private SSLSocketFactory createSSLSocketFactory() throws Exception {
        char[] password = "changeit".toCharArray();

        // Load truststore (chứa certificate của server)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("clienttruststore.jks")) {
            trustStore.load(fis, password);
        }

        // Khởi tạo TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Tạo SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }

    // === Các phương thức tiện ích ===

    private void appendChat(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + time + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void resetUI() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
            hostField.setEnabled(true);
            portField.setEnabled(true);
            statusLabel.setText("🔴 Đã ngắt kết nối");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { /* fallback */ }
            new SecureClient().setVisible(true);
        });
    }
}
