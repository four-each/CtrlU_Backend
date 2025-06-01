package org.example.ctrlu.domain.todo.dto.response;

import java.util.List;

public record GetRecentUploadFriendsResponse(
        Me me,
        List<Friend> friends,
        int totalPageCount,
        int totalElementCount
) {

    public record Me(
            long id,
            String profileImage,
            Status status
    ) {}

    public record Friend(
            long id,
            String profileImage,
            Status status
    ) {}

    public enum Status {
        GRAY, GREEN
    }
}
