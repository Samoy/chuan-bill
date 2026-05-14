package com.samoy.chuanbillserver.utils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.samoy.chuanbillserver.vo.BillVO;
import java.io.OutputStream;
import java.util.List;

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
            table.addCell(new Phrase(
                    bill.getTime() != null ? bill.getTime().toString().replace("T", " ") : "", dataFont));
            table.addCell(
                    new Phrase(bill.getCategory() != null ? bill.getCategory().getName() : "", dataFont));
            table.addCell(new Phrase(
                    bill.getPaymentMethod() != null ? bill.getPaymentMethod().getName() : "", dataFont));
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
