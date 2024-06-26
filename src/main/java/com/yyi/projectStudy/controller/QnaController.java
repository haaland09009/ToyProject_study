package com.yyi.projectStudy.controller;

import com.yyi.projectStudy.dto.*;
import com.yyi.projectStudy.service.*;
import com.yyi.projectStudy.time.StringToDate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {
    private final QnaService qnaService;
    private final QnaReplyService qnaReplyService;
    private final QnaReplyCommentService qnaReplyCommentService;
    private final UserService userService;
    private final utils utils;
    private final CookieService cookieService;

    /* 메인 페이지 - 기술, 커리어, 기타 모두 조회 */
    @GetMapping(value = {"", "/topic/{category}"})
    public String mainPage(Model model,
                           @PathVariable(name = "category", required = false) String category,
                           @RequestParam(name = "sortKey", required = false) String sortKey,
                           String searchWord,
                           @RequestParam(name = "tagName", required = false) String tagName) {


        /*List<QnaDTO> qnaDTOList = qnaService.findByCondition(category, sortKey, searchWord, tagName);
*/
        List<QnaDTO> qnaDTOList;
        if (category != null && category.equals("tech")) {
            if (sortKey != null) {
                qnaDTOList = qnaService.getQnaDTOListByReplySort(1L);
            } else {
                qnaDTOList = qnaService.findAllByTopic(1L);
            }
            model.addAttribute("category", "tech");
        } else if (category != null && category.equals("career")) {
            if (sortKey != null) {
                qnaDTOList = qnaService.getQnaDTOListByReplySort(2L);
            } else {
                qnaDTOList = qnaService.findAllByTopic(2L);
            }
            model.addAttribute("category", "career");
        } else if (category == null) {
            if (sortKey != null) {
                qnaDTOList = qnaService.getQnaDTOListByReplySort(null);
            } else {
                qnaDTOList = qnaService.findAll(searchWord, tagName);
            }
        } else {
            qnaDTOList = qnaService.findAll(searchWord, tagName);
        }


        if (category != null) {
            model.addAttribute("category", category);
        }

        /* 검색 키워드가 존재할 경우 */
        if (searchWord != null) {
            model.addAttribute("searchWord", searchWord);
        }

        /* 해시태그가 존재할 경우 */
        if (tagName != null) {
            model.addAttribute("tagName", tagName);
        }

        /* 기술, 커리어, 기타 카테고리 여부 dto에 저장 */
        for (QnaDTO qnaDTO : qnaDTOList) {

            /* 날짜 변환하기 */
            String formatDateTime = StringToDate.formatDateTime(String.valueOf(qnaDTO.getRegDate()));
            qnaDTO.setFormattedDate(formatDateTime);

            String content = qnaDTO.getContent();
            content = content.replaceAll("<br>", "\r\n");
            qnaDTO.setContent(content);

            TopicDTO topicDTO = qnaService.findTopic(qnaDTO.getId());
            qnaDTO.setCategoryName(topicDTO.getName());

            int replyCount = qnaReplyService.count(qnaDTO.getId());
            qnaDTO.setReplyCount(replyCount);

            /* 게시글 당 답변 모두 조회 */
            List<QnaReplyDTO> qnaReplyDTOList = qnaReplyService.findAll(qnaDTO.getId());
            int commentCount = 0;
            /* 답변에 달린 댓글 수 모두 조회 */
            for (QnaReplyDTO qnaReplyDTO : qnaReplyDTOList) {
                commentCount += qnaReplyCommentService.commentCount(qnaReplyDTO.getId());
            }
            qnaDTO.setCommentCount(commentCount);

            /* 해시태그 조회 */
            QnaTagsDTO qnaTagsDTO = qnaService.findHashTag(qnaDTO.getId());
            if (qnaTagsDTO != null) {
                qnaDTO.setHashTag(qnaTagsDTO.getTag());
            }
        }
        model.addAttribute("qnaList", qnaDTOList);

        /* 베스트 답변 */
        List<QnaBestReplyDTO> bestReplyList = qnaService.findBestReplyList();
        model.addAttribute("bestReplyList", bestReplyList);

        /* 베스트 질문 */
        List<QnaBestDTO> qnaBestDTOList = qnaService.findBestQnaList();
        model.addAttribute("bestQnaList", qnaBestDTOList);

        return "qna/list";
    }

    /* 답변이 등록되지 않은 질문 목록 */
    @GetMapping("/no-questions")
    public String mainPage(Model model) {

        List<QnaDTO> qnaDTOList = qnaService.getNoRelyQnaList();

        for (QnaDTO qnaDTO : qnaDTOList) {
            /* 날짜 변환하기 */
            String formatDateTime = StringToDate.formatDateTime(String.valueOf(qnaDTO.getRegDate()));
            qnaDTO.setFormattedDate(formatDateTime);

            String content = qnaDTO.getContent();
            content = content.replaceAll("<br>", "\r\n");
            qnaDTO.setContent(content);

            TopicDTO topicDTO = qnaService.findTopic(qnaDTO.getId());
            qnaDTO.setCategoryName(topicDTO.getName());

            int replyCount = qnaReplyService.count(qnaDTO.getId());
            qnaDTO.setReplyCount(replyCount);

            /* 댓글 수 */
            /* 게시글 당 답변 모두 조회 */
            List<QnaReplyDTO> qnaReplyDTOList = qnaReplyService.findAll(qnaDTO.getId());
            int commentCount = 0;
            /* 답변에 달린 댓글 수 모두 조회 */
            for (QnaReplyDTO qnaReplyDTO : qnaReplyDTOList) {
                commentCount += qnaReplyCommentService.commentCount(qnaReplyDTO.getId());
            }
            qnaDTO.setCommentCount(commentCount);

            /* 해시태그 조회 */
            QnaTagsDTO qnaTagsDTO = qnaService.findHashTag(qnaDTO.getId());
            if (qnaTagsDTO != null) {
                qnaDTO.setHashTag(qnaTagsDTO.getTag());
            }
        }
        model.addAttribute("qnaList", qnaDTOList);
        model.addAttribute("category", "no-Reply");
        return "qna/list";
    }



    /* 게시글 작성 폼 */
    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");
        if (sessionUser == null) {
            return "redirect:/user/loginPage";
        } else {
            List<TopicDTO> topicDTOList =  qnaService.findAllTopic();
            model.addAttribute("topicList", topicDTOList);
            return "qna/write";
        }
    }

    /* 게시글 작성 */
    @PostMapping("/write")
    public String write(@ModelAttribute QnaTopicDTO qnaTopicDTO,
                        @ModelAttribute QnaDTO qnaDTO,
                        @RequestParam(value = "tags", required = false) String tags) {

        String content = qnaDTO.getContent();
        content = content.replaceAll("\r\n", "<br>");
        qnaDTO.setContent(content);
        /* 게시글 저장 후 pk 가져오기 */
        Long savedId = qnaService.saveQna(qnaDTO);
        /* 게시글의 토픽 저장 */
        QnaDTO dto = qnaService.findById(savedId);
        TopicDTO topicDTO = qnaService.findByIdForTopic(qnaTopicDTO.getTopicId());
        qnaService.saveQnaTopic(dto, topicDTO);

        /* 해시태그 저장 */
        if (!tags.isEmpty()) {
            String tag = utils.hashtagParse(tags);
            qnaService.saveHashTag(dto, tag);
        }
        return "redirect:/qna/" + savedId;
    }


    /* 게시글 상세보기 */
    @GetMapping("/{id}")
    public String findById(@PathVariable("id") Long id, Model model, HttpSession session,
                           HttpServletRequest request, HttpServletResponse response) {
        /* 토픽 카테고리 조회 */
        TopicDTO topicDTO = qnaService.findTopic(id);
        model.addAttribute("topic", topicDTO);

        /* 게시글 정보 */
        QnaDTO qnaDTO = qnaService.findById(id);
        String content = qnaDTO.getContent();
/*        content = content.replaceAll("<br>", "\r\n");*/
        content = content.replaceAll("<br>", "\n");
        qnaDTO.setContent(content);
        model.addAttribute("qna", qnaDTO);

        /* 날짜 변환하기 */
        String formatDateTime = StringToDate.formatDateTime(String.valueOf(qnaDTO.getRegDate()));
        qnaDTO.setFormattedDate(formatDateTime);

        /* 클릭 시 조회수 증가 */
        cookieService.checkCookieForReadCount(request, response, "qna", id);


        /* 회원 직업 조회 */
        JobDTO jobDTO = userService.findJob(qnaDTO.getUserId());
        model.addAttribute("job", jobDTO.getName());

        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");

        /* 답변 목록 불러오기 */
        List<QnaReplyDTO> qnaReplyDTOList = qnaReplyService.findAll(id);

        for (QnaReplyDTO dto : qnaReplyDTOList) {
            /* 날짜 변환하기 */
            dto.setFormattedDate(StringToDate.formatDateTime(String.valueOf(dto.getRegDate())));

            String replyContent = dto.getContent();
            replyContent = replyContent.replaceAll("<br>", "\n");
            dto.setContent(replyContent);

            int likeCount = qnaReplyService.likeCount(dto.getId());
            dto.setLikeCount(likeCount);

            JobDTO replyJob = userService.findJob(dto.getUserId());
            dto.setJobName(replyJob.getName());

            /* 답변에 달린 댓글 목록 조회 */
            List<QnaReplyCommentDTO> qnaReplyCommentDTOList = qnaReplyCommentService.findAll(dto.getId());
            List<QnaReplyCommentDTO> commentList = new ArrayList<>();
            for (QnaReplyCommentDTO qnaReplyCommentDTO : qnaReplyCommentDTOList) {
                /* 날짜 변환하기 */
                qnaReplyCommentDTO.setFormattedDate(StringToDate.formatDateTime(String.valueOf(qnaReplyCommentDTO.getRegDate())));

                String commentContent = qnaReplyCommentDTO.getContent();
                commentContent = commentContent.replaceAll("<br>", "\n");
                qnaReplyCommentDTO.setContent(commentContent);

                JobDTO commentJob = userService.findJob(qnaReplyCommentDTO.getUserId());
                qnaReplyCommentDTO.setJobName(commentJob.getName());

                commentList.add(qnaReplyCommentDTO);
            }
            dto.setCommentList(commentList);

            /* 댓글 수 */
            int commentCount = qnaReplyCommentService.commentCount(dto.getId());
            dto.setCommentCount(commentCount);

            if (sessionUser == null) {
                dto.setIsLike(0);
            } else {
                int isLike = qnaReplyService.checkReplyLikeForColor(dto.getId(), sessionUser.getId());
                dto.setIsLike(isLike);
            }
        }
        model.addAttribute("replyList", qnaReplyDTOList);

        /* 답변 수 */
        int replyCount = qnaReplyService.count(qnaDTO.getId());
        model.addAttribute("replyCount", replyCount);

        /* 좋아요, 싫어요 수 */
        int likeCount = qnaService.likeCount(id);
        int dislikeCount = qnaService.disLikeCount(id);
        qnaDTO.setLikeCount(likeCount);
        qnaDTO.setDislikeCount(dislikeCount);

        /* 좋아요, 싫어요 여부 */

        if (sessionUser == null) {
            model.addAttribute("isLike", 0);
            model.addAttribute("isDisLike", 0);
        } else {
            int isLike = qnaService.checkQnaLikeForColor(id, sessionUser.getId());
            int isDisLike = qnaService.checkQnaDisLikeForColor(id, sessionUser.getId());
            model.addAttribute("isLike", isLike);
            model.addAttribute("isDisLike", isDisLike);
        }

        /* 랜덤 게시글 */
        List<QnaDTO> randomQnaList = qnaService.randomQnaList(id);
        for (QnaDTO randomDTO : randomQnaList) {
            /* 추천해요 수 */
            randomDTO.setLikeCount(qnaService.likeCount(randomDTO.getId()));
            /* 답변 수 */
            randomDTO.setReplyCount(qnaReplyService.count(randomDTO.getId()));
        }
        model.addAttribute("randomQnaList", randomQnaList);

        /* 게시물 스크랩 여부 확인 */
        if (sessionUser != null) {
            QnaClipDTO qnaClipDTO = new QnaClipDTO();
            qnaClipDTO.setQnaId(id);
            qnaClipDTO.setUserId(sessionUser.getId());
            int clipCount = qnaService.checkClipYn(qnaClipDTO);
            model.addAttribute("clipCount", clipCount);
        }

        /* 해시태그 조회 */
        QnaTagsDTO qnaTagsDTO = qnaService.findHashTag(id);
        if (qnaTagsDTO != null) {
            qnaDTO.setHashTag(qnaTagsDTO.getTag());
        }

       /* 베스트 답변 pk 리스트 조회 */
        List<Long> bestReplyPkList = qnaReplyService.getBestReplyList();
        model.addAttribute("bestReplyPkList", bestReplyPkList);

        return "qna/detail";
    }


    /* 게시물 삭제 이전에 작성된 답변 수 확인 */
    @GetMapping("/checkReplyCount/{id}")
    public @ResponseBody int checkReplyCount(@PathVariable("id") Long id) {
        return qnaReplyService.count(id);
    }

    /* 게시글 삭제 */
    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable("id") Long id) {
        qnaService.deleteById(id);
        return "redirect:/qna";
    }

    /* 게시글 수정 폼 */
    @GetMapping("/update/{id}")
    public String updateForm(@PathVariable("id") Long id, Model model, HttpSession session) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");
        if (sessionUser == null) {
            return "redirect:/user/loginPage";
        } else {
            /* 모든 토픽 조회 */
            List<TopicDTO> topicDTOList =  qnaService.findAllTopic();
            model.addAttribute("topicList", topicDTOList);

            QnaDTO qnaDTO = qnaService.findById(id);
            String content = qnaDTO.getContent();
            content = content.replaceAll("<br>", "\n");
            qnaDTO.setContent(content);

            TopicDTO topicDTO = qnaService.findTopic(id);
            model.addAttribute("qna", qnaDTO);
            model.addAttribute("selectedTopic", topicDTO);

            QnaTagsDTO qnaTagsDTO = qnaService.findHashTag(id);

            /* 해시태그 분리 */
            String tag = "";
            if (qnaTagsDTO != null && !utils.isStringEmptyOrNull(qnaTagsDTO.getTag())) {
                tag = utils.hashtagSeparate(qnaTagsDTO.getTag());
                model.addAttribute("hashTags", tag);
            }

            return "qna/update";
        }
    }

    /* 게시글 수정 */
    @PostMapping("/update")
    public String update(@ModelAttribute QnaDTO qnaDTO,
                         @ModelAttribute QnaTopicDTO qnaTopicDTO,
                         Model model, HttpSession session,
                         @RequestParam(value = "tags", required = false) String tags) {
        QnaDTO updateQnaDTO = qnaService.updateQna(qnaDTO);

        String content = updateQnaDTO.getContent();
        content = content.replaceAll("<br>", "\r\n");
        updateQnaDTO.setContent(content);

        TopicDTO updateTopicDTO = qnaService.updateQnaTopic(qnaDTO.getId(), qnaTopicDTO);

        model.addAttribute("qna", updateQnaDTO);
        model.addAttribute("topic", updateTopicDTO);

        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");
        JobDTO jobDTO = userService.findJob(sessionUser.getId());
        model.addAttribute("job", jobDTO.getName());

        List<QnaReplyDTO> qnaReplyDTOList = qnaReplyService.findAll(qnaDTO.getId());

        for (QnaReplyDTO dto : qnaReplyDTOList) {
            String replyContent = dto.getContent();
            replyContent = replyContent.replaceAll("<br>", "\n");
            dto.setContent(replyContent);
            int likeCount = qnaReplyService.likeCount(dto.getId());
            dto.setLikeCount(likeCount);

            JobDTO replyJob = userService.findJob(dto.getUserId());
            dto.setJobName(replyJob.getName());

            /* 날짜 변환하기 */
            String formatDateTime = StringToDate.formatDateTime(String.valueOf(dto.getRegDate()));
            dto.setFormattedDate(formatDateTime);

            List<QnaReplyCommentDTO> qnaReplyCommentDTOList = qnaReplyCommentService.findAll(dto.getId());
            List<QnaReplyCommentDTO> commentList = new ArrayList<>();
            for (QnaReplyCommentDTO qnaReplyCommentDTO : qnaReplyCommentDTOList) {
                String commentContent = qnaReplyCommentDTO.getContent();
                commentContent = commentContent.replaceAll("<br>", "\n");
                qnaReplyCommentDTO.setContent(commentContent);
                commentList.add(qnaReplyCommentDTO);

                JobDTO commentJob = userService.findJob(qnaReplyCommentDTO.getUserId());
                qnaReplyCommentDTO.setJobName(commentJob.getName());

                /* 날짜 변환하기 */
                String formatDateTimeComment = StringToDate.formatDateTime(String.valueOf(qnaReplyCommentDTO.getRegDate()));
                qnaReplyCommentDTO.setFormattedDate(formatDateTimeComment);
            }
            dto.setCommentList(commentList);

            int commentCount = qnaReplyCommentService.commentCount(dto.getId());
            dto.setCommentCount(commentCount);


            if (sessionUser == null) {
                dto.setIsLike(0);
            } else {
                int isLike = qnaReplyService.checkReplyLikeForColor(dto.getId(), sessionUser.getId());
                dto.setIsLike(isLike);
            }
        }

        model.addAttribute("replyList", qnaReplyDTOList);

        int replyCount = qnaReplyService.count(qnaDTO.getId());
        model.addAttribute("replyCount", replyCount);

        int likeCount = qnaService.likeCount(qnaDTO.getId());
        int dislikeCount = qnaService.disLikeCount(qnaDTO.getId());
        updateQnaDTO.setLikeCount(likeCount);
        updateQnaDTO.setDislikeCount(dislikeCount);

        if (sessionUser == null) {
            model.addAttribute("isLike", 0);
            model.addAttribute("isDisLike", 0);
        } else {
            int isLike = qnaService.checkQnaLikeForColor(qnaDTO.getId(), sessionUser.getId());
            int isDisLike = qnaService.checkQnaDisLikeForColor(qnaDTO.getId(), sessionUser.getId());
            model.addAttribute("isLike", isLike);
            model.addAttribute("isDisLike", isDisLike);
        }

        List<QnaDTO> randomQnaList = qnaService.randomQnaList(qnaDTO.getId());
        for (QnaDTO randomDTO : randomQnaList) {
            randomDTO.setLikeCount(qnaService.likeCount(randomDTO.getId()));
            randomDTO.setReplyCount(qnaReplyService.count(randomDTO.getId()));
        }
        model.addAttribute("randomQnaList", randomQnaList);

        /* 해시태그 저장 */
        if (!tags.isEmpty()) {
            String tag = utils.hashtagParse(tags);
            qnaService.saveHashTag(qnaDTO, tag);
        }
        // !!!! 해시태그 조회 (수정 시 태그 안보여지는 오류)
        QnaTagsDTO qnaTagsDTO = qnaService.findHashTag(qnaDTO.getId());
        if (qnaTagsDTO != null) {
            updateQnaDTO.setHashTag(qnaTagsDTO.getTag());
        }

        /* 베스트 답변 pk 리스트 조회 */
        List<Long> bestReplyPkList = qnaReplyService.getBestReplyList();
        model.addAttribute("bestReplyPkList", bestReplyPkList);


        return "qna/detail";
    }

    /* 본인이 작성한 게시글인지 확인 (만약 눌렀을 때 삭제된 게시물일 경우 후처리 해야함) */
    @GetMapping("/isYourQna")
    public @ResponseBody boolean isYourQna(@ModelAttribute QnaDTO qnaDTO) {

        Long userId = qnaDTO.getUserId(); // sessionId
        Long writerId = qnaService.isYourQna(qnaDTO.getId());
        if (userId.equals(writerId)) {
            return true;
        } else {
            return false;
        }
    }

    /* 게시글 좋아요를 눌렀을 때 싫어요 여부 확인 */
    @GetMapping("/checkQnaDisLike")
    public @ResponseBody int checkQnaDisLike(@ModelAttribute QnaDTO qnaDTO) {
        return qnaService.checkQnaDisLike(qnaDTO);
    }


    /* 게시글 좋아요 클릭 */
    @PostMapping("/like")
    public @ResponseBody void like(@ModelAttribute QnaLikeDTO qnaLikeDTO) {
        qnaService.like(qnaLikeDTO);
    }

    /* 댓글 좋아요 수 확인 */
    @GetMapping("/likeCount/{id}")
    public @ResponseBody int qnaLikeCount(@PathVariable("id") Long id) {
        return qnaService.likeCount(id);
    }

    /* 게시글 싫어요를 눌렀을 때 좋어요 여부 확인 */
    @GetMapping("/checkQnaLike")
    public @ResponseBody int checkQnaLike(@ModelAttribute QnaDTO qnaDTO) {
        return qnaService.checkQnaLike(qnaDTO);
    }

    /* 게시글 싫어요 클릭 */
    @PostMapping("/dislike")
    public @ResponseBody void disLike(@ModelAttribute QnaDisLikeDTO qnaDisLikeDTO) {
        qnaService.disLike(qnaDisLikeDTO);
    }

    /* 게시글 싫어요 수 확인 */
    @GetMapping("/disLikeCount/{id}")
    public @ResponseBody int qnaDisLikeCount(@PathVariable("id") Long id) {
        return qnaService.disLikeCount(id);
    }

    /* 사용자가 게시글에 좋아요를 눌렀는지 확인 (색깔 변경 목적) */
    @GetMapping("/checkQnaLikeForColor/{id}")
    public @ResponseBody boolean checkQnaLikeForColor(@PathVariable("id") Long id,
            HttpSession session) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");

        int count = qnaService.checkQnaLikeForColor(id, sessionUser.getId());
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    /* 사용자가 게시글에 싫어요를 눌렀는지 확인 (색깔 변경 목적) */
    @GetMapping("/checkQnaDisLikeForColor/{id}")
    public @ResponseBody boolean checkQnaDisLikeForColor(@PathVariable("id") Long id,
                                                         HttpSession session) {
        UserDTO sessionUser = (UserDTO) session.getAttribute("userDTO");
        int count = qnaService.checkQnaDisLikeForColor(id, sessionUser.getId());
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    /* 게시물 스크랩 */
    @PostMapping("/clip")
    public @ResponseBody void clip(@ModelAttribute QnaClipDTO qnaClipDTO) {
        qnaService.clip(qnaClipDTO);
    }

    /* 게시물 스크랩 여부 확인 */
    @GetMapping("/checkClipYn")
    public @ResponseBody boolean checkClipYn(@ModelAttribute QnaClipDTO qnaClipDTO) {
        int count = qnaService.checkClipYn(qnaClipDTO);
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

}
