package de.fh;

import de.fh.agent.WumpusHunterAgent;
import de.fh.blanks.HunterWorld;
import de.fh.wumpus.HunterPercept;
import de.fh.wumpus.enums.HunterAction;
import de.fh.wumpus.enums.HunterActionEffect;

/*
 * DIESE KLASSE VER�NDERN SIE BITTE NUR AN DEN GEKENNZEICHNETEN STELLEN
 * wenn die Bonusaufgabe bewertet werden soll.
 */
public class MyAgent extends WumpusHunterAgent {

	HunterWorld hunterWorld = new HunterWorld();
	
	public static void main(String[] args) {

		MyAgent agent = new MyAgent("");
		MyAgent.start(agent,"127.0.0.1", 5000);
	}

	public MyAgent(String name) {
		super(name);
	}

	/**
	 * In dieser Methode kann das Wissen �ber die Welt (der State, der Zustand)
	 * entsprechend der aktuellen Wahrnehmungen anpasst, und die "interne Welt",
	 * die Wissensbasis, des Agenten kontinuierlich ausgebaut werden.
	 *
	 * Wichtig: Diese Methode wird aufgerufen, bevor der Agent handelt, d.h.
	 * bevor die action()-Methode aufgerufen wird...
	 *
	 * @param percept Aktuelle Wahrnehmung
	 * @param actionEffect Reaktion des Servers auf vorhergew�hlte Aktion
	 */
	@Override
	public void updateState(HunterPercept percept, HunterActionEffect actionEffect) {
		this.hunterWorld.updateState(percept, actionEffect, nextAction);
	}

	/**
	 * Diesen Part erweitern Sie so, dass die n�chste(n) sinnvolle(n) Aktion(en),
	 * auf Basis der vorhandenen Zustandsinformationen und gegebenen Zielen, ausgef�hrt wird/werden.
	 * Der action-Part soll den Agenten so intelligent wie m�glich handeln lassen
	 *
	 * Beispiel: Wenn die letzte Wahrnehmung
	 * "percept.isGlitter() == true" enthielt, ist "HunterAction.GRAB" eine
	 * geeignete T�tigkeit. Wenn Sie wissen, dass ein Quadrat "unsicher"
	 * ist, k�nnen Sie wegziehen
	 *
	 * @return Die n�chste HunterAction die vom Server ausgef�hrt werden soll
	 */
	@Override
	public HunterAction action() {

		/*
		HunterAction
        M�gliche HunterActions sind m�glich:

       	HunterAction.GO_FORWARD
       	HunterAction.TURN_LEFT
		HunterAction.TURN_RIGHT
		HunterAction.SHOOT
		HunterAction.SIT
		HunterAction.GRAB
		HunterAction.QUIT_GAME
		*/
		
		nextAction = this.hunterWorld.getNextAction();
		
		return nextAction;
	}
}