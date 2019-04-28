//
// Created by https://github.com/CardinalPanda
//
//Licensed under the GNU General Public License Version 3 (GNU GPL v3),
//    available at: https://www.gnu.org/licenses/gpl-3.0.txt

#ifndef DANDERE2X_DIFFERENCEBLOCKS_H
#define DANDERE2X_DIFFERENCEBLOCKS_H

#include "../../Dandere2xUtils/VectorDisplacement.h"
#include <vector>

class DifferenceBlocks {

public:
    int size;
    int xMax;
    int yMax;
    int xCount;
    int yCount;
    int xDimension;
    int yDimension;
    std::vector<VectorDisplacement> list = std::vector<VectorDisplacement>();


    DifferenceBlocks(int xDimension, int yDimension, int size) {
        this->xDimension = xDimension;
        this->yDimension = yDimension;
        this->xMax = xDimension / size;
        this->yMax = yDimension / size;
        this->xCount = 0;
        this->yCount = 0;
        this->size = size;
    }

    void add(int x, int y) {
        size++;
        if (xCount + 1 < xMax) {
            xCount++;
            list.push_back(VectorDisplacement(x, y, xCount, yCount));
            //xCount++;
        } else {
            yCount++;
            xCount = 0;
            list.push_back(VectorDisplacement(x, y, xCount, yCount));

        }
    }
};

#endif //DANDERE2X_DIFFERENCEBLOCKS_H
