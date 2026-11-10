import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

abstract class Person {

    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }

    public abstract String getDetails();
}

class Student extends Person {

    private String studentID;
    private String course;
    private String yearLevel;

    public Student(String studentID, String name, int age,
                   String course, String yearLevel) {
        super(name, age);
        this.studentID = studentID;
        this.course = course;
        this.yearLevel = yearLevel;
    }

    public String getStudentID() { return studentID; }
    public String getCourse() { return course; }
    public String getYearLevel() { return yearLevel; }
    public void setCourse(String course) { this.course = course; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    @Override
    public String getDetails() {
        return studentID + " - " + getName() + " - " + course;
    }
}

class StudentManager {

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, name, age, course, year_level) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getStudentID());
            ps.setString(2, student.getName());
            ps.setInt(3, student.getAge());
            ps.setString(4, student.getCourse());
            ps.setString(5, student.getYearLevel());
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (add):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean updateStudent(String id, String name, int age,
                                 String course, String yearLevel) {
        String sql = "UPDATE students SET name=?, age=?, course=?, year_level=? " +
                     "WHERE student_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, course);
            ps.setString(4, yearLevel);
            ps.setString(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (update):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean deleteStudent(String id) {
        String sql = "DELETE FROM students WHERE student_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (delete):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public ArrayList<Student> searchStudents(String keyword) {
        ArrayList<Student> results = new ArrayList<>();
        keyword = keyword.trim();
        if (keyword.isEmpty()) return results;

        String exactSql = "SELECT * FROM students WHERE student_id = ?";
        String likeSql  = "SELECT * FROM students " +
                          "WHERE LOWER(student_id) LIKE ? " +
                          "   OR LOWER(name)       LIKE ? " +
                          "   OR LOWER(course)     LIKE ?";

        try (Connection conn = DBConnection.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(exactSql)) {
                ps.setString(1, keyword);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        results.add(mapRow(rs));
                        return results;
                    }
                }
            }

            String like = "%" + keyword.toLowerCase() + "%";
            try (PreparedStatement ps = conn.prepareStatement(likeSql)) {
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) results.add(mapRow(rs));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (search):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return results;
    }

    public ArrayList<Student> getAllStudents() {
        ArrayList<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY student_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (load):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return list;
    }

    public ArrayList<Student> sortStudents() {
        ArrayList<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY LOWER(name) ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database error (sort):\n" + ex.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return list;
    }

    public int getTotalStudents() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
            rs.getString("student_id"),
            rs.getString("name"),
            rs.getInt("age"),
            rs.getString("course"),
            rs.getString("year_level")
        );
    }
}

class StudentGUI extends JFrame {

    private StudentManager manager;
    private JTextField idField, nameField, ageField, courseField, yearField, searchField;
    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLabel;

    public StudentGUI() {

        manager = new StudentManager();

        setTitle("Student Management System");
        setSize(1050, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 45, 85));
        header.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("STUDENT MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        totalLabel = new JLabel("Total Students: 0");
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        header.add(title, BorderLayout.WEST);
        header.add(totalLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(240, 244, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 8, 12));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Student Information"));

        idField     = new JTextField();
        nameField   = new JTextField();
        ageField    = new JTextField();
        courseField = new JTextField();
        yearField   = new JTextField();

        formPanel.add(new JLabel("Student ID:")); formPanel.add(idField);
        formPanel.add(new JLabel("Full Name:"));  formPanel.add(nameField);
        formPanel.add(new JLabel("Age:"));        formPanel.add(ageField);
        formPanel.add(new JLabel("Course:"));     formPanel.add(courseField);
        formPanel.add(new JLabel("Year Level:")); formPanel.add(yearField);

        JButton addButton    = new JButton("Add Student");
        JButton updateButton = new JButton("Update Student");
        formPanel.add(addButton);
        formPanel.add(updateButton);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setBackground(Color.WHITE);

        JButton deleteButton  = new JButton("Delete Student");
        JButton clearButton   = new JButton("Clear Fields");
        JButton sortButton    = new JButton("Sort by Name");
        JButton refreshButton = new JButton("Show All");

        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(sortButton);
        buttonPanel.add(refreshButton);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(new Color(240, 244, 250));
        leftPanel.add(formPanel, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Students"));

        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        String[] columns = {"Student ID", "Full Name", "Age", "Course", "Year Level"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(240, 244, 250));
        rightPanel.add(searchPanel, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.38);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        clearButton.addActionListener(e -> clearFields());
        searchButton.addActionListener(e -> searchStudent());
        sortButton.addActionListener(e -> displayStudents(manager.sortStudents()));
        refreshButton.addActionListener(e -> displayStudents(manager.getAllStudents()));
        searchField.addActionListener(e -> searchStudent());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                idField.setText(model.getValueAt(row, 0).toString());
                nameField.setText(model.getValueAt(row, 1).toString());
                ageField.setText(model.getValueAt(row, 2).toString());
                courseField.setText(model.getValueAt(row, 3).toString());
                yearField.setText(model.getValueAt(row, 4).toString());
            }
        });

        // Load existing rows from the DB at startup
        displayStudents(manager.getAllStudents());
    }

    private void addStudent() {
        try {
            String id     = idField.getText().trim();
            String name   = nameField.getText().trim();
            String course = courseField.getText().trim();
            String year   = yearField.getText().trim();

            if (id.isEmpty() || name.isEmpty()
                    || ageField.getText().trim().isEmpty()
                    || course.isEmpty() || year.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please complete all fields!");
                return;
            }

            int age = Integer.parseInt(ageField.getText().trim());
            if (age <= 0 || age > 120) {
                JOptionPane.showMessageDialog(this, "Please enter a valid age!");
                return;
            }

            Student student = new Student(id, name, age, course, year);

            if (manager.addStudent(student)) {
                JOptionPane.showMessageDialog(this, "Student added successfully!");
                displayStudents(manager.getAllStudents());
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Student ID already exists!");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be a valid number!");
        }
    }

    private void updateStudent() {
        try {
            String id     = idField.getText().trim();
            String name   = nameField.getText().trim();
            String course = courseField.getText().trim();
            String year   = yearField.getText().trim();

            if (id.isEmpty() || name.isEmpty()
                    || ageField.getText().trim().isEmpty()
                    || course.isEmpty() || year.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please complete all fields!");
                return;
            }

            int age = Integer.parseInt(ageField.getText().trim());
            if (age <= 0 || age > 120) {
                JOptionPane.showMessageDialog(this, "Please enter a valid age!");
                return;
            }

            if (manager.updateStudent(id, name, age, course, year)) {
                JOptionPane.showMessageDialog(this, "Student updated successfully!");
                displayStudents(manager.getAllStudents());
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Student not found!");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be a valid number!");
        }
    }

    private void deleteStudent() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select a Student ID!");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this student?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (manager.deleteStudent(id)) {
                JOptionPane.showMessageDialog(this, "Student deleted successfully!");
                displayStudents(manager.getAllStudents());
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Student not found!");
            }
        }
    }

    private void searchStudent() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search keyword!");
            return;
        }
        ArrayList<Student> results = manager.searchStudents(keyword);
        displayStudents(results);
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No student found!");
        }
    }

    private void displayStudents(ArrayList<Student> students) {
        model.setRowCount(0);
        for (Student s : students) {
            model.addRow(new Object[]{
                s.getStudentID(),
                s.getName(),
                s.getAge(),
                s.getCourse(),
                s.getYearLevel()
            });
        }
        totalLabel.setText("Total Students: " + manager.getTotalStudents());
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        ageField.setText("");
        courseField.setText("");
        yearField.setText("");
        searchField.setText("");
        table.clearSelection();
    }
}

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StudentGUI gui = new StudentGUI();
            gui.setVisible(true);
        });
    }
}
