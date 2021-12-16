#include "LifeParallelImplementation.h"

using namespace std;

LifeParallelImplementation::LifeParallelImplementation()
{
	int id;
#pragma omp parallel private(id) shared(buffer)
	{
		id = omp_get_thread_num();
		// srand48_r(0, &buffer[id]);
		srand48_r(time(0)*(1+id), &buffer[id]);
	}
}

// do zrownoleglenia
void LifeParallelImplementation::oneStep()
{
	int neighbours;
	int id;
	double result;
	int row, col;
#pragma omp parallel private(row, neighbours, id, result, buffer) shared(age, cells, nextGeneration)
	{
		id = omp_get_thread_num();
#pragma omp for schedule(auto) collapse(2)
		for (row = 0; row < size; ++row)
			for (col = 0; col < size; ++col)
			{
				neighbours = liveNeighbours(row, col);

				if (cells[row][col])
				{
					drand48_r(&buffer[id], &result);
					// komorka zyje
					if (rules->cellDies(neighbours, age[row][col], result))
					{
						// smierc komorki
						nextGeneration[row][col] = 0;
						age[row][col] = 0;
					}
					else
					{
						// komorka zyje nadal, jej wiek rosnie
						nextGeneration[row][col] = 1;
						++age[row][col];
					}
				}
				else
				{
					drand48_r(&buffer[id], &result);
					// komorka nie zyje
					if (rules->cellBecomesLive(neighbours,
											   neighboursAgeSum(row, col), result))
					{
						// narodziny
						nextGeneration[row][col] = 1;
						age[row][col] = 0;
					}
					else
					{
						nextGeneration[row][col] = 0;
					}
				}
			}
	}
	int **tmp = cells;
	cells = nextGeneration;
	nextGeneration = tmp;
}

// do zrownoleglenia
double LifeParallelImplementation::avgNumerOfLiveNeighboursOfLiveCell()
{
	int sumOfNeighbours = 0;
	int counter = 0;
	int row, col;
#pragma omp parallel for schedule(auto) collapse(2) private(row, col) shared(cells, sumOfNeighbours) reduction(+ \
																											   : counter)
	for (row = 1; row < size - 1; ++row)
		for (col = 1; col < size - 1; ++col)
		{
			if (cells[row][col])
			{
				sumOfNeighbours += liveNeighbours(row, col);
				counter++;
			}
		}

	if (counter == 0)
		return 0.0;
	return (double)sumOfNeighbours / (double)counter;
}

// do zrownoleglenia
int LifeParallelImplementation::maxSumOfNeighboursAge()
{
	int sumOfNeighboursAge;
	int max = 0;
	int row, col;
#pragma omp parallel for schedule(auto) collapse(2) private(sumOfNeighboursAge, row, col) shared(max)
	for (row = 1; row < size - 1; ++row)
		for (col = 1; col < size - 1; ++col)
		{
			sumOfNeighboursAge = neighboursAgeSum(row, col);

			if (max < sumOfNeighboursAge)
			{
				max = sumOfNeighboursAge;
			}
		}
	return max;
}

// do zrownoleglenia
int *LifeParallelImplementation::numberOfNeighboursStatistics()
{
	int i, row, col, result;
	int *tbl = new int[9]; // od 0 do 8 sąsiadów włącznie

	for (i = 0; i < 9; i++)
		tbl[i] = 0;

#pragma omp parallel for collapse(2) schedule(static) private(row, col, result) reduction(+ \
																						  : tbl[:10])
	for (row = 1; row < size - 1; ++row)
		for (col = 1; col < size - 1; ++col)
		{
			result = liveNeighbours(row, col);
			tbl[result]++;
		}

	return tbl;
}
