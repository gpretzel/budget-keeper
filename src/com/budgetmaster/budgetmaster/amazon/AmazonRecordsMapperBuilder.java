package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.Functional.ThrowingBiFunction;
import com.budgetmaster.budgetmaster.MarketplaceTransactionsInitializer;
import com.budgetmaster.budgetmaster.PluggableSupplier;
import java.nio.file.Path;
import com.budgetmaster.budgetmaster.RecordsMapper;
import com.budgetmaster.budgetmaster.Util;
import com.budgetmaster.budgetmaster.amazon.AmazonRecord.CsvOrderHeaders;
import com.budgetmaster.budgetmaster.amazon.AmazonRecord.CsvRefundHeaders;
import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class AmazonRecordsMapperBuilder implements PluggableSupplier<RecordsMapper> {

    @Override
    public RecordsMapper get() {
        var rawOrders = readCsv(inputOrdersCsvFile, CsvOrderHeaders.class);
        var rawRefunds = readCsv(inputRefundsCsvFile, CsvRefundHeaders.class);

        return new AmazonRecordsMapper(rawOrders, rawRefunds, tags,
                addOrderIdTag, mti, unclaimedOrdersCsvFile,
                unclaimedRefundsCsvFile);
    }

    @Override
    public void initFromXml(Element root) {
        mti.initFromXml(root);
        
        NodeList tagNodes = Util.queryNodes("tag", root);
        for (int i = 0; i < tagNodes.getLength(); i++) {
            Element el = (Element) tagNodes.item(i);
            if (tags == null) {
                tags = new HashSet<>();
            }
            tags.add(el.getFirstChild().getNodeValue());
        }
        
        addOrderIdTag = Util.queryNodes("with-order-id-tag", root).getLength()
                != 0;

        inputOrdersCsvFile = Util.readLastElementAsPath(root, "orders-csv");

        inputRefundsCsvFile = Util.readLastElementAsPath(root, "refunds-csv");

        unclaimedOrdersCsvFile = Util.readLastElementAsPath(root,
                "unclaimed-orders-collector-csv");

        unclaimedRefundsCsvFile = Util.readLastElementAsPath(root,
                "unclaimed-refunds-collector-csv");
    }

    private static AmazonRawRecords readCsv(Path file,
            Class<? extends Enum<?>> headerEnum) {
        return ThrowingBiFunction.toBiFunction(AmazonRawRecords::load).apply(
                file, headerEnum);
    }

    private Set<String> tags;
    private boolean addOrderIdTag;
    private Path inputOrdersCsvFile;
    private Path inputRefundsCsvFile;
    private Path unclaimedOrdersCsvFile;
    private Path unclaimedRefundsCsvFile;

    private final MarketplaceTransactionsInitializer mti = new MarketplaceTransactionsInitializer();
}
