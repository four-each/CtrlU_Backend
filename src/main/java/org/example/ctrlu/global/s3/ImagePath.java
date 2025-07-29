package org.example.ctrlu.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImagePath {
	PROFILE("user/profile"),
	START_IMAGE("todo/startImage"),
	END_IMAGe("todo/endImage");

	private final String path;
}