# university-management-system
A comprehensive Spring-Boot-based university management system supporting student enrollment, course management, class scheduling, and reporting.

## How to Use

### Quick Start

**1. Clone and build the project**

```bash
git clone https://github.com/your-username/university-management-system.git
cd university-management-system
mvn clean install
```

**2. Run the application**

```bash
mvn spring-boot:run
```
##### Or Run directly from your IDE  


**3. Accessing Swagger Documentation**

After running the application, visit:

```bash
http://localhost:8080/api/v1/swagger-ui/index.html
```

**3. Test the application**

```bash
mvn test
```
### Or Run the tests directly from your IDE  


### Required Dependencies

```xml
<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.bucket4j</groupId>
			<artifactId>bucket4j-core</artifactId>
			<version>8.1.0</version>
		</dependency>
		<dependency>
			<groupId>javax.validation</groupId>
			<artifactId>validation-api</artifactId>
			<version>2.0.0.Final</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
			<version>4.0.0-M3</version>
		</dependency>
		<dependency>
			<groupId>org.modelmapper</groupId>
			<artifactId>modelmapper</artifactId>
			<version>3.2.2</version>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.8.6</version>
		</dependency>
	</dependencies>
```

### Prerequisites

1. Java 21+ (required)
2. Lombok for reduced boilerplate code
3. Maven/Gradle for dependency management
4. PostgreSQL

## Features

### Student Management

1. Create and manage student records
2. Class enrollment with schedule conflict detection
3. Department assignment and tracking
4. Course Management

### Course creation and curriculum management

1. Department-based course organization
2. Lecturer assignment validation
3. Class Session Management

### Time slot scheduling with conflict prevention

1. Capacity management and enrollment limits
2. Lecturer availability validation
3. Lecturer Management

### Lecturer assignment to courses and departments

1. Schedule conflict detection
2. Teaching capacity management
3. Department Management

### Academic department organization

1. Student, lecturer, and course tracking
2. Department-level reporting
3. Reporting System

### Student reports in TXT and CSV formats

1. Department overview reports
2. File export capabilities
3. Thread-Safe Concurrency

### Concurrent Report generation

1. Async single and bulk reports with virtual threads.
2. Lists available reports asynchronously.
3. Thread-safe and non-blocking.

## Application Flow

**StudentController() - >** CRUD Operations for students, enroll in classes, validate conflicts

**CourseController() - >** CRUD Operations for courses, assign to departments, manage curriculum

**ClassSessionController() - >** CRUD Operations for Class Sessions, Schedule classes, validate lecturer availability, get available seats

**LecturerController() ->**  CRUD Operations for lecturers, Assign lecturers to courses, validate department consistency

**DepartmentController() - >** CRUD Operations for departments, manage organizational structure

**ReportController() - >** Generate student and department reports in multiple formats, reports are generated and stored in the "reports" directory under the main directory (in both sync and async endpoints)

**ReportControllerAsync() - >** Generate student and department reports in multiple formats asynchronously
