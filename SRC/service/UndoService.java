package SRC.service;

import SRC.model.UndoOperation;
import SRC.repository.BookRepository;
import SRC.repository.StudentRepository;
import SRC.util.LogUtil;
import java.util.Stack;

public class UndoService {
    private final Stack<UndoOperation> undoStack;

    public UndoService() {
        this.undoStack = new Stack<>();
    }

    public synchronized void push(UndoOperation operation) {
        undoStack.push(operation);
    }

    public synchronized UndoOperation pop() {
        if (undoStack.isEmpty()) {
            return null;
        }
        return undoStack.pop();
    }

    public synchronized int size() {
        return undoStack.size();
    }

    public synchronized void clear() {
        undoStack.clear();
    }

    public synchronized boolean undoLast(BookRepository bookRepository, StudentRepository studentRepository) {
        UndoOperation op = pop();
        if (op == null) {
            return false;
        }
        
        LogUtil.info("Undoing operation: " + op.getType() + " for entity ID: " + op.getEntityId());
        
        switch (op.getType()) {
            case ADD_BOOK:
                bookRepository.deleteById((String) op.getEntityId());
                break;
            case UPDATE_BOOK:
                if (op.getPreviousState() instanceof SRC.model.Book) {
                    bookRepository.save((SRC.model.Book) op.getPreviousState());
                }
                break;
            case DELETE_BOOK:
                if (op.getPreviousState() instanceof SRC.model.Book) {
                    bookRepository.save((SRC.model.Book) op.getPreviousState());
                }
                break;
            case REGISTER_STUDENT:
                studentRepository.deleteById((String) op.getEntityId());
                break;
        }
        return true;
    }
}
