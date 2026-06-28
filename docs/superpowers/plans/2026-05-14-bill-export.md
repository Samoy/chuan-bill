# Bill Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement bill export functionality supporting Excel (.xlsx) and PDF formats, with backend file generation and frontend cross-platform download.

**Architecture:** Backend generates files via FastExcel (Excel) and OpenPDF (PDF), returns binary streams directly through HTTP response. Frontend receives ArrayBuffer and handles file saving per platform (H5 Blob download, mini-program/App file system API).

**Tech Stack:** FastExcel 1.2.0, OpenPDF 2.0.3, Spring Boot 3.5.11, MyBatis-Plus, Alova.js, uni-app

---

### Task 1: Add Maven Dependencies

**Files:**
- Modify: `chuan-bill-server/pom.xml:51-193`

- [ ] **Step 1: Add FastExcel dependency**

Add before the `</dependencies>` closing tag (after line 188, before `</dependencies>`):

```xml
    <!-- FastExcel (Excel 导出) -->
    <dependency>
      <groupId>cn.idev.excel</groupId>
      <artifactId>fastexcel</artifactId>
      <version>1.2.0</version>
    </dependency>

    <!-- OpenPDF (PDF 导出) -->
    <dependency>
      <groupId>com.github.librepdf</groupId>
      <artifactId>openpdf</artifactId>
      <version>2.0.3</version>
    </dependency>
```

- [ ] **Step 2: Verify dependencies resolve**

Run: `cd chuan-bill-server && mvn dependency:resolve -pl . -DincludeArtifactIds=fastexcel,openpdf`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/pom.xml
git commit -m "chore: add FastExcel and OpenPDF dependencies for bill export"
```

---

### Task 2: Create ExportBillDTO

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/ExportBillDTO.java`

- [ ] **Step 1: Create ExportBillDTO**

```java
package com.samoy.chuanbillserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "账单导出参数")
public class ExportBillDTO {

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式错误")
    @Schema(description = "开始日期", example = "2024-01-01")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式错误")
    @Schema(description = "结束日期", example = "2024-03-31")
    private String endDate;

    @Schema(description = "分类 ID", example = "123456")
    private String categoryId;

    @Pattern(regexp = "^(income|expense|)$", message = "类型不正确")
    @Schema(description = "账单类型：income-收入，expense-支出，空字符串：全部", example = "expense")
    private String type;

    @Schema(description = "支付方式 ID", example = "123456")
    private String paymentMethodId;

    @Schema(description = "家庭 ID", example = "family123")
    private String familyId;

    @NotBlank(message = "导出格式不能为空")
    @Pattern(regexp = "^(excel|pdf)$", message = "格式仅支持 excel 或 pdf")
    @Schema(description = "导出格式：excel 或 pdf", example = "excel", requiredMode = Schema.RequiredMode.REQUIRED)
    private String format;
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/ExportBillDTO.java
git commit -m "feat: add ExportBillDTO for bill export parameters"
```

---

### Task 3: Add Export Error Codes to ResultEnum

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java:41-49`

- [ ] **Step 1: Add export error codes**

Add after `BILL_ANALYSIS_FAILED(2103, ...)` (line 47), before `AI_ANALYSIS_RATE_LIMITED`:

```java
    BILL_EXPORT_TIME_RANGE_EXCEEDED(2105, "时间范围不能超过3个月"),
    BILL_EXPORT_DATA_TOO_LARGE(2106, "数据量过大，请缩小筛选范围"),
    BILL_EXPORT_NO_DATA(2107, "无导出数据"),
    BILL_EXPORT_FAILED(2108, "导出失败"),
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java
git commit -m "feat: add export error codes to ResultEnum"
```

---

### Task 4: Create ExcelExportUtil

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/ExcelExportUtil.java`

- [ ] **Step 1: Create ExcelExportUtil**

```java
package com.samoy.chuanbillserver.utils;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.samoy.chuanbillserver.vo.BillVO;
import java.io.OutputStream;
import java.util.List;
import lombok.Data;

public class ExcelExportUtil {

    @Data
    public static class BillExcelRow {

        @ExcelProperty("账单名称")
        @ColumnWidth(20)
        private String name;

        @ExcelProperty("类型")
        @ColumnWidth(8)
        private String type;

        @ExcelProperty("金额")
        @ColumnWidth(12)
        private String amount;

        @ExcelProperty("时间")
        @ColumnWidth(18)
        private String time;

        @ExcelProperty("分类")
        @ColumnWidth(12)
        private String categoryName;

        @ExcelProperty("支付方式")
        @ColumnWidth(12)
        private String paymentMethodName;

        @ExcelProperty("用户")
        @ColumnWidth(10)
        private String userNickname;

        @ExcelProperty("家庭")
        @ColumnWidth(12)
        private String familyName;

        @ExcelProperty("备注")
        @ColumnWidth(30)
        private String remark;
    }

    public static void export(List<BillVO> bills, OutputStream outputStream) {
        List<BillExcelRow> rows = bills.stream().map(bill -> {
            BillExcelRow row = new BillExcelRow();
            row.setName(bill.getName());
            row.setType("income".equals(bill.getType()) ? "收入" : "支出");
            row.setAmount(bill.getAmount() != null ? bill.getAmount().toPlainString() : "");
            row.setTime(bill.getTime() != null ? bill.getTime().toString().replace("T", " ") : "");
            row.setCategoryName(bill.getCategory() != null ? bill.getCategory().getName() : "");
            row.setPaymentMethodName(
                    bill.getPaymentMethod() != null ? bill.getPaymentMethod().getName() : "");
            row.setUserNickname(bill.getUserNickname() != null ? bill.getUserNickname() : "");
            row.setFamilyName(bill.getFamilyName() != null ? bill.getFamilyName() : "");
            row.setRemark(bill.getRemark() != null ? bill.getRemark() : "");
            return row;
        }).toList();

        EasyExcel.write(outputStream, BillExcelRow.class).sheet("账单").doWrite(rows);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/ExcelExportUtil.java
git commit -m "feat: add ExcelExportUtil for bill Excel export"
```

---

### Task 5: Create PdfExportUtil

**Files:**
- Create: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/PdfExportUtil.java`

- [ ] **Step 1: Create PdfExportUtil**

```java
package com.samoy.chuanbillserver.utils;

import com.samoy.chuanbillserver.vo.BillVO;
import java.io.OutputStream;
import java.util.List;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

public class PdfExportUtil {

    private static final String[] HEADERS = {"账单名称", "类型", "金额", "时间", "分类", "支付方式", "用户", "家庭", "备注"};
    private static final float[] COLUMN_WIDTHS = {12f, 6f, 8f, 14f, 10f, 10f, 8f, 10f, 22f};

    public static void export(List<BillVO> bills, OutputStream outputStream) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font headerFont = createChineseFont(10, Font.BOLD);
        Font dataFont = createChineseFont(9, Font.NORMAL);

        PdfPTable table = new PdfPTable(COLUMN_WIDTHS);
        table.setWidthPercentage(100);

        // 表头
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setGrayFill(0.9f);
            table.addCell(cell);
        }

        // 数据行
        for (BillVO bill : bills) {
            table.addCell(new Phrase(bill.getName(), dataFont));
            table.addCell(new Phrase("income".equals(bill.getType()) ? "收入" : "支出", dataFont));
            table.addCell(new Phrase(bill.getAmount() != null ? bill.getAmount().toPlainString() : "", dataFont));
            table.addCell(new Phrase(bill.getTime() != null ? bill.getTime().toString().replace("T", " ") : "", dataFont));
            table.addCell(new Phrase(bill.getCategory() != null ? bill.getCategory().getName() : "", dataFont));
            table.addCell(new Phrase(bill.getPaymentMethod() != null ? bill.getPaymentMethod().getName() : "", dataFont));
            table.addCell(new Phrase(bill.getUserNickname() != null ? bill.getUserNickname() : "", dataFont));
            table.addCell(new Phrase(bill.getFamilyName() != null ? bill.getFamilyName() : "", dataFont));
            table.addCell(new Phrase(bill.getRemark() != null ? bill.getRemark() : "", dataFont));
        }

        document.add(table);
        document.close();
    }

    private static Font createChineseFont(float size, int style) {
        try {
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            // fallback to default font
            return FontFactory.getFont(FontFactory.HELVETICA, size, style);
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/PdfExportUtil.java
git commit -m "feat: add PdfExportUtil for bill PDF export"
```

---

### Task 6: Add exportBill to IBillService Interface

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java:1-96`

- [ ] **Step 1: Add import and method signature**

Add import after existing imports (after line 14):

```java
import com.samoy.chuanbillserver.dto.ExportBillDTO;
import jakarta.servlet.http.HttpServletResponse;
```

Add method before the closing `}` of the interface:

```java
    /**
     * 导出账单
     *
     * @param userId 用户ID
     * @param dto    导出参数
     * @param response HTTP响应
     */
    void exportBill(String userId, ExportBillDTO dto, HttpServletResponse response);
```

- [ ] **Step 2: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java
git commit -m "feat: add exportBill method to IBillService interface"
```

---

### Task 7: Implement exportBill in BillServiceImpl

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java:1-482`

- [ ] **Step 1: Add imports**

Add after existing imports (after line 39):

```java
import com.samoy.chuanbillserver.dto.ExportBillDTO;
import com.samoy.chuanbillserver.utils.ExcelExportUtil;
import com.samoy.chuanbillserver.utils.PdfExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
```

- [ ] **Step 2: Add exportBill method**

Add after the `getMonthlyStats` method (after line 236), before the `sendFamilyBillNotification` method:

```java
    @Override
    public void exportBill(String userId, ExportBillDTO dto, HttpServletResponse response) {
        // 1. 校验时间范围不超过3个月
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            LocalDate start = LocalDate.parse(dto.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate end = LocalDate.parse(dto.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1));
            if (months > 3) {
                throw new BusinessException(ResultEnum.BILL_EXPORT_TIME_RANGE_EXCEEDED);
            }
        }

        // 2. 构建查询条件（复用筛选逻辑，不分页）
        BillListDTO listDTO = new BillListDTO();
        listDTO.setStartDate(dto.getStartDate());
        listDTO.setEndDate(dto.getEndDate());
        listDTO.setType(dto.getType());
        listDTO.setCategoryId(dto.getCategoryId());
        listDTO.setPaymentMethodId(dto.getPaymentMethodId());
        listDTO.setFamilyId(dto.getFamilyId());

        if (listDTO.getFamilyId() != null && !familyService.isMember(userId, listDTO.getFamilyId())) {
            throw new BusinessException(ResultEnum.FAMILY_NOT_MEMBER);
        }

        LambdaQueryWrapper<Bill> wrapper = buildQueryWrapper(userId, listDTO);

        // 3. 查询记录数量限制
        long count = this.count(wrapper);
        if (count == 0) {
            throw new BusinessException(ResultEnum.BILL_EXPORT_NO_DATA);
        }
        if (count > 10000) {
            throw new BusinessException(ResultEnum.BILL_EXPORT_DATA_TOO_LARGE);
        }

        // 4. 查询数据并转换
        List<Bill> billList = baseMapper.selectList(wrapper);
        List<BillVO> billVOList = convertToBillVOList(billList);

        // 5. 生成文件并写入响应
        try {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if ("excel".equals(dto.getFormat())) {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename=bills_" + dateStr + ".xlsx");
                ExcelExportUtil.export(billVOList, response.getOutputStream());
            } else {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=bills_" + dateStr + ".pdf");
                PdfExportUtil.export(billVOList, response.getOutputStream());
            }
            response.getOutputStream().flush();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导出账单失败", e);
            throw new BusinessException(ResultEnum.BILL_EXPORT_FAILED);
        }
    }
```

- [ ] **Step 3: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java
git commit -m "feat: implement exportBill in BillServiceImpl"
```

---

### Task 8: Add Export Endpoint to BillController

**Files:**
- Modify: `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java:1-109`

- [ ] **Step 1: Add import**

Add after existing imports (after line 10):

```java
import com.samoy.chuanbillserver.dto.ExportBillDTO;
import jakarta.servlet.http.HttpServletResponse;
```

- [ ] **Step 2: Add export endpoint**

Add after the `getMonthlyStats` method (after line 108), before the closing `}`:

```java
    @PostMapping("/export")
    @Operation(summary = "导出账单", description = "根据筛选条件导出账单，支持 Excel 和 PDF 格式")
    public void exportBill(@Validated @RequestBody ExportBillDTO dto, HttpServletResponse response) {
        String userId = StpUtil.getLoginIdAsString();
        billService.exportBill(userId, dto, response);
    }
```

- [ ] **Step 3: Verify compilation**

Run: `cd chuan-bill-server && mvn compile -q`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java
git commit -m "feat: add bill export endpoint to BillController"
```

---

### Task 9: Add Frontend API Definition

**Files:**
- Modify: `chuan-bill-app/src/api/apiDefinitions.ts:19-75`

- [ ] **Step 1: Add bill.export definition**

Add after line 43 (`'bill.addBill': ...`):

```typescript
  'bill.export': ['POST', '/bill/export'],
```

- [ ] **Step 2: Regenerate API types**

Run: `cd chuan-bill-app && pnpm alova-gen`
Expected: globals.d.ts updated

- [ ] **Step 3: Run alova-api-fix**

Run: `cd chuan-bill-app && python .claude/skills/alova-api-fix/scripts/fix_alova_api.py src/api/globals.d.ts`
Expected: Script completes successfully, amount fields fixed to string type

- [ ] **Step 4: Type check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add chuan-bill-app/src/api/apiDefinitions.ts chuan-bill-app/src/api/globals.d.ts
git commit -m "feat: add bill export API definition"
```

---

### Task 10: Update ExportFilterPopup.vue

**Files:**
- Modify: `chuan-bill-app/src/pages/mine/components/ExportFilterPopup.vue:1-186`

- [ ] **Step 1: Update handleExport function**

Replace the `handleExport` function (lines 33-78) with:

```typescript
async function handleExport() {
  if (loading.value) {
    return
  }

  loading.value = true
  try {
    const params = { ...filterData.value, format: selectedFormat.value }
    const res = await Apis.bill.export({ data: params })

    // res is ArrayBuffer (binary stream)
    const format = selectedFormat.value
    const ext = format === 'excel' ? '.xlsx' : '.pdf'
    const fileName = `bills_${dayjs().format('YYYY-MM-DD')}${ext}`

    // #ifdef H5
    const blob = new Blob([res], {
      type: format === 'excel'
        ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        : 'application/pdf',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
    // #endif

    // #ifdef MP-WEIXIN
    const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
    const fs = uni.getFileSystemManager()
    fs.writeFileSync(filePath, res, 'binary')
    await uni.openDocument({ filePath, showMenu: true })
    // #endif

    // #ifdef APP-PLUS
    const filePath = `${plus.io.PRIVATE_DOC}/${fileName}`
    const fs = plus.io.getFileSystemManager()
    fs.writeFileSync(filePath, res, 'binary')
    await uni.openDocument({ filePath, showMenu: true })
    // #endif

    toast.success('导出成功')
    modelValue.value = false
  }
  catch (e: any) {
    // If backend returned JSON error (not binary), parse it
    if (e?.message) {
      toast.error(e.message)
    }
    else {
      toast.error('导出失败，请重试')
    }
  }
  finally {
    loading.value = false
  }
}
```

- [ ] **Step 2: Lint check**

Run: `cd chuan-bill-app && pnpm lint:fix`
Expected: No errors

- [ ] **Step 3: Type check**

Run: `cd chuan-bill-app && pnpm type-check`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/src/pages/mine/components/ExportFilterPopup.vue
git commit -m "feat: implement bill export with cross-platform file download"
```

---

### Task 11: Error Response Handling in Frontend

**Files:**
- Modify: `chuan-bill-app/src/api/core/handlers.ts` (if needed)

- [ ] **Step 1: Check current response handler**

Read `chuan-bill-app/src/api/core/handlers.ts` and check how errors are handled for arraybuffer responses.

The export endpoint returns binary on success but JSON on error. The Alova response handler needs to detect this. If the Content-Type of an error response is `application/json`, parse it as JSON and throw.

- [ ] **Step 2: Add arraybuffer error handling if needed**

If the current handler doesn't handle this case, add logic to check `Content-Type` for arraybuffer responses:

```typescript
// In the response error handler, for arraybuffer responses:
if (response.headers.get('content-type')?.includes('application/json')) {
  // Parse as JSON error
  const text = await response.clone().text()
  const json = JSON.parse(text)
  throw new Error(json.message || '请求失败')
}
```

- [ ] **Step 3: Lint and type check**

Run: `cd chuan-bill-app && pnpm lint:fix && pnpm type-check`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add chuan-bill-app/src/api/core/handlers.ts
git commit -m "fix: handle arraybuffer error responses in API handler"
```

---

### Task 12: Final Verification

- [ ] **Step 1: Backend full build**

Run: `cd chuan-bill-server && mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Frontend lint and type check**

Run: `cd chuan-bill-app && pnpm lint:fix && pnpm type-check`
Expected: No errors

- [ ] **Step 3: Verify all files**

Check that all created/modified files exist:
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/dto/ExportBillDTO.java`
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/ExcelExportUtil.java`
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/utils/PdfExportUtil.java`
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/controller/BillController.java` (modified)
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/IBillService.java` (modified)
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/service/impl/BillServiceImpl.java` (modified)
- `chuan-bill-server/src/main/java/com/samoy/chuanbillserver/result/ResultEnum.java` (modified)
- `chuan-bill-app/src/api/apiDefinitions.ts` (modified)
- `chuan-bill-app/src/pages/mine/components/ExportFilterPopup.vue` (modified)
