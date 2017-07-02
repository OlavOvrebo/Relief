# Relief

Relief is a Java program to generate a .stl-file from an input image, for example to 3D-print a relief of said image (hence the name). 

The generated file has a single layer only, and must be manually expanded with walls and a bottom to attain volume. This makes it easier to adjust the scaling of the generated relief, as these as a standard may be considered too flat. Depth/height of the relief is dependent on the average color value of various points around the input image.

Generated objects are of high resolution (2 triangular facets per input pixel), and may thus be unfit for use in time-critical software (though it can serve as a quick way to generate, for example, large expanses for 3D-games from quick sketches). For the same reason, execution time can be lengthy depending on the size of the input image, as the program uses no hardware accelleration. There is no way to generate overhang using this software.

For an anecdote and example of use, I wrote this after noting the heightmap of EU4 as a .bmp including the ocean floor, and thinking it might make for a nice plaque if 3D-printed. (After some downsizing the image, that is, as the generated 20 000 000 + facet stl-object proved too much for the editing software I use to handle.)
