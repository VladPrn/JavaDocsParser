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
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class ClassParser extends BaseParser {
	private final OWLClass classes;
	private final OWLClass interfaces;
	
	public ClassParser(OWLOntology ont, List<String> paths) {	
		man = OWLManager.createOWLOntologyManager();
		fact =  man.getOWLDataFactory();
		classes = fact.getOWLClass(IRI.create("#Classes"));
		interfaces = fact.getOWLClass(IRI.create("#Interfaces"));
		this.ont = ont;
		this.paths = paths;
	}
	
	//Начать работу
	public void start() {
		if (messager == null || progress == null) {
			return;
		}
		
		for (String path : paths) {
			if (stop) {
				break;
			}
			parseClassPage(path);
		}
	}
	
	//Распарсить страницу класса JavaDocs
	private void parseClassPage(String path) {
		Document doc;
		try {
			doc = Jsoup.parse(new File(path), "utf-8");
		} catch (IOException ex) {
			messager.send("can'not find - " + path);
			return;
		}
		
		String sName = htmlToName(path);
		
		OWLClass currentClass = fact.getOWLClass(IRI.create("#Class_" + sName));
		
		try {		
			messager.send("begin parse - " + sName);
			
			List<Element> content = doc.getElementsByClass("contentContainer");
			if (content.size() != 1) {
				return;
			}
			
			List<Element> divInheritances = content.get(0).getElementsByClass("inheritance");
			parseBaseClass(currentClass, divInheritances);
			
			List<Element> divDescriptions = content.get(0).getElementsByClass("description");
			parseImplements(currentClass, divDescriptions);
			
			List<Element> summary = content.get(0).getElementsByClass("summary");
			if (summary.size() != 1) {
				return;
			}
			List<Element> overviews = summary.get(0).getElementsByClass("memberSummary");	
			
			for (int i = 0; i < overviews.size(); i++)
			{
				String str = overviews.get(i).getElementsByTag("caption").get(0).text();
				if (specialEquals("All Methods", str)) {
					parseMethods(sName, currentClass, overviews.get(i));
				} else if (specialEquals("Constructors", str)) {
					parseConstructors(sName, currentClass, overviews.get(i));
				}
			}
	
			messager.send("end parse - " + sName);
			parsedCount++;
			progress.alreadyDid(parsedCount);
		} catch (IndexOutOfBoundsException ex) {
			messager.send("IndexOutOfBoundsException");
		}
	}
	
	//Распарсить и добавить связи к реализуемым интерфейсам
	private void parseImplements(OWLClass currentClass, List<Element> divInheritances) {
		Element Inheritance = divInheritances.get(0);
		List<Element> dls = Inheritance.getElementsByTag("dl");
		String href;
		String name;
		for (Element dl : dls) {
			if (specialEquals("All Implemented Interfaces", dl.text())) {
				List<Element> as = dl.getElementsByTag("a");
				for (Element a : as) {
					href = a.attr("href");
					name = href.substring(href.lastIndexOf("/") + 1, href.lastIndexOf("."));
					
					OWLClass inter = fact.getOWLClass(IRI.create("#Interface_" + name));
					
					OWLSubClassOfAxiom axiom1 = fact.getOWLSubClassOfAxiom(currentClass, inter);
					man.addAxiom(ont, axiom1);
					
					OWLSubClassOfAxiom axiom2 = fact.getOWLSubClassOfAxiom(inter, interfaces);
					man.addAxiom(ont, axiom2);
				}
				break;
			}
		}
	}
	
	//Распарсить и добавить связь к суперклассу
	private void parseBaseClass(OWLClass currentClass, List<Element> divInheritances) {		
		Element lastInheritance = divInheritances.get(divInheritances.size() - 2);
		String lastHref = lastInheritance.getElementsByTag("a").get(0).attr("href");
		String name = htmlToName(lastHref);
		if ("Object".equals(name)) {
			OWLSubClassOfAxiom axiom1 = fact.getOWLSubClassOfAxiom(currentClass, classes);
			man.addAxiom(ont, axiom1);
		} else {
			OWLClass BaseClass = fact.getOWLClass(IRI.create("#Class_" + name));
			OWLSubClassOfAxiom axiom1 = fact.getOWLSubClassOfAxiom(currentClass, BaseClass);
			man.addAxiom(ont, axiom1);
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
			String about;
			try {
				returned = tr.getElementsByClass("colFirst").get(0).getElementsByTag("code").get(0).text();
				method = tr.getElementsByClass("colLast").get(0).getElementsByTag("code").get(0).text();
				about = tr.getElementsByClass("colLast").get(0).getElementsByClass("block").get(0).text();
			} catch (IndexOutOfBoundsException ex) {
				messager.send("Can'not parse method");
				continue;
			}
			
			String methodName = method.substring(0, method.lastIndexOf("("));
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
			OWLNamedIndividual ind = fact.getOWLNamedIndividual(IRI.create("#Class_" + sName + "_" + entry.getKey()));
			OWLClassAssertionAxiom axiom2 = fact.getOWLClassAssertionAxiom(currentClass, ind);
			man.addAxiom(ont, axiom2);
			
			OWLAnnotation anno = fact.getOWLAnnotation(fact.getRDFSComment(), fact.getOWLLiteral(entry.getValue().toString()));
			OWLAxiom axiom3 = fact.getOWLAnnotationAssertionAxiom(ind.getIRI(), anno);
			man.addAxiom(ont, axiom3);
		}
	}
	
	//Распарсить конструкторы и добавить их в онтологию
	private void parseConstructors(String sName, OWLClass currentClass, Element divMethods) {
		List<Element> trs = divMethods.getElementsByTag("tr");
		
		StringBuilder stringConstructors = new StringBuilder("Constructors:" + System.lineSeparator());
		for (int i = 1; i < trs.size(); i++) {
			Element tr = trs.get(i);
			String method;
			String about;
			
			try {
				method = tr.getElementsByTag("code").get(0).text();
				about = tr.getElementsByClass("block").get(0).text();
			} catch (IndexOutOfBoundsException ex) {
				messager.send("Can'not parse constructor");
				continue;
			}
			
			stringConstructors.append("//").append(about).append(System.lineSeparator());
			stringConstructors.append(method).append(System.lineSeparator()).append(System.lineSeparator());
		}
		
		OWLNamedIndividual ind = fact.getOWLNamedIndividual(IRI.create("#" + sName + "_Constructor"));
		OWLClassAssertionAxiom axiom2 = fact.getOWLClassAssertionAxiom(currentClass, ind);
		man.addAxiom(ont, axiom2);
			
		OWLAnnotation anno = fact.getOWLAnnotation(fact.getRDFSComment(), fact.getOWLLiteral(stringConstructors.toString()));
		OWLAxiom axiom3 = fact.getOWLAnnotationAssertionAxiom(ind.getIRI(), anno);
		man.addAxiom(ont, axiom3);
	}
}