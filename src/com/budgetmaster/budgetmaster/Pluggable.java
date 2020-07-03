package com.budgetmaster.budgetmaster;

import org.w3c.dom.Element;

public interface Pluggable {

    default public void initFromXml(Element root) {}
}
