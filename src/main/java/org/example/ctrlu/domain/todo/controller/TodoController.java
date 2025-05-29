package org.example.ctrlu.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.todo.application.TodoService;
import org.example.ctrlu.domain.todo.dto.request.CompleteTodoRequest;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.CreateTodoResponse;
import org.example.ctrlu.domain.todo.dto.response.GetTodoResponse;
import org.example.ctrlu.domain.todo.dto.response.GetTodosResponse;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @PostMapping("/{todoId}/complete")
    public BaseResponse<Void> completeTodo(
            @RequestParam long userId,
            @PathVariable long todoId,
            @RequestPart("request") @Valid CompleteTodoRequest request,
            @RequestPart("endImage") MultipartFile endImage
    ){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        todoService.completeTodo(userId, todoId, request, endImage);
        return new BaseResponse<>(null);
    }

    @PostMapping("/{todoId}/giveUp")
    public BaseResponse<Void> giveUpTodo(@RequestParam long userId, @PathVariable long todoId){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        todoService.giveUpTodo(userId, todoId);
        return new BaseResponse<>(null);
    }

    @DeleteMapping("/{todoId}")
    public BaseResponse<Void> deleteTodo(@RequestParam long userId, @PathVariable long todoId){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        todoService.deleteTodo(userId, todoId);
        return new BaseResponse<>(null);
    }

    @GetMapping
    public BaseResponse<GetTodosResponse> getTodos(@RequestParam long userId,
                                                   @RequestParam String target,
                                                   @RequestParam TodoStatus status,
                                                   @RequestParam
                                                   @PageableDefault(size = 10, sort = "createdAt", page= 0) Pageable pageable){
        //todo: 인증 구현 후 userId 받아오는 로직 구현 필요
        if(!target.equals("me") && !target.equals("friend")) throw new IllegalArgumentException("잘못된 접근입니다.");
        GetTodosResponse response = todoService.getTodos(1L, target, status, pageable);
        return new BaseResponse<>(response);
    }
}
