# Apathy-Based Rendering (ABR)

It’s a rendering pipeline that resents the geometry it processes. 

ABR is an OpenGL ES 3.0 experiment for Android that collapses the pretension of three-dimensional space into a bitter, two-dimensional stencil. It simulates the physical trauma of aerosol paint on an indifferent surface, applying brutalist culling, monochromatic void-mapping, and gravity-driven drip vectors.

## The Pipeline

1.  **Vertex:** We take perfectly good vertices, strip them of their depth, and slam them against a flat projection matrix.
2.  **Fragment:** A noise texture introduces chaos (the drip), dragging the UV coordinates downward. Time makes it bleed.
3.  **The Cut:** A stencil mask acts as a binary arbiter of reality. If the fragment isn't part of the joke, it gets discarded.
4.  **The Void:** All surviving fragments are stripped of color. Hope is clamped to an edge.

## Implementation

Manifest the void in your layout:

~~~xml
<com.hereliesaz.abr.ApathySurfaceView
    android:id="@+id/apathy_surface"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
~~~

Feed it the required trauma in your Activity:

~~~kotlin
apathySurface.manifest(
    baseResId = R.drawable.the_wall,
    noiseResId = R.drawable.the_chaos,
    stencilResId = R.drawable.the_punchline
)
~~~

## License
Do whatever you want. It's just going to rot anyway.
