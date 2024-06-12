package com.sihai.web.controller;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.sihai.maker.generator.main.GenerateTemplate;
import com.sihai.maker.generator.main.ZipGenerator;
import com.sihai.maker.meta.Meta;
import com.sihai.maker.meta.MetaValidatorPro;
import com.sihai.web.annotation.AuthCheck;
import com.sihai.web.common.BaseResponse;
import com.sihai.web.common.DeleteRequest;
import com.sihai.web.common.ErrorCode;
import com.sihai.web.common.ResultUtils;
import com.sihai.web.constant.UserConstant;
import com.sihai.web.exception.BusinessException;
import com.sihai.web.exception.ThrowUtils;
import com.sihai.web.manager.CacheManager;
import com.sihai.web.manager.CosManager;
import com.sihai.web.model.dto.Generator.*;
import com.sihai.web.model.entity.Generator;
import com.sihai.web.model.entity.User;
import com.sihai.web.model.vo.GeneratorVO;
import com.sihai.web.service.GeneratorService;
import com.sihai.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private CacheManager cacheManager;

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
     * 精简分页获取资源列表
     */
    @PostMapping("/list/page/vo/fast")
    public BaseResponse<Page<GeneratorVO>> listFastGeneratorVOByPage(@RequestBody GeneratorQueryRequest generatorQueryRequest,
                                                                     HttpServletRequest request) {
        long current = generatorQueryRequest.getCurrent();
        long size = generatorQueryRequest.getPageSize();
        // 优先从缓存读取
        String cacheKey = getPageCacheKey(generatorQueryRequest);
        // 多级缓存
        Object cacheValue = cacheManager.get(cacheKey);
        if (cacheValue != null) {
            return ResultUtils.success((Page<GeneratorVO>) cacheValue);
        }

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Generator> queryWrapper = generatorService.getQueryWrapper(generatorQueryRequest);
        queryWrapper.select("id", "name", "description", "tags", "picture", "status", "userId", "updateTime", "createTime");
        Page<Generator> generatorPage = generatorService.page(new Page<>(current, size), queryWrapper);
        Page<GeneratorVO> generatorVOPage = generatorService.getGeneratorVOPage(generatorPage, request);

        // 写入多级缓存
        cacheManager.put(cacheKey, generatorVOPage);
        return ResultUtils.success(generatorVOPage);
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
        log.info("用户 {} 下载了 {}", loginUser.getId(), filepath);

        // 设置响应头
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filepath);

        // 如果有缓存，则使用缓存
        String zipFilePath = getCacheFilePath(id, filepath);
        if (FileUtil.exist(zipFilePath)) {
            // 写入响应
            Files.copy(Paths.get(zipFilePath), response.getOutputStream());
            return;
        }

        COSObjectInputStream cosObjectInput = null;
        try {
            // 计时器
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);

            stopWatch.stop();
            log.info("文件下载耗时：{}ms", stopWatch.getTotalTimeMillis());

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
     * 校验用户输入的请求参数
     *
     * @param generatorUseRequest
     */
    private void validateRequest(GeneratorUseRequest generatorUseRequest) {
        if (generatorUseRequest == null || generatorUseRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }

    /**
     * 找到脚本文件
     *
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
     * 设置文件响应头
     *
     * @param response
     * @param file
     * @throws IOException
     */
    private void setResponseHeader(HttpServletResponse response, File file) throws IOException {
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=result.zip");
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException e) {
            log.error("文件下载过程中发生错误", e);
            response.reset(); // 清空响应内容，防止错误文件被下载
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "使用代码生成器下载失败");
        }
    }

    /**
     * 异步删除临时文件
     *
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


    /**
     * 使用代码生成器
     *
     * @param generatorUseRequest
     * @param request
     * @param response
     * @throws IOException
     */
    @PostMapping("/use")
    public void useGenerator(@RequestBody GeneratorUseRequest generatorUseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        validateRequest(generatorUseRequest);

        Long id = generatorUseRequest.getId();
        Map<String, Object> dataModel = generatorUseRequest.getDataModel();

        // 2. 校验用户是否登录
        User loginUser = userService.getLoginUser(request);
        log.info("用户 {} 使用了生成器, id = {}", loginUser.getId(), id);

        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 生成器的存储路径
        String distPath = generator.getDistPath();
        if (StrUtil.isBlank(distPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        // 定义独立的工作空间
        String projectPath = System.getProperty("user.dir");
        String tempDirPath = String.format("%s/.temp/use/%s", projectPath, id);
        String zipFilePath = tempDirPath + "/dist.zip";

        if (!FileUtil.exist(zipFilePath)) {
            FileUtil.touch(zipFilePath);
        }


        try {
            cosManager.download(distPath, zipFilePath);
            log.info("用户 {} 下载了 {}", loginUser.getId(), distPath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成器下载失败");
        }

        // 解压压缩包，得到脚本文件
        File unzipDistDir = ZipUtil.unzip(zipFilePath);
        log.info("用户 {} 解压了 {}", loginUser.getId(), zipFilePath);

        // 将用户输入的参数写到 json 文件中
        String dataModelFilePath = tempDirPath + "/dataModel.json";
        String jsonStr = JSONUtil.toJsonStr(dataModel);
        FileUtil.writeUtf8String(jsonStr, dataModelFilePath);
        log.info("用户 {} 写入了 {}", loginUser.getId(), dataModelFilePath);

        // 执行脚本
        // 找到脚本文件所在路径
        // 要注意，如果不是 windows 系统，找 generator 文件而不是 bat
        File scriptFile = FileUtil.loopFiles(unzipDistDir, 2, null)
                .stream()
                .filter(file -> file.isFile()
                        && (SystemUtil.getOsInfo().isWindows() ? "generator.bat".equals(file.getName()) : "generator".equals(file.getName())))
                .findFirst()
                .orElseThrow(RuntimeException::new);

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

        // 构造命令
        File scriptDir = scriptFile.getParentFile();
        // 注意，如果是 mac / linux 系统，要用 "./generator"
        String scriptAbsolutePath = SystemUtil.getOsInfo().isWindows()
                ? scriptFile.getAbsolutePath().replace("\\", "/")
                : scriptFile.getAbsolutePath();
        String[] commands = new String[]{scriptAbsolutePath, "json-generate", "--file=" + dataModelFilePath};

        // 这里一定要拆分！
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(scriptDir);

        try {
            Process process = processBuilder.start();

            // 读取命令的输出
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("命令执行结束，退出码：" + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行生成器脚本错误");
        }

        // 压缩得到的生成结果，返回给前端
        String generatedPath = SystemUtil.getOsInfo().isWindows()
                ? scriptFile.getParentFile().getAbsolutePath().replace("\\", "/") + "/generated"
                : scriptFile.getParentFile().getAbsolutePath() + "/generated";
        String zipResultPath = SystemUtil.getOsInfo().isWindows()
                ? tempDirPath.replace("\\", "/") + "/result.zip"
                : tempDirPath + "/result.zip";
        File resultZip = ZipUtil.zip(generatedPath, zipResultPath);
        log.info("用户 {} 压缩了 {}", loginUser.getId(), zipResultPath);

        // 设置响应头
        setResponseHeader(response, resultZip);

        // 8. 异步删除临时文件
        deleteTempFilesAsync(tempDirPath);
    }

    /**
     * 制作代码生成器
     *
     * @param generatorMakeRequest
     * @param request
     * @param response
     * @throws IOException
     */
    @PostMapping("/make")
    public void makeGenerator(@RequestBody GeneratorMakeRequest generatorMakeRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 读取请求参数
        Meta meta = generatorMakeRequest.getMeta();
        String zipFilePath = generatorMakeRequest.getZipFilePath();
        // 校验请求参数
        if (meta == null || StrUtil.isBlank(zipFilePath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验用户是否登录
        User loginUser = userService.getLoginUser(request);
        log.info("用户 {} 正在制作生成器", loginUser.getId());

        // 3. 创建独立的工作空间，下载压缩包到本地
        String projectPath = System.getProperty("user.dir");
        String id = IdUtil.getSnowflakeNextId() + RandomUtil.randomString(6);
        String tempDirPath = String.format("%s/.temp/make/%s", projectPath, id);
        String localZipFilePath = tempDirPath + "/project.zip";
        if (!FileUtil.exist(localZipFilePath)) {
            FileUtil.touch(localZipFilePath);
        }

        // 下载文件
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            cosManager.download(zipFilePath, localZipFilePath);
            log.info("用户 {} 下载了 {}", loginUser.getId(), localZipFilePath);
            stopWatch.stop();
            log.info("下载压缩包耗时 {} 毫秒", stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩包下载失败");
        }

        // 4. 解压压缩包，得到模板文件
        File unzipDistDir = ZipUtil.unzip(localZipFilePath);
        if (unzipDistDir == null || !unzipDistDir.exists()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解压失败，压缩包可能有误");
        }
        log.info("用户 {} 解压了 {}", loginUser.getId(), localZipFilePath);

        // 5. 构造 meta 对象和生成器的输出路径
        String sourceRootPath = unzipDistDir.getAbsolutePath();
        meta.getFileConfig().setSourceRootPath(sourceRootPath);
        // 校验和处理默认值
        MetaValidatorPro.doValidAndFillDefaultValue(meta);
        String outputPath = String.format("%s/generated/%s", tempDirPath, meta.getName());

        // 6. 调用 maker 方法制作生成器
        GenerateTemplate generateTemplate = new ZipGenerator();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            generateTemplate.doGenerate(meta, outputPath);
            log.info("用户 {} 制作了 {}", loginUser.getId(), outputPath);
            stopWatch.stop();
            log.info("制作生成器耗时 {} 毫秒", stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成器制作失败");
        }

        // 7. 下载制作好的生成器压缩包
        String suffix = "-dist.zip";
        String zipFileName = meta.getName() + suffix;
        // 生成器压缩包的绝对路径
        String distZipFilePath = outputPath + suffix;

        // 设置响应头
        response.setContentType("application/octet-stream;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);
        try (InputStream inputStream = Files.newInputStream(Paths.get(distZipFilePath))) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
            log.info("用户 {} 下载了制作代码生成器: {}", loginUser.getId(), zipFileName);
        } catch (IOException e) {
            log.error("文件下载过程中发生错误", e);
            response.reset(); // 清空响应内容，防止错误文件被下载
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "制作代码生成器下载失败");
        }

        // 8. 删除临时文件
        deleteTempFilesAsync(tempDirPath);

    }

    /**
     * 写入缓存代码生成器
     */
    @PostMapping("/cache")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void cacheDownload(@RequestBody GeneratorCacheRequest generatorCacheRequest) {
        if (generatorCacheRequest == null || generatorCacheRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的ID");
        }
        long id = generatorCacheRequest.getId();
        Generator generator = generatorService.getById(id);
        if (generator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到生成器");
        }

        String distPath = generator.getDistPath();
        if (StrUtil.isBlank(distPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "产物包不存在");
        }

        String zipFilePath = getCacheFilePath(id, distPath);

        try {
            cosManager.download(distPath, zipFilePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }

    /**
     * 获取缓存文件路径
     */
    public String getCacheFilePath(long id, String distPath) {
        String projectPath = System.getProperty("user.dir");
        String tempDirPath = String.format("%s/.temp/cache/%s", projectPath, id);
        return tempDirPath + "/" + distPath;
    }

    /**
     * 获取分页缓存 key
     */
    public static String getPageCacheKey(GeneratorQueryRequest generatorQueryRequest) {
        String jsonStr = JSONUtil.toJsonStr(generatorQueryRequest);
        String base64 = Base64Encoder.encode(jsonStr);
        return "generator:page:" + base64;
    }

}
