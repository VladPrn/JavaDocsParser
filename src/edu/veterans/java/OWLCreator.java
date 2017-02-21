package edu.veterans.java;

import java.io.File;
import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class OWLCreator extends Thread {
	//Чтобы каждый раз не выбирать
	public String currentPath = "C:/Users/mvideo/Downloads/api/";
	
	private MessageSend messager = null;
	private Progress progressClasses = null;
	private Progress progressInterfaces = null;
	
	private final OWLOntologyManager man;
	private final OWLDataFactory fact;
	private final OWLOntology ont;
	
	private OWLClass classes;
	private OWLClass interfaces;
	private ClassParser clParser;
	private InterfaceParser inParser;
	
	public OWLCreator() throws OWLOntologyCreationException {
		man = OWLManager.createOWLOntologyManager();
		fact =  man.getOWLDataFactory();
		ont = man.createOntology(IRI.create(new File("java ontologia.owl")));
	}
	
	//Начало нового потока
	public void run() {
		if (messager == null) {
			return;
		}
		
		createBaseStruct();		
		parseAllClasses();
		parseAllInterfaces();
		
		
		try {
			ont.saveOntology(new OWLXMLOntologyFormat());
		} catch (OWLOntologyStorageException e) {
			messager.send("can'not save ontologia" + System.lineSeparator());
		}
	}
	
	//Прервать работу
	public void stopCreating() {
		if (clParser != null) {
			clParser.stopCreating();
		}
		if (inParser != null) {
			inParser.stopCreating();
		}
	}
	
	public void addMessager(MessageSend messager) {
		this.messager = messager;
	}
	
	public void addProgressClasses(Progress progress) {
		this.progressClasses = progress;
	}
	
	public void addProgressInterfaces(Progress progress) {
		this.progressInterfaces = progress;
	}
	
	//Создать базовую структуру онтологии
	private void createBaseStruct() {
		classes = fact.getOWLClass(IRI.create("#Classes"));
		OWLSubClassOfAxiom axiom1 = fact.getOWLSubClassOfAxiom(classes, man.getOWLDataFactory().getOWLThing());
		man.addAxiom(ont, axiom1);
		
		interfaces = fact.getOWLClass(IRI.create("#Interfaces"));
		OWLSubClassOfAxiom axiom2 = fact.getOWLSubClassOfAxiom(interfaces, man.getOWLDataFactory().getOWLThing());
		man.addAxiom(ont, axiom2);
	}
	
	//Извлечь все адреса html-страниц классов и отправить в ClassParser
	private void parseAllClasses() {
		Document doc = null;
		try {
			doc = Jsoup.parse(new File(currentPath + "allclasses-frame.html"), "utf-8");
		} catch (IOException e) {
			messager.send("can'not find - " + currentPath + "allclasses-frame.html");
		}
		
		String path;
		List<String> paths = new LinkedList<>();
		List<Element> hrefs = doc.getElementsByTag("a");
		
		for (Element href : hrefs) {
			path = href.attr("href");
			paths.add(currentPath + path);
		}
		
		clParser = new ClassParser(ont, paths);
		clParser.addMessager(messager);
		clParser.addProgress(progressClasses);
		clParser.start();
	}
	
	//Извлечь все адреса html-страниц интерфейсов и отправить в InterfaceParser
	private void parseAllInterfaces() {
		Document doc = null;
		Document packageDoc = null;
		try {
			doc = Jsoup.parse(new File(currentPath + "overview-frame.html"), "utf-8");
		} catch (IOException e) {
			messager.send("can'not find - " + currentPath + "overview-frame.html");
		}
		
		String path = null;
		List<String> paths = new LinkedList<>();
		
		List<Element> div = doc.getElementsByClass("indexContainer");
		if (div.size() != 1) {
			messager.send("can'not parse - " + currentPath + "overview-frame.html");
			return;
		}
		
		List<Element> divPackage = div.get(0).getElementsByAttributeValue("title", "Packages");
		if (divPackage.size() != 2) {
			messager.send("can'not parse - " + currentPath + "overview-frame.html");
			return;
		}
		
		String interfacePath;
		List<Element> hrefs = divPackage.get(1).getElementsByTag("a"); 
		for (Element href : hrefs) {
			path = href.attr("href");
			try {
				packageDoc = Jsoup.parse(new File(currentPath + path), "utf-8");
			} catch (IOException e) {
				messager.send("can'not find - " + currentPath + path);
				continue;
			}
			
			List<Element> divCont = packageDoc.getElementsByClass("indexContainer");
			List<Element> divInterfaces = divCont.get(0).getElementsByAttributeValue("title", "Interfaces");
			if (divInterfaces.size() != 2) {
				messager.send("can'not parse - " + currentPath);
				continue;
			}
			
			List<Element> interfacesHrefs = divInterfaces.get(1).getElementsByTag("a");
			for (Element el: interfacesHrefs) {
				interfacePath = el.attr("href");
				paths.add(currentPath + path.substring(0, path.lastIndexOf("/")) + "/" + interfacePath);
			}		
		}
		
		inParser = new InterfaceParser(ont, paths);
		inParser.addMessager(messager);
		inParser.addProgress(progressInterfaces);
		inParser.start();
	}
}