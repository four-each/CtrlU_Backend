package org.example.ctrlu.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.todo.application.TodoService;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.CreateTodoResponse;
import org.example.ctrlu.domain.todo.dto.response.GetTodoResponse;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/todos")
public class TodoController {
    private final TodoService todoService;

    @PostMapping
    public BaseResponse<CreateTodoResponse> createTodo(
            @RequestParam Long userId,
            @RequestPart("request") @Valid CreateTodoRequest request,
            @RequestPart("startImage") MultipartFile startImage
    ){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        CreateTodoResponse response = todoService.createTodo(userId, request, startImage);
        return new BaseResponse<>(response);
    }

    @GetMapping("/{todoId}")
    public BaseResponse<GetTodoResponse> getTodo(@PathVariable long todoId){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        GetTodoResponse response = todoService.getTodo(1L, todoId);
        return new BaseResponse<>(response);
    }
}
