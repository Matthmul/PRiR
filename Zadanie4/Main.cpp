/*
 * Main.cpp
 *
 *  Created on: 4 lis 2021
 *      Author: oramus
 */

#include"Life.h"
#include"Rules.h"
#include"RandomRules.h"
#include"LifeSequentialImplementation.h"
#include"LifeParallelImplementation.h"
#include<iostream>
#include <unistd.h>

using namespace std;

void showTable(int **cells, int size) {
	for (int row = 0; row < size; row++) {
		for (int col = 0; col < size; col++)
			cout << (cells[row][col] ? "X " : ". ");
		cout << endl;
	}
}

void showVector( int *tbl, int size ) {
	for ( int i = 0; i < size; i++ )
		cout << tbl[ i ] << ", ";
	cout << endl;
}

void glider(Life *l, int col, int row) {
	l->setLiveCell(col, row);
	l->setLiveCell(col + 1, row);
	l->setLiveCell(col + 2, row);
	l->setLiveCell(col, row + 1);
	l->setLiveCell(col + 1, row + 2);
}

int main(int argc, char **argv) {

	const int SIZE = 20;

//	Life *l = new LifeSequentialImplementation(); // dla porownania
	Life *l = new LifeParallelImplementation();
	Rules *r = new RandomRules();

	l->setRules(r);
	l->setSize(SIZE);

	glider(l, 5, 5);
	glider(l, 10, 5);
	glider(l, 10, 10);
	glider(l, 5, 10);

	int *stat;
	while (true) {
		l->oneStep();
		showTable(l->getCurrentState(), SIZE);
		cout << l->avgNumerOfLiveNeighboursOfLiveCell() << ", " << l->maxSumOfNeighboursAge() << endl;
		stat = l->numberOfNeighboursStatistics();
		showVector( stat, 9 );
		delete[] stat;
		usleep(250000);
	}

	return 0;
}

