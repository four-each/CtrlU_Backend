package org.example.ctrlu.domain.todo.application;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.CreateTodoResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.*;
import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;

    public CreateTodoResponse createTodo(long userId, CreateTodoRequest request, MultipartFile startImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));
        if(todoRepository.existsByUserIdAndEndImageIsNull(userId)){
            throw new TodoException(ALREADY_EXIST_PROCEEDING_TODO);
        }
        String startImageUrl = awsS3Service.uploadImage(startImage);
        Todo newTodo = Todo.builder().title(request.title()).startImage(startImageUrl).user(user).challengeTime(request.challengeTime()).build();
        Long todoId = todoRepository.save(newTodo).getId();
        return new CreateTodoResponse(todoId);
    }


}
