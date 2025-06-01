package org.example.ctrlu.domain.todo.application;

import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.request.CompleteTodoRequest;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.CreateTodoResponse;
import org.example.ctrlu.domain.todo.dto.response.GetTodoResponse;
import org.example.ctrlu.domain.todo.dto.response.GetTodosResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.*;
import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final FriendShipRepository friendShipRepository;
    private final Clock clock;

    protected LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public CreateTodoResponse createTodo(long userId, CreateTodoRequest request, MultipartFile startImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));
        if(!todoRepository.findAllByUserIdAndStatus(userId, TodoStatus.IN_PROGRESS).isEmpty())
            throw new TodoException(ALREADY_EXIST_IN_PROGRESS_TODO);

        String startImageUrl = awsS3Service.uploadImage(startImage);
        Todo newTodo = Todo.builder().title(request.title()).startImage(startImageUrl).user(user).challengeTime(request.challengeTime()).build();
        Long todoId = todoRepository.save(newTodo).getId();
        return new CreateTodoResponse(todoId);
    }


    public GetTodoResponse getTodo(long userId, long todoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(NOT_FOUND_TODO));
        TodoStatus status = todo.getStatus();
        if (status.equals(TodoStatus.DELETED))
            throw new TodoException(FAIL_TO_GET_TODO, "상태: " + status.name());

        int durationTime = DurationTimeCalculator.calculate(todo, now());
        boolean isMine = getIsMine(todo,user);

        return GetTodoResponse.from(todo,durationTime,isMine);
    }

    private boolean getIsMine(Todo todo, User user) {
        if(todo.getUser()== user) return true;
        return false;
    }

    public void completeTodo(long userId, long todoId, CompleteTodoRequest request, MultipartFile endImage) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoException(NOT_FOUND_TODO));
        if(todo.getUser()!=user) throw new TodoException(NOT_YOUR_TODO);
        if(!todo.getStatus().equals(TodoStatus.IN_PROGRESS)) throw new TodoException(NOT_IN_PROGRESS_TODO, "상태: " +todo.getStatus().name());

        String endImageUrl = awsS3Service.uploadImage(endImage);
        todo.complete(request.durationTime(), endImageUrl);
    }

    public void giveUpTodo(long userId, long todoId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoException(NOT_FOUND_TODO));
        if(todo.getUser()!=user) throw new TodoException(NOT_YOUR_TODO);
        if(!todo.getStatus().equals(TodoStatus.IN_PROGRESS)) throw new TodoException(NOT_IN_PROGRESS_TODO, "상태: " +todo.getStatus().name());

        todo.giveUp();
    }

    public void deleteTodo(long userId, long todoId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoException(NOT_FOUND_TODO));
        if(todo.getUser()!=user) throw new TodoException(NOT_YOUR_TODO);
        if(todo.getStatus().equals(TodoStatus.DELETED)) throw new TodoException(NOT_FOUND_TODO);
        todo.delete();
    }

    public GetTodosResponse getTodos(long userId, String target, TodoStatus status, Pageable pageable) {
        if(target.equals("me")) return getMyTodos(userId, status, pageable);
        return getFriendTodos(userId, status, pageable);
    }

    private GetTodosResponse getFriendTodos(long userId, TodoStatus status, Pageable pageable) {
        if (!status.equals(TodoStatus.IN_PROGRESS)) throw new TodoException(FAIL_TO_GET_FRIEND_TODOS);
        List<Long> friendIds = friendShipRepository.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return new GetTodosResponse(List.of(), 0, 0);
        }

        Page<Todo> todosPage = todoRepository.findAllByUserIdInAndStatus(friendIds, status, pageable);
        return GetTodosResponse.from(todosPage, now());
    }

    private GetTodosResponse getMyTodos(long userId, TodoStatus status, Pageable pageable) {
        if (status.equals(TodoStatus.DELETED)) throw new TodoException(FAIL_TO_GET_TODO, TodoStatus.DELETED.name());
        userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Page<Todo> todosPage = todoRepository.findAllByUserIdAndStatus(userId, status, pageable);
        return GetTodosResponse.from(todosPage, now());
    }
}
