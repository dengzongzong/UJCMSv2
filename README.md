# 考试教育平台

基于 **SpringBoot + Vue2 + MySQL** 的全栈考试教育平台，包含用户端（PC/移动适配）和管理后台。

## 项目结构

```
exam-platform/
├── backend/          # SpringBoot 后端 (端口 8080)
├── admin-web/        # 管理后台 Vue2 + Element UI (端口 8081)
├── user-web/         # 用户端 Vue2 + Vant (端口 8082)
└── README.md
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | SpringBoot 2.7 + MyBatis-Plus 3.5 + MySQL 8.0 + JWT + Druid + EasyExcel + Hutool |
| 管理后台 | Vue 2.7 + Element UI 2.15 + Vue Router 3 + Vuex 3 + Axios |
| 用户端 | Vue 2.7 + Vant 2.12 + Vue Router 3 + Vuex 3 + Axios |
| 数据库 | MySQL 8.0 |

## 快速启动

### 1. 数据库准备

```bash
# 登录 MySQL，执行建表脚本
mysql -u root -p < backend/sql/schema.sql
```

脚本会自动创建 `exam_platform` 数据库、26 张表及种子数据（包含 合作咨询配置、网站声明、合作咨询/投诉建议 留言 三张新表）。

### 2. 启动后端

```bash
cd backend

# 修改数据库连接配置（如需要）
# vi src/main/resources/application.yml

# 编译打包
mvn clean package -DskipTests

# 运行
java -jar target/exam-platform-1.0.0.jar
```

后端启动在 `http://localhost:8080/api`。

### 3. 启动管理后台

```bash
cd admin-web
npm install
npm run serve
```

访问 `http://localhost:8081`，默认账号 `admin / admin123`。

### 4. 启动用户端

```bash
cd user-web
npm install
npm run serve
```

访问 `http://localhost:8082`，测试账号 `13800138000 / 123456`。

## 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 超级管理员 | admin | admin123 |
| 测试学生 | 13800138000 | 123456 |
| 学生默认密码（批量导入） | - | 123456 |

## 功能清单

### 项目背景

- [x] 网页基础适配（PC + 移动端响应式）
- [x] 手机号验证码注册（设置密码、确认密码、勾选协议）
- [x] 账号密码登录 / 后台导入学生默认密码登录
- [x] 用户协议/隐私政策合规校验
- [x] 专业科目切换（建筑工程、CPA、财会金融、基金从业、考公考研）

### 用户端

- [x] 首页轮播图（1-9张、可跳转试卷/课程、可纯展示）
- [x] 课程列表（缩略图、名称、标签、价格展示、小节数、播放进度）
- [x] 课程详情（大图、文本介绍、价格、总时长、目录、视频列表）
- [x] 视频播放（总时长、暂停播放、清晰度切换、快进、半屏/全屏切换）
- [x] 考试列表（封面图、名称、题目数、总分、时长、上次成绩）
- [x] 试卷介绍（名称、题数、总分、倒计时、介绍、开始结束时间、去考试入口）
- [x] 考试倒计时（结束前2分钟弹窗提示、结束自动交卷、中途退出自动交卷）
- [x] 答题页面（题目序号、类型、图文展示、答题卡已答/未答标注、实时用时）
- [x] 考试结果（对错标注、正确率、总分、答案解析、重新答题）
- [x] 个人信息管理（头像、昵称、手机号）
- [x] 我的课程（学习进度、已学习时长、继续学习）
- [x] 我的考试（历史记录、分数、提交时间、时长、答案解析）
- [x] 我的错题（错题列表、移除错题、错题练习）
- [x] 关于我们（客服电话、二维码、富文本介绍）
- [x] 修改密码（手机号验证码重置）
- [x] 退出登录
- [x] **首页底部蓝线上方链接:合作咨询 / 网站声明 / 投诉建议**(三栏式弹窗,留言提交后管理员后台可查)
- [x] **合作咨询弹窗**:左栏单位背景/中栏合作流程&联系电话&邮箱&意向表下载/右栏给我们留言表单

### 总管理后台

- [x] 子管理员设置（账号、角色、密码、功能权限）
- [x] 学生管理 - 搜索（手机号、注册时间段、账号状态）
- [x] 学生管理 - 列表（手机号、注册时间、上次登录、状态）
- [x] 学生管理 - 详情（已开通课程、已开通考试）
- [x] 学生管理 - 冻结/解冻
- [x] 学生管理 - 开通课程（多选/单选/全选）
- [x] 学生管理 - 开通考试（多选/单选/全选）
- [x] 学生管理 - 批量导入（默认密码登录）
- [x] 系统设置 - 专业设置
- [x] 系统设置 - 关于我们信息设置
- [x] 系统设置 - **合作咨询配置**(单位背景介绍、合作流程、联系电话、邮箱、意向表附件上传 + 留言列表)
- [x] 系统设置 - **网站声明配置**(标题、正文)
- [x] 系统设置 - **投诉建议列表**(查看、标记已处理、备注、删除)
- [x] 系统设置 - 视频分类设置
- [x] 系统设置 - 题目分类设置
- [x] 视频管理 - 搜索（名称）、分类筛选
- [x] 视频管理 - 列表（名称、时长、大小、上传时间、分类、播放量）
- [x] 视频管理 - 详情、新增/编辑、删除（引用检查）
- [x] 视频管理 - 按播放量排序
- [x] 课程管理 - 搜索（名称、创建时间段、标签、小节数量）
- [x] 课程管理 - 列表（序号、缩略图、名称、标签、价格、小节数、创建时间、状态）
- [x] 课程管理 - 详情、新增/编辑/删除、开通学生管理
- [x] 课程学习记录 - 搜索（课程名称、时间段、状态、学生手机号）
- [x] 课程学习记录 - 列表、详情、导出 Excel
- [x] 题目管理 - 搜索（类型、分类、关键词、时间段、是否可用）
- [x] 题目管理 - 列表（类型、分类、题干、选项、解析、创建时间、可用）
- [x] 题目管理 - 新增/编辑/删除、导出、批量导入（不含图片）
- [x] 题库管理 - 考试搜索（名称、时间段、状态）
- [x] 题库管理 - 考试列表（封面图、名称、题数、总分、时长、创建时间、状态）
- [x] 题库管理 - 考试详情、新增/编辑/删除、开通学生管理
- [x] 考试记录 - 搜索（考试名称、学生手机号、提交状态）
- [x] 考试记录 - 列表（封面图、名称、题数、总分、时长、学生、分数、提交时间、证书）
- [x] 考试记录 - 详情（学生作答详情）、导出 Excel

## API 路径说明

| 路径前缀 | 说明 | 鉴权 |
|----------|------|------|
| `/api/auth/**` | 认证（登录/注册/重置密码） | 否 |
| `/api/public/**` | 公开接口（轮播图/专业科目/关于我们） | 否 |
| `/api/user/**` | 用户端接口 | 是（学生Token） |
| `/api/admin/**` | 管理后台接口 | 是（管理员Token） |
| `/api/file/**` | 文件上传 | 是 |

## 数据库表结构

共 23 张表：admin、student、profession、subject、banner、video_category、video、course、course_section、course_section_video、student_course、video_study_record、question_category、question、question_option、exam、exam_question、student_exam、exam_record、exam_answer、wrong_question、about_us、system_setting。

## Java 版本约束

后端锁定 **Java 8**（`pom.xml` → `java.version=1.8`，并显式声明 `maven-compiler-plugin source/target=1.8`）。

**禁止使用**以下语法（Checkstyle 会在 `mvn verify` 阶段报警）：

| 特性 | 最低 JDK | 替代写法 |
|------|---------|---------|
| `List.of / Set.of / Map.of` | 9 | `Arrays.asList / new HashSet / new HashMap` |
| `Map.entry` | 9 | `new AbstractMap.SimpleEntry<>(k, v)` |
| `Stream.toList()` | 16 | `.collect(Collectors.toList())` |
| `String.isBlank / repeat / strip*` | 11 | Apache Commons / Hutool / 手写 |
| `Files.readString` | 7 | `new String(Files.readAllBytes(p))` |
| `var` 关键字 | 10 | 写完整类型 |
| `Collectors.toUnmodifiable*` | 10 | `Collectors.toList() + Collections.unmodifiableXxx()` |
| `instanceof 模式变量` | 16 | `if (x instanceof X) { X y = (X) x; ... }` |

**一键验证**（在 `backend/` 目录执行，应输出 0 命中）：

```bash
cd backend
bash ../verify_java8.sh
```

或在 IDE 中跑 `mvn checkstyle:check` —— 项目自带 checkstyle 配置，违规会输出警告（不阻塞 build）。
