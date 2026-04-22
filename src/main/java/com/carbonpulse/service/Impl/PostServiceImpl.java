package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.entity.Comment;
import com.carbonpulse.entity.LikeRecord;
import com.carbonpulse.entity.Post;
import com.carbonpulse.mapper.CommentMapper;
import com.carbonpulse.mapper.LikeRecordMapper;
import com.carbonpulse.mapper.PostMapper;
import com.carbonpulse.mapper.UserMapper;
import com.carbonpulse.service.PostService;
import com.carbonpulse.common.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private LikeRecordMapper likeRecordMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long userId, String content, String images, Long relatedBehaviorId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(images);
        post.setRelatedBehaviorId(relatedBehaviorId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        this.save(post);
        return post.getId();
    }


    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "post:list:";
    // 缓存过期时间（秒）
    private static final long CACHE_EXPIRE_SECONDS = 300; // 5分钟

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Map<String, Object>> getPostList(int page, int size) {
        // 1. 参数校验
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;

        // 2. 构建缓存键
        String cacheKey = CACHE_KEY_PREFIX + page + ":" + size;

        // 3. 尝试从 Redis 获取缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                // 假设存储的是 JSON 字符串
                String json = (String) cached;
                PaginatedResult<Map<String, Object>> result = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructParametricType(PaginatedResult.class, Map.class));
                return result;
            } catch (Exception e) {
                // 反序列化失败，继续从数据库查询
                log.warn("Failed to deserialize cached post list");
            }
        }

        // 4. 从数据库查询
        int offset = (page - 1) * size;
        List<Map<String, Object>> currentPageData = postMapper.selectPostListWithUser(offset, size);
        long totalRecords = postMapper.selectPostCount();

        // 5. 构造分页结果
        PaginatedResult<Map<String, Object>> result = new PaginatedResult<>(currentPageData, totalRecords, page, size);

        // 6. 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(CACHE_EXPIRE_SECONDS));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize post list for cache", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getPostDetail(Long postId) {
        // 查询动态基础信息
        Post post = this.getById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }
        // 查询作者信息
        Map<String, Object> author = userMapper.selectByIdWithMap(post.getUserId());
        // 查询评论列表（包含评论人信息）
        List<Map<String, Object>> comments = commentMapper.selectByPostIdWithUser(postId);
        Map<String, Object> detail = new HashMap<>();
        detail.put("post", post);
        detail.put("author", author);
        detail.put("comments", comments);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(Long postId, Long userId) {
        Post post = this.getById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }
        // 权限校验：只有作者本人可以删除
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此动态");
        }
        // 删除评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        // 删除点赞记录
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>().eq(LikeRecord::getPostId, postId));
        // 删除动态本身
        return this.removeById(postId);
    }

    @Override
    public PaginatedResult<Map<String, Object>> getPostListById(int page, int size, Long userId) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> data = postMapper.selectPostsByUserId(userId, offset, size);
        long total = postMapper.selectPostCountByUserId(userId);
        return new PaginatedResult<>(data, total, page, size);
    }

}