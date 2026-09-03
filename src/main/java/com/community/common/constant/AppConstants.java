package com.community.common.constant;

import java.util.List;

public final class AppConstants {

    // JWT
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    // Redis
    // 인증
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String BLACKLIST_PREFIX = "blacklist:";

    // 게시글 조회수
    public static final String POST_VIEW_WEEKLY_PREFIX = "post:view:week:";
    public static final String POST_VIEW_DEDUP_PREFIX = "post:view:dedup:";

    // 사용자 랭킹
    public static final String USER_RANK_WEEKLY_PREFIX = "user:rank:week";
    public static final String USER_RANK_DEDUP_VISIT_PREFIX = "user:rank:dedup:visit:";

    // TTL
    public static final long POST_VIEW_WEEKLY_DAYS = 8;
    public static final long POST_VIEW_DEDUP_DAYS = 1;
    public static final long USER_RANK_WEEKLY_DAYS = 8;
    public static final long USER_RANK_DEDUP_VISIT_HOURS = 26;

    // 게시물
    public static final int POST_MAX_PINNED_COUNT = 10;
    
    // 댓글
    public static final int COMMENT_MAX_DEPTH = 1;

    // 사용자 등급
    public static final int GOLD_MIN_VISIT_COUNT = 30;
    public static final int GOLD_MIN_POST_COUNT = 10;
    public static final int GOLD_MIN_COMMENT_COUNT = 30;
    public static final int SILVER_MIN_VISIT_COUNT = 10;
    public static final int SILVER_MIN_POST_COUNT = 3;
    public static final int SILVER_MIN_COMMENT_COUNT = 10;

    // 파일
    public static final String FILE_UPLOAD_DIR = "uploads/";
    public static final long FILE_MAX_SIZE = 10 * 1024 * 1024;  // 10MB
    public static final int FILE_MAX_COUNT = 10;
    public static final int FILE_RETENTION_DAYS = 30;
    public static final List<String> FILE_ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "txt", "doc", "docx", "xls", "xlsx",
            "json", "xml", "csv", "md", "zip"
    );
    public static final List<String> FILE_BLOCKED_EXTENSIONS = List.of(
            "exe", "bat", "sh", "jar", "war", "dll",
            "jsp", "php", "asp", "aspx"
    );
}
