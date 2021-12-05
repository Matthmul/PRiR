/*
 * Main.cpp
 *
 *  Created on: 4 lis 2021
 *      Author: oramus
 */

#include "Life.h"
#include "Rules.h"
#include "RandomRules.h"
#include "LifeSequentialImplementation.h"
#include "LifeParallelImplementation.h"
#include <iostream>
#include <unistd.h>

using namespace std;

void showTable(int **cells, int size)
{
	for (int row = 0; row < size; row++)
	{
		for (int col = 0; col < size; col++)
			cout << (cells[row][col] ? "X " : ". ");
		cout << endl;
	}
}

void showVector(int *tbl, int size)
{
	for (int i = 0; i < size; i++)
		cout << tbl[i] << ", ";
	cout << endl;
}

void glider(Life *l, int col, int row)
{
	l->setLiveCell(col, row);
	l->setLiveCell(col + 1, row);
	l->setLiveCell(col + 2, row);
	l->setLiveCell(col, row + 1);
	l->setLiveCell(col + 1, row + 2);
}

int main(int argc, char **argv)
{

	const int SIZE = 800;

	// Life *l = new LifeSequentialImplementation();
	Life *l = new LifeParallelImplementation();
	Rules *r = new RandomRules();

	l->setRules(r);
	l->setSize(SIZE);

	glider(l, 5, 5);
	glider(l, 10, 5);
	glider(l, 10, 10);
	glider(l, 5, 10);
	// glider(l, 1, 1);

	int *stat;
	// while (true) {
	for (int i = 0; i < 800; ++i)
	{
		l->oneStep();
		// showTable(l->getCurrentState(), SIZE);
		// cout << l->avgNumerOfLiveNeighboursOfLiveCell() << ", " << l->maxSumOfNeighboursAge() << endl;
		// l->avgNumerOfLiveNeighboursOfLiveCell();
		// l->maxSumOfNeighboursAge();
		// stat = l->numberOfNeighboursStatistics();
		// showVector(stat, 9);
		// delete[] stat;
		// usleep(250000);
	}

	// Life *ll = new LifeSequentialImplementation(); // dla porownania
	// Rules *rr = new RandomRules();

	// ll->setRules(rr);
	// ll->setSize(SIZE);
	// glider(ll, 1, 1);

	// int *statt;
	// for (int i = 0; i < 2; ++i)
	// {
	// 	ll->oneStep();
	// 	showTable(ll->getCurrentState(), SIZE);
	// 	cout << ll->avgNumerOfLiveNeighboursOfLiveCell() << ", " << ll->maxSumOfNeighboursAge() << endl;
	// 	statt = ll->numberOfNeighboursStatistics();
	// 	showVector(statt, 9);
	// 	delete[] statt;
	// 	usleep(250000);
	// }

	return 0;
}
