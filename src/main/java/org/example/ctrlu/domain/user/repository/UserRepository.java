package org.example.ctrlu.domain.user.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	Optional<User> findByEmailAndStatus(String email, UserStatus status);
	Optional<User> findByVerifyToken(String verifyToken);
	Optional<User> findByVerifyTokenAndStatus(String verifyToken, UserStatus status);
	Optional<User> findByIdAndStatus(Long userId, UserStatus status);

	@Query("SELECT u.profileImageKey FROM User u WHERE u.id = :id")
    String getImageById(@Param("id") Long id);

	@Query("""
    	SELECT u FROM User u
    	WHERE LOWER(u.email) LIKE CONCAT(:keyword, '%@%')
    	ORDER BY u.id ASC
    """)
	Slice<User> findByEmailStartsWithFirstPage(@Param("keyword") String keyword, Pageable pageable);

	@Query("""
    	SELECT u FROM User u
    	WHERE u.id > :cursorId AND LOWER(u.email) LIKE CONCAT(:keyword, '%@%')
    	ORDER BY u.id ASC
    """)
	Slice<User> findByEmailStartsWithNextPage(@Param("keyword") String keyword, @Param("cursorId") Long cursorId, Pageable pageable);

	List<UserImageProjection> findAllProjectionByIdIn(List<Long> ids);
	default Map<Long, String> findImageMapByIdIn(List<Long> userIds) {
		return findAllProjectionByIdIn(userIds).stream()
				.collect(Collectors.toMap(
						UserImageProjection::getId,
						projection -> projection.getProfileImageKey() == null
								? ""
								: projection.getProfileImageKey()
				));
	}
}
