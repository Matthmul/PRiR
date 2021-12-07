#ifndef LIFEPARALLELIMPLEMENTATION_H_
#define LIFEPARALLELIMPLEMENTATION_H_

#include "Life.h"
#include <stdlib.h>
#include <time.h>
#include <omp.h>
#include <iostream>

class LifeParallelImplementation : public Life
{
public:
	LifeParallelImplementation();
	double avgNumerOfLiveNeighboursOfLiveCell();
	int maxSumOfNeighboursAge();
	int *numberOfNeighboursStatistics();
	void oneStep();

private:
static const int MAX_THREAD_NUM = 6;

drand48_data buffer [MAX_THREAD_NUM];
};

#endif /* LIFEPARALLELIMPLEMENTATION_H_ */
