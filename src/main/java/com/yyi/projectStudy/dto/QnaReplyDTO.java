package com.yyi.projectStudy.dto;

import com.yyi.projectStudy.entity.QnaReplyEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QnaReplyDTO {
    private Long id;
    private Long qnaId;
    private Long userId;
    private String content;
    private LocalDateTime regDate;

    private String writer;
    private int fileAttached;
    private String storedFileName;

    public static QnaReplyDTO toQnaReplyDTO(QnaReplyEntity qnaReplyEntity) {
        QnaReplyDTO qnaReplyDTO = new QnaReplyDTO();
        qnaReplyDTO.setId(qnaReplyEntity.getId());
        qnaReplyDTO.setQnaId(qnaReplyEntity.getQnaEntity().getId());
        qnaReplyDTO.setUserId(qnaReplyEntity.getUserEntity().getId());
        qnaReplyDTO.setContent(qnaReplyEntity.getContent());
        qnaReplyDTO.setRegDate(qnaReplyEntity.getRegDate());

        qnaReplyDTO.setWriter(qnaReplyEntity.getUserEntity().getNickname());
        qnaReplyDTO.setFileAttached(qnaReplyEntity.getUserEntity().getFileAttached());
        if (qnaReplyEntity.getUserEntity().getFileAttached() == 1) {
            qnaReplyDTO.setStoredFileName(qnaReplyEntity.getUserEntity().getUserImageFileEntityList().get(0).getStoredFileName());
        }
        return qnaReplyDTO;
    }
}