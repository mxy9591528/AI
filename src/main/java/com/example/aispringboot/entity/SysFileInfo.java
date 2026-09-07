package com.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value="sys_file_info")
public class SysFileInfo {
    @TableId(type=IdType.AUTO)
    private Long id;
    @TableField(value="original_name")
    private String originalName;
    @TableField(value="file_path")
    private String filePath;
    @TableField(value="file_size")
    private Long fileSize;
    @TableField(value="file_type")
    private String fileType;
    @TableField(value="business_type")
    private String businessType;
    @TableField(value="business_id")
    private String businessId;
    @TableField(value="business_field")
    private String businessField;
    @TableField(value="upload_user_id")
    private Long uploadUserId;
    @TableField(value="is_temp")
    private Integer isTemp;
    private Integer status;
    @TableField(value="create_time")
    private LocalDateTime createTime;
    @TableField(value="expire_time")
    private LocalDateTime expireTime;
}

