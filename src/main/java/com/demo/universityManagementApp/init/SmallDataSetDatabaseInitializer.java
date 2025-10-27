package com.demo.universityManagementApp.init;

import com.demo.universityManagementApp.repository.*;
import com.demo.universityManagementApp.repository.entity.ClassSession;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.TimeSlot;
import com.demo.universityManagementApp.repository.entity.Lecturer;
import com.demo.universityManagementApp.repository.entity.Student;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static com.demo.universityManagementApp.util.Helper.Generate.generateLecturerEmail;
import static com.demo.universityManagementApp.util.Helper.Generate.generateStudentEmail;

/**
 * Test profile-specific Spring component responsible for generating and populating
 * a small-scale, deterministic dataset for integration, functional, and developer testing purposes.
 * <p>
 * This initializer leverages Spring Data JPA repositories to insert a limited number of entities,
 * including departments, students, lecturers, courses, and class sessions, with predefined
 * relationships and fixed data for predictable testing outcomes.
 * <p>
 * Key responsibilities include:
 * <ul>
 *     <li>Flushing all existing data in reverse dependency order to maintain referential integrity.</li>
 *     <li>Creating a small number of departments with hardcoded codes and external IDs.</li>
 *     <li>Creating a set of predefined students with explicit names, enrollment years, phone numbers, and department assignments.</li>
 *     <li>Creating lecturers with fixed names, phone numbers, and department assignments, while generating unique emails.</li>
 *     <li>Creating courses with predefined names and codes, associating them with departments, and assigning lecturers.</li>
 *     <li>Creating class sessions for courses with specific time slots, locations, maximum capacities, and lecturer assignments.</li>
 *     <li>Enrolling students into class sessions randomly within capacity constraints while avoiding duplicate enrollments.</li>
 *     <li>Generating unique external IDs for students, lecturers, departments, courses, and class sessions using incremental logic based on repository counts.</li>
 * </ul>
 * <p>
 * Unlike the large dataset initializer, this component produces deterministic, small datasets
 * suitable for unit testing, developer sandboxing, and functional tests that require predictable
 * entity relationships and counts.
 * <p>
 * All operations are transactional to guarantee atomicity of inserts and updates,
 * preserving consistency across interdependent entities.
 * <p>
 * Usage is restricted to test environments (annotated with {@code @Profile("test")})
 * and should not be used in production.
 */
@Profile("test")
@Component
@RequiredArgsConstructor
@Slf4j
public class SmallDataSetDatabaseInitializer {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository classSessionRepository;

    @Transactional
    public void run(String... args) throws Exception {
        log.info("Running Small-Dataset DatabaseInitializer...");

        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose an option to initialize data in the database: ");
        System.out.println("1. Delete all data");
        System.out.println("2. Populate database");
        System.out.println("3. Both (recommended)");
        System.out.print("Enter your choice (1-3): ");
        String input = scanner.nextLine();

        switch (input) {
            case "1", "delete" -> flushAll();
            case "2", "populate" -> initializeData();
            case "3", "both" -> {
                flushAll();
                initializeData();
            }
            default -> System.out.println("Skipping database initialization.");
        }
    }

    @Transactional
    public void flushAll() {
        log.info("Deleting all data in reverse dependency order...");

        classSessionRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        lecturerRepository.deleteAll();
        departmentRepository.deleteAll();

        log.info("All data deleted successfully!");
    }

    @Transactional
    public void initializeData() {
        try {
            Department csDept = createDepartment("Computer Science", "CS");
            Department mathDept = createDepartment("Mathematics", "MATH");
            Department engineeringDept = createDepartment("Engineering", "ENG");

            Student student1 = createStudent("Sanad Anwar Jarrad", "0771234567", csDept, 2024);
            Student student2 = createStudent("Hamza Haneifi", "0782345678", mathDept, 2023);
            Student student3 = createStudent("Student Three Example", "0793456789", csDept, 2025);
            Student student4 = createStudent("Student Four Example", "0774567890", csDept, 2022);
            Student student5 = createStudent("Student Five Example", "0785678901", engineeringDept, 2023);
            List<Student> students = List.of(student1, student2, student3, student4, student5);

            Lecturer lecturer1 = createLecturer("Azzam Msameh", "0771111111", csDept);
            Lecturer lecturer2 = createLecturer("Walid Sharaiyra", "0782222222", csDept);
            Lecturer lecturer3 = createLecturer("Omar Ismail Lozi", "0793333333", csDept);
            Lecturer lecturer4 = createLecturer("Lecturer Four Example", "0774444444", engineeringDept);
            List<Lecturer> lecturers = List.of(lecturer1, lecturer2, lecturer3, lecturer4);

            Course javaCourse = createCourse("OOP Java Programming", "CS101", csDept);
            Course algorithmsCourse = createCourse("Algorithms and Data Structures", "CS201", csDept);
            Course databaseCourse = createCourse("Database Systems", "CS301", csDept);
            Course discreteMathCourse = createCourse("Discrete Mathematics", "CS102", csDept);
            Course linearAlgebraCourse = createCourse("Linear Algebra", "MATH102", mathDept);
            Course circuitsCourse = createCourse("Electrical Circuits", "ENG201", engineeringDept);
            List<Course> courses = List.of(javaCourse, algorithmsCourse, databaseCourse, discreteMathCourse, linearAlgebraCourse, circuitsCourse);

            assignLecturerToCourse(lecturer1, javaCourse);
            assignLecturerToCourse(lecturer1, algorithmsCourse);
            assignLecturerToCourse(lecturer2, discreteMathCourse);
            assignLecturerToCourse(lecturer2, linearAlgebraCourse);
            assignLecturerToCourse(lecturer3, databaseCourse);
            assignLecturerToCourse(lecturer4, circuitsCourse);

            TimeSlot mondayMorning = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(11, 0), DayOfWeek.MONDAY);
            TimeSlot mondayAfternoon = new TimeSlot(LocalTime.of(14, 0), LocalTime.of(16, 0), DayOfWeek.MONDAY);
            TimeSlot tuesdayMorning = new TimeSlot(LocalTime.of(10, 0), LocalTime.of(12, 0), DayOfWeek.TUESDAY);
            TimeSlot wednesdayMorning = new TimeSlot(LocalTime.of(9, 30), LocalTime.of(11, 30), DayOfWeek.WEDNESDAY);
            TimeSlot wednesdayAfternoon = new TimeSlot(LocalTime.of(13, 0), LocalTime.of(15, 0), DayOfWeek.WEDNESDAY);
            TimeSlot thursdayMorning = new TimeSlot(LocalTime.of(8, 0), LocalTime.of(10, 0), DayOfWeek.THURSDAY);

            ClassSession javaClass1 = createClassSession(javaCourse, lecturer1, mondayMorning, "Room A101", 30);
            ClassSession javaClass2 = createClassSession(javaCourse, lecturer1, wednesdayAfternoon, "Room B205", 25);
            ClassSession algorithmsClass = createClassSession(algorithmsCourse, lecturer1, tuesdayMorning, "Room C301", 35);
            ClassSession databaseClass = createClassSession(databaseCourse, lecturer3, thursdayMorning, "Lab L101", 20);
            ClassSession discreteMathClass = createClassSession(discreteMathCourse, lecturer2, mondayAfternoon, "Room M201", 40);
            ClassSession linearAlgebraClass = createClassSession(linearAlgebraCourse, lecturer2, wednesdayMorning, "Room M203", 30);
            ClassSession circuitsClass = createClassSession(circuitsCourse, lecturer4, wednesdayAfternoon, "Lab E201", 15);
            List<ClassSession> classes = List.of(javaClass1, javaClass2, algorithmsClass, databaseClass, discreteMathClass, linearAlgebraClass, circuitsClass);

            Random random = new Random();
            int totalEnrollments = 0;

            for (Student student : students) {
                int classesToEnroll = 2 + random.nextInt(4);
                for (int i = 0; i < classesToEnroll; i++) {
                    ClassSession randomClass = classes.get(random.nextInt(classes.size()));
                    if (!student.isEnrolledInClass(randomClass) && !randomClass.isFull()) {
                        student.addClassSession(randomClass);
                        randomClass.addStudent(student);
                        totalEnrollments++;
                    }
                }
                studentRepository.save(student);
            }

            classSessionRepository.saveAll(classes);

            log.info("Database initialization completed successfully!");
            log.info("Created {} departments, {} students, {} lecturers, {} courses, {} class sessions, {} enrollments",
                    departmentRepository.count(), studentRepository.count(),
                    lecturerRepository.count(), courseRepository.count(),
                    classSessionRepository.count(), totalEnrollments);

        } catch (Exception e) {
            log.error("Error occurred during database initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private Department createDepartment(String name, String code) {
        Department department = new Department(name, code);
        department.setExternalId(generateDepartmentExternalId());
        Department savedDepartment = departmentRepository.save(department);
        log.info("Created department: {} ({})", name, code);
        return savedDepartment;
    }

    private Student createStudent(String name, String phone, Department department, int enrollmentYear) {
        String email = generateStudentEmail(name, enrollmentYear);

        Student student = Student.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .department(department)
                .enrollmentYear(enrollmentYear)
                .externalId(generateStudentExternalId())
                .build();

        Student savedStudent = studentRepository.save(student);
        department.addStudent(savedStudent);
        departmentRepository.save(department);

        log.info("Created student: {} ({})", name, email);
        return savedStudent;
    }

    private Lecturer createLecturer(String name, String phone, Department department) {
        String email = generateLecturerEmail(name);

        Lecturer lecturer = Lecturer.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .department(department)
                .externalId(generateLecturerExternalId())
                .build();

        Lecturer savedLecturer = lecturerRepository.save(lecturer);
        department.addLecturer(savedLecturer);
        departmentRepository.save(department);

        log.info("Created lecturer: {} ({})", name, email);
        return savedLecturer;
    }

    private Course createCourse(String name, String code, Department department) {
        Course course = new Course(name, code, department);
        course.setExternalId(generateCourseExternalId());
        Course savedCourse = courseRepository.save(course);
        department.addCourse(savedCourse);
        departmentRepository.save(department);

        log.info("Created course: {} ({})", name, code);
        return savedCourse;
    }

    private void assignLecturerToCourse(Lecturer lecturer, Course course) {
        lecturer.addCourse(course);
        course.addLecturer(lecturer);
        lecturerRepository.save(lecturer);
        courseRepository.save(course);

        log.info("Assigned lecturer {} to course {}", lecturer.getName(), course.getName());
    }

    private ClassSession createClassSession(Course course, Lecturer lecturer, TimeSlot timeSlot, String location, int maxCapacity) {
        ClassSession classSession = new ClassSession(course, lecturer, timeSlot, location, maxCapacity);
        classSession.setExternalId(generateClassSessionExternalId());
        ClassSession savedClassSession = classSessionRepository.save(classSession);
        course.addClassSession(savedClassSession);
        lecturer.addClassSession(savedClassSession);

        courseRepository.save(course);
        lecturerRepository.save(lecturer);

        log.info("Created class session: {} at {} on {}", course.getName(), location, timeSlot.getDay());
        return savedClassSession;
    }

    private String generateStudentExternalId() {
        long count = studentRepository.count();
        return String.valueOf(15000 + count + 1);
    }

    private String generateLecturerExternalId() {
        long count = lecturerRepository.count();
        return String.format("LECT%d", 5000 + count + 1);
    }

    private String generateDepartmentExternalId() {
        long count = departmentRepository.count();
        return String.format("DEP%d", count + 1);
    }

    private String generateCourseExternalId() {
        long count = courseRepository.count();
        return String.format("CRS%d", count + 1);
    }

    private String generateClassSessionExternalId() {
        long count = classSessionRepository.count();
        return String.format("CL%d", 100 + count + 1);
    }
}
