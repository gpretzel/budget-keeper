package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;


public class Test {
    public static void main(String[] args) throws Exception {
        List<Record> records;
        records = new TdBankCcStatementCsv().read(Path.of("C:\\Users\\asemenyu\\Downloads\\$\\transactions_2018-12-10.csv"));
//        records = new TdBankStatementPdf().read(Path.of("C:\\Users\\asemenyu\\Downloads\\$\\View PDF Statement_2019-03-05.pdf"));
//        records = new DcuStatementPdf().read(Path.of("C:/Users/asemenyu/Downloads/stmt_20180131.pdf"));
//        records = new DcuCcStatementPdf().read(Path.of("C:/Users/asemenyu/Downloads/$/dcu_stmt_011019.pdf"));
        
        UnaryOperator<Record> processor = new RecordMapperBuilder().createFromXml(Path.of("C:\\Users\\asemenyu\\Documents\\NetBeansProjects\\BudgetMaster\\src\\main\\java\\com\\budgetmaster\\budgetmaster\\config.xml"));
        
        new RecordCsvSerializer().saveToFile(records.stream().map(processor), Path.of("budgetmaster.csv"));
    }
}
