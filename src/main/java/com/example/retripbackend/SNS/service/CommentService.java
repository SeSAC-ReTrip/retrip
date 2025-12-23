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

    // 댓글 삭제
    @Transactional
    public void deleteComment(Comment comment) {
        Post post = comment.getPost();

        // 게시글의 댓글 수 감소
        post.decrementCommentCount();

        commentRepository.delete(comment);
    }
}
