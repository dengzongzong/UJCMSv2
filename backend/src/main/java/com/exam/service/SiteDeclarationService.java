package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.SiteDeclarationDTO;
import com.exam.entity.SiteDeclaration;

public interface SiteDeclarationService extends IService<SiteDeclaration> {

    /** 获取网站声明 */
    SiteDeclaration getDeclaration();

    /** 更新网站声明 */
    void updateDeclaration(SiteDeclarationDTO dto);
}
