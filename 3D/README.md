# Current enclosure

Design: **Ing. Alex M. Rivera**. Updated **12 October 2026**.

![Body, cavity and lid](enclosure-preview.png)

| File | Part | Mesh bounding box, interpreted in mm | Triangles |
| --- | --- | --- | --- |
| [cuerpo.stl](cuerpo.stl) | Body | 52 × 51.99 × 67.5 | 9,498 |
| [tapa.stl](tapa.stl) | Lid | 52 × 52 × 20 | 7,056 |

These are the current user-supplied enclosure meshes. They replace the old
`CASE.stl`, `LID.stl`, `NECK.stl` and `SENSOR-HOLDER.stl` exports. Those files
remain recoverable in Git history; the animations in `../img/3d/` depict the
earlier geometry.

STL does not declare units. The bounding boxes above assume millimetres and
are overall mesh extents, not verified internal cavity dimensions. The body
and lid have their supplied coordinates; do not add their heights to infer
assembled height. Confirm their seating and insertion depth in mechanical CAD
or a physical assembly.

Only the 80-byte binary STL headers were updated to include the design
attribution. Triangle data, normals, coordinates and per-triangle attributes
were preserved. The preview renders the supplied meshes individually; it
does not show a verified assembled enclosure or installed electronics.

## Fit checks with the electronics

Use the [upper revision 0.10 alignment](../hardware/README.md):

- Preserve the base PCB's cable relief; it is filled on the upper PCB only.
- Verify the nominal 44 mm internal cylinder against the mesh and printed part.
- Check the 12.5 × 12.5 mm cuvette exterior, optical axes and opaque barriers.
- Check the upright ADS1115, connector mating heights and trimmed solder tails.
  The PCB-stack budget is provisionally 36–38 mm, before lid and cable space.
- Fit the compact plug at XIAO's left-facing USB socket and support both cables.
- Verify the actual cable diameter, gland, lid seal and light exclusion.

The models have not been used to verify sealing, print tolerances or complete
PCB-to-enclosure clearance. Measure and test the physical assembly before
fabrication is considered complete.
