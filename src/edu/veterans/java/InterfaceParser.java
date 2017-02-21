package edu.veterans.java;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class InterfaceParser extends BaseParser {
	private final OWLClass interfaces;
	
	public InterfaceParser(OWLOntology ont, List<String> paths) {	
		man = OWLManager.createOWLOntologyManager();
		fact =  man.getOWLDataFactory();
		interfaces = fact.getOWLClass(IRI.create("#Interfaces"));;
		this.ont = ont;
		this.paths = paths;
	}

	//Начало работы
	public void start() {
		if (messager == null || progress == null) {
			return;
		}
		
		for (String path : paths) {
			if (stop) {
				break;
			}
			parseInterfacePage(path);
		}
	}
	
	//Распарсить страницу интерфейса JavaDocs
	private void parseInterfacePage(String path) {
		Document doc;
		try {
			doc = Jsoup.parse(new File(path), "utf-8");
		} catch (IOException ex) {
			messager.send("can'not find - " + path);
			return;
		}
		
		String sName = htmlToName(path);
		
		OWLClass currentInterface = fact.getOWLClass(IRI.create("#Interface_" + sName));
		OWLSubClassOfAxiom axiom1 = fact.getOWLSubClassOfAxiom(currentInterface, interfaces);
		man.addAxiom(ont, axiom1);
		
		try {		
			messager.send("begin parse - " + sName);
			
			List<Element> content = doc.getElementsByClass("contentContainer");
			if (content.size() != 1) {
				return;
			}
			
			List<Element> summary = content.get(0).getElementsByClass("summary");
			if (summary.size() != 1) {
				return;
			}
			
			List<Element> overviews = summary.get(0).getElementsByClass("memberSummary");			
			
			for (int i = 0; i < overviews.size(); i++)
			{
				String str = overviews.get(i).getElementsByTag("caption").get(0).text();
				if (specialEquals("All Methods", str)) {
					parseMethods(sName, currentInterface, overviews.get(i));
				}
			}

			
			messager.send("end parse - " + sName);
			parsedCount++;
			progress.alreadyDid(parsedCount);
		} catch (IndexOutOfBoundsException ex) {
			messager.send("IndexOutOfBoundsException");
		}
	}
	
	//Распарсить методы и добавить их в онтологию
	private void parseMethods(String sName, OWLClass currentClass, Element divMethods) {
		List<Element> trs = divMethods.getElementsByTag("tr");
		Map<String, StringBuilder> methodsMap = new HashMap<>();
		
		StringBuilder stringMethods;
		for (int i = 1; i < trs.size(); i++) {
			Element tr = trs.get(i);
			String returned;
			String method;
			String methodName;
			String about;
			
			try {
				returned = tr.getElementsByClass("colFirst").get(0).getElementsByTag("code").get(0).text();
				method = tr.getElementsByClass("colLast").get(0).getElementsByTag("code").get(0).text();
				methodName = method.substring(0, method.lastIndexOf("("));
				about = tr.getElementsByClass("colLast").get(0).getElementsByClass("block").get(0).text();
			} catch (IndexOutOfBoundsException ex) {
				messager.send("Can'not parse method");
				continue;
			}
			
			stringMethods = new StringBuilder();
			stringMethods.append("//").append(about).append(System.lineSeparator());
			stringMethods.append(returned).append(" ").append(method).append(System.lineSeparator()).append(System.lineSeparator());
			if (methodsMap.get(methodName) == null) {
				methodsMap.put(methodName, new StringBuilder("Methods:" + System.lineSeparator()).append(stringMethods));
			} else {
				methodsMap.get(methodName).append(stringMethods);
			}
		}
		
		for (Map.Entry<String, StringBuilder> entry : methodsMap.entrySet()) {
			OWLNamedIndividual ind = fact.getOWLNamedIndividual(IRI.create("#Interface_" + sName + "_" + entry.getKey()));
			OWLClassAssertionAxiom axiom2 = fact.getOWLClassAssertionAxiom(currentClass, ind);
			man.addAxiom(ont, axiom2);
			
			OWLAnnotation anno = fact.getOWLAnnotation(fact.getRDFSComment(), fact.getOWLLiteral(entry.getValue().toString()));
			OWLAxiom axiom3 = fact.getOWLAnnotationAssertionAxiom(ind.getIRI(), anno);
			man.addAxiom(ont, axiom3);
		}
	}
}