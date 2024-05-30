package com.sihai.web.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.sihai.web.annotation.AuthCheck;
import com.sihai.web.common.BaseResponse;
import com.sihai.web.common.DeleteRequest;
import com.sihai.web.common.ErrorCode;
import com.sihai.web.common.ResultUtils;
import com.sihai.web.constant.UserConstant;
import com.sihai.web.exception.BusinessException;
import com.sihai.web.exception.ThrowUtils;
import com.sihai.web.manager.CosManager;
import com.sihai.web.meta.Meta;
import com.sihai.web.model.dto.Generator.*;
import com.sihai.web.model.entity.Generator;
import com.sihai.web.model.entity.User;
import com.sihai.web.model.vo.GeneratorVO;
import com.sihai.web.service.GeneratorService;
import com.sihai.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 帖子接口
 */
@RestController
@RequestMapping("/generator")
@Slf4j
public class GeneratorController {

    @Resource
    private GeneratorService generatorService;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    // region 增删改查

    /**
     * 创建
     *
     * @param generatorAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addGenerator(@RequestBody GeneratorAddRequest generatorAddRequest, HttpServletRequest request) {
        if (generatorAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorAddRequest, generator);
        List<String> tags = generatorAddRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorAddRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorAddRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, true);
        User loginUser = userService.getLoginUser(request);
        generator.setUserId(loginUser.getId());
        generator.setStatus(0);
        boolean result = generatorService.save(generator);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newGeneratorId = generator.getId();
        return ResultUtils.success(newGeneratorId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteGenerator(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldGenerator.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = generatorService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param generatorUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateGenerator(@RequestBody GeneratorUpdateRequest generatorUpdateRequest) {
        if (generatorUpdateRequest == null || generatorUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorUpdateRequest, generator);
        List<String> tags = generatorUpdateRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorUpdateRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorUpdateRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, false);
        long id = generatorUpdateRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = generatorService.updateById(generator);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<GeneratorVO> getGeneratorVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(generatorService.getGeneratorVO(generator, request));
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param generatorQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Generator>> listGeneratorByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param generatorQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<GeneratorVO>> listGeneratorVOByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                 HttpServletRequest request) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorService.getGeneratorVOPage(generatorPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param generatorQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<GeneratorVO>> listMyGeneratorVOByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                   HttpServletRequest request) {
        if (generatorQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        generatorQueryRequest.setUserId(loginUser.getId());
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size),
                generatorService.getQueryWrapper(generatorQueryRequest));
        return ResultUtils.success(generatorService.getGeneratorVOPage(generatorPage, request));
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param generatorEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editGenerator(@RequestBody GeneratorEditRequest generatorEditRequest, HttpServletRequest request) {
        if (generatorEditRequest == null || generatorEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Generator generator = new Generator();
        BeanUtils.copyProperties(generatorEditRequest, generator);
        List<String> tags = generatorEditRequest.getTags();
        generator.setTags(JSONUtil.toJsonStr(tags));
        Meta.FileConfig fileConfig = generatorEditRequest.getFileConfig();
        generator.setFileConfig(JSONUtil.toJsonStr(fileConfig));
        Meta.ModelConfig modelConfig = generatorEditRequest.getModelConfig();
        generator.setModelConfig(JSONUtil.toJsonStr(modelConfig));

        // 参数校验
        generatorService.validGenerator(generator, false);
        User loginUser = userService.getLoginUser(request);
        long id = generatorEditRequest.getId();
        // 判断是否存在
        Generator oldGenerator = generatorService.getById(id);
        ThrowUtils.throwIf(oldGenerator == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldGenerator.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = generatorService.updateById(generator);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 下载
     */
    @GetMapping("/download")
    public void download(long id, HttpServletResponse response, HttpServletRequest request) throws IOException {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的ID");
        }
        // 获取登录信息
        User loginUser = userService.getLoginUser(request);
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到生成器");
        }

        String filepath = generator.getDistPath();
        if (StrUtil.isBlank(filepath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        // 追踪事件
        log.info("用户 {} 下载了 {}", loginUser, filepath);

        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    /**
     * 使用代码生成器
     * @param generatorUseRequest
     * @param response
     * @param request
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping("/use")
    public void useGenerator(@RequestBody GeneratorUseRequest generatorUseRequest, HttpServletResponse response, HttpServletRequest request) throws IOException, InterruptedException {
        // 1. 获取用户输入的请求参数
        validateRequest(generatorUseRequest);

        Long id = generatorUseRequest.getId();
        Map<String, Object> dataModel = generatorUseRequest.getDataModel();

        // 2. 校验用户是否登录
        User loginUser = userService.getLoginUser(request);
        log.info("用户 {} 使用了生成器, id = {}", loginUser.getId(), id);

        // 初始化临时目录路径
        String tempDirPath = String.format("%s/.temp/use/%s", System.getProperty("user.dir"), id);

        // 3. 从对象存储中下载生成器的压缩包
        String zipFilePath = downloadGeneratorZip(id, generatorUseRequest, loginUser);

        // 4. 解压压缩包，得到脚本文件
        File unzipDistDir = ZipUtil.unzip(zipFilePath);
        log.info("用户 {} 解压了 {}", loginUser.getId(), zipFilePath);

        // 5. 将用户输入的参数写入到 json 文件中
        String dataModelJsonPath = writeDataModelJson(dataModel, loginUser, id);

        // 6. 执行脚本文件
        File scriptFile = findScriptFile(unzipDistDir);
        addExecutePermission(scriptFile);
        executeScript(scriptFile, dataModelJsonPath);

        // 7. 压缩生成结果并返回给前端
        String generatedPath = scriptFile.getParentFile().getAbsolutePath() + "/generated";
        String zipResultPath = tempDirPath + "/result.zip";
        File resultZip = ZipUtil.zip(generatedPath, zipResultPath);
        log.info("用户 {} 压缩了 {}", loginUser.getId(), zipResultPath);

        // 设置响应头
        setResponseHeader(response, resultZip);

        // 8. 异步删除临时文件
        deleteTempFilesAsync(tempDirPath);
    }

    /**
     * 校验用户输入的请求参数
     * @param generatorUseRequest
     */
    private void validateRequest(GeneratorUseRequest generatorUseRequest) {
        if (generatorUseRequest == null || generatorUseRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }

    /**
     * 从对象存储中下载生成器的压缩包
     * @param id
     * @param generatorUseRequest
     * @param loginUser
     * @return
     * @throws IOException
     */
    private String downloadGeneratorZip(Long id, GeneratorUseRequest generatorUseRequest, User loginUser) throws IOException {
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String filepath = generator.getDistPath();
        if (StrUtil.isBlank(filepath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        String projectPath = System.getProperty("user.dir");
        String tempDirPath = String.format("%s/.temp/use/%s", projectPath, id);
        String zipFilePath = tempDirPath + "/dist.zip";
        if (!FileUtil.exist(zipFilePath)) {
            FileUtil.touch(zipFilePath);
        }
        try {
            cosManager.download(filepath, zipFilePath);
            log.info("用户 {} 下载了 {}", loginUser.getId(), filepath);
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成器下载失败");
        }

        return zipFilePath;
    }

    /**
     * 将用户输入的参数写入到 json 文件中
     * @param dataModel
     * @param loginUser
     * @param id
     * @return
     */
    private String writeDataModelJson(Map<String, Object> dataModel, User loginUser, Long id) {
        String tempDirPath = String.format("%s/.temp/use/%s", System.getProperty("user.dir"), id);
        String dataModelJsonPath = tempDirPath + "/dataModel.json";
        String jsonStr = JSONUtil.toJsonStr(dataModel);
        FileUtil.writeUtf8String(jsonStr, dataModelJsonPath);
        log.info("用户 {} 写入了 {}", loginUser.getId(), dataModelJsonPath);
        return dataModelJsonPath;
    }

    /**
     * 找到脚本文件
     * @param unzipDistDir
     * @return
     */
    private File findScriptFile(File unzipDistDir) {
        return FileUtil.loopFiles(unzipDistDir, 2, null)
                .stream()
                // 必须是文件
                .filter(file -> file.isFile() && file.getName().endsWith(".bat"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("找不到 .bat 脚本文件"));
    }

    /**
     * 为脚本文件添加执行权限
     * @param scriptFile
     */
    private void addExecutePermission(File scriptFile) {
        if (!SystemUtil.getOsInfo().isWindows()) {
            try {
                Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
                Files.setPosixFilePermissions(scriptFile.toPath(), permissions);
            } catch (Exception e) {
                log.error("设置脚本文件权限失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置脚本文件权限失败");
            }
        } else {
            log.info("当前系统为Windows，跳过设置脚本执行权限");
        }
    }

    /**
     * 执行脚本文件
     * @param scriptFile
     * @param dataModelJsonPath
     * @throws IOException
     * @throws InterruptedException
     */
    private void executeScript(File scriptFile, String dataModelJsonPath) throws IOException, InterruptedException {
        String scriptAbsolutePath = scriptFile.getAbsolutePath().replace("\\", "/");
        String[] commands = {scriptAbsolutePath, "json-generator", "--file=" + dataModelJsonPath};
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(scriptFile.getParentFile());

        try {
            Process process = processBuilder.start();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.info(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("命令执行失败，结束码：" + exitCode);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "命令执行失败");
            }
            log.info("命令执行结束，结束码：{}", exitCode);
        } catch (IOException | InterruptedException e) {
            log.error("执行生成器脚本命令失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行生成器脚本命令失败");
        }
    }

    /**
     * 设置文件响应头
     * @param response
     * @param file
     * @throws IOException
     */
    private void setResponseHeader(HttpServletResponse response, File file) throws IOException {
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=result.zip");
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }

    /**
     * 异步删除临时文件
     * @param tempDirPath
     */
    private void deleteTempFilesAsync(String tempDirPath) {
        CompletableFuture.runAsync(() -> {
            try {
                FileUtil.del(tempDirPath);
                log.info("删除临时文件成功");
            } catch (Exception e) {
                log.error("删除临时文件失败", e);
            }
        });
    }

}
