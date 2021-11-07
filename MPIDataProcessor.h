/*
 * MPIDataProcessor.h
 */

#ifndef MPIDATAPROCESSOR_H_
#define MPIDATAPROCESSOR_H_

#include "DataProcessor.h"
#include "Alloc.h"
#include <iostream>
#include <math.h>
#include "mpi.h" 

class MPIDataProcessor : public DataProcessor {
private:
	static const int TAB_ARRAY_SIZE = 2;
	static const int MAIN_PROC_ID = 0;
	static const int ROW = 0;
	static const int COL = 1;

	int tabSize[TAB_ARRAY_SIZE];
	int rank;
    int procNum;
    MPI_Status status;
	
	void createDataPortion( int row, int col, double *buffer );
	void calcTabSize(int maxProcNum, int procNum) ;
	double **tableAlloc( int *tabSize );
	void syncData();
	void sendTopMargin( int dest );
	void sendDownMargin( int dest );
	void recvTopMargin( int dest );
	void recvDownMargin( int dest );
	inline int getMargin()
	{
		return (margin * 2);
	}

protected:
	void singleExecution();
	void collectData();
	void shareData();
public:
	MPIDataProcessor();
	double** getResult() {
		return data;
	}
};

#endif /* MPIDATAPROCESSOR_H_ */
