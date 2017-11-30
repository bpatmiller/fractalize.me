## Fractalize.me

### What is this?
Given an input image, this program breaks the image up into smaller components and then calculates and renders a julia set that approximates each small component.

### How do I use it?
Use `make` to build the source files.

Use `make demo` to run an example.

`Fractalize --scale scale --ratio ratio --maxiters maxiters --lejas lejas --colors colors --cutoff cutoff -i fname`

By default, the input image is set to `in/in.png`.  Use `-i` to select an input image from `in/`.


### To do:
redo methods for better style/memory efficiency

rewrite sobel to handle pixel data directly/pass info as pixel data instead of bufferedimage

better realignment method

### Credits
[Fekete polynomials and shapes of Julia sets](https://arxiv.org/abs/1607.05055)

[Princeton's Complex.java type](https://introcs.cs.princeton.edu/java/32class/Complex.java.html)

[someone's sobel operator implementation](https://en.wikipedia.org/wiki/Sobel_operator)

[someone's k-means implementation](https://en.wikipedia.org/wiki/K-means_clustering)