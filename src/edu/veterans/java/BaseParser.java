package edu.veterans.java;

import java.util.List;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public abstract class BaseParser {
	//Для отправки сообщений в поток GUI
	protected MessageSend messager = null;
	protected Progress progress = null;
	
	//Необходимы для работы с онтологией
	protected OWLOntologyManager man;
	protected OWLDataFactory fact;
	protected OWLOntology ont;
	
	//Список путей к html-страницам
	protected List<String> paths;
	//Количество распарсенных html-страницам на данный момент
	protected int parsedCount = 0;
	//Прерван
	protected boolean stop;
	
	public void addMessager(MessageSend messager) {
		this.messager = messager;
	}
	
	public void addProgress(Progress progress) {
		this.progress = progress;
	}
	
	public void stopCreating() {
		stop = true;
	}
	
	/* 
	 * Эквивалентность начала строк разной длинны
	 * (Начало - строка меньшей длинны)
	*/
	protected boolean specialEquals(String norm, String html) {
		if (norm.length() > html.length()) {
			return false;
		}
		return norm.equals(html.substring(0, norm.length()));
	}
	
	//Извлечь из адреса класса его имя
	protected String htmlToName(String path) {
		return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
	}
}