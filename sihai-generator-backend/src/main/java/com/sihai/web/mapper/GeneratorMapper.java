package com.sihai.web.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sihai.web.model.entity.Generator;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author 13169
 * @description 针对表【generator(代码生成器)】的数据库操作Mapper
 * @createDate 2024-05-27 01:53:34
 * @Entity generator.domain.Generator
 */
public interface GeneratorMapper extends BaseMapper<Generator> {

    @Select("SELECT id, distPath FROM generator where isDelete = 1")
    List<Generator> listDeletedGenerator();

}




