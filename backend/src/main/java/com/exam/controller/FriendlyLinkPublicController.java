package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.FriendlyLink;
import com.exam.service.FriendlyLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 友情链接(公开查询,学员端调用)
 */
@RestController
@RequestMapping("/public/friendly-link")
public class FriendlyLinkPublicController {

    @Autowired
    private FriendlyLinkService service;

    @GetMapping("/list")
    public Result<List<FriendlyLink>> list() {
        return Result.success(service.listEnabled());
    }
}
