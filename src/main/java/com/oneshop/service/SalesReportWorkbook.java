package com.oneshop.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Small dependency-free OOXML export. Text uses inline strings, never spreadsheet formulas. */
public final class SalesReportWorkbook {
    private SalesReportWorkbook(){}
    public static byte[] write(SalesReportService.Report r){
        var rows=new ArrayList<List<?>>();
        rows.add(List.of("BÁO CÁO DOANH THU ONESHOP"));rows.add(List.of("Từ ngày",r.from().toString(),"Đến ngày",r.to().toString()));
        rows.add(List.of("Doanh thu sản phẩm (không gồm vận chuyển)",r.revenue()));rows.add(List.of("Đơn hoàn tất",r.completed(),"Đơn hủy",r.cancelled()));
        rows.add(List.of("Cửa hàng","Đơn hoàn tất","Doanh thu (VND)"));r.stores().forEach(s->rows.add(List.of(s.name(),s.orders(),s.revenue())));
        rows.add(List.of());rows.add(List.of("Sản phẩm","Số lượng bán","Doanh thu (VND)"));r.products().forEach(p->rows.add(List.of(p.name(),p.quantity(),p.revenue())));
        rows.add(List.of());rows.add(List.of("Ngày hoàn tất","Doanh thu (VND)"));r.days().forEach(d->rows.add(List.of(d.day(),d.revenue())));
        StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols><col min=\"1\" max=\"1\" width=\"65\" customWidth=\"1\"/><col min=\"2\" max=\"4\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");
        int i=0;for(var row:rows){sheet.append("<row r=\"").append(++i).append("\">");int col=0;for(Object value:row){String cell=""+(char)('A'+col++)+i;
            if(value instanceof Number)sheet.append("<c r=\"").append(cell).append("\"><v>").append(value).append("</v></c>");
            else sheet.append("<c r=\"").append(cell).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(value.toString())).append("</t></is></c>");
        }sheet.append("</row>");}sheet.append("</sheetData></worksheet>");
        try(var output=new ByteArrayOutputStream();var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)){
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Doanh thu OneShop\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());zip.finish();return output.toByteArray();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static void entry(ZipOutputStream zip,String name,String text)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    private static String xml(String text){return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
