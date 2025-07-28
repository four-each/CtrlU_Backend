package org.example.ctrlu.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {
	PROFILE("profiles"),
	START_IMAGE("startImage"),
	END_IMAGe("endImage");

	private final String path;
}