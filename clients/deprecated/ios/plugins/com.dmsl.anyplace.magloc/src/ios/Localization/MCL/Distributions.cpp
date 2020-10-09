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

#include "Distributions.hpp"

/**
 * normal_pdf() - returns probability of certain x in for normal distribution with mean m, dispersion s
 */
template <typename T>
T normal_pdf(T x, T m, T s)
{
    static const T inv_sqrt_2pi = 0.3989422804014327;
    T a = (x - m) / s;
    
    return inv_sqrt_2pi / s * std::exp(-T(0.5) * a * a);
}

/**
 * uniform_pdf() - returns probability of certain x in for uniform distribution with mean m, dispersion s
 */

template <typename T>
T uniform_pdf(T x, T m, T s)
{
    T sqrt3s = sqrt(3*s);
    T a = m - sqrt3s;
    T b = m + sqrt3s;
    
    return (x < a || x > b) ? 0 : 1.0/(b-a) ;
}

/**
 * trianlge_sym_pdf - returns probability of certain x in for triangle symmetric distribution with mean m, dispersion s
 */

template <typename T>
T triangle_sym_pdf(T x, T m, T s)
{
    T sqrt6s = sqrt(6*s);
    T a = m - sqrt6s;
    T b = m;
    T c = m + sqrt6s;
    
    if (x < a || x > c)
        return 0;
    else if (x <= m)
        return 2*(x-a)/( sqrt6s*2*sqrt6s );
    else
        return 2*(b-x)/( -sqrt6s*2*sqrt6s );
    return (x < a || x > c) ? 0 : 1.0/(b-a) ;
}

double NormalDistribution::pdf(double x, double m, double s) {
    return normal_pdf(x, m, s);
}

double NormalDistribution::next(double m, double s) {
    return distribution(_random_generator)*s + m;
}