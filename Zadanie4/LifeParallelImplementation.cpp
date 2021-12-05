/*
 * LifeParallelImplementation.cpp
 *
 *  Created on: 5 lis 2021
 *      Author: oramus
 */

#include "LifeParallelImplementation.h"
#include <stdlib.h>

LifeParallelImplementation::LifeParallelImplementation() {
}

// do zrownoleglenia
void LifeParallelImplementation::oneStep() {
	int neighbours;
	for (int row = 0; row < size; row++)
		for (int col = 0; col < size; col++) {
			neighbours = liveNeighbours(row, col);

			if (cells[row][col]) {
				// komorka zyje
				if (rules->cellDies(neighbours, age[row][col], drand48())) {
					// smierc komorki
					nextGeneration[row][col] = 0;
					age[row][col] = 0;
				} else {
					// komorka zyje nadal, jej wiek rosnie
					nextGeneration[row][col] = 1;
					age[row][col]++;
				}
			} else {
				// komorka nie zyje
				if (rules->cellBecomesLive(neighbours,
						neighboursAgeSum(row, col), drand48())) {
					// narodziny
					nextGeneration[row][col] = 1;
					age[row][col] = 0;
				} else {
					nextGeneration[row][col] = 0;
				}
			}
		}
	int **tmp = cells;
	cells = nextGeneration;
	nextGeneration = tmp;
}

// do zrownoleglenia
double LifeParallelImplementation::avgNumerOfLiveNeighboursOfLiveCell() {
	int sumOfNeighbours = 0;
	int counter = 0;
	for (int row = 1; row < size - 1; row++)
		for (int col = 1; col < size - 1; col++) {
			if (cells[row][col]) {
				sumOfNeighbours += liveNeighbours(row, col);
				counter++;
			}
		}
	if (counter == 0)
		return 0.0;
	return (double) sumOfNeighbours / (double) counter;
}

// do zrownoleglenia
int LifeParallelImplementation::maxSumOfNeighboursAge() {
	int sumOfNeighboursAge;
	int max = 0;

	for (int row = 1; row < size - 1; row++)
		for (int col = 1; col < size - 1; col++) {
			sumOfNeighboursAge = neighboursAgeSum(row, col);

			if (max < sumOfNeighboursAge) {
				max = sumOfNeighboursAge;
			}
		}

	return max;
}

// do zrownoleglenia
int* LifeParallelImplementation::numberOfNeighboursStatistics() {
	int *tbl = new int[9]; // od 0 do 8 sąsiadów włącznie
	for (int i = 0; i < 9; i++)
		tbl[i] = 0;

	for (int row = 1; row < size - 1; row++)
		for (int col = 1; col < size - 1; col++) {
			tbl[liveNeighbours(row, col)]++;
		}

	return tbl;
}

