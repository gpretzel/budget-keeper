package com.budgetmaster.budgetmaster;

import java.io.IOException;
import org.w3c.dom.Element;

public interface RecordsMapperBuilder {

    public RecordsMapper create();

    public void initFromXml(Element root) throws IOException;
}
