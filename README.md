# material-invoice-preaudit-platform
材料费发票智能预审与管理平台
## 环境要求
后端:JDK21,Maven,MYSQL,操作系统
前端:NODE.js
OCR调用:阿里云OCR
AI调用:阿里云百炼 qwen-plus模型
## 环境变量
后端配置环境变量:
DASHSCOPE_API_KEY-通义千问 API Key
ALIBABA_CLOUD_ACCESS_KEY_ID-阿里云 RAM 用户 AccessKey
ALIBABA_CLOUD_ACCESS_KEY_SECRET-阿里云 RAM 用户 Secret
## 启动方式
后端启动:直接运行,访问http://localhost:8080
前端:终端输入npm run dev,访问http://localhost:5173
## 主要技术选型
后端:Springboot,Mybatis-Plus,MYSQL,jwt,BCrypt,PDFBox,阿里云 OCR,qwen-plus
前端:Vue 3 + Vite + TypeScript,Element Plus,Pinia,Axios
## AI方案
AI参与结构化和品类审核
model:qwen-plus
AI结构化:
1.设置系统提示词
2.拼接用户提示词,输入发票提取文本和规定输出JSON格式
3.调用qwen接口
4.解析AI输出JSON落库
5.结构化异常人工兜底
AI品类审核:
1.设置系统提示词输入品类报销规则和规定输出JSON格式
2.调用接口输入商品名
3.解析校验结果后落库
4.审核异常/存疑人工兜底
## OCR方案
使用阿里云OCR
1. PDFBox 加载 PDF 文档
2. PDFRenderer将每一页 PDF 页面渲染成内存图片 BufferedImage
3. 将图片转为 PNG 字节数组（不落地存磁盘，纯内存）
4. 把图片二进制传给阿里云 OCR 远程接口，接口识别图片上的文字返回文本
5. 循环所有页面，把每页识别结果拼接，最后返回完整文档文本
## 数据库设计思路
### 1.设计原则
采用自增主键
status:数据库存整数,Java层定义相应枚举
使用逻辑删除
create_time,update_time自动填充
### 2.表结构与职责
账号:admin表,存储管理员基本信息
发票相关:
invoice表(发票主表)：结构化字段、三维状态、AI 原始结果、周累计冗余
invoice_item(商品明细表):逐项品类判定、单价类别、AI 判断理由
支付凭证相关:
payment_evidence_group(支付凭证组):一组支付凭证只能关联一张发票,可以有多个支付凭证组
order_screenshot(订单截图):一组多张
payment_record(支付记录):一组多张
品类规则:category_rule:]管理员可维护
操作日志:operation_log,状态修改留痕,防止覆盖,可追溯
文件:invoice_file:存储原始文件,支持pdf,zip,png,jpg,可记录zip与pdf的父子关系
### 3.invoice表记录发票三个状态
qualify_status(资质审核状态):0-5,待审核/待补凭证/待人工/通过/不通过/已取消
finance_status(财务提交状态):0-未提交,1-已提交
reimburse_status(报销状态):0-未完成,1-成功,2-失败
### 4.周累计
week_start记录发票提交所在周周一
### 5.品类规则
category_rule表存品类规则（品类名、关键词、结论），管理员可通过接口增删改,规则引擎(资质审核第一步:品类关键词匹配)判定时从数据库读取品类规则
### 6.操作日志留痕
operation_log 表记录所有关键操作:
action:动作类型（PRE_AUDIT / MANUAL_EDIT / EVIDENCE_REVIEW 等）
before_value / after_value：变化前后值（JSON 或文本）
operator_id / operator_name：操作人
create_time：时间
## 已实现功能
### 管理员模块
登录,注册
### 文件模块
单/多PDF上传,zip上传
### 发票模块
1.文本提取: PDF 文本层提取, OCR 兜底,人工兜底
2. AI 结构化:通义千问 qwenplus 按固定 Schema 提取字段,失败/字段缺失人工兜底
3.品类审核:
关键词匹配 category_rule 表，符合直接用规则结论
AI 品类审核，输出可以 / 存疑 / 不可以
人工兜底兜底：AI 判存疑或调用失败时转人工
4.按照规则引擎输出结果:
 APPROVED：全部条件满足，直接通过
 PENDING_EVIDENCE：低值品或周累计超 1000，需补凭证
 PENDING_MANUAL：品类存疑、无明细、内容提取失败
 REJECTED：资产、周累计超 3000、品类不可
 5.查询发票列表/详情,上传发票,管理员修改发票数据,管理员更新发票状态,审核
 ### 支付凭证模块
上传凭证组:：一次上传多张订单截图 + 多张支付记录，关联指定发票
查询凭证组列表/详情
人工审核支付凭证
### 操作留痕
记录关键操作,查看发票列表时按时间显示
### 品类规则维护模块
查看/新增/修改/删除/启用/禁用品类规则
### 重复发票检测
检测规则
强匹配：发票号码 + 销售方税号
弱匹配：销售方税号 + 开票日期 + 价税合计
提醒管理员
## 已知问题
1.周累计查询无缓存.每次都从数据库查询计算,数据量大时有性能瓶颈
2.重复发票仅做提醒,计算周累计金额时没有排除
