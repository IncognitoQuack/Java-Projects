import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class Main extends JFrame {

    // Components
    private JTextField taskField;
    private JTextField timeField;
    private JButton addButton;
    private JTable taskTable;
    private JButton deleteButton;
    private JButton completeButton;
    private JButton deleteCompletedButton;
    private JCheckBox timeToggle;
    private DefaultTableModel tableModel;
    private FadingLabel statusBar;

    public Main() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}

        // Set up the frame
        setTitle("Personal Task Manager");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up components
        taskField = new JTextField(25);
        timeField = new JTextField(16);
        timeField.setEnabled(false); 
        addButton = new JButton("Add Task");
        timeToggle = new JCheckBox("Set Time");
        timeToggle.setSelected(false);

        // Set up the date-time picker
        JButton timePickerButton = new JButton("📅");
        timePickerButton.setEnabled(false); 

        // Set up the table model with three columns
        tableModel = new DefaultTableModel(new Object[]{"Task", "Status", "Time"}, 0);
        taskTable = new JTable(tableModel);

        // Adjust table settings
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setRowHeight(30);
        taskTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taskTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));
        taskTable.getTableHeader().setBackground(new Color(0x4CAF50));
        taskTable.getTableHeader().setForeground(Color.WHITE);

        // Set up custom cell renderers
        taskTable.getColumnModel().getColumn(2).setCellRenderer(new TimeCellRenderer());
        taskTable.getColumnModel().getColumn(1).setCellRenderer(new StatusCellRenderer());

        // Set up buttons
        deleteButton = new JButton("Delete Task");
        completeButton = new JButton("Mark as Complete");
        deleteCompletedButton = new JButton("Delete Completed Tasks");

        JLabel signatureLabel = new JLabel("  © Encryptera ~ @cheetah  @sher");
        signatureLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signatureLabel.setForeground(Color.GRAY.brighter()); 
        signatureLabel.setCursor(Cursor.getDefaultCursor()); 

        signatureLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://encryptera.netlify.app/login"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        statusBar = new FadingLabel("Ready");
        statusBar.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(new EmptyBorder(12, 10, 12, 10)); 
        inputPanel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Task Label and Field
        gc.gridx = 0;
        gc.gridy = 0;
        inputPanel.add(new JLabel("New Task:"), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        inputPanel.add(taskField, gc);

        // Time Toggle
        gc.gridx = 2;
        gc.weightx = 0;
        inputPanel.add(timeToggle, gc);

        // Time Field and Picker
        gc.gridx = 3;
        inputPanel.add(timeField, gc);

        gc.gridx = 4;
        inputPanel.add(timePickerButton, gc);

        // Add Button
        gc.gridx = 5;
        inputPanel.add(addButton, gc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(deleteButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(deleteCompletedButton);

        // Bottom panel to hold buttons and signature
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(signatureLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        JScrollPane tableScrollPane = new JScrollPane(taskTable);

        // Main panel to hold all components
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add components to frame
        add(mainPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // Add action listeners
        addButton.addActionListener(e -> addTask());
        taskField.addActionListener(e -> addTask());
        timeField.addActionListener(e -> addTask());
        deleteButton.addActionListener(e -> deleteTask());
        completeButton.addActionListener(e -> completeTask());
        deleteCompletedButton.addActionListener(e -> deleteCompletedTasks());

        // Time Toggle action listener
        timeToggle.addActionListener(e -> {
            boolean isSelected = timeToggle.isSelected();
            timeField.setEnabled(isSelected);
            timePickerButton.setEnabled(isSelected);
            if (!isSelected) {
                timeField.setText("");
            }
        });

        // Time Picker Button action listener
        timePickerButton.addActionListener(e -> showDateTimePicker());

        // Double-click to edit
        taskTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && taskTable.getSelectedRow() != -1) {
                    editTask();
                }
            }
        });

        // Deselect task when clicking on empty area
        taskTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                if (row == -1) {
                    table.clearSelection();
                }
            }
        });

        // Deselect when clicking outside the table
        mainPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                taskTable.clearSelection();
            }
        });

        // Create a Timer to repaint the table periodically
        Timer timer = new Timer(10000, e -> taskTable.repaint()); // repaint every 10 seconds
        timer.start();
    }

    private void addTask() {
        String taskText = taskField.getText().trim();
        String timeText = timeField.getText().trim();
        boolean isTimeSet = timeToggle.isSelected();

        if (!taskText.isEmpty()) {
            if (isTimeSet) {
                // Validate time format
                if (isValidDateTime(timeText)) {
                    tableModel.addRow(new Object[]{taskText, "Incomplete", timeText});
                    taskField.setText("");
                    timeField.setText("");
                    statusBar.setTextWithFade("Task added.");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid date/time format. Please use YYYY-MM-DD HH:mm.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                tableModel.addRow(new Object[]{taskText, "Incomplete", "No time set"});
                taskField.setText("");
                statusBar.setTextWithFade("Task added.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Task description cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidDateTime(String dateTime) {
        try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow);
            statusBar.setTextWithFade("Task deleted.");
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completeTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.setValueAt("Complete", selectedRow, 1);
            statusBar.setTextWithFade("Task marked as complete.");
            taskTable.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to mark as complete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCompletedTasks() {
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            if ("Complete".equals(tableModel.getValueAt(i, 1))) {
                tableModel.removeRow(i);
            }
        }
        statusBar.setTextWithFade("All completed tasks deleted.");
    }

    private void editTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            String currentTask = tableModel.getValueAt(selectedRow, 0).toString();
            String newTask = JOptionPane.showInputDialog(this, "Edit Task", currentTask);
            if (newTask != null && !newTask.trim().isEmpty()) {
                tableModel.setValueAt(newTask.trim(), selectedRow, 0);
                statusBar.setTextWithFade("Task edited.");
            }

            String currentTime = tableModel.getValueAt(selectedRow, 2).toString();
            if (!"No time set".equals(currentTime)) {
                String newTime = JOptionPane.showInputDialog(this, "Edit Time (YYYY-MM-DD HH:mm)", currentTime);
                if (newTime != null && !newTime.trim().isEmpty()) {
                    if (isValidDateTime(newTime.trim())) {
                        tableModel.setValueAt(newTime.trim(), selectedRow, 2);
                        statusBar.setTextWithFade("Time edited.");
                        taskTable.repaint();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid date/time format. Please use YYYY-MM-DD HH:mm.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    // Custom cell renderer for the Time column
    class TimeCellRenderer extends DefaultTableCellRenderer {
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String timeStr = (String) value;
            String status = (String) table.getValueAt(row, 1);

            if ("Complete".equals(status)) {
                c.setForeground(Color.GRAY); // Time color for completed tasks
            } else {
                if (!"No time set".equals(timeStr)) {
                    try {
                        LocalDateTime taskTime = LocalDateTime.parse(timeStr, formatter);
                        LocalDateTime now = LocalDateTime.now();

                        if (now.isBefore(taskTime)) {
                            c.setForeground(Color.GREEN.darker());
                        } else {
                            c.setForeground(Color.RED);
                        }
                    } catch (DateTimeParseException e) {
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.GRAY);
                }
            }

            // Set background color for odd and even rows
            if (!isSelected) {
                if (row % 2 == 0) {
                    c.setBackground(new Color(0xFFFFFF)); // white
                } else {
                    c.setBackground(new Color(0xF0F0F0)); // light gray
                }
            } else {
                c.setBackground(table.getSelectionBackground());
            }

            return c;
        }
    }

    // Custom cell renderer for the Status column
    class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;

            if ("Complete".equals(status)) {
                label.setForeground(Color.BLACK);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setBorder(null); // Remove any border or effect
            } else {
                label.setForeground(Color.BLACK);
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
                label.setBorder(null);
            }

            // Set background color for odd and even rows
            if (!isSelected) {
                if (row % 2 == 0) {
                    label.setBackground(new Color(0xFFFFFF)); // white
                } else {
                    label.setBackground(new Color(0xF0F0F0)); // light gray
                }
            } else {
                label.setBackground(table.getSelectionBackground());
            }

            return label;
        }
    }

    private void showDateTimePicker() {
        DateTimePickerDialog dateTimePicker = new DateTimePickerDialog(this);
        String selectedDateTime = dateTimePicker.getSelectedDateTime();
        if (selectedDateTime != null) {
            timeField.setText(selectedDateTime);
        }
    }

    // DateTimePickerDialog class
    class DateTimePickerDialog extends JDialog {
        private LocalDateTime selectedDateTime;
        private boolean confirmed = false;

        public DateTimePickerDialog(Frame parent) {
            super(parent, "Select Date and Time", true);

            // Date picker
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd HH:mm"));

            // Buttons
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");

            okButton.addActionListener(e -> {
                Date date = ((SpinnerDateModel) dateSpinner.getModel()).getDate();
                selectedDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener(e -> dispose());

            // Layout
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(dateSpinner, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(panel);
            pack();
            setLocationRelativeTo(parent);
            setVisible(true);
        }

        public String getSelectedDateTime() {
            if (confirmed && selectedDateTime != null) {
                return selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            return null;
        }
    }

    // FadingLabel class for status bar
    class FadingLabel extends JLabel {
        private float alpha = 1.0f;
        private Timer fadeInTimer;
        private Timer fadeOutTimer;

        public FadingLabel(String text) {
            super(text);
        }

        public void setTextWithFade(String text) {
            setText(text);
            fadeIn();
        }

        private void fadeIn() {
            if (fadeOutTimer != null && fadeOutTimer.isRunning()) {
                fadeOutTimer.stop();
            }
            alpha = 0.0f;
            repaint();

            fadeInTimer = new Timer(50, e -> {
                alpha += 0.05f;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    fadeInTimer.stop();
                    // Start fade out after 5 seconds
                    new Timer(5000, ae -> fadeOut()).start();
                }
                repaint();
            });
            fadeInTimer.start();
        }

        private void fadeOut() {
            if (fadeInTimer != null && fadeInTimer.isRunning()) {
                fadeInTimer.stop();
            }

            fadeOutTimer = new Timer(50, e -> {
                alpha -= 0.05f;
                if (alpha <= 0.0f) {
                    alpha = 0.0f;
                    fadeOutTimer.stop();
                }
                repaint();
            });
            fadeOutTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintComponent(g2d);
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
