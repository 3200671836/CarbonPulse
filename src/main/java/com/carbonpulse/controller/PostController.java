package com.carbonpulse.controller;

import com.carbonpulse.common.Result;
import com.carbonpulse.service.CommentService;
import com.carbonpulse.service.LikeRecordService;
import com.carbonpulse.service.PostService;
import com.carbonpulse.utils.JwtUtil;
import com.carbonpulse.common.PaginatedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
@Tag(name = "社区动态", description = "动态发布、列表浏览、评论、点赞")
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

    @Operation(summary = "发布动态", description = "创建一条社区动态，可关联低碳行为记录")
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

    @Operation(summary = "动态列表（分页）", description = "获取全站动态，支持分页，无需登录")
    @GetMapping("/list")
    public Result getPostList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        try {
            PaginatedResult<Map<String, Object>> result = postService.getPostList(page, size);
            return Result.success(result);
        } catch (Exception e) {
            String errorMsg = "分页查询失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Result.error(errorMsg);
        }
    }

    @Operation(summary = "动态详情", description = "根据动态ID获取详情，含作者信息、评论、点赞数")
    @GetMapping("/detail/{postId}")
    public Result getPostDetail(
            @Parameter(description = "动态ID") @PathVariable Long postId) {
        try {
            return Result.success(postService.getPostDetail(postId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除动态", description = "仅作者可删除自己的动态")
    @DeleteMapping("/delete/{postId}")
    public Result deletePost(
            @Parameter(description = "动态ID") @PathVariable Long postId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = postService.deletePost(postId, userId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "添加评论", description = "对指定动态发表评论")
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

    @Operation(summary = "删除评论", description = "仅评论作者可删除自己的评论")
    @DeleteMapping("/comment/{commentId}")
    public Result deleteComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = commentService.deleteComment(commentId, userId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "点赞/取消点赞", description = "对动态进行点赞或取消点赞（切换操作）")
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

    @Operation(summary = "用户动态列表（分页）", description = "获取指定用户的动态列表")
    @GetMapping("/user")
    public Result getPostListByUserId(
            @Parameter(description = "目标用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        try {
            PaginatedResult<Map<String, Object>> result = postService.getPostListById(page, size, userId);
            return Result.success(result);
        } catch (Exception e) {
            String errorMsg = "分页查询失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Result.error(errorMsg);
        }
    }
}
