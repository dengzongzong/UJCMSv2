package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.CertificateUser;
import org.apache.ibatis.annotations.Select;

public interface CertificateUserMapper extends BaseMapper<CertificateUser> {

    /** 统计去重后的证书用户数(按身份证号去重) */
    @Select("SELECT COUNT(DISTINCT id_card) FROM certificate_user WHERE id_card IS NOT NULL AND id_card != ''")
    int countDistinctByIdCard();
}
