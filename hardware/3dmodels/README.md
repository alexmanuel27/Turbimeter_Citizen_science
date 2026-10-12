# Assembly models — revision 0.9

Assembly design: **Ing. Alex M. Rivera**. Updated 12 October 2026.

Supplied source CAD files retain their original authorship and content.
These reference models do not replace delivered-part measurements.

| Model | Used for | Treatment |
| --- | --- | --- |
| XIAO_SAMD21_supplied.step | Upper A1 | Supplied XIAO body, oriented USB toward the LEFT; two KiCad 1×7 headers added |
| ADS1115_supplied.step | Upper U2 | Supplied flat module; derived ADS1115_body.wrl excludes its straight header; KiCad right-angle 1×10 header added |
| BPW34.step | Base D1/D2 | Supplied body; derived BPW34_body.wrl clips long straight tails and combines with provisional formed-lead geometry |
| KiCad LED_D5.0mm_Horizontal_O1.27mm_Z3.0mm_IRGrey.step | Base D3 | Restores horizontal IR LED, nominal axis 3 mm above carrier |
| StackPins_*.wrl | Base J1/J4 | Provisional extensions of standard header pins to illustrate the 14 mm stack spacing |
| Standard KiCad package models | Remaining components | DIP-8, 1206 passives, radial capacitor and interboard sockets/headers |

J2/J5 are four bare through-hole wire lands. Cable conductors, strain relief, USB male pigtail,
optical holder, cuvette and enclosure are not modelled. The ADS1115.SLDPRT
and supplied ZIP/image files are retained as source references.

**ADS pinout discrepancy:** this STEP prints A3/A2/A1/A0, while the current
schematic assumes A0/A1/A2/A3. Confirm the real purchased module before
routing its analog inputs. No circuit nets were changed to match a 3D asset.
The two supplied ADS models also differ mechanically; their dimensions and
component side are estimates until compared with the delivered 006632.

Stock header/DIP/LED tails in standard models are not automatically trimmed.
Trim and insulate real solder tails, check header insertion and module support,
and verify every protrusion within the 44 mm cylinder. Long interboard pins
illustrate required reach; choose an actual matching socket/header pair.
The preferred 35 mm total height is not verified. Budget roughly 36–38 mm
with trimmed tails, then allow room for lid and cable bends.

The supplied STEP sources are unchanged. `rebuild_visual_models.py` uses
KiCad's native STEP-to-GLTF exporter and writes coloured VRML viewer meshes;
clipped lead ends are visual aids, not watertight manufacturing solids.
Source geometry names/printed labels are retained in the ADS viewer mesh.
The source file's solid STEP remains available if a mechanical CAD tool
needs it. KiCad's STEP exporter does not include the derived VRML bodies.

`../render_assembly.py` renders the active boards and a temporary assembly;
it leaves the editable PCB projects unchanged. Renders are in `../renders/`.
Temporary assembly files use absolute resolved model paths and a 14 mm
TOP-to-TOP spacing; they are not extra PCB projects.
