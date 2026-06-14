# 账单导出功能设计

## 概述

为"小川记账"添加账单导出功能，支持 Excel（.xlsx）和 PDF 两种格式。后端生成文件流直接返回，前端根据平台适配下载。

## 技术选型

| 用途 | 库 | 版本 |
|------|------|------|
| Excel 生成 | FastExcel (cn.idev.excel) | 1.2.0 |
| PDF 生成 | OpenPDF (com.github.librepdf) | 2.0.3 |

## 后端架构

### 文件结构

```
chuan-bill-server/src/main/java/com/samoy/chuanbillserver/
├── dto/ExportBillDTO.java          ← 新增
├── controller/BillController.java  ← 修改：新增 export 接口
├── service/IBillService.java       ← 修改：新增 export 方法
├── service/impl/BillServiceImpl.java ← 修改：实现 export
├── utils/ExcelExportUtil.java      ← 新增
├── utils/PdfExportUtil.java        ← 新增
```

### ExportBillDTO

复用 BillListDTO 的筛选字段，去掉分页，加上 format：

```java
@Data
public class ExportBillDTO {
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式错误")
    private String startDate;
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式错误")
    private String endDate;
    private String type;          // income/expense/空(全部)
    private String categoryId;
    private String paymentMethodId;
    private String familyId;
    @NotBlank(message = "导出格式不能为空")
    @Pattern(regexp = "excel|pdf", message = "格式仅支持 excel 或 pdf")
    private String format;
}
```

### Controller 接口

```java
@PostMapping("/export")
@Operation(summary = "导出账单")
public void exportBill(@RequestBody @Validated ExportBillDTO dto, HttpServletResponse response) {
    String userId = StpUtil.getLoginIdAsString();
    billService.exportBill(userId, dto, response);
}
```

Controller 直接将 HttpServletResponse 传给 Service，由 Service 写入文件流。

### Service 流程

1. 校验时间范围：起止日期跨度不超过 3 个月（约 90 天）
2. 构建查询条件（与 listBill 类似的 LambdaQueryWrapper，但不分页）
3. 查询记录数量，超过 10000 条则拒绝
4. 查询账单数据，转换为 BillVO（复用现有批量预加载逻辑）
5. 根据 dto.format 调用 ExcelExportUtil 或 PdfExportUtil
6. 设置响应头（Content-Type, Content-Disposition）
7. 将文件流写入 response.getOutputStream()

### 导出限制

| 限制项 | 规则 | 异常 |
|--------|------|------|
| 时间范围 | 起止日期跨度 ≤ 90 天 | BusinessException("时间范围不能超过3个月") |
| 记录数量 | 查询结果 ≤ 10000 条 | BusinessException("数据量过大，请缩小筛选范围") |

先校验时间范围，再查询数量。

### ExcelExportUtil

使用 FastExcel，基于注解定义列映射。内部定义 BillExcelRow 数据对象：

| 列名 | 字段 | 来源 |
|------|------|------|
| 账单名称 | name | Bill.name |
| 类型 | type | "收入"/"支出" |
| 金额 | amount | Bill.amount |
| 时间 | time | Bill.time |
| 分类 | categoryName | CategoryVO.name |
| 支付方式 | paymentMethodName | PaymentMethodVO.name |
| 用户 | userNickname | BillVO.userNickname |
| 家庭 | familyName | BillVO.familyName |
| 备注 | remark | Bill.remark |

使用 `@ExcelProperty` 注解定义列名和顺序，`@ColumnWidth` 设置列宽。

### PdfExportUtil

使用 OpenPDF 的 PdfPTable 生成简洁表格，列与 Excel 一致。

注意事项：
- 需要注册中文字体，否则中文显示为方块
- 通过 `BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED)` 加载系统中文字体
- 中文字体文件放置在 resources 目录下

### 响应头设置

```
// Excel
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename=bills_2026-05-14.xlsx

// PDF
Content-Type: application/pdf
Content-Disposition: attachment; filename=bills_2026-05-14.pdf
```

文件名格式：`bills_{日期}.{ext}`

## 前端集成

### API 定义

在 apiDefinitions.ts 中新增：

```typescript
'bill.export': {
  url: '/bill/export',
  method: 'POST',
  dataType: 'json',
  responseType: 'arraybuffer',  // 关键：接收二进制流
}
```

生成 API 定义后需运行 `alova-api-fix` 修复类型。

### ExportFilterPopup.vue 改造

核心逻辑：
1. 移除 toast.info('导出功能开发中')
2. 调用 Apis.bill.export，传入筛选参数 + format
3. 接收 ArrayBuffer 响应
4. 调用 saveFile() 根据平台保存文件

### 平台下载适配

先用 uni-app 统一 API 尝试，如果某平台不行再用条件编译适配：

- **H5**：Blob + `<a>` 标签下载
- **微信小程序**：`wx.env.USER_DATA_PATH` 写入临时文件 → `uni.openDocument` 预览
- **App**：类似小程序，使用 `plus.io` API

### 错误处理

| 场景 | 后端处理 | 前端处理 |
|------|---------|---------|
| 参数校验失败 | @Validated 自动返回 400 | Alova 拦截器统一处理 |
| 时间范围超限 | 返回 Result.error | toast 提示错误信息 |
| 查询结果为空 | 返回 Result.error(2001) | toast 提示"无导出数据" |
| 记录数超限 | 返回 Result.error | toast 提示"数据量过大" |
| 文件生成异常 | 返回 Result.error(3001) | toast 提示"导出失败" |

导出接口的错误响应格式是 JSON（Result），成功响应是二进制流。前端通过 HTTP 状态码或 Content-Type 区分。Alova 的 responseType: 'arraybuffer' 下，如果后端返回 JSON 错误，需要在拦截器中检测 Content-Type 判断是否为错误响应。

## 完整数据流

```
用户点击"导出"
    ↓
ExportFilterPopup 组装参数 { startDate, endDate, type, format }
    ↓
POST /bill/export （Alova 请求，responseType: arraybuffer）
    ↓
BillController.exportBill() → 获取 userId
    ↓
BillServiceImpl.exportBill()
    ├── 校验时间范围（≤ 90 天）
    ├── 构建 LambdaQueryWrapper（复用筛选逻辑，不分页）
    ├── 查询记录数量（≤ 10000 条）
    ├── 查询 Bill 列表 → 转换为 BillVO
    ├── format == "excel" → ExcelExportUtil.export(bills, outputStream)
    └── format == "pdf"   → PdfExportUtil.export(bills, outputStream)
    ↓
响应头设置 Content-Type + Content-Disposition
    ↓
前端接收 ArrayBuffer
    ↓
根据平台调用 saveFile() 保存/打开文件
```
