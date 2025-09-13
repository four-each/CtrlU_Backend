package org.example.ctrlu.domain.todo.application;

import lombok.RequiredArgsConstructor;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.example.ctrlu.domain.todo.dto.projection.TodoProjection;
import org.example.ctrlu.domain.todo.dto.request.CompleteTodoRequest;
import org.example.ctrlu.domain.todo.dto.request.CreateTodoRequest;
import org.example.ctrlu.domain.todo.dto.response.*;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoErrorCode;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.todo.util.DurationTimeCalculator;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.example.ctrlu.domain.todo.exception.TodoErrorCode.*;
import static org.example.ctrlu.domain.user.exception.UserErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final FriendshipRepository friendshipRepository;
    private final Clock clock;
    private final RedisTemplate<String,Object> redisTemplate;
    private static final String REDIS_KEY_PREFIX = "recentTodo:seen:";

    protected LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public CreateTodoResponse createTodo(long userId, CreateTodoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));
        if(!todoRepository.findAllByUserIdAndStatus(userId, TodoStatus.IN_PROGRESS).isEmpty())
            throw new TodoException(ALREADY_EXIST_IN_PROGRESS_TODO);

        Todo newTodo = Todo.builder().title(request.title()).startImage(request.startImageKey()).user(user).challengeTime(request.challengeTime()).build();
        Long todoId = todoRepository.save(newTodo).getId();
        return new CreateTodoResponse(todoId);
    }

    @Transactional(readOnly = true)
    public GetTodoResponse getTodo(long userId, long todoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(NOT_FOUND_TODO));

        int durationTime = DurationTimeCalculator.calculate(todo, now());
        boolean isMine = getIsMine(todo,user);

        return GetTodoResponse.from(todo,user.getProfileImageKey(),durationTime, isMine, awsS3Service);
    }

    private boolean getIsMine(Todo todo, User user) {
        if(todo.getUser()== user) return true;
        return false;
    }

    public void completeTodo(long userId, long todoId, CompleteTodoRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoException(NOT_FOUND_TODO));
        if(todo.getUser()!=user) throw new TodoException(NOT_YOUR_TODO);
        if(!todo.getStatus().equals(TodoStatus.IN_PROGRESS)) throw new TodoException(NOT_IN_PROGRESS_TODO, "상태: " +todo.getStatus().name());

        todo.complete(request.durationTime(), request.endImageKey());
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
        awsS3Service.deleteImage(todo.getStartImage());
        awsS3Service.deleteImage(todo.getEndImage());
        todoRepository.delete(todo);
    }

    @Transactional(readOnly = true)
    public GetTodosResponse getTodos(long userId, String target, TodoStatus status, Pageable pageable) {
        if(target.equals("me")) return getMyTodos(userId, status, pageable);
        return getFriendTodos(userId, status, pageable);
    }

    private GetTodosResponse getFriendTodos(long userId, TodoStatus status, Pageable pageable) {
        if (!status.equals(TodoStatus.IN_PROGRESS)) throw new TodoException(FAIL_TO_GET_FRIEND_TODOS);
        List<Long> friendIds = friendshipRepository.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return new GetTodosResponse(List.of(), 0, 0);
        }

        Page<TodoProjection> todosPage = todoRepository.findTodoProjectionsBy(friendIds, status, pageable);
        return GetTodosResponse.from(todosPage, now());
    }

    private GetTodosResponse getMyTodos(long userId, TodoStatus status, Pageable pageable) {
        userRepository.findById(userId).orElseThrow(() -> new UserException(NOT_FOUND_USER));
        TodoProjection todosPage = todoRepository.findTodoProjectionBy(userId, status);
        return GetTodosResponse.from(todosPage, now());
    }

    @Transactional(readOnly = true)
    public GetRecentUploadFriendsResponse getRecentUploadFriends(long userId, Pageable pageable) {
        List<Long> friendIds = friendshipRepository.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return new GetRecentUploadFriendsResponse(setMyData(userId, now()), List.of(), 0, 0);
        }

        LocalDateTime since = now().minusHours(24);

        // 1. Page 객체를 사용하여 데이터와 전체 카운트를 한 번에 조회
        Page<Todo> latestTodosPage = todoRepository.findPagedLatestTodoPerFriend(friendIds, since, pageable);
        List<Todo> latestTodos = latestTodosPage.getContent();
        
        List<GetRecentUploadFriendsResponse.Friend> responseFriends = setFriendsData(userId, latestTodos);
        GetRecentUploadFriendsResponse.Me me = setMyData(userId, now());

        return new GetRecentUploadFriendsResponse(
                me,
                responseFriends,
                latestTodosPage.getTotalPages(),
                (int) latestTodosPage.getTotalElements());
    }

    private List<GetRecentUploadFriendsResponse.Friend> setFriendsData(long userId, List<Todo> latestTodos) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();

        if (latestTodos.isEmpty()) {
            return Collections.emptyList();
        }

        // N+1 해결: 친구 프로필 이미지 정보와 Redis의 '본상태' 정보를 한 번에 조회
        List<Long> friendIds = latestTodos.stream()
                .map(todo -> todo.getUser().getId())
                .toList();
        Map<Long, String> friendProfileImages = userRepository.findImageMapByIdIn(friendIds);
        String redisKey = REDIS_KEY_PREFIX + userId;
        List<String> friendIdStrings = friendIds.stream().map(String::valueOf).toList();
        List<String> seenTodoIdObjects = hashOperations.multiGet(redisKey, friendIdStrings);

        Map<Long, Long> seenTodoMap = new HashMap<>();
        for (int i = 0; i < friendIds.size(); i++) {
            if (seenTodoIdObjects.get(i) != null) {
                seenTodoMap.put(friendIds.get(i), Long.parseLong(seenTodoIdObjects.get(i)));
            }
        }

        // 루프 내에서는 조회 없이 Map에서 데이터를 가져와 가공만 함
        List<GetRecentUploadFriendsResponse.Friend> responseFriends = latestTodos.stream().map(todo -> {
            long friendId = todo.getUser().getId();
            long latestTodoId = todo.getId();

            long seenTodoId = seenTodoMap.getOrDefault(friendId, -1L);

            GetRecentUploadFriendsResponse.Status status = seenTodoId < latestTodoId
                    ? GetRecentUploadFriendsResponse.Status.GREEN
                    : GetRecentUploadFriendsResponse.Status.GRAY;

            String profileImage = friendProfileImages.get(friendId);

            return new GetRecentUploadFriendsResponse.Friend(
                    friendId,
                    awsS3Service.generateGetPresignedUrl(profileImage),
                    todo.getUser().getNickname(),
                    status,
                    todo.getCreatedAt()
            );
        }).collect(Collectors.toList());

        // 최적화된 정렬: Comparator를 사용하여 한 번에 정렬
        responseFriends.sort(Comparator
                // 1차 정렬: GREEN 상태가 먼저 오도록 (GREEN=0, GRAY=1)
                .comparing((GetRecentUploadFriendsResponse.Friend f) -> f.status() == GetRecentUploadFriendsResponse.Status.GREEN ? 0 : 1)
                // 2차 정렬: 1차 정렬이 같을 경우, createdAt을 기준으로 내림차순 정렬
                .thenComparing(GetRecentUploadFriendsResponse.Friend::createdAt, Comparator.reverseOrder()));

        return responseFriends;
    }



    private GetRecentUploadFriendsResponse.Me setMyData(long userId, LocalDateTime now) {
        String myProfileImage = userRepository.getImageById(userId);
        GetRecentUploadFriendsResponse.Status status;
        boolean exists = todoRepository.existsByUserIdAndCreatedAtAfterAndStatusNot(userId, now.minusHours(24), TodoStatus.GIVEN_UP);
        status = exists ? GetRecentUploadFriendsResponse.Status.GRAY : GetRecentUploadFriendsResponse.Status.NONE;

        GetRecentUploadFriendsResponse.Me me = new GetRecentUploadFriendsResponse.Me(
                userId,
                awsS3Service.generateGetPresignedUrl(myProfileImage),
                status
        );
        return me;
    }

    @Transactional(readOnly = true)
    public GetRecentUploadTodoResponse getRecentUploadTodo(long userId, long targetId, long nowId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        String seenValue = (String) redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new UserException(NOT_FOUND_USER));

        //target의 최근 24시간 내 등록된 할 일(오래된 순) 조회
        List<Todo> recentTodos = todoRepository.findAllRecentTodosByUserId(
                targetId, now().minusHours(24), TodoStatus.GIVEN_UP
        );
        if (recentTodos.isEmpty()) { throw new TodoException(TodoErrorCode.NO_RECENT_TODO);}

        //초기 요청(nowId == 0)인 경우
        if (nowId == 0) {
            //Redis 에 이 전에 본 이력이 있는 경우
            if (seenValue != null) {
                long seenTodoId = Long.parseLong(seenValue);
                //Redis 의 본 이력 id가 몇 번째로 최신 할 일인지 탐색
                int latestIndex = IntStream.range(0, recentTodos.size())
                        .filter(i -> recentTodos.get(i).getId() == seenTodoId)
                        .findFirst()
                        .orElse(-1);
                //유효하지 않은 본 이력(이미 24시간 이후)이거나 이미 가장 최신 할 일인경우
                if (latestIndex == -1 || latestIndex == recentTodos.size() - 1){
                    Todo first = recentTodos.get(0);
                    if (latestIndex == -1) redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(first.getId()));
                    return GetRecentUploadTodoResponse.from(now(), target.getProfileImageKey(), target.getNickname() ,first, null, getNextId(recentTodos, 0), recentTodos.size(), awsS3Service);
                }
                //유효한 본 이력이고 아직 가장 최신 할 일을 조회하지 않은 경우
                else {
                    Todo next = recentTodos.get(latestIndex + 1);
                    redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(next.getId()));
                    return GetRecentUploadTodoResponse.from(now(), target.getProfileImageKey(),  target.getNickname() , next, recentTodos.get(latestIndex).getId(), getNextId(recentTodos, latestIndex + 1), recentTodos.size(), awsS3Service);
                }
            }
            //Redis 에 이 전에 본 이력이 없는 경우
            else {
                Todo first = recentTodos.get(0);
                redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(first.getId()));
                return GetRecentUploadTodoResponse.from(now(), target.getProfileImageKey(), target.getNickname() , first, null, getNextId(recentTodos, 0), recentTodos.size(), awsS3Service);

            }
        }

        //다음 커서 요청(nowId > 0)인 경우
        int currentIndex = IntStream.range(0, recentTodos.size())
                .filter(i -> recentTodos.get(i).getId() == nowId)
                .findFirst()
                .orElse(-1);

        if (currentIndex == -1) {
            throw new TodoException(TodoErrorCode.NOT_FOUND_TODO);
        }

        if(!Objects.equals(recentTodos.get(currentIndex).getUser(), target)) {
            throw new TodoException(NOT_TARGET_TODO);
        }

        String existingSeen = (String) redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId));
        if (existingSeen == null || Long.parseLong(existingSeen) < nowId) {
            // Redis에 본 이력이 없거나 Redis 본 이력보다 더 최신의 할 일을 조회한 경우 -> Redis 갱신 필요
            redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(nowId));
        }

        Long prevId = currentIndex > 0 ? recentTodos.get(currentIndex - 1).getId() : null;
        Long nextId = getNextId(recentTodos, currentIndex);
        return GetRecentUploadTodoResponse.from(now(), target.getProfileImageKey(),  target.getNickname() ,recentTodos.get(currentIndex), prevId, nextId, recentTodos.size(), awsS3Service);
    }

    private Long getNextId(List<Todo> todos, int index) {
        //다음 할 일이 없는 경우 null 반환
        return (index + 1 < todos.size()) ? todos.get(index + 1).getId() : null;
    }

}
