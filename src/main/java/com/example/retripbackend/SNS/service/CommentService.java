package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Comment;
import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.repository.CommentRepository;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;

    // 댓글 목록 조회
    public List<Comment> getPostComments(Post post) {
        return commentRepository.findByPostOrderByCreatedAtAsc(post);
    }

    // 댓글 작성
    @Transactional
    public Comment createComment(Post post, User author, String content) {
        // Comment.of()가 내부적으로 빌더나 정적 팩토리 메서드로 잘 구현되어 있다고 가정합니다.
        Comment comment = Comment.of(post, author, content);

        // 게시글의 댓글 수 증가
        post.incrementCommentCount();

        return commentRepository.save(comment);
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Comment comment, String content) {
        comment.update(content);
    }

    /**
     * ✅ 댓글 삭제 (컨트롤러의 호출 방식에 맞게 수정)
     * @param commentId 삭제할 댓글의 ID
     * @param currentUser 삭제를 요청한 현재 로그인 유저
     */
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        // 2. 권한 체크 (작성자 본인 확인)
        // Comment 엔티티에 isAuthor 또는 getAuthor() 메서드가 있어야 합니다.
        if (!comment.getAuthor().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("댓글 삭제 권한이 없습니다.");
        }

        // 3. 게시글의 댓글 수 감소
        Post post = comment.getPost();
        post.decrementCommentCount();

        // 4. 삭제 수행
        commentRepository.delete(comment);
    }
}
