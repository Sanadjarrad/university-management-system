package com.demo.universityManagementApp.init;

import com.demo.universityManagementApp.repository.*;
import com.demo.universityManagementApp.repository.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test profile-specific Spring component responsible for generating and populating
 * a large-scale dataset for integration and performance testing purposes.
 * <p>
 * This initializer leverages both Spring Data JPA repositories and JDBC batch operations
 * to efficiently insert thousands of entities, including departments, students, lecturers,
 * courses, and class sessions, while preserving referential integrity.
 * <p>
 * Key responsibilities include:
 * <ul>
 *     <li>Flushing all existing data in reverse dependency order to prevent foreign key violations.</li>
 *     <li>Creating a configurable number of departments with unique codes and external IDs.</li>
 *     <li>Generating students with random names, unique emails, and enrollment years, ensuring department assignment.</li>
 *     <li>Generating lecturers with unique emails and assigning them to multiple courses.</li>
 *     <li>Generating courses with unique codes, randomized names using prefix/subject/suffix patterns, and associating them to departments.</li>
 *     <li>Generating class sessions with randomized time slots, locations, and capacities while assigning lecturers and courses.</li>
 *     <li>Enrolling students into class sessions randomly, respecting class capacity and avoiding duplicate enrollments.</li>
 *     <li>Optimizing bulk inserts using JDBC batch updates to maximize performance and reduce database round-trips.</li>
 *     <li>Ensuring all generated external IDs (students, lecturers, departments, courses, class sessions) are unique.</li>
 * </ul>
 * <p>
 * All operations are transactional where required, guaranteeing atomicity for batch inserts,
 * and leveraging Spring's transaction management to maintain data consistency across multiple entity types.
 * <p>
 * Usage is intended solely for test environments (annotated with {@code @Profile("test")}) and should
 * never be used in production.
 */
@Profile("test")
@Component
@RequiredArgsConstructor
@Slf4j
public class LargeDataSetDatabaseInitializer {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository classSessionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    private final Random random = new Random();

    private static final String[] FIRST_NAMES = {"ExampleNameOne", "ExampleNameTwo", "ExampleNameThree", "ExampleNameFour",
            "ExampleNameFive", "ExampleNameSix", "ExampleNameSeven", "ExampleNameEight", "ExampleNameNine", "ExampleNameTen",
            "ExampleNameEleven", "ExampleNameTwelve", "ExampleNameThirteen", "ExampleNameFourteen", "ExampleNameFifteen",
            "ExampleNameSixteen", "ExampleNameSeventeen", "ExampleNameEighteen", "ExampleNameNineteen", "ExampleNameTwenty",
            "ExampleNameTwentyOne", "ExampleNameTwentyTwo", "ExampleNameTwentyThree", "ExampleNameTwentyFour", "ExampleNameTwentyFive",
            "ExampleNameTwentySix", "ExampleNameTwentySeven", "ExampleNameTwentyEight", "ExampleNameTwentyNine", "ExampleNameThirty"};

    private static final String[] LAST_NAMES = {"LastNameOne", "LastNameTwo", "LastNameThree", "LastNameFour", "LastNameFive", "LastNameSix",
            "LastNameSeven", "LastNameEight", "LastNameNine", "LastNameTen", "LastNameEleven", "LastNameTwelve", "LastNameThirteen",
            "LastNameFourteen", "LastNameFifteen", "LastNameSixteen", "LastNameSeventeen", "LastNameEighteen", "LastNameNineteen",
            "LastNameTwenty", "LastNameTwentyOne", "LastNameTwentyTwo", "LastNameTwentyThree", "LastNameTwentyFour", "LastNameTwentyFive",
            "LastNameTwentySix", "LastNameTwentySeven", "LastNameTwentyEight", "LastNameTwentyNine", "LastNameThirty"};

    private static final String[] DEPARTMENT_NAMES = {
            "Computer Science", "Mathematics", "Engineering", "Physics", "Chemistry",
            "Biology", "Business Administration", "Economics", "Psychology", "Sociology",
            "History", "English Literature", "Fine Arts", "Music", "Medicine",
            "Law", "Education", "Philosophy", "Political Science", "Languages",
            "Environmental Science", "Statistics", "Architecture", "Nursing", "Pharmacy"
    };

    private static final String[] COURSE_PREFIXES = {"Introduction to", "Advanced", "Fundamentals of", "Principles of",
            "Applied", "Theoretical", "Modern", "Classical", "Digital", "Computer", "Data", "Software", "Hardware",
            "Network", "Security", "Artificial", "Machine", "Web", "Mobile", "Cloud"};

    private static final String[] COURSE_SUBJECTS = {"Programming", "Algorithms", "Databases", "Networks", "Security",
            "Mathematics", "Statistics", "Physics", "Chemistry", "Biology", "Engineering", "Design", "Architecture",
            "Systems", "Analysis", "Development", "Computing", "Intelligence", "Learning", "Vision"};

    private static final String[] COURSE_SUFFIXES = {"I", "II", "III", "and Applications", "and Systems",
            "and Implementation", "Theory", "Practice", "Methods", "Techniques"};

    @Transactional
    public void run(String... args) throws Exception {
        log.info("Running Large-Dataset DatabaseInitializer...");

        flushAll();
        initializeLargeDataset();
    }

    @Transactional
    public void flushAll() {
        log.info("Deleting all data in reverse dependency order...");

        jdbcTemplate.execute("DELETE FROM student_class_sessions");
        jdbcTemplate.execute("DELETE FROM lecturer_courses");
        jdbcTemplate.execute("DELETE FROM class_sessions");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.execute("DELETE FROM students");
        jdbcTemplate.execute("DELETE FROM lecturers");
        jdbcTemplate.execute("DELETE FROM departments");

        log.info("All data deleted successfully!");
    }

    @Transactional
    public void initializeLargeDataset() {
        try {
            log.info("Starting large dataset initialization using JDBC...");

            List<Department> departments = createDepartments(25);
            log.info("Created {} departments", departments.size());

            List<Student> students = createStudents(1000, departments);
            log.info("Created {} students", students.size());

            List<Lecturer> lecturers = createLecturers(100, departments);
            log.info("Created {} lecturers", lecturers.size());

            List<Course> courses = createCourses(100, departments);
            log.info("Created {} courses", courses.size());

            assignLecturersToCourses(lecturers, courses);
            log.info("Assigned lecturers to courses");

            List<ClassSession> classSessions = createClassSessions(500, courses, lecturers);
            log.info("Created {} class sessions", classSessions.size());

            int totalEnrollments = enrollStudentsInClasses(students, classSessions);
            log.info("Created {} student enrollments", totalEnrollments);

            log.info("Large dataset initialization completed successfully!");

        } catch (Exception e) {
            log.error("Error occurred during large dataset initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Large dataset initialization failed", e);
        }
    }

    private List<Department> createDepartments(int count) {
        String sql = "INSERT INTO departments (name, code, external_id, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> departmentCodes = new HashSet<>();

        Map<String, String> departmentNameToCode = Map.ofEntries(
                Map.entry("Computer Science", "CS"),
                Map.entry("Mathematics", "MATH"),
                Map.entry("Engineering", "ENG"),
                Map.entry("Physics", "PHY"),
                Map.entry("Chemistry", "CHEM"),
                Map.entry("Biology", "BIO"),
                Map.entry("Business Administration", "BUS"),
                Map.entry("Economics", "ECON"),
                Map.entry("Psychology", "PSY"),
                Map.entry("Sociology", "SOC"),
                Map.entry("History", "HIST"),
                Map.entry("English Literature", "ENGL"),
                Map.entry("Fine Arts", "ART"),
                Map.entry("Music", "MUS"),
                Map.entry("Medicine", "MED"),
                Map.entry("Law", "LAW"),
                Map.entry("Education", "EDU"),
                Map.entry("Philosophy", "PHIL"),
                Map.entry("Political Science", "POLI"),
                Map.entry("Languages", "LANG"),
                Map.entry("Environmental Science", "ENV"),
                Map.entry("Statistics", "STAT"),
                Map.entry("Architecture", "ARCH"),
                Map.entry("Nursing", "NURS"),
                Map.entry("Pharmacy", "PHARM")
        );

        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            String name = DEPARTMENT_NAMES[i - 1];
            String code = departmentNameToCode.get(name);

            if (code == null || departmentCodes.contains(code)) code = generateDepartmentCode(name, departmentCodes);

            departmentCodes.add(code);
            String externalId = generateDepartmentExternalId(i);

            batchArgs.add(new Object[]{
                    name,
                    code,
                    externalId,
                    now,
                    now,
                    0L
            });

            if (i % 10 == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return departmentRepository.findAll();
    }

    private List<Student> createStudents(int count, List<Department> departments) {
        String sql = "INSERT INTO students (name, email, phone, department_id, enrollment_year, external_id, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> emails = new HashSet<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            String name = generateRandomName();
            String phone = generateRandomPhone();
            Department department = departments.get(random.nextInt(departments.size()));
            int enrollmentYear = 2018 + random.nextInt(7);
            String email = generateUniqueEmail(name, emails);
            String externalId = generateStudentExternalId(i);

            batchArgs.add(new Object[]{
                    name,
                    email,
                    phone,
                    department.getId(),
                    enrollmentYear,
                    externalId,
                    now,
                    now,
                    0L
            });
            emails.add(email);

            if (i % 100 == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return studentRepository.findAll();
    }


    private List<Lecturer> createLecturers(int count, List<Department> departments) {
        String sql = "INSERT INTO lecturers (name, email, phone, department_id, external_id, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> emails = new HashSet<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            String name = generateRandomName();
            String phone = generateRandomPhone();
            Department department = departments.get(random.nextInt(departments.size()));
            String email = generateUniqueLecturerEmail(name, emails);
            String externalId = generateLecturerExternalId(i);

            batchArgs.add(new Object[]{
                    name,
                    email,
                    phone,
                    department.getId(),
                    externalId,
                    now,
                    now,
                    0L
            });
            emails.add(email);

            if (i % 100 == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return lecturerRepository.findAll();
    }

    private List<Course> createCourses(int count, List<Department> departments) {
        String sql = "INSERT INTO courses (name, code, department_id, external_id, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> courseCodes = new HashSet<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            String name = generateCourseName();
            String code = generateCourseCode(courseCodes);
            Department department = departments.get(random.nextInt(departments.size()));
            String externalId = generateCourseExternalId(i);

            batchArgs.add(new Object[]{
                    name,
                    code,
                    department.getId(),
                    externalId,
                    now,
                    now,
                    0L
            });

            if (i % 100 == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return courseRepository.findAll();
    }

    private List<ClassSession> createClassSessions(int count, List<Course> courses, List<Lecturer> lecturers) {
        String sql = "INSERT INTO class_sessions (course_id, lecturer_id, start_time, end_time, day_of_week, location, max_capacity, external_id, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        List<TimeSlot> timeSlots = generateTimeSlots();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= count; i++) {
            Course course = courses.get(random.nextInt(courses.size()));
            Lecturer lecturer = lecturers.get(random.nextInt(lecturers.size()));
            TimeSlot timeSlot = timeSlots.get(random.nextInt(timeSlots.size()));
            String location = generateLocation();
            int maxCapacity = 20 + random.nextInt(81);
            String externalId = generateClassSessionExternalId(i);

            batchArgs.add(new Object[]{
                    course.getId(),
                    lecturer.getId(),
                    timeSlot.getStartTime(),
                    timeSlot.getEndTime(),
                    timeSlot.getDay().toString(),
                    location,
                    maxCapacity,
                    externalId,
                    now,
                    now,
                    0L
            });

            if (i % 100 == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return classSessionRepository.findAll();
    }

    private void assignLecturersToCourses(List<Lecturer> lecturers, List<Course> courses) {
        String sql = "INSERT INTO lecturer_courses (course_id, lecturer_id) VALUES (?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();

        for (Lecturer lecturer : lecturers) {
            int coursesToTeach = 2 + random.nextInt(5);
            Set<Long> assignedCourseIds = new HashSet<>();

            while (assignedCourseIds.size() < coursesToTeach) {
                Course randomCourse = courses.get(random.nextInt(courses.size()));
                if (assignedCourseIds.add(randomCourse.getId())) {
                    batchArgs.add(new Object[]{randomCourse.getId(), lecturer.getId()});
                }
            }

            if (batchArgs.size() >= 100) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private int enrollStudentsInClasses(List<Student> students, List<ClassSession> classSessions) {
        String sql = "INSERT INTO student_class_sessions (class_session_id, student_id) VALUES (?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        AtomicInteger totalEnrollments = new AtomicInteger();

        for (Student student : students) {
            int classesToEnroll = 3 + random.nextInt(6);
            Set<Long> enrolledClassIds = new HashSet<>();

            while (enrolledClassIds.size() < classesToEnroll && enrolledClassIds.size() < classSessions.size()) {
                ClassSession randomClass = classSessions.get(random.nextInt(classSessions.size()));
                if (enrolledClassIds.add(randomClass.getId())) {
                    batchArgs.add(new Object[]{randomClass.getId(), student.getId()});
                    totalEnrollments.incrementAndGet();
                }
            }

            if (batchArgs.size() >= 100) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) jdbcTemplate.batchUpdate(sql, batchArgs);

        return totalEnrollments.get();
    }

    private String generateRandomName() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return String.format("%s %s", firstName, lastName);
    }

    private String generateRandomPhone() {
        return String.format("077%07d", random.nextInt(10000000));
    }

    private String generateUniqueEmail(String name, Set<String> existingEmails) {
        String cleanName = name.toLowerCase().replaceAll("[^a-z ]", "").replace(" ", ".");
        String baseEmail = String.format("%s@digitinaryIntern.edu", cleanName);
        String email = baseEmail;
        int counter = 1;

        while (existingEmails.contains(email)) {
            email = String.format("%s%d@digitinaryIntern.edu", cleanName, counter);
            counter++;
        }

        return email;
    }

    private String generateUniqueLecturerEmail(final String name, final Set<String> existingEmails) {
        String cleanName = name.toLowerCase().replaceAll("[^a-z ]", "").replace(" ", ".");
        String baseEmail = String.format("%s@digitinary.edu", cleanName);
        String email = baseEmail;
        int counter = 1;

        while (existingEmails.contains(email)) {
            email = String.format("%s%d@digitinary.edu", cleanName, counter);
            counter++;
        }

        return email;
    }

    private String generateDepartmentCode(final String name, final Set<String> existingCodes) {
        Map<String, String> commonCodes = Map.of(
                "Computer Science", "CS",
                "Mathematics", "MATH",
                "Engineering", "ENG",
                "Physics", "PHY",
                "Chemistry", "CHEM",
                "Biology", "BIO",
                "Business Administration", "BUS",
                "Economics", "ECON",
                "Psychology", "PSY",
                "Sociology", "SOC"
        );

        String code = commonCodes.get(name);
        if (code == null) {
            String[] words = name.split(" ");
            if (words.length >= 2) {
                code = (words[0].substring(0, Math.min(1, words[0].length())) + words[1].substring(0, Math.min(3, words[1].length()))).toUpperCase();
            } else {
                code = name.substring(0, Math.min(4, name.length())).toUpperCase();
            }
        }

        String finalCode = code;
        int counter = 1;
        while (existingCodes.contains(finalCode)) {
            finalCode = code + counter;
            counter++;
        }

        existingCodes.add(finalCode);
        return finalCode;
    }

    private String generateCourseName() {
        String prefix = COURSE_PREFIXES[random.nextInt(COURSE_PREFIXES.length)];
        String subject = COURSE_SUBJECTS[random.nextInt(COURSE_SUBJECTS.length)];
        String suffix = random.nextBoolean() ? " " + COURSE_SUFFIXES[random.nextInt(COURSE_SUFFIXES.length)] : "";
        return String.format("%s %s%s", prefix, subject, suffix);
    }

    private String generateCourseCode(Set<String> existingCodes) {
        String code;
        do {
            int number = 100 + random.nextInt(900);
            String prefix = "CS";
            if (random.nextDouble() < 0.3) {
                String[] prefixes = {"MATH", "ENG", "PHY", "CHEM", "BIO", "BUS", "ECON"};
                prefix = prefixes[random.nextInt(prefixes.length)];
            }
            code = prefix + number;
        } while (existingCodes.contains(code));

        existingCodes.add(code);
        return code;
    }

    private String generateLocation() {
        String[] buildings = {"A", "B", "C", "D", "E", "F", "G", "H"};
        String[] roomTypes = {"Room", "Lab", "Hall", "Studio"};
        String building = buildings[random.nextInt(buildings.length)];
        int floor = random.nextInt(5);
        int room = 100 + random.nextInt(100);

        return roomTypes[random.nextInt(roomTypes.length)] + " " + building + floor + "-" + room;
    }

    private List<TimeSlot> generateTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        DayOfWeek[] days = DayOfWeek.values();

        for (DayOfWeek day : days) {
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                timeSlots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(10, 0), day));
                timeSlots.add(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(12, 0), day));
                timeSlots.add(new TimeSlot(LocalTime.of(13, 0), LocalTime.of(15, 0), day));
                timeSlots.add(new TimeSlot(LocalTime.of(15, 0), LocalTime.of(17, 0), day));
            }
        }
        return timeSlots;
    }

    private String generateStudentExternalId(int index) {
        return String.valueOf(14999 + index);
    }

    private String generateLecturerExternalId(int index) {
        return String.format("LECT%d", index);
    }

    private String generateDepartmentExternalId(int index) {
        return String.format("DEP%d", index);
    }

    private String generateCourseExternalId(int index) {
        return String.format("CRS%d", index);
    }

    private String generateClassSessionExternalId(int index) {
        return String.format("CL%d", index);
    }
}
