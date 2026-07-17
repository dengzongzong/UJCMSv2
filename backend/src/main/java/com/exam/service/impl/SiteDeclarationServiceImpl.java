package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.dto.SiteDeclarationDTO;
import com.exam.entity.SiteDeclaration;
import com.exam.mapper.SiteDeclarationMapper;
import com.exam.service.SiteDeclarationService;
import org.springframework.stereotype.Service;

@Service
public class SiteDeclarationServiceImpl
        extends ServiceImpl<SiteDeclarationMapper, SiteDeclaration>
        implements SiteDeclarationService {

    @Override
    public SiteDeclaration getDeclaration() {
        SiteDeclaration one = this.getOne(new LambdaQueryWrapper<SiteDeclaration>().last("LIMIT 1"));
        if (one == null) {
            SiteDeclaration def = new SiteDeclaration();
            def.setId(1L);
            def.setTitle("网站声明");
            def.setContent("");
            return def;
        }
        return one;
    }

    @Override
    public void updateDeclaration(SiteDeclarationDTO dto) {
        SiteDeclaration existing = this.getOne(new LambdaQueryWrapper<SiteDeclaration>().last("LIMIT 1"));
        SiteDeclaration target = existing == null ? new SiteDeclaration() : existing;
        target.setTitle(dto.getTitle());
        target.setContent(dto.getContent());
        if (target.getId() == null) {
            this.save(target);
        } else {
            this.updateById(target);
        }
    }
}
