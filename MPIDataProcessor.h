/*
 * MPIDataProcessor.h
 */

#ifndef MPIDATAPROCESSOR_H_
#define MPIDATAPROCESSOR_H_

#include "DataProcessor.h"

class MPIDataProcessor : public DataProcessor {
private:
	static const int TAB_ARRAY_SIZE = 2;
	static const int MAIN_PROC_ID = 0;
	static const int ROW = 0;
	static const int COL = 1;

	int tabSize[TAB_ARRAY_SIZE];

	void createDataPortion( int row, int col, double *buffer );
	void calcDataToDivine(int procNum);
	double **tableAlloc( int *tabSize );

protected:
	void singleExecution();
	void collectData();
	void shareData();
public:
	double** getResult() {
		return data;
	}
};

#endif /* MPIDATAPROCESSOR_H_ */
