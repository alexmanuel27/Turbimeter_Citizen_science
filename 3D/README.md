# Enclosure and Fusion exports

Design: **Ing. Alex M. Rivera**. Updated **8 November 2026**.

Alex supplied these exports for the instrument sent to Havana. The earlier
`cuerpo.stl` and `tapa.stl` were removed by the designer; the old
`enclosure-preview.png` illustrates the earlier design and is historical.

| File | Description | Mesh extents if interpreted in mm |
| --- | --- | --- |
| [body_tur.stl](fusion-export/body_tur.stl) | Body | 61.980 × 61.990 × 93.000 |
| [Tapa_tur.stl](fusion-export/Tapa_tur.stl) | Lid | 61.986 × 61.993 × 22.500 |
| [Tur.obj](fusion-export/Tur.obj) | Wavefront model with `Tapa` and `Cuerpo` groups | Coordinates differ numerically from the STLs by about a factor of 10; check units when importing |
| [Tur.mtl](fusion-export/Tur.mtl) | Material file referenced by `Tur.obj` | Keep beside the OBJ |

The STL files are structurally valid binary meshes (4,110 and 18,334
triangles respectively). STL and OBJ do not reliably specify units: confirm
the intended dimensions against Fusion or a physical part before printing.
The exported body and lid have separate source coordinates; their bounding
boxes do not show the assembled height or establish how they fit together.

Print one body and one lid as a starting assumption, and compare with the
assembled instrument and Fusion assembly before committing to a print. Use
millimetres only after confirming scale; place each part on the print bed in
the slicer. Orientation, supports, clearances, gland fit, sealing, cuvette
alignment and complete electronic-stack fit have not been verified from these
meshes. See the [assembly guide](../docs/assembly.md).
