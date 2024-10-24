package org.project.paysystem.dto;

import lombok.Builder;
import lombok.Getter;
import org.project.paysystem.entity.Video;
import org.project.paysystem.entity.VideoStatus;

@Getter
@Builder
public class UserHistoryResponseDto {
    private Long id;
    private UserResponseDto user;
    private Video video;
    private VideoStatus status;

}
