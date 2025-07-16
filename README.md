# Jacoco 增强版工具：解决不同class版本覆盖率合并问题

基于 ray 大佬分支二次开发的 Jacoco 增强工具，新增按IP（或自定义规则）划分覆盖率、精确标记分支覆盖、过滤反射获取的JacocoData等功能。感谢 ray 佬的开源贡献！核心实现思路参考博文：[《Jacoco 二次开发：增量覆盖率与多版本合并方案》](https://blog.csdn.net/qq_34418450/article/details/135386280?spm=1001.2014.3001.5501)


## 🚀 核心功能
- **IP/自定义规则划分覆盖率**：支持按请求者IP或其他业务规则拆分覆盖率数据
- **精确分支覆盖标记**：更细致地标记代码分支的覆盖情况
- **反射数据过滤**：自动过滤通过反射获取的JacocoData，避免干扰覆盖率统计
- **增量覆盖率统计**：通过`--diffcode`参数统计指定增量代码的覆盖率（需配合代码差异数据）
- **多版本探针数据合并**：支持将多个exec文件合并，生成统一的覆盖率报告
- **全量/增量报告生成**：灵活生成全量覆盖率报告或仅增量代码的覆盖率报告


## 📦 快速使用
无需自行编译，可直接从发布版下载已编译好的`agent包`和`cli包`。


## 🔨 编译方法
若需自定义编译，执行以下Maven命令（已移除冗余模块和插件）：
```bash
mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```


## 📝 使用说明

### 1. 命令参数说明
| 参数名 | 作用 |
|--------|------|
| `--diffcode` | 启用增量统计（不带则统计全量） |
| `--diffCodeFiles` | 代码差异数据文件路径（可通过[code-diff](https://gitee.com/Dray/code-diff)生成） |
| `--onlyMergeExec=true` | 仅合并探针数据，不生成报告 |
| `--mergeExecfilepath` | 需要合并的exec探针文件路径（可指定多个） |
| `--mergeClassfilepath` | 对应合并exec文件的class文件路径（可指定多个） |
| `--mergeExec` | 合并后生成的exec文件路径 |
| `--classfiles` | 目标class文件路径（可指定多个） |
| `--sourcefiles` | 源代码文件路径 |
| `--html` | 生成的HTML报告输出路径 |


### 2. 代码调用示例
在项目中引入jacoco-cli包后，可直接通过代码执行：
```java
File htmlReportDir = new File("D:\\coverage-report");
new Main().execute("report", 
    "D:\\jacoco_merge_test.exec",  // 基础exec文件
    "--classfiles", "D:\\project\\target\\classes",  // 目标class文件
    "--mergeExecfilepath", "D:\\history\\1.exec",    // 需合并的历史exec
    "--mergeClassfilepath", "D:\\history\\v1\\classes",  // 历史class路径
    "--sourcefiles", "D:\\project\\src\\main\\java",    // 源代码路径
    "--onlyMergeExec", "false",  // 生成报告（非仅合并）
    "--mergeExec", "D:\\merged.exec",  // 合并后的exec输出路径
    "--diffCodeFiles", "D:\\diff\\diff.json",  // 增量代码差异文件
    "--html", htmlReportDir.getAbsolutePath()  // HTML报告输出
);
```


### 3. JAR包执行示例（JDK1.8）

#### 3.1 直接生成带增量统计的报告
```bash
java -jar org.jacoco.cli-0.8.7-SNAPSHOT-nodeps.jar report \
  F:\webDemo\exec\2.exec \
  --classfiles F:\webDemo\target\classes \
  --mergeExecfilepath F:\webDemo\exec\1.exec \
  --mergeClassfilepath F:\webDemo\classes_1 \
  --diffCodeFiles F:\home\code_diff\diff.json \
  --sourcefiles F:\webDemo\src\main\java \
  --html F:\webDemo\exec\report_diff
```

#### 3.2 仅合并exec文件
```bash
java -jar org.jacoco.cli-0.8.7-SNAPSHOT-nodeps.jar report \
  F:\webDemo\exec\2.exec \
  --classfiles F:\webDemo\target\classes \
  --mergeExecfilepath F:\webDemo\exec\1.exec \
  --mergeClassfilepath F:\webDemo\classes_1 \
  --onlyMergeExec true \
  --mergeExec F:\webDemo\exec\merged.exec
```

#### 3.3 多轮合并注意事项
- 第一次合并：A.exec + B.exec → AB.exec（`mergeClassfilepath`指定B的class路径）
- 第二次合并：AB.exec + C.exec → ABC.exec（`mergeClassfilepath`指定C的class路径）
- 每次合并需保证`mergeClassfilepath`对应最新版本的class文件路径


## ❓ 问题交流
如有任何使用问题，可加入作者微信群交流（扫码或联系作者获取群二维码）。


> 注：除IP划分、精确分支覆盖、反射数据过滤外，其他功能已开源，详细使用说明可参考项目wiki。
![输入图片说明](image2.png)
