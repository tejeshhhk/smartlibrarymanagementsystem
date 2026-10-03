package SRC.service;

import SRC.exception.StudentNotFoundException;
import SRC.model.Student;
import SRC.model.UndoOperation;
import SRC.repository.StudentRepository;
import SRC.util.LogUtil;
import java.util.List;
import java.util.Optional;

public class StudentService {
    private final StudentRepository studentRepository;
    private final UndoService undoService;

    public StudentService(StudentRepository studentRepository, UndoService undoService) {
        this.studentRepository = studentRepository;
        this.undoService = undoService;
    }

    public synchronized List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public synchronized Optional<Student> getStudentById(String studentId) {
        return studentRepository.findById(studentId);
    }

    public synchronized Student registerStudent(Student student) {
        studentRepository.save(student);
        LogUtil.info("Registered student: " + student.getName() + " (ID: " + student.getStudentId() + ")");
        
        // Save undo operation snapshot
        Student studentSnapshot = new Student(student);
        undoService.push(new UndoOperation(UndoOperation.Type.REGISTER_STUDENT, student.getStudentId(), null, studentSnapshot));
        
        return student;
    }

    public synchronized Student updateStudent(Student updatedStudent) throws StudentNotFoundException {
        studentRepository.findById(updatedStudent.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student with ID " + updatedStudent.getStudentId() + " not found."));
        
        studentRepository.save(updatedStudent);
        LogUtil.info("Updated student details: " + updatedStudent.getName() + " (ID: " + updatedStudent.getStudentId() + ")");
        return updatedStudent;
    }
}
