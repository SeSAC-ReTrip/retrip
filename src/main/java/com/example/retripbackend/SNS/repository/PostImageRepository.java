package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByPostOrderByDisplayOrderAsc(Post post);
    void deleteByPost(Post post);
}
