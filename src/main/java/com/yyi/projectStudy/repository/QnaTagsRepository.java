package com.yyi.projectStudy.repository;

import com.yyi.projectStudy.entity.QnaEntity;
import com.yyi.projectStudy.entity.QnaTagsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QnaTagsRepository extends JpaRepository<QnaTagsEntity, Long> {

    // 게시물 당 해시태그 조회
    //   select * from qna_tags_table;
    Optional<QnaTagsEntity> findByQnaEntity(QnaEntity qnaEntity);


    // 해시태그 수정
    @Modifying
    @Query(value = "update qna_tags_table q set q.tag = :tag where qna_id = :qnaId", nativeQuery = true)
    void updateTags(@Param("qnaId") Long qnaId, @Param("tag") String tag);
}
