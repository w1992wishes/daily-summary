package me.w1992wishes.common.util;

import com.alibaba.fastjson.JSON;
import me.w1992wishes.common.exception.MyRuntimeException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 读取 json 为配置
 *
 * @author w1992wishes 2019/3/27 15:39
 */
public class Configuration {

    private Object root;

    private Configuration(final String json) {
        try {
            this.root = JSON.parse(json);
        } catch (Exception e) {
            throw new MyRuntimeException(
                    String.format("配置信息错误. 您提供的配置信息不是合法的JSON格式: %s . 请按照标准json格式提供配置信息. ", e.getMessage()));
        }
    }

    /**
     * 用户指定部分path，获取Configuration的子集
     * <p/>
     * <br>
     * 如果path获取的路径或者对象不存在，返回null
     */
    public Configuration getConfiguration(final String path) {
        Object object = this.get(path);
        if (null == object) {
            return null;
        }

        return Configuration.from(Configuration.toJSONString(object));
    }

    /**
     * 根据用户提供的json path，寻址具体的对象。
     * <p/>
     * <br>
     * <p/>
     * NOTE: 目前仅支持Map以及List下标寻址, 例如:
     * <p/>
     * <br />
     * <p/>
     * 对于如下JSON
     * <p/>
     * {"a": {"b": {"c": [0,1,2,3]}}}
     * <p/>
     * config.get("") 返回整个Map <br>
     * config.get("a") 返回a下属整个Map <br>
     * config.get("a.b.c") 返回c对应的数组List <br>
     * config.get("a.b.c[0]") 返回数字0
     *
     * @return Java表示的JSON对象，如果path不存在或者对象不存在，均返回null。
     */
    public Object get(final String path) {
        this.checkPath(path);
        try {
            return this.findObject(path);
        } catch (Exception e) {
            return null;
        }
    }

    private void checkPath(final String path) {
        if (null == path) {
            throw new IllegalArgumentException(
                    "系统编程错误, 该异常代表系统编程错误, 传进来的 path 不能为 null!.");
        }

        for (final String each : StringUtils.split(".")) {
            if (StringUtils.isBlank(each)) {
                throw new IllegalArgumentException(String.format(
                        "系统编程错误, 路径[%s]不合法, 路径层次之间不能出现空白字符 .", path));
            }
        }
    }

    private Object findObject(final String path) {
        boolean isRootQuery = StringUtils.isBlank(path);
        if (isRootQuery) {
            return this.root;
        }

        Object target = this.root;

        for (final String each : split2List(path)) {
            if (isPathMap(each)) {
                target = findObjectInMap(target, each);
            } else {
                target = findObjectInList(target, each);
            }
        }

        return target;
    }

    private List<String> split2List(final String path) {
        return Arrays.asList(StringUtils.split(split(path), "."));
    }

    private String split(final String path) {
        return StringUtils.replace(path, "[", ".[");
    }

    private boolean isPathMap(final String path) {
        return StringUtils.isNotBlank(path) && !isPathList(path);
    }

    private boolean isPathList(final String path) {
        return path.contains("[") && path.contains("]");
    }

    @SuppressWarnings("unchecked")
    private Object findObjectInMap(final Object target, final String index) {
        boolean isMap = (target instanceof Map);
        if (!isMap) {
            throw new IllegalArgumentException(String.format(
                    "您提供的配置文件有误. 路径[%s]需要配置Json格式的Map对象，但该节点发现实际类型是[%s]. 请检查您的配置并作出修改.",
                    index, target.getClass().toString()));
        }

        Object result = ((Map<String, Object>) target).get(index);
        if (null == result) {
            throw new IllegalArgumentException(String.format(
                    "您提供的配置文件有误. 路径[%s]值为null，datax无法识别该配置. 请检查您的配置并作出修改.", index));
        }

        return result;
    }

    @SuppressWarnings({"unchecked"})
    private Object findObjectInList(final Object target, final String each) {
        boolean isList = (target instanceof List);
        if (!isList) {
            throw new IllegalArgumentException(String.format(
                    "您提供的配置文件有误. 路径[%s]需要配置Json格式的Map对象，但该节点发现实际类型是[%s]. 请检查您的配置并作出修改.",
                    each, target.getClass().toString()));
        }

        String index = each.replace("[", "").replace("]", "");
        if (!StringUtils.isNumeric(index)) {
            throw new IllegalArgumentException(
                    String.format(
                            "系统编程错误，列表下标必须为数字类型，但该节点发现实际类型是[%s] ，该异常代表系统编程错误, 请联系DataX开发团队 !",
                            index));
        }

        return ((List<Object>) target).get(Integer.valueOf(index));
    }

    /**
     * 从JSON字符串加载Configuration
     */
    public static Configuration from(String json) {
        return new Configuration(json);
    }

    /**
     * 从包括json的File对象加载Configuration
     */
    public static Configuration from(File file) {
        try {
            return Configuration.from(IOUtils.toString(new FileInputStream(file), "UTF-8"));
        } catch (FileNotFoundException e) {
            throw new MyRuntimeException(String.format("配置信息错误，您提供的配置文件[%s]不存在. 请检查您的配置文件.", file.getAbsolutePath()));
        } catch (IOException e) {
            throw new MyRuntimeException(String.format("配置信息错误. 您提供配置文件[%s]读取失败，错误原因: %s. 请检查您的配置文件的权限设置.", file.getAbsolutePath(), e));
        }
    }

    /**
     * 从包括json的InputStream对象加载Configuration
     */
    public static Configuration from(InputStream is) {
        try {
            return Configuration.from(IOUtils.toString(is, "UTF-8"));
        } catch (IOException e) {
            throw new MyRuntimeException(String.format("请检查您的配置文件. 您提供的配置文件读取失败，错误原因: %s. 请检查您的配置文件的权限设置.", e));
        }
    }

    /**
     * 从Map对象加载Configuration
     */
    public static Configuration from(final Map<String, Object> object) {
        return Configuration.from(Configuration.toJSONString(object));
    }

    /**
     * 从List对象加载Configuration
     */
    public static Configuration from(final List<Object> object) {
        return Configuration.from(Configuration.toJSONString(object));
    }

    private static String toJSONString(final Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 根据用户提供的json path，寻址Map对象，如果对象不存在，返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(final String path) {
        Map<String, Object> result = this.get(path, Map.class);
        if (null == result) {
            return null;
        }
        return result;
    }

    /**
     * 根据用户提供的json path，寻址具体的对象，并转为用户提供的类型
     * <p/>
     * <br>
     * <p/>
     * NOTE: 目前仅支持Map以及List下标寻址, 例如:
     * <p/>
     * <br />
     * <p/>
     * 对于如下JSON
     * <p/>
     * {"a": {"b": {"c": [0,1,2,3]}}}
     * <p/>
     * config.get("") 返回整个Map <br>
     * config.get("a") 返回a下属整个Map <br>
     * config.get("a.b.c") 返回c对应的数组List <br>
     * config.get("a.b.c[0]") 返回数字0
     *
     * @return Java表示的JSON对象，如果转型失败，将抛出异常
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String path, Class<T> clazz) {
        this.checkPath(path);
        return (T) this.get(path);
    }

    /**
     * 根据用户提供的json path，寻址String对象
     *
     * @return String对象，如果path不存在或者String不存在，返回null
     */
    public String getString(final String path) {
        Object string = this.get(path);
        if (null == string) {
            return null;
        }
        return String.valueOf(string);
    }

    /**
     * 根据用户提供的json path，寻址String对象，如果对象不存在，返回默认字符串
     *
     * @return String对象，如果path不存在或者String不存在，返回默认字符串
     */
    public String getString(final String path, final String defaultValue) {
        String result = this.getString(path);

        if (null == result) {
            return defaultValue;
        }

        return result;
    }

    /**
     * 根据用户提供的json path，寻址Integer对象
     *
     * @return Integer对象，如果path不存在或者Integer不存在，返回null
     */
    public Integer getInt(final String path) {
        String result = this.getString(path);
        if (null == result) {
            return null;
        }

        try {
            return Integer.valueOf(result);
        } catch (Exception e) {
            throw new MyRuntimeException(
                    String.format("任务读取配置文件出错. 配置文件路径[%s] 值非法, 期望是整数类型: %s. 请检查您的配置并作出修改.", path,
                            e.getMessage()));
        }
    }


    /**
     * 根据用户提供的json path，寻址Integer对象，如果对象不存在，返回默认Integer对象
     *
     * @return Integer对象，如果path不存在或者Integer不存在，返回默认Integer对象
     */
    public Integer getInt(final String path, int defaultValue) {
        Integer object = this.getInt(path);
        if (null == object) {
            return defaultValue;
        }
        return object;
    }

    /**
     * 根据用户提供的json path，寻址List对象，如果对象不存在，返回默认List
     */
    public <T> List<T> getList(final String path, final List<T> defaultList,
                               Class<T> t) {
        List<T> list = this.getList(path, t);
        if (null == list) {
            return defaultList;
        }
        return list;
    }

    /**
     * 根据用户提供的json path，寻址List对象，如果对象不存在，返回null
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String path, Class<T> t) {
        Object object = this.get(path, List.class);
        if (null == object) {
            return null;
        }

        List<T> result = new ArrayList<T>();

        List<Object> origin = (List<Object>) object;
        for (final Object each : origin) {
            result.add((T) each);
        }

        return result;
    }
}
