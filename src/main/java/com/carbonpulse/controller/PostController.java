package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.CommentService;
import com.carbonpulse.service.LikeRecordService;
import com.carbonpulse.service.PostService;
import com.carbonpulse.utils.JwtUtil;
import com.carbonpulse.common.PaginatedResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeRecordService likeRecordService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 发布动态
     * POST /api/post/create
     * 请求体：{ "content": "...", "images": "[\"url1\",\"url2\"]", "relatedBehaviorId": 123 }
     */
    @PostMapping("/create")
    public Result createPost(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String content = (String) params.get("content");
            String images = (String) params.get("images");
            Long relatedBehaviorId = params.get("relatedBehaviorId") != null ?
                    Long.valueOf(params.get("relatedBehaviorId").toString()) : null;
            Long postId = postService.createPost(userId, content, images, relatedBehaviorId);
            return Result.success(postId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 动态列表（分页）
     * GET /api/post/list?page=1&size=10
     */
    @GetMapping("/list")
    public Result getPostList(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        try {
            PaginatedResult<Map<String, Object>> result =
                    postService.getPostList(page, size);
            return Result.success(result);
        } catch (Exception e) {
            // 保留原始异常信息
            String errorMsg = "分页查询失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Result.error(errorMsg);
        }
    }

    /**
     * 动态详情
     * GET /api/post/detail/{postId}
     */
    @GetMapping("/detail/{postId}")
    public Result getPostDetail(@PathVariable Long postId) {
        try {
            return Result.success(postService.getPostDetail(postId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除动态
     * DELETE /api/post/delete/{postId}
     */
    @DeleteMapping("/delete/{postId}")
    public Result deletePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = postService.deletePost(postId, userId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加评论
     * POST /api/post/comment
     * 请求体：{ "postId": 1, "content": "好棒！" }
     */
    @PostMapping("/comment")
    public Result addComment(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long postId = Long.valueOf(params.get("postId").toString());
            String content = (String) params.get("content");
            Long commentId = commentService.addComment(postId, userId, content);
            return Result.success(commentId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除评论
     * DELETE /api/post/comment/{commentId}
     */
    @DeleteMapping("/comment/{commentId}")
    public Result deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = commentService.deleteComment(commentId, userId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 点赞/取消点赞
     * POST /api/post/like
     * 请求体：{ "postId": 1 }
     */
    @PostMapping("/like")
    public Result likeOrUnlike(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long postId = Long.valueOf(params.get("postId").toString());
            boolean liked = likeRecordService.likeOrUnlike(postId, userId);
            String msg = liked ? "点赞成功" : "取消点赞成功";
            return Result.success(msg);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 动态列表（分页）
     * GET /api/post/list?page=1&size=10
     */
    @GetMapping("/user")
    public Result getPostListByUserId(@RequestParam Long userId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        try {

            PaginatedResult<Map<String, Object>> result =
                    postService.getPostListById(page, size, userId);
            return Result.success(result);
        } catch (Exception e) {
            // 保留原始异常信息
            String errorMsg = "分页查询失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Result.error(errorMsg);
        }
    }
}