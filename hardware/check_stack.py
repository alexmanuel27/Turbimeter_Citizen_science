#!/usr/bin/env python3
"""Historical two-board placement checks. Design: Ing. Alex M. Rivera.

These assertions describe earlier layouts and predate J1 P2.54, H1 and later
builder edits. They are not a current release gate. See hardware/README.md.
"""
import argparse
from collections import Counter
import json
import math
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

import pcbnew

HW = Path(__file__).resolve().parent
CLI = shutil.which('kicad-cli') or '/Applications/KiCad/KiCad.app/Contents/MacOS/kicad-cli'
PROJECTS = {'sensor': HW / 'esque', 'controller': HW.parent / 'hardware-controller/controller'}
USB_PINS = ['USB_VBUS', 'GND', 'USB_DM', 'USB_DP']
CABLE_ARC = 'fc186bd1-ee77-4e34-9eb4-581373b05ef9'


def command(*args):
    result = subprocess.run([CLI, *map(str, args)], capture_output=True, text=True)
    assert result.returncode == 0, result.stdout + result.stderr


def contour(board, skip_cable_arc=False):
    result = []
    for shape in board.GetDrawings():
        if shape.GetLayer() == pcbnew.Edge_Cuts:
            if skip_cable_arc and shape.m_Uuid.AsString() == CABLE_ARC:
                continue
            points = [shape.GetStart(), shape.GetEnd()]
            if shape.GetShape() == pcbnew.S_ARC:
                points.append(shape.GetArcMid())
            result.append((shape.GetShape(), shape.GetWidth(), tuple((p.x, p.y) for p in points)))
    return sorted(result)


def net_nodes(path):
    result = {}
    for net in ET.parse(path).getroot().findall('nets/net'):
        name = net.attrib['name'].lstrip('/')
        if name.startswith('unconnected-'):
            continue
        for node in net.findall('node'):
            if not node.attrib['ref'].startswith('#'):
                result[(node.attrib['ref'], node.attrib['pin'])] = name
    return result


def check_upper_against_base():
    """Validate the upper board against the user's routed base without editing it."""
    base = pcbnew.LoadBoard(str(PROJECTS['sensor'].with_suffix('.kicad_pcb')))
    upper = pcbnew.LoadBoard(str(PROJECTS['controller'].with_suffix('.kicad_pcb')))
    assert contour(upper, True) == contour(base, True), 'upper outline differs beyond cable relief'
    parts = [{f.GetReference(): f for f in b.GetFootprints()} for b in (base, upper)]
    assert set(parts[1]) == {'A1', 'U2', 'C5', 'J1', 'J4', 'J5'}
    assert 'Ing. Alex M. Rivera' in upper.GetTitleBlock().GetComment(0)
    for ref, f in parts[1].items():
        assert f.IsFlipped() == (ref in {'A1', 'U2', 'J5'}), (ref, 'B components / F solder side')
    for t in upper.GetTracks():
        assert not isinstance(t, pcbnew.PCB_VIA) and t.GetLayer() == pcbnew.F_Cu
    assert all(z.GetLayer() == pcbnew.F_Cu for z in upper.Zones())
    for ref, bounds in [('A1', (-8.95, 8.95, -10.65, 11.95)), ('U2', (-2.62, 25.53, -7.1, -5.5))]:
        f = parts[1][ref]; a = math.radians(f.GetOrientationDegrees())
        ox, oy = pcbnew.ToMM(f.GetPosition())
        for x in bounds[:2]:
            for y in bounds[2:]:
                y = -y if f.IsFlipped() else y
                assert math.hypot(ox+math.cos(a)*x+math.sin(a)*y-34.278,
                                  oy-math.sin(a)*x+math.cos(a)*y-33.2356) < 22, (ref, '44 mm envelope')
    with tempfile.TemporaryDirectory(prefix='turbimeter-upper-') as directory:
        tmp = Path(directory); nets = []
        for i, name in enumerate(('sensor', 'controller')):
            xml = tmp/f'{name}.xml'
            command('sch', 'export', 'netlist', PROJECTS[name].with_suffix('.kicad_sch'), '-o', xml, '--format', 'kicadxml')
            nets.append(net_nodes(xml))
        for ref, count in [('J1', 6), ('J4', 4)]:
            socket = parts[1][ref]; ox, oy = pcbnew.ToMM(socket.GetPosition())
            a = math.radians(socket.GetOrientationDegrees()); mirror = -1 if socket.IsFlipped() else 1
            pins = {p.GetNumber(): p for p in parts[0][ref].Pads()}
            for i in range(count):
                x = 0 if ref == 'J1' else (.635 if i % 2 == 0 else -.635)
                y = ((i-2.5)*2 if ref == 'J1' else (i//2-.5)*1.27)*mirror
                px, py = pcbnew.ToMM(pins[str(i+1)].GetPosition())
                assert math.hypot(px-ox-math.cos(a)*x-math.sin(a)*y,
                                  py-oy+math.sin(a)*x-math.cos(a)*y) < .00001, (ref, i+1, 'mating axis')
                assert nets[0][ref, str(i+1)] == nets[1][ref, str(i+1)], (ref, i+1, 'net mismatch')
        assert [nets[0]['J2', str(i)] for i in range(1, 5)] == ['USB_DM', 'USB_DP', 'USB_VBUS', 'GND']
        assert [nets[1]['J5', str(i)] for i in range(1, 5)] == USB_PINS
        command('sch', 'erc', PROJECTS['controller'].with_suffix('.kicad_sch'), '-o', tmp/'upper-erc.rpt', '--exit-code-violations')
        command('pcb', 'drc', PROJECTS['controller'].with_suffix('.kicad_pcb'), '-o', tmp/'upper-drc.json', '--format', 'json', '--schematic-parity')
        report = json.loads((tmp/'upper-drc.json').read_text())
        assert not report['violations'], report['violations']
        assert not report['schematic_parity'], report['schematic_parity']
        print(f"PASS: upper ERC/DRC/parity 0; {len(report['unconnected_items'])} unrouted items; all 10 mating contacts align and carry matching nets.")
        print('Base checked as a mechanical/electrical reference only; its routing and DRC findings are not modified or approved.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--unrouted', action='store_true', help='Require zero tracks/zones; allow unrouted nets.')
    parser.add_argument('--match-base', action='store_true', help='Check upper placement against the current routed base; allow upper unrouted nets.')
    args = parser.parse_args()
    if args.match_base:
        check_upper_against_base()
        return
    boards = {name: pcbnew.LoadBoard(str(base.with_suffix('.kicad_pcb'))) for name, base in PROJECTS.items()}
    parts = {name: {f.GetReference(): f for f in board.GetFootprints()} for name, board in boards.items()}
    original = contour(pcbnew.LoadBoard(str(HW / 'archive/single-board-v0.5/esque.kicad_pcb')))
    expected = {'sensor': set('D1 D2 D3 U1 R1 R2 R3 R4 R5 R6 C1 C2 C3 C4 C6 J1 J2 J4'.split()),
                'controller': {'A1', 'U2', 'C5', 'J1', 'J4', 'J5'}}
    bottom = {'sensor': set('R1 R2 R3 R4 R5 R6 C1 C2 C3 C4'.split()),
              'controller': {'C5', 'J1', 'J4'}}
    assert contour(boards['sensor']) == original, 'base contour changed'
    assert contour(boards['controller'], True) == contour(boards['sensor'], True), 'upper contour changed beyond cable relief'
    for name, board in boards.items():
        outline = pcbnew.SHAPE_POLY_SET()
        assert board.GetBoardPolygonOutlines(outline), (name, 'invalid outline')
        point = lambda x, y: pcbnew.VECTOR2I(pcbnew.FromMM(x), pcbnew.FromMM(y))
        assert not outline.Contains(point(34, 24)), (name, 'cuvette opening obstructed')
        assert outline.Contains(point(20.94, 41)) == (name == 'controller'), (name, 'cable relief')
    # Transform nominal body corners from each footprint's local coordinates.
    # ADS component protrusion is estimated; replace this envelope after measuring the module.
    for ref, bounds in [('A1', (-8.95, 8.95, -10.65, 11.95)),
                        ('U2', (-2.62, 25.53, -7.1, -5.5))]:
        part = parts['controller'][ref]
        angle = math.radians(part.GetOrientationDegrees())
        ox, oy = pcbnew.ToMM(part.GetPosition().x), pcbnew.ToMM(part.GetPosition().y)
        xmin, xmax, ymin, ymax = bounds
        for x in (xmin, xmax):
            for y in (ymin, ymax):
                wx = ox + math.cos(angle) * x + math.sin(angle) * y
                wy = oy - math.sin(angle) * x + math.cos(angle) * y
                assert math.hypot(wx - 34.278, wy - 33.2356) < 22, (ref, 'module exceeds cylinder')
    with tempfile.TemporaryDirectory(prefix='turbimeter-check-') as tmp:
        tmp = Path(tmp)
        baseline = tmp / 'baseline.xml'
        command('sch', 'export', 'netlist', HW / 'archive/single-board-v0.5/esque.kicad_sch', '-o', baseline, '--format', 'kicadxml')
        previous = {key: net for key, net in net_nodes(baseline).items() if not key[0].startswith('J')}
        merged = {}
        exported = {}
        for name, base in PROJECTS.items():
            board = boards[name]
            assert set(parts[name]) == expected[name], (name, 'component inventory changed')
            assert 'Ing. Alex M. Rivera' in board.GetTitleBlock().GetComment(0), name
            assert {ref for ref, part in parts[name].items() if part.IsFlipped()} == bottom[name], name
            for ref, part in parts[name].items():
                if ref in ({'J2'} if name == 'sensor' else {'J5'}):
                    assert len(part.Pads()) == 4 and all(p.GetAttribute() == pcbnew.PAD_ATTRIB_PTH and abs(pcbnew.ToMM(p.GetDrillSize().x)-1)<.001 for p in part.Pads()), (name, ref, 'four 1mm THT wire holes required')
                smd = bool(part.GetAttributes() & pcbnew.FP_SMD)
                assert smd == part.IsFlipped(), (name, ref, 'THT must be top; SMD bottom')
                if (name, ref) not in [('sensor', 'J2'), ('controller', 'J5')]:
                    assert len(part.Models()), (name, ref, 'missing 3D model')
                for model in part.Models():
                    models_dir = PROJECTS[name].parent/'3dmodels'
                    filename = model.m_Filename.replace('${TURBIMETER_3DMODEL_DIR}', str(models_dir))
                    filename = filename.replace('${KICAD9_3DMODEL_DIR}', '/Applications/KiCad/KiCad.app/Contents/SharedSupport/3dmodels')
                    assert Path(filename).is_file(), (name, ref, filename)
            for track in board.GetTracks():
                assert not isinstance(track, pcbnew.PCB_VIA) and track.GetLayer() == pcbnew.B_Cu, (name, 'B.Cu routing only; no vias')
            assert all(zone.GetLayer() == pcbnew.B_Cu for zone in board.Zones()), (name, 'B.Cu zones only')
            if args.unrouted:
                assert len(board.GetTracks()) == 0 and board.GetAreaCount() == 0, (name, 'unexpected copper routing')
            erc = tmp / (name + '-erc.rpt')
            xml = tmp / (name + '.xml')
            drc = tmp / (name + '-drc.json')
            command('sch', 'erc', base.with_suffix('.kicad_sch'), '-o', erc, '--exit-code-violations')
            command('sch', 'export', 'netlist', base.with_suffix('.kicad_sch'), '-o', xml, '--format', 'kicadxml')
            command('pcb', 'drc', base.with_suffix('.kicad_pcb'), '-o', drc, '--format', 'json', '--schematic-parity')
            report = json.loads(drc.read_text())
            assert not report['violations'], (name, report['violations'])
            assert not report['schematic_parity'], (name, report['schematic_parity'])
            if not args.unrouted:
                assert not report['unconnected_items'], (name, 'finish routing first')
            exported[name] = net_nodes(xml)
            merged.update({key: net for key, net in exported[name].items() if not key[0].startswith('J')})
            print(f"{name}: ERC 0; placement DRC 0; parity 0; {len(report['unconnected_items'])} unrouted items")
        assert merged == previous, ('Circuit differs from reviewed baseline', set(merged.items()) ^ set(previous.items()))
        # Every original component occurs once; mating connector names may repeat across floors.
        originals = [ref for floor in parts.values() for ref in floor if not ref.startswith('J')]
        assert len(originals) == 18 and all(n == 1 for n in Counter(originals).values())
        for lower, upper, ref in [('sensor', 'controller', 'J1'), ('sensor', 'controller', 'J4')]:
            male, socket = parts[lower][ref], parts[upper][ref]
            origin = socket.GetPosition()
            angle = math.radians(socket.GetOrientationDegrees())
            pins = {p.GetNumber(): p for p in male.Pads()}
            for i in range(6 if ref == 'J1' else 4):
                local_x = 0 if ref == 'J1' else (.635 if i % 2 == 0 else -.635)
                local_y = (i - 2.5) * 2 if ref == 'J1' else (i // 2 - .5) * 1.27
                # KiCad Flip(..., False) mirrors the footprint's local Y before rotation.
                target_x = pcbnew.ToMM(origin.x) + math.cos(angle) * local_x - math.sin(angle) * local_y
                target_y = pcbnew.ToMM(origin.y) - math.sin(angle) * local_x - math.cos(angle) * local_y
                pin = pins[str(i + 1)].GetPosition()
                assert math.hypot(pcbnew.ToMM(pin.x) - target_x, pcbnew.ToMM(pin.y) - target_y) < 0.00001, (ref, i + 1, 'mating axes')
                assert exported[lower][ref, str(i + 1)] == exported[upper][ref, str(i + 1)], (ref, i + 1, 'net mismatch')
        for floor, ref in [('sensor', 'J2'), ('sensor', 'J4'), ('controller', 'J4'), ('controller', 'J5')]:
            assert [exported[floor][ref, str(i)] for i in range(1, 5)] == USB_PINS, (floor, ref, 'USB pin mapping')
        assert all(net != 'USB_CC' for nodes in exported.values() for net in nodes.values()), 'four-wire boards must not depend on a CC conductor'
        # USB reaches the module through the external pigtail, never through invented edge pins.
        assert ('A1', '14') not in exported['controller'], 'XIAO post-diode 5V pin must remain NC'
    print('PASS: circuit preserved; base contour intact; upper cable relief filled; USB and mating pins aligned.')
    print('Mechanical heights and optical alignment still require a dry assembly with the actual parts.')


if __name__ == '__main__':
    main()
