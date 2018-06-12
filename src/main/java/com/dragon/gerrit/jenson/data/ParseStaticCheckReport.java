package com.dragon.gerrit.jenson.data;

import static com.dragon.gerrit.jenson.util.Localization.getLocalized;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.dragon.gerrit.jenson.data.entity.ScrIssue;
import com.dragon.gerrit.jenson.data.entity.ScrReport;
import com.dragon.gerrit.jenson.util.Logger;

public class ParseStaticCheckReport {
	
	private ScrReport report;
	
	public ScrReport getScrReport(InputStream inputStream, Logger logger) {
		report = new ScrReport();
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(inputStream);
		} catch (DocumentException e) {
			logger.e(getLocalized("jenkins.plugin.error.sonar.report.invalid"));
			return null;
		}
		Element root = document.getRootElement();
		parseNodes(root, logger);
		
		return report;
	}
	
	private void parseNodes(Element node, Logger logger) {
    	if (node.getName().compareTo("results") == 0) {
            List<Attribute> list = node.attributes();  
            for(Attribute attribute : list){  
                report.setVersion(attribute.getValue());
            } 
    	} else if (node.getName().compareTo("error") == 0) {
            ScrIssue scrIssue = new ScrIssue();
            List<Attribute> list = node.attributes();
            
            for (Attribute attribute : list){  
                if (attribute.getName().compareTo("file") == 0) {
                	scrIssue.setFile(attribute.getValue());
                } else if (attribute.getName().compareTo("line") == 0) {
                	scrIssue.setLine(Integer.valueOf(attribute.getValue()));
                } else if (attribute.getName().compareTo("id") == 0) {
                	scrIssue.setId(attribute.getValue());
                } else if (attribute.getName().compareTo("severity") == 0) {
                	scrIssue.setSeverity(attribute.getValue());
                } else if (attribute.getName().compareTo("msg") == 0) {
                	scrIssue.setMessage(attribute.getValue());
                }
            }
            
            report.getIssues().add(scrIssue);
    	}
    	
        Iterator<Element> iterator = node.elementIterator();  
        while(iterator.hasNext()){  
            Element e = iterator.next();  
            parseNodes(e, logger);  
        } 
    } 
}



