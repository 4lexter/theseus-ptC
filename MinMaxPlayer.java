/* CHANGES THAT MUST BE DONE
 * CHANGE EXTENDS HEURISTICPLAYER TO EXTENDS PLAYER (SOME FUNCTION MUST BE KEPT THOUGH)
 * COMPLETE GETNEXTMOVE
 */

import java.util.ArrayList;

public class MinMaxPlayer extends HeuristicPlayer {

	public MinMaxPlayer() {
		super();
	}
	
	public MinMaxPlayer(int playerId, String name, Board board, int score, int x, int y, ArrayList<Integer[]> path, int ability, boolean wallAbility) {
		super(playerId, name, board, score, x, y, path, ability, wallAbility);
	}
	
	public MinMaxPlayer(MinMaxPlayer player) {
		super(player.getPlayerId(), player.getName(), player.getBoard(), player.getScore(), player.getX(), player.getY(), player.getPath(), player.getAbility(), player.getWallAbility());
	}
	
	
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
			players[0] = new MinMaxPlayer(playerId, name, tempBoard, score, x, y, path, ability, wallAbility);
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
}