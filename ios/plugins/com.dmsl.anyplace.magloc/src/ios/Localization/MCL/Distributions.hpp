/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Artem Nikitin
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */


#ifndef Distributions_cpp
#define Distributions_cpp

#include "stdafx.hpp"

template <typename T> T normal_pdf(T x, T m, T s);
template <typename T> T uniform_pdf(T x, T m, T s);
template <typename T> T triangle_sym_pdf(T x, T m, T s);

/**
 * Distribution - base for distribution classes.
 * pdf() - returns probability for certain point and certain mean m, dispersion s
 * next() - returns next random sample from the distribution with certain mean m, dispersion s
 */

class Distribution {
protected:
    std::default_random_engine _random_generator;
public:
    Distribution() : Distribution(std::default_random_engine((unsigned int)time(0))) { }
    Distribution(std::default_random_engine random_generator) {
        _random_generator = random_generator;
    }
    virtual double pdf(double x, double m, double s) = 0;
    virtual double next(double m, double s) = 0;
    
    virtual ~Distribution() {};
};

/**
 * NormalDistribution - normal (Gaussian) distribution class
 */
class NormalDistribution : public Distribution {
private:
    std::normal_distribution<double> distribution;
public:
    
    NormalDistribution() : Distribution() {}
    NormalDistribution(std::default_random_engine random_generator) : Distribution(random_generator) {};
    ~NormalDistribution(){};
    
    double pdf(double x, double m, double s);
    double next(double m, double s);
};



#endif /* Distributions_cpp */
