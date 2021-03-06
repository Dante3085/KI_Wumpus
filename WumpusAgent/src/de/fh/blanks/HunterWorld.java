package de.fh.blanks;

import de.fh.suche.Suche;
import de.fh.uninformedSearch.Breitensuche;
import de.fh.wumpus.HunterPercept;
import de.fh.wumpus.enums.HunterAction;
import de.fh.wumpus.enums.HunterActionEffect;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

public class HunterWorld {
	private ArrayList<ArrayList<CellInfo>> view;

	private HunterAction previousAction;
	private HunterAction nextAction;
	private Hashtable<Integer, Integer> previousStenchRadar;
	private Point hunterPosition = new Point(1, 1);
	private Direction hunterDirection = Direction.EAST;
	private int numArrows = 15;
	private int maxArrows = numArrows;
	private int wumpiKilled = 0;
	private Point goldPosition = new Point(-1, -1);
	private boolean hasGold = false;
	private boolean worldCompletelyDiscovered = false;
	private boolean gameOver = false;
	private boolean wumpusCertainReachable = false;

	/**
	 * Hier wird einen Puffer von Actions gespeichert. Zum beispiel wenn der HUNTER
	 * von einem Quadrat A zu einem anderen B hingehen soll, dann sind die zu
	 * ausf�hrenden Actions hier gespeichert.
	 */
	private LinkedList<HunterAction> bufferActions = new LinkedList<>();

	/*
	 * Konfiguration zum T�ten vom Wumpi
	 */
	private boolean turned = false;
	// F�r ein wumpus auf 0 und mehere Wumpus auf 1 setzen.
	private final int MAX_COUNT_SUCCESSIVE_TURN = 1;
	private int countSuccessiveTurn = 0;

	public HunterWorld() {
		view = new ArrayList<ArrayList<CellInfo>>();
		CellInfo.getUnknownCells().clear();
	}

	/**
	 * Hier wird nur: Wenn action == TURN_LEFT oder TURN_RIGHT, die hunterDirection
	 * aktualisiert. Wenn action == GO_FORWARD, die hunterPosition aktualisiert.
	 * 
	 * Hier wird nicht bestimmt welche Aktion als n�chstes ausgef�hrt wird, das wird
	 * in der updateState Methode gemacht.
	 * 
	 * @param previousAction : entpricht letzte Action, die auf dem Server
	 *                       ausgef�hrt wurde.
	 */
	private void updateHunterPosition(HunterAction previousAction) {
		this.previousAction = previousAction;

		switch (previousAction) {
		case TURN_LEFT: {
			switch (hunterDirection) {
			case NORTH:
				this.hunterDirection = Direction.WEST;
				break;
			case EAST:
				this.hunterDirection = Direction.NORTH;
				break;
			case SOUTH:
				this.hunterDirection = Direction.EAST;
				break;
			case WEST:
				this.hunterDirection = Direction.SOUTH;
				break;
			default:
				throw new IllegalArgumentException("Unzul�ssige HunterAction");
			}
			break;
		}
		case TURN_RIGHT: {
			switch (hunterDirection) {
			case NORTH:
				this.hunterDirection = Direction.EAST;
				break;
			case EAST:
				this.hunterDirection = Direction.SOUTH;
				break;
			case SOUTH:
				this.hunterDirection = Direction.WEST;
				break;
			case WEST:
				this.hunterDirection = Direction.NORTH;
				break;
			default:
				throw new IllegalArgumentException("Unzul�ssige HunterAction");
			}
			break;
		}
		case GO_FORWARD: {
			switch (this.hunterDirection) {
			case NORTH:
				this.hunterPosition.setY(this.hunterPosition.getY() - 1);
				break;
			case EAST:
				this.hunterPosition.setX(this.hunterPosition.getX() + 1);
				break;
			case SOUTH:
				this.hunterPosition.setY(this.hunterPosition.getY() + 1);
				break;
			case WEST:
				this.hunterPosition.setX(this.hunterPosition.getX() - 1);
				break;
			default:
				throw new IllegalArgumentException("Unzul�ssige HunterAction");
			}
			break;
		}
		default:
			// Hier soll nichts gemacht werden
			break;
		}
	}

	/**
	 * Wichtig: Die updateState Methode wird zuerst ausgef�hrt, dann
	 * die action Methode.
	 * 
	 * @param percept: Information, die wir von der Server bekommen haben, durch
	 *        unsere letzte Action.
	 * @param actionEffect: Information, die wir von der Server bekommen haben,
	 *        durch unsere letzte Action.
	 */
	public void updateState(HunterPercept percept, HunterActionEffect actionEffect, HunterAction previousAction) {
		/**
		 * Je nach Sichtbarkeit & Schwierigkeitsgrad (laut Serverkonfiguration) aktuelle
		 * Wahrnehmung des Hunters. Beim Wumpus erhalten Sie je nach Level mehr oder
		 * weniger Mapinformationen.
		 */

		// Aktuelle Reaktion des Server auf die letzte �bermittelte Action.

		// Alle m�glichen Serverr�ckmeldungen:

		if (actionEffect == HunterActionEffect.BUMPED_INTO_WALL) {
			this.setWallInToView(this.hunterPosition, this.hunterDirection);
		}

		if (actionEffect == HunterActionEffect.WUMPUS_KILLED) {
			++wumpiKilled;
		}

		if (actionEffect == HunterActionEffect.GAME_OVER) {
			// Das Spiel ist zum Ende.
			printQuitGame();
			print();
		}

		/*
		 * M�gliche Percepts �ber die Welt erh�lt der Wumpushunter:
		 * 
		 * percept.isBreeze(); percept.isStench(); percept.isGlitter();
		 * percept.isRumble(); percept.isScream(); percept.isBump();
		 * percept.getWumpusStenchRadar()
		 */

		if (actionEffect == HunterActionEffect.MOVEMENT_SUCCESSFUL
				|| actionEffect == HunterActionEffect.GAME_INITIALIZED
				|| actionEffect == HunterActionEffect.GOLD_FOUND) {

			// wenn Letzte Bewegungsaktion war g�ltig, dann update HunterPosition.
			this.updateHunterPosition(previousAction);

			if (actionEffect == HunterActionEffect.GOLD_FOUND) {
				// Nach HunterAction.GRAB wurde das Gold gefunden.
				this.hasGold = true;
				this.goldPosition.set(-1, -1);
			}

			// Letzte Bewegungsaktion war g�ltig
			if (this.previousAction != HunterAction.TURN_RIGHT && this.previousAction != HunterAction.TURN_LEFT) {

				if (percept.isBreeze() || percept.isStench() || percept.isGlitter()) {

					if (percept.isBreeze()) {
						this.updateCell(CellType.BREEZE);
					}
					if (percept.isGlitter()) {
						this.goldPosition.set(this.hunterPosition);
						this.updateCell(CellType.GOLD);
						this.bufferActions.push(HunterAction.GRAB);
					}
				} else {
					this.updateCell(CellType.EMPTY);
				}
			}
		}

		if ((actionEffect == HunterActionEffect.NO_MORE_SHOOTS) || (this.wumpiKilled > 0 && this.hasGold)) {
			if (!gameOver) {
				this.quitGame();
			}
			this.gameOver = true;
		}

		/*
		 * percept.getWumpusStenchRadar() enth�lt alle Wumpi in max. R(ie)eichweite in
		 * einer Hashtable. Jeder Wumpi besitzt eine unique WumpusID (getKey). Die
		 * Manhattendistanz zum jeweiligen Wumpi ergibt sich aus der Gestanksitensit�t
		 * (getValue).
		 */

		Hashtable<Integer, Integer> stenchRadar = percept.getWumpusStenchRadar();

		// Gebe alle riechbaren Wumpis aus;
		if (stenchRadar.isEmpty()) {
//			System.out.println("Kein Wumpi zu riechen");
		} else {
			Point virtualPosition = this.getVirtualPositionHunter(hunterPosition, hunterDirection);
			if (virtualPosition.getX() == 0 || virtualPosition.getY() == 0
					|| (this.get(virtualPosition) != null && this.get(virtualPosition).getType() == CellType.WALL)
					|| (this.previousAction == HunterAction.SHOOT && actionEffect != HunterActionEffect.WUMPUS_KILLED
							&& this.countSuccessiveTurn < this.MAX_COUNT_SUCCESSIVE_TURN)) {

				this.bufferActions.push(HunterAction.TURN_RIGHT);
				this.bufferActions.push(HunterAction.TURN_LEFT);
				this.turned = true;

			} else {
				Map.Entry<Integer, Integer> entry = stenchRadar.entrySet().iterator().next();
				int key = (int) entry.getKey();
				int value = (int) entry.getValue();

				if (value == 1)
					this.wumpusCertainReachable = true;

//				if (this.turned && !percept.isBump()) { // F�r Maps mit viele W�nde in der Mitte
				if (this.turned) {
					this.bufferActions.push(HunterAction.SHOOT);
					if (numArrows > 0)
						--numArrows;
				} else {
					if (value == 3) {
						this.bufferActions.push(HunterAction.SIT);
					} else if (value == 2 || value == 1) {
						if (this.previousStenchRadar == null || this.previousStenchRadar.get(key) == null) {
							this.bufferActions.push(HunterAction.SIT);
						} else if (percept.isRumble() && value < this.previousStenchRadar.get(key)) {
							this.bufferActions.push(HunterAction.SHOOT);
							if (numArrows > 0)
								--numArrows;
						} else {
							this.bufferActions.push(HunterAction.SIT);
						}
					}
				}
				this.turned = false;
			}
			this.previousStenchRadar = stenchRadar;
		}

		if (this.turned) {
			this.countSuccessiveTurn++;
		} else {
			this.countSuccessiveTurn = 0;
		}

		if (bufferActions.isEmpty()) {
			if (worldCompletelyDiscovered) {
				this.searchWumpus();
			} else {
				this.exploreWorld();
			}
		}

		this.nextAction = this.bufferActions.remove();
	}

	/**
	 * Gibt die n�chste Action, die vom Hunter ausgef�hrt werden soll.
	 */
	public HunterAction getNextAction() {
		return this.nextAction;
	}

	/**
	 * Gibt das Element von der gew�nschte Postion, sonst null wenn das Element
	 * nicht existiert.
	 */
	public CellInfo get(int x, int y) {
		if (x < 0 || y < 0) {
			return null;
		}

		try {
			ArrayList<CellInfo> row = this.view.get(y);
			return row.get(x);

		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public CellInfo get(Point p) {
		return this.get(p.getX(), p.getY());
	}

	/**
	 * F�gt ein Element an der gew�nschte Position in der View ein, oder aktualisiert
	 * ihn, wenn er schon vorhanden ist
	 * 
	 * @return das vorherige Element, wenn ein Element aktualisiert wurde oder null
	 *         falls kein Element aktualisiert wurde.
	 */
	public CellInfo set(int x, int y, CellInfo newCellInfo) {
		if (x < 0 || y < 0) {
			throw new IllegalArgumentException("SET: Unzul�ssige Koordinaten " + "(" + x + ", " + y + ")" + "\n"
					+ "x und y Koodirnaten m�ssen gr��er gleich 0 sein.");
		}

		try {
			ArrayList<CellInfo> row = this.view.get(y);
			return row.set(x, newCellInfo);

		} catch (IndexOutOfBoundsException e) {
			this.add(x, y, newCellInfo);
			return null;
		}
	}

	/**
	 * F�gt ein Element an der gew�nschte Postion ein
	 * 
	 * Wird nur in der Set Methode genutzt.
	 */
	private boolean add(int x, int y, CellInfo newCellInfo) {
		if (x < 0 || y < 0) {
			return false;
		}

		try {
			if (y >= this.view.size()) {

				ArrayList<CellInfo> newRow = null;
				for (int j = this.view.size(); j <= y; j++) {
					newRow = new ArrayList<CellInfo>();
					this.view.add(newRow);
					for (int k = 0; k < x; k++) {
						newRow.add(null);
					}
				}
				newRow.add(newCellInfo);
			} else {

				ArrayList<CellInfo> row = this.view.get(y);
				if (x >= row.size()) {
					for (int i = row.size(); i < x; i++) {
						row.add(null);
					}
					row.add(newCellInfo);
				} else {
					this.set(x, y, newCellInfo);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		return true;
	}

	/**
	 * Gibt alle Information von der HunterWorld aus der Konsole aus.
	 */
	public void print() {
		String out = "HUNTER_WORLD\n{ hunterPosition: " + hunterPosition + ", hunterDirection: " + hunterDirection
				+ ", goldPosition: " + goldPosition + ", hasGold: " + hasGold + ", Anzahl Pfeile" + ", arrow shot: "
				+ (maxArrows - numArrows) + ", Anzahl Wumpus get�tet: " + this.wumpiKilled + " }\n";

		int rows = view.size();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < view.get(row).size(); col++) {
				CellInfo cellInfo = get(col, row);
				if (cellInfo != null)
					out += cellInfo.toString()
							+ new String(new char[12 - cellInfo.toString().length()]).replace('\0', ' ');
				else
					out += "(NULL)" + new String(new char[12 - 6]).replace('\0', ' ');
				;
			}
			out += "\n";
		}

		System.out.println(out);
	}

	/**
	 * Gibt die gefragten Information am Ende des Spiels aus der Konsole aus.
	 */
	public void printQuitGame() {
		String gold = (hasGold) ? "Ja" : "Nein";
		String out = "Anzahl Pfeile: " + maxArrows + ", Pfeile geschossen: " + (maxArrows - numArrows)
				+ ", Wumpi get�tet: " + wumpiKilled + ", Gold gefunden?: " + gold;
//		if (wumpiKilled > 0)
//			out += "\nDurschnittliche benutzte Pfeile pro Wumpus: " + (maxArrows - numArrows) / (double) wumpiKilled;
		System.out.println(out);
	}

	public Direction getHunterDirection() {
		return hunterDirection;
	}

	/**
	 * Aktualisiert eine Zelle, an der Stelle, wo der Hunter liegt, auf Basis von der
	 * Information, die vom Server bekommen wurden.
	 * 
	 * @param cellType
	 */
	public void updateCell(CellType cellType) {
		if (cellType == CellType.GOLD) {
			this.goldPosition.set(this.hunterPosition.getX(), this.hunterPosition.getY());
		}

		CellInfo previousCellInfo = this.set(this.hunterPosition.getX(), this.hunterPosition.getY(),
				new CellInfo(cellType));

		// pr�fe auf null wegen erster Aufruf( game initialized )
		// pr�fe auf CellType.TARGET :: Methode wird aufgerufen, nur wenn der Hunter
		// eine ganz neue Zelle entdeckt.
		if (previousCellInfo == null || previousCellInfo.getType() == CellType.TARGET) {
			this.setProbabilityAllAroundCell(cellType);
		}

	}

	private void setProbabilityOfCell(CellType cellType, int x, int y) {
		CellInfo targetUnkwonCell = this.get(x, y);

		if (targetUnkwonCell != null) {

			if (targetUnkwonCell.getType() == CellType.WALL) {
				return;
			}

			if (cellType == CellType.BREEZE) {
				targetUnkwonCell.setProbabilityPit(targetUnkwonCell.getProbabilityPit() + 50.0);
			} else if (cellType == CellType.EMPTY) {
				targetUnkwonCell.setProbabilityPit(0.0);
			}
		} else {
			if (cellType == CellType.BREEZE) {
				this.set(x, y, new CellInfo(x, y, 50.0));
			} else if (cellType == CellType.EMPTY) {
				this.set(x, y, new CellInfo(x, y, 0.0));
			}
		}
	}

	/**
	 * Aktulisiert bzw. setzt passende CellInfo von Typ "Unkwon" rund um den
	 * Aktuelle Position der Hunter.
	 * 
	 * @param cellType
	 */
	private void setProbabilityAllAroundCell(CellType cellType) {
		// WEST
		int x = this.hunterPosition.getX() - 1;
		int y = this.hunterPosition.getY();
		if (x > 0)
			setProbabilityOfCell(cellType, x, y);

		// NORTH
		x = this.hunterPosition.getX();
		y = this.hunterPosition.getY() - 1;
		if (y > 0)
			setProbabilityOfCell(cellType, x, y);

		// EAST
		x = this.hunterPosition.getX() + 1;
		y = this.hunterPosition.getY();
		setProbabilityOfCell(cellType, x, y);

		// SOUTH
		x = this.hunterPosition.getX();
		y = this.hunterPosition.getY() + 1;
		setProbabilityOfCell(cellType, x, y);

		/**
		 * Nach alle Aenderung hier wird die UnkwonCells sortiert damit die am wenigsten
		 * gef�hrliche "UNKWON" Cell am Anfang der Liste stehen.
		 */
		CellInfo.sortUnkwonCells();
	}

	/**
	 * Holt sich das erste Element(CellInfo) von der unKwonCells-Liste.
	 * Bestimmt die notwendigen Actions um diese Zelle zu erreichen 
	 * und speichert die in der bufferActions-Liste.
	 */
	public void exploreWorld() {

		try {
			CellInfo targetCell = CellInfo.getUnknownCells().remove();
			if (targetCell.getProbabilityPit() >= 100) {
				System.out.println("Ende :: sichere Welt komplett entdeckt!");
				this.worldCompletelyDiscovered = true;
				if (this.wumpusCertainReachable && this.wumpiKilled == 0) {
					this.searchWumpus();
				} else {
					if (!gameOver) {
						this.quitGame();
					}
					this.gameOver = true;
				}
				return;
			}
			Point targetPosition = targetCell.getPosition();
			this.get(targetPosition.getX(), targetPosition.getY()).setType(CellType.TARGET);
			Suche suche = new Breitensuche(this, targetPosition);
			this.bufferActions = suche.start();
		} catch (NoSuchElementException e) {
			System.out.println("Ende :: Welt komplett entdeckt!");
			this.worldCompletelyDiscovered = true;
			if (this.wumpusCertainReachable && this.wumpiKilled == 0) {
				this.searchWumpus();
			} else {
				if (!gameOver) {
					this.quitGame();
				}
				this.gameOver = true;
			}
		}
	}

	/**
	 * Setzt eine entdeckte Wand-Zelle in der Welt.
	 * 
	 * @param hunterPosition  :: aktuelle hunterPosition.
	 * @param hunterDirection :: aktuelle hunterDirection.
	 */
	private void setWallInToView(Point hunterPosition, Direction hunterDirection) {
		Point virtualHunterPosition = getVirtualPositionHunter(hunterPosition, hunterDirection);
		this.set(virtualHunterPosition.getX(), virtualHunterPosition.getY(), new CellInfo(CellType.WALL));
	}

	private Point getVirtualPositionHunter(Point hunterPosition, Direction hunterDirection) {
		Point virtualHunterPosition = new Point(hunterPosition.getX(), hunterPosition.getY());

		switch (hunterDirection) {
		case NORTH:
			virtualHunterPosition.setY(virtualHunterPosition.getY() - 1);
			break;
		case EAST:
			virtualHunterPosition.setX(virtualHunterPosition.getX() + 1);
			break;
		case SOUTH:
			virtualHunterPosition.setY(virtualHunterPosition.getY() + 1);
			break;
		case WEST:
			virtualHunterPosition.setX(virtualHunterPosition.getX() - 1);
			break;
		default:
			throw new IllegalArgumentException("Unzul�ssige HunterAction");
		}
		return virtualHunterPosition;
	}

	public ArrayList<ArrayList<CellInfo>> getView() {
		return this.view;
	}

	public Point getHunterPosition() {
		return this.hunterPosition;
	}

	/**
	 * Hunter geht wieder zur Position (1, 1), dann das Spiel beenden
	 */
	private void quitGame() {
		this.bufferActions.clear();
		Point targetPosition = new Point(1, 1);
		this.get(targetPosition.getX(), targetPosition.getY()).setType(CellType.TARGET);
		Suche suche = new Breitensuche(this, targetPosition);
		this.bufferActions = suche.start();
		this.bufferActions.add(HunterAction.QUIT_GAME);
	}

	private Point getRandomPointFromView() {
		int Max = this.view.size();
		int Min = 1;

		int y = (int) ((Math.random() * (Max - Min)) + Min);
		Max = this.view.get(y).size();
		int x = (int) ((Math.random() * (Max - Min)) + Min);

		CellInfo cellInfo = this.get(x, y);
		Point point = new Point(x, y);
		if (cellInfo == null || cellInfo.getType() == CellType.WALL || cellInfo.getType() == CellType.UNKWON
				|| point.equals(this.hunterPosition)) {
			return getRandomPointFromView();
		}
		return point;
	}

	/**
	 * Wird aufgerufen falls man sicher gestellt hat, dass der Wumpus in der erreichbare Welt ist.
	 * und Nachdem der Hunter die komplette Welt entdeckt hat, ohne den Wumpus get�tet zu haben.
	 * 
	 *  Suche den Wumpus, indem z�fallige Zelle in der erreichbare sichere Welt als Ziel gesetzt werden,
	 *  und die AKtions um diese Zelle zu erreichen bestimmen werden. 
	 */
	private void searchWumpus() {
		Point targetPosition = this.getRandomPointFromView();
		this.get(targetPosition.getX(), targetPosition.getY()).setType(CellType.TARGET);
		Suche suche = new Breitensuche(this, targetPosition);
		this.bufferActions = suche.start();
	}
}
