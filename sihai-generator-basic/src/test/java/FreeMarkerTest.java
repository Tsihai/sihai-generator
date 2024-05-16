import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * freeMarker 创建 Configuration 实例
 * @Author sihai
 * @Date 2024/1/8 20:40
 */
public class FreeMarkerTest {

    @Test
    public void test() throws IOException, TemplateException {
        // 创建模板版本{
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        // 设置模板路径
        cfg.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        // 设置模板文件使用的字符集
        cfg.setDefaultEncoding("UTF-8");
        // 设置默认生成的数字格式
        cfg.setNumberFormat("0.######");

        // 创建模板对象，加载指定模板文件
        Template template = cfg.getTemplate("myweb.html.ftl");

        // 设置数据模型
        HashMap<String, Object> dataModel = new HashMap<>();
        dataModel.put("name", "sihai");
        dataModel.put("currentYear", 2024);
        List<Map<String, Object>> menuItems = new ArrayList<>();
        HashMap<String, Object> menuItem1 = new HashMap<>();
        menuItem1.put("url", "sihai59.cn");
        menuItem1.put("label", "智能BI");
        HashMap<String, Object> menuItem2 = new HashMap<>();
        menuItem2.put("url", "Tsihai.github.io");
        menuItem2.put("label", "四海blog");
        menuItems.add(menuItem1);
        menuItems.add(menuItem2);
        dataModel.put("menuItems", menuItems);

        // 文件输出
        Writer out = new FileWriter("myweb.html");

        template.process(dataModel, out);

        out.close();
    }


}
