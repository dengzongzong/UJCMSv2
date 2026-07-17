package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.CertificateFieldDTO;
import com.exam.entity.CertificateField;
import com.exam.service.CertificateFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/certificate/field")
public class CertificateFieldController {

    @Autowired
    private CertificateFieldService fieldService;

    @GetMapping("/list")
    public Result<List<CertificateField>> list(@RequestParam(required = false) Integer type) {
        List<CertificateField> data;
        if (type != null && type == 1) {
            data = fieldService.listSystem();
        } else if (type != null && type == 2) {
            data = fieldService.listCustom();
        } else {
            data = fieldService.listAll();
        }
        return Result.success(data);
    }

    @PostMapping
    public Result<Void> add(@RequestBody CertificateFieldDTO dto) {
        fieldService.add(dto);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody CertificateFieldDTO dto) {
        fieldService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fieldService.delete(id);
        return Result.success();
    }
}
