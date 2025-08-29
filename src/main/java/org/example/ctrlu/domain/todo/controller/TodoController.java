package org.example.ctrlu.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.todo.application.TodoService;
import org.example.ctrlu.domain.todo.dto.request.CompleteTodoRequest;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.*;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.global.response.BaseResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/todos")
public class TodoController {
    private final TodoService todoService;

    @PostMapping
    public BaseResponse<CreateTodoResponse> createTodo(
            @RequestBody @Valid CreateTodoRequest request,
            @AuthenticationPrincipal Long userId
    ){
        CreateTodoResponse response = todoService.createTodo(userId, request);
        return new BaseResponse<>(response);
    }

    @GetMapping("/{todoId}")
    public BaseResponse<GetTodoResponse> getTodo(@PathVariable long todoId,
                                                 @AuthenticationPrincipal Long userId){
        GetTodoResponse response = todoService.getTodo(userId, todoId);
        return new BaseResponse<>(response);
    }

    @PostMapping("/{todoId}/complete")
    public BaseResponse<Void> completeTodo(
            @PathVariable long todoId,
            @RequestBody @Valid CompleteTodoRequest request,
            @AuthenticationPrincipal Long userId
    ){
        todoService.completeTodo(userId, todoId, request);
        return new BaseResponse<>(null);
    }

    @PostMapping("/{todoId}/giveUp")
    public BaseResponse<Void> giveUpTodo(@PathVariable long todoId,
                                         @AuthenticationPrincipal Long userId){
        todoService.giveUpTodo(userId, todoId);
        return new BaseResponse<>(null);
    }

    @DeleteMapping("/{todoId}")
    public BaseResponse<Void> deleteTodo(@PathVariable long todoId,
                                         @AuthenticationPrincipal Long userId){
        todoService.deleteTodo(userId, todoId);
        return new BaseResponse<>(null);
    }

    @GetMapping
    public BaseResponse<GetTodosResponse> getTodos(@RequestParam(value="target") String target,
                                                   @AuthenticationPrincipal Long userId,
                                                   @RequestParam(value="status") TodoStatus status,
                                                   @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC, page= 0) Pageable pageable){
        if(!target.equals("me") && !target.equals("friend")) throw new IllegalArgumentException("잘못된 접근입니다.");
        GetTodosResponse response = todoService.getTodos(userId, target, status, pageable);
        return new BaseResponse<>(response);
    }

    @GetMapping("/within-24hours")
    public BaseResponse<GetRecentUploadFriendsResponse> getRecentUploadFriends(@AuthenticationPrincipal Long userId,
                                                                               @PageableDefault(size = 10, page= 0) Pageable pageable){
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(userId, pageable);
        return new BaseResponse<>(response);
    }

    @GetMapping("/detail/within-24hours")
    public BaseResponse<GetRecentUploadTodoResponse> getRecentUploadTodo(@AuthenticationPrincipal Long userId,
                                                                         @RequestParam("targetId") long targetId,
                                                                         @RequestParam("nowId") long nowId){
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, nowId);
        return new BaseResponse<>(response);
    }
}
