## Fractalize.me

### What is this?
Given an input image, this program breaks the image up into smaller components and then calculates and renders a julia set that approximates each small component.

Based off of the information found in [Shapes of Polynomial Julia Sets](https://arxiv.org/find/math/1/au:+Lindsey_K/0/1/0/all/0/1) and [Fekete polynomials and shapes of Julia sets](https://arxiv.org/abs/1607.05055).

## [Examples](https://drive.google.com/open?id=1uUPe0SEhWO_JWV8Nn8tcMeepV_d_Wxcp)

### Setup
Use `make` to build the source files.

Use `make demo` to run an example.

### Usage
`java Fractalize --scale scale --ratio ratio --maxiters maxiters --lejas lejas --colors colors --cutoff cutoff -i fname`

The default settings are `--scale 1.0 --ratio 1.0 --maxiters 16 --lejas 80 --colors 4 --cutoff 50 -i in.png`. Simply omit one to use its default value.

### To do:
redo methods for better style/memory efficiency

rewrite sobel to handle pixel data directly/pass info as pixel data instead of bufferedimage

better realignment method

### Credits
[Fekete polynomials and shapes of Julia sets](https://arxiv.org/abs/1607.05055)

[Princeton's Complex.java type](https://introcs.cs.princeton.edu/java/32class/Complex.java.html)

[someone's sobel operator implementation](https://en.wikipedia.org/wiki/Sobel_operator)

[someone's k-means implementation](https://en.wikipedia.org/wiki/K-means_clustering)
