package org.fuwjax.minesweeper;

import java.util.ArrayList;
import java.util.List;

import org.fuwjax.game.Tile;

public class Cell implements GameCell {
	private Cover cover = Cover.PLAIN;
	private Content content = Content.EMPTY;
	private List<Cell> adjacent = new ArrayList<>();
	
	public void addAdjacent(Cell cell){
		this.adjacent.add(cell);
	}
	
	public void makeMine() {
		assert content != Content.MINE;
		content = Content.MINE;
		for(Cell cell: adjacent){
			cell.content = cell.content.adjacent();
		}
	}

	public Tile tile() {
		return cover.tile(content);
	}

	public int uncover() {
		return cover.uncover(this);
	}

	public int flag() {
		return cover.toggleFlag(this);
	}

	public int uncoverAdjacent() {
		assert content == Content.EMPTY;
		int revealed = 0;
		for(Cell cell: adjacent){
			revealed += cell.uncover();
		}
		return revealed;
	}

	public int reveal() {
		cover = Cover.NONE;
		return content.reveal(this);
	}

	public void setCover(Cover cover) {
		this.cover = cover;
	}
}
