import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamSchedulingSystem extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);
    private final Map<String, JPanel> panels = new HashMap<>();

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/exam_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "nithu0428";
    private Connection connection;

    public ExamSchedulingSystem() {
        configureFrame();
        initializeDatabase();
        createPanels();
        showLoginScreen();
    }

    private void configureFrame() {
        setTitle("Exam Scheduling and Notifications System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(mainPanel);
    }

    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create courses table
            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "id VARCHAR(10) PRIMARY KEY, " +
                    "name VARCHAR(50) NOT NULL, " +
                    "credits INT NOT NULL)");

            // Create classrooms table
            stmt.execute("CREATE TABLE IF NOT EXISTS classrooms (" +
                    "id VARCHAR(10) PRIMARY KEY, " +
                    "name VARCHAR(50) NOT NULL, " +
                    "capacity INT NOT NULL, " +
                    "building VARCHAR(50) NOT NULL)");

            // Create exams table
            stmt.execute("CREATE TABLE IF NOT EXISTS exams (" +
                    "id VARCHAR(10) PRIMARY KEY, " +
                    "course_id VARCHAR(10) NOT NULL, " +
                    "classroom_id VARCHAR(10) NOT NULL, " +
                    "exam_date DATE NOT NULL, " +
                    "start_time TIME NOT NULL, " +
                    "end_time TIME NOT NULL, " +
                    "FOREIGN KEY (course_id) REFERENCES courses(id), " +
                    "FOREIGN KEY (classroom_id) REFERENCES classrooms(id))");

            // Create notifications table
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "exam_id VARCHAR(10) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "notification_date DATETIME NOT NULL, " +
                    "status VARCHAR(20) DEFAULT 'Pending', " +
                    "FOREIGN KEY (exam_id) REFERENCES exams(id))");
        }
    }

    private void createPanels() {
        panels.put("login", createLoginPanel());
        panels.put("dashboard", createDashboardPanel());
        panels.put("course", createCoursePanel());
        panels.put("classroom", createClassroomPanel());
        panels.put("exam", createExamPanel());
        panels.put("notification", createNotificationPanel());

        for (Map.Entry<String, JPanel> entry : panels.entrySet()) {
            mainPanel.add(entry.getValue(), entry.getKey());
        }
    }

    private JPanel createLoginPanel() {
        JPanel panel = new GradientPanel(new Color(44, 62, 80), new Color(52, 73, 94));
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        JLabel logo = new JLabel("Exam Scheduling System", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.BOLD, 36));
        logo.setForeground(Color.WHITE);
        panel.add(logo, gbc);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 15));
        form.setOpaque(false);

        JTextField username = new JTextField(15);
        JPasswordField password = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");

        form.add(new JLabel("Username:"));
        form.add(username);
        form.add(new JLabel("Password:"));
        form.add(password);
        form.add(new JLabel());
        form.add(loginBtn);

        loginBtn.addActionListener(e -> {
            if ("admin".equals(username.getText()) && "admin123".equals(new String(password.getPassword()))) {
                cardLayout.show(mainPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(panel, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(form, gbc);
        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new GradientPanel(new Color(52, 152, 219), new Color(41, 128, 185));
        panel.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Exam Scheduling Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        header.add(logout, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 3, 20, 20));
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] modules = {"Course", "Classroom", "Exam", "Notification"};
        String[] icons = {"📚", "🏫", "📝", "🔔"};

        for (int i = 0; i < modules.length; i++) {
            JPanel card = createCard(modules[i], icons[i]);
            final String panelName = modules[i].toLowerCase();
            card.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    cardLayout.show(mainPanel, panelName);
                    refreshTable(panelName);
                }
            });
            cards.add(card);
        }

        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCard(String title, String icon) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(255, 255, 255, 150));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(44, 62, 80));

        card.add(iconLbl, BorderLayout.CENTER);
        card.add(titleLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createCoursePanel() {
        JPanel panel = new GradientPanel(new Color(46, 204, 113), new Color(39, 174, 96));
        panel.setLayout(new BorderLayout());

        panel.add(createModuleHeader("Course Management"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addBtn = new JButton("Add Course");
        addBtn.addActionListener(e -> showCourseForm(null));
        toolbar.add(addBtn);

        JButton editBtn = new JButton("Edit Course");
        editBtn.addActionListener(e -> editSelectedCourse());
        toolbar.add(editBtn);

        JButton deleteBtn = new JButton("Delete Course");
        deleteBtn.addActionListener(e -> deleteSelectedCourse());
        toolbar.add(deleteBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable("course"));
        toolbar.add(refreshBtn);

        content.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Credits"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCourse();
                }
            }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        panel.add(createFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private void editSelectedCourse() {
        JTable table = getTableFromPanel(panels.get("course"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String courseId = (String) table.getValueAt(row, 0);
                try {
                    Course course = Course.getById(connection, courseId);
                    if (course != null) {
                        showCourseForm(course);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading course data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a course to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedCourse() {
        JTable table = getTableFromPanel(panels.get("course"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String courseId = (String) table.getValueAt(row, 0);
                String courseName = (String) table.getValueAt(row, 1);

                try {
                    // Check if course is referenced in exams
                    if (isCourseReferenced(courseId)) {
                        JOptionPane.showMessageDialog(this,
                                "Cannot delete course. It is referenced in exam records.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to delete course " + courseName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        if (Course.delete(connection, courseId)) {
                            refreshTable("course");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to delete course", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a course to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private boolean isCourseReferenced(String courseId) throws SQLException {
        String query = "SELECT COUNT(*) FROM exams WHERE course_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        }
        return false;
    }

    private JPanel createClassroomPanel() {
        JPanel panel = new GradientPanel(new Color(155, 89, 182), new Color(142, 68, 173));
        panel.setLayout(new BorderLayout());

        panel.add(createModuleHeader("Classroom Management"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addBtn = new JButton("Add Classroom");
        addBtn.addActionListener(e -> showClassroomForm(null));
        toolbar.add(addBtn);

        JButton editBtn = new JButton("Edit Classroom");
        editBtn.addActionListener(e -> editSelectedClassroom());
        toolbar.add(editBtn);

        JButton deleteBtn = new JButton("Delete Classroom");
        deleteBtn.addActionListener(e -> deleteSelectedClassroom());
        toolbar.add(deleteBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable("classroom"));
        toolbar.add(refreshBtn);

        content.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Building", "Capacity"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedClassroom();
                }
            }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        panel.add(createFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private void editSelectedClassroom() {
        JTable table = getTableFromPanel(panels.get("classroom"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String classroomId = (String) table.getValueAt(row, 0);
                try {
                    Classroom classroom = Classroom.getById(connection, classroomId);
                    if (classroom != null) {
                        showClassroomForm(classroom);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading classroom data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a classroom to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedClassroom() {
        JTable table = getTableFromPanel(panels.get("classroom"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String classroomId = (String) table.getValueAt(row, 0);
                String classroomName = (String) table.getValueAt(row, 1);

                try {
                    // Check if classroom is referenced in exams
                    if (isClassroomReferenced(classroomId)) {
                        JOptionPane.showMessageDialog(this,
                                "Cannot delete classroom. It is referenced in exam records.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to delete classroom " + classroomName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        if (Classroom.delete(connection, classroomId)) {
                            refreshTable("classroom");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to delete classroom", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a classroom to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private boolean isClassroomReferenced(String classroomId) throws SQLException {
        String query = "SELECT COUNT(*) FROM exams WHERE classroom_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, classroomId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        }
        return false;
    }

    private JPanel createExamPanel() {
        JPanel panel = new GradientPanel(new Color(52, 152, 219), new Color(41, 128, 185));
        panel.setLayout(new BorderLayout());

        panel.add(createModuleHeader("Exam Scheduling"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addBtn = new JButton("Schedule Exam");
        addBtn.addActionListener(e -> showExamForm(null));
        toolbar.add(addBtn);

        JButton editBtn = new JButton("Edit Exam");
        editBtn.addActionListener(e -> editSelectedExam());
        toolbar.add(editBtn);

        JButton deleteBtn = new JButton("Delete Exam");
        deleteBtn.addActionListener(e -> deleteSelectedExam());
        toolbar.add(deleteBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable("exam"));
        toolbar.add(refreshBtn);

        content.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Course", "Classroom", "Date", "Start Time", "End Time"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedExam();
                }
            }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        panel.add(createFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private void editSelectedExam() {
        JTable table = getTableFromPanel(panels.get("exam"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String examId = (String) table.getValueAt(row, 0);
                try {
                    Exam exam = Exam.getById(connection, examId);
                    if (exam != null) {
                        showExamForm(exam);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading exam data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an exam to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedExam() {
        JTable table = getTableFromPanel(panels.get("exam"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String examId = (String) table.getValueAt(row, 0);
                String courseName = (String) table.getValueAt(row, 1);

                try {
                    // Check if exam has notifications
                    if (hasNotifications(examId)) {
                        JOptionPane.showMessageDialog(this,
                                "Cannot delete exam. It has associated notifications.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Are you sure you want to delete exam for " + courseName + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        if (Exam.delete(connection, examId)) {
                            refreshTable("exam");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to delete exam", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an exam to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private boolean hasNotifications(String examId) throws SQLException {
        String query = "SELECT COUNT(*) FROM notifications WHERE exam_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, examId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;
        }
        return false;
    }

    private JPanel createNotificationPanel() {
        JPanel panel = new GradientPanel(new Color(230, 126, 34), new Color(211, 84, 0));
        panel.setLayout(new BorderLayout());

        panel.add(createModuleHeader("Notification Management"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JButton addBtn = new JButton("Add Notification");
        addBtn.addActionListener(e -> showNotificationForm(null));
        toolbar.add(addBtn);

        JButton editBtn = new JButton("Edit Notification");
        editBtn.addActionListener(e -> editSelectedNotification());
        toolbar.add(editBtn);

        JButton deleteBtn = new JButton("Delete Notification");
        deleteBtn.addActionListener(e -> deleteSelectedNotification());
        toolbar.add(deleteBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTable("notification"));
        toolbar.add(refreshBtn);

        JButton sendBtn = new JButton("Send Pending");
        sendBtn.addActionListener(e -> sendPendingNotifications());
        toolbar.add(sendBtn);

        content.add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Exam", "Message", "Scheduled Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedNotification();
                }
            }
        });

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        panel.add(createFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private void editSelectedNotification() {
        JTable table = getTableFromPanel(panels.get("notification"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int notificationId = (Integer) table.getValueAt(row, 0);
                try {
                    Notification notification = Notification.getById(connection, notificationId);
                    if (notification != null) {
                        showNotificationForm(notification);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading notification data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a notification to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedNotification() {
        JTable table = getTableFromPanel(panels.get("notification"));
        if (table != null) {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int notificationId = (Integer) table.getValueAt(row, 0);
                String examName = (String) table.getValueAt(row, 1);

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete notification for " + examName + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        if (Notification.delete(connection, notificationId)) {
                            refreshTable("notification");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to delete notification", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a notification to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void sendPendingNotifications() {
        try {
            List<Notification> pendingNotifications = Notification.getPendingNotifications(connection);
            if (pendingNotifications.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No pending notifications to send", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int count = 0;
            for (Notification notification : pendingNotifications) {
                if (notification.send(connection)) {
                    count++;
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Sent " + count + " of " + pendingNotifications.size() + " pending notifications",
                    "Notification Status", JOptionPane.INFORMATION_MESSAGE);

            refreshTable("notification");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error sending notifications: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createModuleHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        JButton backBtn = new JButton("Back to Dashboard");
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "dashboard"));
        header.add(backBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel status = new JLabel("Exam Scheduling System v1.0");
        status.setForeground(Color.WHITE);
        footer.add(status, BorderLayout.WEST);

        JButton settings = new JButton("Settings");
        settings.addActionListener(e -> showSettings());
        footer.add(settings, BorderLayout.EAST);

        return footer;
    }

    private void showSettings() {
        JOptionPane.showMessageDialog(this, "Settings would be implemented here", "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLoginScreen() {
        cardLayout.show(mainPanel, "login");
    }

    private JTable getTableFromPanel(JPanel panel) {
        if (panel == null) return null;

        for (Component comp : panel.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                return (JTable) scrollPane.getViewport().getView();
            }
            if (comp instanceof JPanel) {
                JTable table = getTableFromPanel((JPanel) comp);
                if (table != null) return table;
            }
        }
        return null;
    }

    private void showCourseForm(Course course) {
        JDialog dialog = new JDialog(this, course == null ? "Add Course" : "Edit Course", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField creditsField = new JTextField();

        if (course != null) {
            idField.setText(course.getId());
            nameField.setText(course.getName());
            creditsField.setText(String.valueOf(course.getCredits()));
            idField.setEditable(false);
        }

        form.add(new JLabel("Course ID:"));
        form.add(idField);
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Credits:"));
        form.add(creditsField);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(e -> {
            String id = idField.getText();
            String name = nameField.getText();
            String creditsStr = creditsField.getText();

            if (id.isEmpty() || name.isEmpty() || creditsStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int credits = Integer.parseInt(creditsStr);
                Course c = new Course(id, name, credits);
                if (c.save(connection)) {
                    refreshTable("course");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save course", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Credits must be a number", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(e -> dialog.dispose());

        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showClassroomForm(Classroom classroom) {
        JDialog dialog = new JDialog(this, classroom == null ? "Add Classroom" : "Edit Classroom", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField buildingField = new JTextField();
        JTextField capacityField = new JTextField();

        if (classroom != null) {
            idField.setText(classroom.getId());
            nameField.setText(classroom.getName());
            buildingField.setText(classroom.getBuilding());
            capacityField.setText(String.valueOf(classroom.getCapacity()));
            idField.setEditable(false);
        }

        form.add(new JLabel("Classroom ID:"));
        form.add(idField);
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Building:"));
        form.add(buildingField);
        form.add(new JLabel("Capacity:"));
        form.add(capacityField);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(e -> {
            String id = idField.getText();
            String name = nameField.getText();
            String building = buildingField.getText();
            String capacityStr = capacityField.getText();

            if (id.isEmpty() || name.isEmpty() || building.isEmpty() || capacityStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int capacity = Integer.parseInt(capacityStr);
                Classroom c = new Classroom(id, name, building, capacity);
                if (c.save(connection)) {
                    refreshTable("classroom");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save classroom", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Capacity must be a number", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(e -> dialog.dispose());

        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showExamForm(Exam exam) {
        JDialog dialog = new JDialog(this, exam == null ? "Schedule Exam" : "Edit Exam", true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(6, 2, 5, 5));

        JTextField idField = new JTextField();
        JComboBox<String> courseCombo = new JComboBox<>();
        JComboBox<String> classroomCombo = new JComboBox<>();
        JTextField dateField = new JTextField();
        JTextField startTimeField = new JTextField();
        JTextField endTimeField = new JTextField();

        try {
            List<Course> courses = Course.getAllCourses(connection);
            for (Course c : courses) {
                courseCombo.addItem(c.getName() + " (" + c.getId() + ")");
            }

            List<Classroom> classrooms = Classroom.getAllClassrooms(connection);
            for (Classroom c : classrooms) {
                classroomCombo.addItem(c.getName() + " (" + c.getId() + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (exam != null) {
            idField.setText(exam.getId());
            try {
                Course course = Course.getById(connection, exam.getCourseId());
                if (course != null) {
                    courseCombo.setSelectedItem(course.getName() + " (" + course.getId() + ")");
                }

                Classroom classroom = Classroom.getById(connection, exam.getClassroomId());
                if (classroom != null) {
                    classroomCombo.setSelectedItem(classroom.getName() + " (" + classroom.getId() + ")");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dateField.setText(exam.getExamDate().toString());
            startTimeField.setText(exam.getStartTime().toString());
            endTimeField.setText(exam.getEndTime().toString());
            idField.setEditable(false);
        }

        form.add(new JLabel("Exam ID:"));
        form.add(idField);
        form.add(new JLabel("Course:"));
        form.add(courseCombo);
        form.add(new JLabel("Classroom:"));
        form.add(classroomCombo);
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Start Time (HH:MM:SS):"));
        form.add(startTimeField);
        form.add(new JLabel("End Time (HH:MM:SS):"));
        form.add(endTimeField);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(e -> {
            String id = idField.getText();
            String courseSelection = (String) courseCombo.getSelectedItem();
            String classroomSelection = (String) classroomCombo.getSelectedItem();
            String date = dateField.getText();
            String startTime = startTimeField.getText();
            String endTime = endTimeField.getText();

            if (id.isEmpty() || courseSelection == null || classroomSelection == null ||
                    date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Extract course ID from selection
                String courseId = courseSelection.substring(courseSelection.indexOf("(") + 1, courseSelection.indexOf(")"));
                String classroomId = classroomSelection.substring(classroomSelection.indexOf("(") + 1, classroomSelection.indexOf(")"));

                Exam ex = new Exam(id, courseId, classroomId, date, startTime, endTime);
                if (ex.save(connection)) {
                    refreshTable("exam");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save exam", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date/time format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(e -> dialog.dispose());

        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showNotificationForm(Notification notification) {
        JDialog dialog = new JDialog(this, notification == null ? "Add Notification" : "Edit Notification", true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));

        JComboBox<String> examCombo = new JComboBox<>();
        JTextArea messageArea = new JTextArea(3, 20);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        JTextField dateField = new JTextField();
        JTextField timeField = new JTextField();

        try {
            List<Exam> exams = Exam.getAllExams(connection);
            for (Exam e : exams) {
                examCombo.addItem(e.getCourseName(connection) + " (" + e.getId() + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (notification != null) {
            try {
                Exam exam = Exam.getById(connection, notification.getExamId());
                if (exam != null) {
                    examCombo.setSelectedItem(exam.getCourseName(connection) + " (" + exam.getId() + ")");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            messageArea.setText(notification.getMessage());
            dateField.setText(notification.getNotificationDate().toString().substring(0, 10));
            timeField.setText(notification.getNotificationDate().toString().substring(11, 19));
        }

        form.add(new JLabel("Exam:"));
        form.add(examCombo);
        form.add(new JLabel("Message:"));
        form.add(messageScroll);
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Time (HH:MM:SS):"));
        form.add(timeField);
        form.add(new JLabel("(Leave empty for immediate notification)"));
        form.add(new JLabel());

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(e -> {
            String examSelection = (String) examCombo.getSelectedItem();
            String message = messageArea.getText();
            String date = dateField.getText();
            String time = timeField.getText();

            if (examSelection == null || message.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select an exam and enter a message", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Extract exam ID from selection
                String examId = examSelection.substring(examSelection.indexOf("(") + 1, examSelection.indexOf(")"));

                // Create notification date (use current time if not specified)
                String notificationDateTime;
                if (date.isEmpty() && time.isEmpty()) {
                    notificationDateTime = new java.sql.Timestamp(System.currentTimeMillis()).toString();
                } else if (!date.isEmpty() && !time.isEmpty()) {
                    notificationDateTime = date + " " + time;
                } else {
                    throw new IllegalArgumentException("Both date and time must be specified or both left empty");
                }

                Notification n;
                if (notification == null) {
                    n = new Notification(0, examId, message, notificationDateTime, "Pending");
                } else {
                    n = new Notification(notification.getId(), examId, message, notificationDateTime, notification.getStatus());
                }

                if (n.save(connection)) {
                    refreshTable("notification");
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save notification", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date/time format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancel.addActionListener(e -> dialog.dispose());

        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void refreshTable(String panelName) {
        try {
            JPanel panel = panels.get(panelName);
            JTable table = getTableFromPanel(panel);
            if (table != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);

                switch (panelName) {
                    case "course":
                        List<Course> courses = Course.getAllCourses(connection);
                        for (Course c : courses) {
                            model.addRow(new Object[]{c.getId(), c.getName(), c.getCredits()});
                        }
                        break;
                    case "classroom":
                        List<Classroom> classrooms = Classroom.getAllClassrooms(connection);
                        for (Classroom c : classrooms) {
                            model.addRow(new Object[]{c.getId(), c.getName(), c.getBuilding(), c.getCapacity()});
                        }
                        break;
                    case "exam":
                        List<Exam> exams = Exam.getAllExams(connection);
                        for (Exam e : exams) {
                            model.addRow(new Object[]{
                                    e.getId(),
                                    e.getCourseName(connection),
                                    e.getClassroomName(connection),
                                    e.getExamDate(),
                                    e.getStartTime(),
                                    e.getEndTime()
                            });
                        }
                        break;
                    case "notification":
                        List<Notification> notifications = Notification.getAllNotifications(connection);
                        for (Notification n : notifications) {
                            model.addRow(new Object[]{
                                    n.getId(),
                                    n.getExamName(connection),
                                    n.getMessage(),
                                    n.getNotificationDate(),
                                    n.getStatus()
                            });
                        }
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error refreshing data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                ExamSchedulingSystem frame = new ExamSchedulingSystem();
                frame.setVisible(true);

                // Add window listener to close database connection
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        try {
                            if (frame.connection != null && !frame.connection.isClosed()) {
                                frame.connection.close();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

class Course {
    private String id;
    private String name;
    private int credits;

    public Course(String id, String name, int credits) {
        this.id = id;
        this.name = name;
        this.credits = credits;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCredits() { return credits; }

    public void setName(String name) { this.name = name; }
    public void setCredits(int credits) { this.credits = credits; }

    // Database operations
    public static List<Course> getAllCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM courses";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                courses.add(new Course(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("credits")
                ));
            }
        }
        return courses;
    }

    public static Course getById(Connection conn, String id) throws SQLException {
        String query = "SELECT * FROM courses WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Course(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("credits")
                );
            }
        }
        return null;
    }

    public boolean save(Connection conn) throws SQLException {
        if (getById(conn, this.id) != null) {
            return update(conn);
        } else {
            return insert(conn);
        }
    }

    private boolean insert(Connection conn) throws SQLException {
        String query = "INSERT INTO courses (id, name, credits) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setInt(3, credits);

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean update(Connection conn) throws SQLException {
        String query = "UPDATE courses SET name = ?, credits = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, credits);
            pstmt.setString(3, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean delete(Connection conn, String id) throws SQLException {
        String query = "DELETE FROM courses WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

class Classroom {
    private String id;
    private String name;
    private String building;
    private int capacity;

    public Classroom(String id, String name, String building, int capacity) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBuilding() { return building; }
    public int getCapacity() { return capacity; }

    public void setName(String name) { this.name = name; }
    public void setBuilding(String building) { this.building = building; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    // Database operations
    public static List<Classroom> getAllClassrooms(Connection conn) throws SQLException {
        List<Classroom> classrooms = new ArrayList<>();
        String query = "SELECT * FROM classrooms";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                classrooms.add(new Classroom(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("building"),
                        rs.getInt("capacity")
                ));
            }
        }
        return classrooms;
    }

    public static Classroom getById(Connection conn, String id) throws SQLException {
        String query = "SELECT * FROM classrooms WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Classroom(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("building"),
                        rs.getInt("capacity")
                );
            }
        }
        return null;
    }

    public boolean save(Connection conn) throws SQLException {
        if (getById(conn, this.id) != null) {
            return update(conn);
        } else {
            return insert(conn);
        }
    }

    private boolean insert(Connection conn) throws SQLException {
        String query = "INSERT INTO classrooms (id, name, building, capacity) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, building);
            pstmt.setInt(4, capacity);

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean update(Connection conn) throws SQLException {
        String query = "UPDATE classrooms SET name = ?, building = ?, capacity = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, building);
            pstmt.setInt(3, capacity);
            pstmt.setString(4, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean delete(Connection conn, String id) throws SQLException {
        String query = "DELETE FROM classrooms WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

class Exam {
    private String id;
    private String courseId;
    private String classroomId;
    private Date examDate;
    private Time startTime;
    private Time endTime;

    public Exam(String id, String courseId, String classroomId, String examDate, String startTime, String endTime) {
        this.id = id;
        this.courseId = courseId;
        this.classroomId = classroomId;
        this.examDate = Date.valueOf(examDate);
        this.startTime = Time.valueOf(startTime);
        this.endTime = Time.valueOf(endTime);
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getClassroomId() { return classroomId; }
    public Date getExamDate() { return examDate; }
    public Time getStartTime() { return startTime; }
    public Time getEndTime() { return endTime; }

    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }
    public void setStartTime(Time startTime) { this.startTime = startTime; }
    public void setEndTime(Time endTime) { this.endTime = endTime; }

    // Database operations
    public static List<Exam> getAllExams(Connection conn) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String query = "SELECT * FROM exams";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                exams.add(new Exam(
                        rs.getString("id"),
                        rs.getString("course_id"),
                        rs.getString("classroom_id"),
                        rs.getDate("exam_date").toString(),
                        rs.getTime("start_time").toString(),
                        rs.getTime("end_time").toString()
                ));
            }
        }
        return exams;
    }

    public static Exam getById(Connection conn, String id) throws SQLException {
        String query = "SELECT * FROM exams WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Exam(
                        rs.getString("id"),
                        rs.getString("course_id"),
                        rs.getString("classroom_id"),
                        rs.getDate("exam_date").toString(),
                        rs.getTime("start_time").toString(),
                        rs.getTime("end_time").toString()
                );
            }
        }
        return null;
    }

    public String getCourseName(Connection conn) throws SQLException {
        Course course = Course.getById(conn, courseId);
        return course != null ? course.getName() : "Unknown Course";
    }

    public String getClassroomName(Connection conn) throws SQLException {
        Classroom classroom = Classroom.getById(conn, classroomId);
        return classroom != null ? classroom.getName() : "Unknown Classroom";
    }

    public boolean save(Connection conn) throws SQLException {
        if (getById(conn, this.id) != null) {
            return update(conn);
        } else {
            return insert(conn);
        }
    }

    private boolean insert(Connection conn) throws SQLException {
        String query = "INSERT INTO exams (id, course_id, classroom_id, exam_date, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            pstmt.setString(2, courseId);
            pstmt.setString(3, classroomId);
            pstmt.setDate(4, examDate);
            pstmt.setTime(5, startTime);
            pstmt.setTime(6, endTime);

            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean update(Connection conn) throws SQLException {
        String query = "UPDATE exams SET course_id = ?, classroom_id = ?, exam_date = ?, " +
                "start_time = ?, end_time = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, classroomId);
            pstmt.setDate(3, examDate);
            pstmt.setTime(4, startTime);
            pstmt.setTime(5, endTime);
            pstmt.setString(6, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean delete(Connection conn, String id) throws SQLException {
        String query = "DELETE FROM exams WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

class Notification {
    private int id;
    private String examId;
    private String message;
    private Timestamp notificationDate;
    private String status;

    public Notification(int id, String examId, String message, String notificationDate, String status) {
        this.id = id;
        this.examId = examId;
        this.message = message;
        this.notificationDate = Timestamp.valueOf(notificationDate);
        this.status = status;
    }

    public int getId() { return id; }
    public String getExamId() { return examId; }
    public String getMessage() { return message; }
    public Timestamp getNotificationDate() { return notificationDate; }
    public String getStatus() { return status; }

    public void setExamId(String examId) { this.examId = examId; }
    public void setMessage(String message) { this.message = message; }
    public void setNotificationDate(Timestamp notificationDate) { this.notificationDate = notificationDate; }
    public void setStatus(String status) { this.status = status; }

    // Database operations
    public static List<Notification> getAllNotifications(Connection conn) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT * FROM notifications";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                notifications.add(new Notification(
                        rs.getInt("id"),
                        rs.getString("exam_id"),
                        rs.getString("message"),
                        rs.getTimestamp("notification_date").toString(),
                        rs.getString("status")
                ));
            }
        }
        return notifications;
    }

    public static List<Notification> getPendingNotifications(Connection conn) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT * FROM notifications WHERE status = 'Pending' AND notification_date <= NOW()";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                notifications.add(new Notification(
                        rs.getInt("id"),
                        rs.getString("exam_id"),
                        rs.getString("message"),
                        rs.getTimestamp("notification_date").toString(),
                        rs.getString("status")
                ));
            }
        }
        return notifications;
    }

    public static Notification getById(Connection conn, int id) throws SQLException {
        String query = "SELECT * FROM notifications WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Notification(
                        rs.getInt("id"),
                        rs.getString("exam_id"),
                        rs.getString("message"),
                        rs.getTimestamp("notification_date").toString(),
                        rs.getString("status")
                );
            }
        }
        return null;
    }

    public String getExamName(Connection conn) throws SQLException {
        Exam exam = Exam.getById(conn, examId);
        return exam != null ? exam.getCourseName(conn) : "Unknown Exam";
    }

    public boolean save(Connection conn) throws SQLException {
        if (id == 0) {
            return insert(conn);
        } else {
            return update(conn);
        }
    }

    private boolean insert(Connection conn) throws SQLException {
        String query = "INSERT INTO notifications (exam_id, message, notification_date, status) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, examId);
            pstmt.setString(2, message);
            pstmt.setTimestamp(3, notificationDate);
            pstmt.setString(4, status);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        this.id = rs.getInt(1);
                    }
                }
                return true;
            }
            return false;
        }
    }

    private boolean update(Connection conn) throws SQLException {
        String query = "UPDATE notifications SET exam_id = ?, message = ?, " +
                "notification_date = ?, status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, examId);
            pstmt.setString(2, message);
            pstmt.setTimestamp(3, notificationDate);
            pstmt.setString(4, status);
            pstmt.setInt(5, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean send(Connection conn) throws SQLException {
        // In a real system, this would send the notification (email, SMS, etc.)
        // For this example, we'll just update the status in the database

        String query = "UPDATE notifications SET status = 'Sent' WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            if (pstmt.executeUpdate() > 0) {
                this.status = "Sent";
                return true;
            }
            return false;
        }
    }

    public static boolean delete(Connection conn, int id) throws SQLException {
        String query = "DELETE FROM notifications WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

class GradientPanel extends JPanel {
    private Color color1;
    private Color color2;

    public GradientPanel(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int w = getWidth();
        int h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
    }
}