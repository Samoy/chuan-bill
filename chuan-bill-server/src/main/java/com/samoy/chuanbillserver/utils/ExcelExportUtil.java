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
        List<BillExcelRow> rows = bills.stream()
                .map(bill -> {
                    BillExcelRow row = new BillExcelRow();
                    row.setName(bill.getName());
                    row.setType("income".equals(bill.getType()) ? "收入" : "支出");
                    row.setAmount(bill.getAmount() != null ? bill.getAmount().toPlainString() : "");
                    row.setTime(
                            bill.getTime() != null ? bill.getTime().toString().replace("T", " ") : "");
                    row.setCategoryName(
                            bill.getCategory() != null ? bill.getCategory().getName() : "");
                    row.setPaymentMethodName(
                            bill.getPaymentMethod() != null
                                    ? bill.getPaymentMethod().getName()
                                    : "");
                    row.setUserNickname(bill.getUserNickname() != null ? bill.getUserNickname() : "");
                    row.setFamilyName(bill.getFamilyName() != null ? bill.getFamilyName() : "");
                    row.setRemark(bill.getRemark() != null ? bill.getRemark() : "");
                    return row;
                })
                .toList();

        EasyExcel.write(outputStream, BillExcelRow.class).sheet("账单").doWrite(rows);
    }
}
