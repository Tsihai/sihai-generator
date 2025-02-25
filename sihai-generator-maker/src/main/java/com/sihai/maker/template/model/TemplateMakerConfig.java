package com.sihai.maker.template.model;

import com.sihai.maker.meta.Meta;
import lombok.Data;

/**
 * 模版制作配置
 */
@Data
public class TemplateMakerConfig {

    private Long id;

    private Meta meta = new Meta();

    private String originProjectPath;

    TemplateMakerModelConfig modelConfig = new TemplateMakerModelConfig();

    TemplateMakerFileConfig fileConfig = new TemplateMakerFileConfig();

    TemplateMakerOutputConfig outputConfig = new TemplateMakerOutputConfig();
}
