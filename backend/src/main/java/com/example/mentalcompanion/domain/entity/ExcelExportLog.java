package com.example.mentalcompanion.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("excel_export_log")
public class ExcelExportLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String filePath;
    private String exportStatus;
    private LocalDateTime createTime;
}

