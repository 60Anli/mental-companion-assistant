package com.example.mentalcompanion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.mapper.WorkflowRecordMapper;
import com.example.mentalcompanion.service.ExcelExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/admin/workflow-records")
public class AdminWorkflowController {

    private final WorkflowRecordMapper workflowRecordMapper;
    private final ExcelExportService excelExportService;

    public AdminWorkflowController(WorkflowRecordMapper workflowRecordMapper, ExcelExportService excelExportService) {
        this.workflowRecordMapper = workflowRecordMapper;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowRecord>> list() {
        return ApiResponse.ok(records());
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> export() {
        Path path = excelExportService.exportAll(records());
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    private List<WorkflowRecord> records() {
        return workflowRecordMapper.selectList(new LambdaQueryWrapper<WorkflowRecord>()
                .orderByDesc(WorkflowRecord::getCreateTime));
    }
}

