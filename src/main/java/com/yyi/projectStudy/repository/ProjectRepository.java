package com.yyi.projectStudy.repository;

import com.yyi.projectStudy.entity.ProjectEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
@Primary

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, ProjectCustom {
    /* 게시글 조회수 증가 */
    @Modifying
    @Query(value = "update ProjectEntity p set p.readCount = p.readCount + 1 where p.id = :id")
    void updateReadCount(@Param("id") Long id);

    /* 게시글 수정 */
    @Modifying
    @Query(value = "update ProjectEntity p set p.title = :title, " +
                    "p.content = :content, p.startDate = :startDate, " +
                    "p.headCount = :headCount, p.updDate = sysdate where p.id = :id")
    void updateProject(@Param("title") String title, @Param("content") String content,
                       @Param("startDate") Date startDate, @Param("headCount") int headCount,
                       @Param("id") Long id);



   /* 메인 페이지 HOT 프로젝트 / 스터디 조회 (일단 조회수 순, 나중에 수정해야함!!!) */
    @Query(value = "select * from (select * from project_table where status = '모집중'" +
                    " order by read_count desc) where rownum <= 6", nativeQuery = true)
    List<ProjectEntity> findAllByOrderByReadCountDesc();



    // 프로젝트 메뉴 - 활동 내역
/*    SELECT * FROM (
            SELECT id, title, content, reg_date FROM project_table WHERE user_id = 43
            UNION ALL
            SELECT id, NULL AS title, content, reg_date FROM project_comment_table WHERE user_id = 43
    )
    ORDER BY reg_date DESC*/
    @Query(value = "SELECT * FROM (\n" +
            "    SELECT id, title, content, reg_date FROM project_table WHERE user_id = :userId\n" +
            "    UNION ALL\n" +
            "    SELECT id, NULL AS title, content, reg_date FROM project_comment_table WHERE user_id = :userId\n" +
            ")\n" +
            "ORDER BY reg_date DESC", nativeQuery = true)
    List<Object[]> getProjectArticles(@Param("userId") Long userId);


    /* Top writers */
    @Query(value = "SELECT user_id, SUM(count) AS total_count\n" +
            "FROM (\n" +
            "    SELECT user_id, COUNT(*) AS count\n" +
            "    FROM project_table\n" +
            "    GROUP BY user_id\n" +
            "    UNION ALL\n" +
            "    SELECT user_id, COUNT(*) AS count\n" +
            "    FROM qna_table\n" +
            "    GROUP BY user_id\n" +
            ")\n" +
            "GROUP BY user_id\n" +
            "ORDER BY total_count DESC", nativeQuery = true)
    List<Object[]> getTopWriters();



   /* 프로젝트, 스터디 여부 확인  */
    @Query(value = "select p from ProjectEntity p join projectStudyCategoryLinkEntityList psc\n" +
                    "where psc.projectStudyCategoryEntity.id = :projectStudyId order by p.id desc")
    List<ProjectEntity> getProjectListByCategory(@Param("projectStudyId") Long projectStudyId);





}
