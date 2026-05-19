package com.example.mentalcompanion.service;

import com.alibaba.excel.EasyExcel;
import com.example.mentalcompanion.config.AppProperties;
import com.example.mentalcompanion.domain.entity.ExcelExportLog;
import com.example.mentalcompanion.domain.entity.WorkflowRecord;
import com.example.mentalcompanion.excel.WorkflowExcelRow;
import com.example.mentalcompanion.mapper.ExcelExportLogMapper;
import com.example.mentalcompanion.mapper.WorkflowRecordMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelExportService {

    private final AppProperties appProperties;
    private final ExcelExportLogMapper excelExportLogMapper;
    private final WorkflowRecordMapper workflowRecordMapper;

    public ExcelExportService(
            AppProperties appProperties,
            ExcelExportLogMapper excelExportLogMapper,
            WorkflowRecordMapper workflowRecordMapper
    ) {
        this.appProperties = appProperties;
        this.excelExportLogMapper = excelExportLogMapper;
        this.workflowRecordMapper = workflowRecordMapper;
    }

    public synchronized Path appendWorkflowRecord(WorkflowRecord record) {
        Path path = Path.of(appProperties.excel().workflowPath());
        try {
            ensureParent(path);
            List<WorkflowExcelRow> rows = new ArrayList<>();
            File file = path.toFile();
            if (file.exists() && file.length() > 0) {
                rows.addAll(EasyExcel.read(file, WorkflowExcelRow.class, null).sheet().doReadSync());
            }
            rows.add(WorkflowExcelRow.from(record));
            EasyExcel.write(file, WorkflowExcelRow.class).sheet("workflow").doWrite(rows);
            record.setExcelExported(true);
            workflowRecordMapper.updateById(record);
            saveLog(record.getId(), path.toString(), "SUCCESS");
            return path;
        } catch (RuntimeException ex) {
            saveLog(record.getId(), path.toString(), "FAILED");
            throw ex;
        }
    }

    public Path exportAll(List<WorkflowRecord> records) {
        Path exportDir = Path.of(appProperties.excel().exportDir());
        String fileName = "workflow-records-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        Path path = exportDir.resolve(fileName);
        try {
            ensureParent(path);
            List<WorkflowExcelRow> rows = records.stream().map(WorkflowExcelRow::from).toList();
            EasyExcel.write(path.toFile(), WorkflowExcelRow.class).sheet("workflow").doWrite(rows);
            saveLog(null, path.toString(), "SUCCESS");
            return path;
        } catch (RuntimeException ex) {
            saveLog(null, path.toString(), "FAILED");
            throw ex;
        }
    }

    private void ensureParent(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create excel directory", ex);
        }
    }

    private void saveLog(Long recordId, String filePath, String status) {
        ExcelExportLog log = new ExcelExportLog();
        log.setRecordId(recordId);
        log.setFilePath(filePath);
        log.setExportStatus(status);
        log.setCreateTime(LocalDateTime.now());
        excelExportLogMapper.insert(log);
    }
}

