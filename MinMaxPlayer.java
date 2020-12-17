/* CHANGES THAT MUST BE DONE
 * COMPLETE GETNEXTMOVE
 */

import java.util.ArrayList;

public class MinMaxPlayer extends Player {
    private ArrayList<Integer[]> path;      //player moves' description [int die, int pickedSupply, int blocksToSupply, int blocksToOpponent, tileId]
    private int ability;
    private boolean wallAbility;
    private double revisitPenalty = 0.01;
    private Board playerMap;
    
	public MinMaxPlayer() {
		super();
	}
	
	public MinMaxPlayer(int playerId, String name, Board board, int score, int x, int y, ArrayList<Integer[]> path, int ability, boolean wallAbility) {
        super(playerId, name, board, score, x, y);
        this.path = path;
        this.ability = ability;
        this.wallAbility = wallAbility;
        playerMap = new Board( board.getN(), board.getS(), board.getW());

        int N = playerMap.getN();
        for(int i = 0; i <= N * N - 1; i++){
            playerMap.getTiles()[i] = new Tile(i, i/N, i%N, false, false, false, false);
			if(i / N == 0) {
				playerMap.getTiles()[i].setDown(true);
			}
			else if(i / N == N - 1) {
				playerMap.getTiles()[i].setUp(true);
			}
			
			if(i % N == 0) {
				playerMap.getTiles()[i].setLeft(true);
			}
			else if(i % N == N - 1) {
				playerMap.getTiles()[i].setRight(true);
			}
		}
        for(int i = 0; i<playerMap.getS(); ++i)
        	playerMap.getSupplies()[i] = new Supply();
	}
	
	public MinMaxPlayer(MinMaxPlayer player) {
		super(player.getPlayerId(), player.getName(), player.getBoard(), player.getScore(), player.getX(), player.getY());
		path = player.getPath();
		ability = player.getAbility();
		wallAbility = player.getWallAbility();
	}
	
    public void setAbility(int ability){
        this.ability = ability;
    }

    public int getAbility(){
        return ability;
    }

    public void setWallAbility(boolean wallAbility){
        this.wallAbility = wallAbility;
    }

    public boolean getWallAbility(){
        return wallAbility;
    }
    
    public ArrayList<Integer[]> getPath(){
    	return path;
    }
    
    public void erasePath(){
        path.clear();
    }
    
    public Board getPlayerMap(){
        return playerMap;
    }
    
    
    //MinMax part
	public int chooseMinMaxMove(Node root) {
		int id = -1;
		double eval = Double.NEGATIVE_INFINITY;
		for(int i = 0 ;  i < 4 ; i++) {
			double childEval = Double.NEGATIVE_INFINITY;
			for(int j = 0 ; j < 4 ; j++) {
				if(root.getChildren().get(i).getChildren().get(j).getNodeEvaluation()>childEval) {
					childEval = root.getChildren().get(i).getChildren().get(j).getNodeEvaluation();
				}
			}
			if(root.getChildren().get(i).getNodeEvaluation() - childEval > eval) {
				eval = root.getChildren().get(i).getNodeEvaluation() - childEval;
				id = i;
			}
		}
		return root.getChildren().get(id).getNodeMove()[2];
	}
	
	public int[] getNextMove(int currentPos, int opponentCurrentPos) {
		Node root = new Node();
		root.setNodeBoard(board);
		createMySubtree(currentPos, opponentCurrentPos, root, 1);
		int result = chooseMinMaxMove(root);
		//movement
		move(result);
		
	}
	
	public void createMySubtree(int currentPos, int opponentCurrentPos, Node root, int depth) {
		for(int i = 0 ; i < 4; i++) {
			//player movement simulation
			Board tempBoard = new Board(root.getNodeBoard());
			MinMaxPlayer[] players = new MinMaxPlayer[2];
			players[0] = new MinMaxPlayer(getPlayerId(), getName(), tempBoard, getScore(), getX(), getY(), getPath(), getAbility(), getWallAbility());
			players[1] = new MinMaxPlayer();
			players[1].setBoard(tempBoard);
			players[1].setX(opponentCurrentPos/tempBoard.getN());
			players[1].setY(opponentCurrentPos%tempBoard.getN());
			players[0].move(2*i+1);
			
			//node
			ArrayList<Node> children = new ArrayList<Node>(0);
			int[] coords = new int[] {players[0].getX(), players[0].getY(), 2*i+1}; 
			Node child = new Node(root, children, depth, coords, tempBoard, players[0].evaluate(coords[0]*tempBoard.getN()+coords[1], opponentCurrentPos , 2*i+1));
			root.getChildren().add(child);
			
			//opponent child nodes
			createOpponentSubtree(coords[0]*tempBoard.getN()+coords[1], opponentCurrentPos, child, depth+1, child.getNodeEvaluation());
		}
	}
	
	public void createOpponentSubtree(int currentPos, int opponentCurrentPos, Node parent, int depth, double parentEval) {
		for(int i = 0 ; i < 4 ; i++) {
			//opponent movement simulation
			Board tempBoard = new Board(parent.getNodeBoard());
			MinMaxPlayer[] players = new MinMaxPlayer[2];
			players[0] = new MinMaxPlayer(getPlayerId(), getName(), tempBoard, getScore(), currentPos/tempBoard.getN(), currentPos%tempBoard.getN(), null, getAbility(), getWallAbility());
			players[1] = new MinMaxPlayer();
			players[1].setBoard(tempBoard);
			players[1].setX(opponentCurrentPos/parent.getNodeBoard().getN());
			players[1].setY(opponentCurrentPos%parent.getNodeBoard().getN());
			players[1].move(2*i+1);
			
			//node
			ArrayList<Node> children = null;
			int[] coords = new int[] {players[1].getX(), players[1].getY(), 2*i+1};
			Node child = new Node(parent, children, depth, coords, tempBoard, players[1].evaluate(coords[0]*tempBoard.getN()+coords[1], opponentCurrentPos, 2*i+1));
			parent.getChildren().add(child);
		}
	}
	
	
    //heuristicPart
    private int[] seeAround(int currentPos, int opponentPos, int die){
        int blocksToOpponent = Integer.MAX_VALUE, blocksToSupply = Integer.MAX_VALUE, blocksToWall = Integer.MAX_VALUE;
        int nId = currentPos;
        for(int i = 0; i<ability; ++i){
            //Interfering wall
            if(board.getTiles()[nId].getWallInDirection(die)) {
                break;
            }
            board.getTiles()[nId].setHaveInfo(true);
            nId = board.getTiles()[nId].neighborTileId(die, board.getN());
            //Opponent
            if(opponentPos == nId){
                blocksToOpponent = i + 1;
            }

            //Supply
            for(int j = 0;  j<board.getS(); ++j) {
            	if(nId == board.getSupplies()[j].getSupplyTileId() && board.getSupplies()[j].isObtainable()) {
            		playerMap.getSupplies()[j].setSupplyId(j+1);
            		playerMap.getSupplies()[j].setX(board.getTiles()[nId].getX());
            		playerMap.getSupplies()[j].setY(board.getTiles()[nId].getY());
            		playerMap.getSupplies()[j].setSupplyTileId(nId);
            		playerMap.getSupplies()[j].setObtainable(true);
		            if(blocksToSupply == Integer.MAX_VALUE){
		                    blocksToSupply = i + 1;
		            }
            		}
            	}
            //Enough data collected
            if(blocksToOpponent != Integer.MAX_VALUE && blocksToSupply != Integer.MAX_VALUE)
                break;
        }
        //BlocksToWall
        //Interfering wall
        if(wallAbility){
            if(board.getTiles()[currentPos].getWallInDirection(die)) {
                blocksToWall = 0;
                playerMap.getTiles()[currentPos].setWallInDirection(die, true);
                nId = playerMap.getTiles()[currentPos].neighborTileId(die, playerMap.getN());
                if(0<nId && nId<playerMap.getN()*playerMap.getN()-2) {
                    int oppositeDie = (die == 1)?(5):(die == 5)?(1):(die == 3)?(7):(3);
                    playerMap.getTiles()[nId].setWallInDirection(oppositeDie, true);
                }
            }
        }

        return new int[] {blocksToSupply, blocksToOpponent, blocksToWall};
    }
    
    public double evaluate(int currentPos, int opponentPos, int die){
        int[] observation = seeAround(currentPos, opponentPos, die);
        int blocksToClosestSupply = Integer.MAX_VALUE;
        int blocksToOpponent = observation[1];
        int blocksToWall = observation[2];
        double penalty = 0;

        //Approach supplies
        if(!board.getTiles()[currentPos].getWallInDirection(die)){
            int neighborTileId = board.getTiles()[currentPos].neighborTileId(die, board.getN());
            Tile neighbor = board.getTiles()[neighborTileId];
            for(int i = 0; i<playerMap.getS(); ++i){
            	if(playerMap.getSupplies()[i].getSupplyTileId() == 0)
            		continue;
            	Tile supplyTile = playerMap.getTiles()[playerMap.getSupplies()[i].getSupplyTileId()];
            	if(board.getSupplies()[i].isObtainable()) {
            		if(blocksToClosestSupply>neighbor.distance(supplyTile) + 1)
            			blocksToClosestSupply = neighbor.distance(supplyTile) + 1;
            	}
            }
        }
        
        if(name.equals("Theseus")){
            //Avoid revisiting a tile
            for(int i = 0; i<path.size(); ++i){
                if(path.get(i)[4] == board.getTiles()[currentPos].neighborTileId(die, board.getN())){
                    penalty+=revisitPenalty;
                }
            }
            ////Special case MS
            if(blocksToOpponent == 1 && blocksToClosestSupply == 1){
                if(score == board.getS() - 1){
                    return Double.POSITIVE_INFINITY;
                }
                return Double.NEGATIVE_INFINITY;
            }
            //Minotaur is one block away
            if(blocksToOpponent == 1 && blocksToWall == 0)
                    return Double.NEGATIVE_INFINITY;
            //Case losing turn
            if(blocksToWall == 0)
                    return -10;
            //Minotaur is two blocks away
            if(blocksToOpponent == 2){
                if(score == board.getS() - 1 && blocksToClosestSupply == 1)
                    return Double.POSITIVE_INFINITY;
                else
                    return Double.NEGATIVE_INFINITY;
            }
            //General case
            return 0.5/(blocksToClosestSupply - 1) - 1.0/(blocksToOpponent - 1)-penalty;
        }
        //This is only for Minotaur

        //avoid back and forth movements
        if(!path.isEmpty()){
            if((path.get(path.size()-1)[0]%4 == die%4) && (path.get(path.size()-1)[0] != die)){
                penalty+=revisitPenalty;
            }
        }
        //Case losing turn
        if(blocksToWall == 0)
            return -10;
        //General case
        return 0.5/(blocksToClosestSupply) + 1.0/(blocksToOpponent - 1)-penalty;  //there's not -1 so bloscksToOpponent is more important
    }
    
    public int[] move(int die) {
		int[] details = new int[4];
        details[3] = -1;
		switch(die) {
		case 1://case UP
			System.out.println(name + " rolled UP.");		
			break;
		case 3://case RIGHT
			System.out.println(name + " rolled RIGHT.");
			break;
		case 5://Case DOWN
			System.out.println(name + " rolled DOWN.");
			break;
		case 7://Case LEFT
			System.out.println(name + " rolled LEFT.");
			break;
		}
		
		//Valid move check
		//Invalid
		if(board.getTiles()[board.getN()*x+y].getWallInDirection(die)) {
			System.out.println(name + " cannot move that way.");
			details[0] = board.getN()*x+y;
			details[1] = board.getTiles()[board.getN()*x+y].getX();
			details[2] = board.getTiles()[board.getN()*x+y].getY();
		}
		//Valid
		else {
			details[0] = board.getTiles()[board.getN()*x+y].neighborTileId(die, board.getN());
			details[1] = board.getTiles()[details[0]].getX();
			details[2] = board.getTiles()[details[0]].getY();
			setX(details[1]);
			setY(details[2]);
		}
        
		//Theseus collected supply check
		if(name.equals("Theseus")) {
			for(int i = 0 ; i < board.getS() ; i++) {
				if((details[0] == board.getSupplies()[i].getSupplyTileId())&&(board.getSupplies()[i].isObtainable())) {
                    System.out.println(name + " picked up supply " + board.getSupplies()[i].getSupplyId() + ".");
					details[3] = i;
					board.getSupplies()[i].setObtainable(false);
					break;
				}
			}
		}
		return details;
	}
}