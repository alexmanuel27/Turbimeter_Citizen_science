#!/usr/bin/env python3
"""Check the three-board design. Design: Ing. Alex M. Rivera.

Run with KiCad's Python and --unrouted before routing; omit that flag afterwards.
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
PROJECTS = {'sensor': HW / 'esque', 'controller': HW / 'controller/controller', 'adc': HW / 'adc/adc'}


def command(*args):
    result = subprocess.run([CLI, *map(str, args)], capture_output=True, text=True)
    assert result.returncode == 0, result.stdout + result.stderr


def contour(board):
    result = []
    for shape in board.GetDrawings():
        if shape.GetLayer() == pcbnew.Edge_Cuts:
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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--unrouted', action='store_true', help='Require zero tracks/zones; allow unrouted nets.')
    args = parser.parse_args()
    boards = {name: pcbnew.LoadBoard(str(base.with_suffix('.kicad_pcb'))) for name, base in PROJECTS.items()}
    parts = {name: {f.GetReference(): f for f in board.GetFootprints()} for name, board in boards.items()}
    original = contour(pcbnew.LoadBoard(str(HW / 'archive/single-board-v0.5/esque.kicad_pcb')))
    expected = {'sensor': set('D1 D2 D3 U1 R1 R2 R3 R4 R5 R6 C1 C2 C3 C4 C6 J1'.split()),
                'controller': {'A1', 'J1', 'J2', 'J3'}, 'adc': {'U2', 'C5', 'J2', 'J3'}}
    bottom = {'sensor': set(), 'controller': {'J1'}, 'adc': {'J2', 'J3'}}
    with tempfile.TemporaryDirectory(prefix='turbimeter-check-') as tmp:
        tmp = Path(tmp)
        baseline = tmp / 'baseline.xml'
        command('sch', 'export', 'netlist', HW / 'archive/single-board-v0.5/esque.kicad_sch', '-o', baseline, '--format', 'kicadxml')
        previous = net_nodes(baseline)
        merged = {}
        exported = {}
        for name, base in PROJECTS.items():
            board = boards[name]
            assert contour(board) == original, (name, 'board contour changed')
            assert set(parts[name]) == expected[name], (name, 'component inventory changed')
            assert 'Ing. Alex M. Rivera' in board.GetTitleBlock().GetComment(0), name
            assert {ref for ref, part in parts[name].items() if part.IsFlipped()} == bottom[name], name
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
        for lower, upper, ref, count, pitch in [('sensor', 'controller', 'J1', 6, 2.0),
                                                ('controller', 'adc', 'J2', 4, 2.54),
                                                ('controller', 'adc', 'J3', 4, 2.54)]:
            socket, male = parts[lower][ref], parts[upper][ref]
            origin = socket.GetPosition()
            angle = math.radians(socket.GetOrientationDegrees())
            pins = {p.GetNumber(): p for p in male.Pads()}
            for i in range(count):
                local_y = (i - (count - 1) / 2) * pitch
                target_x = pcbnew.ToMM(origin.x) + math.sin(angle) * local_y
                target_y = pcbnew.ToMM(origin.y) + math.cos(angle) * local_y
                pin = pins[str(i + 1)].GetPosition()
                assert math.hypot(pcbnew.ToMM(pin.x) - target_x, pcbnew.ToMM(pin.y) - target_y) < 0.00001, (ref, i + 1, 'mating axes')
                assert exported[lower][ref, str(i + 1)] == exported[upper][ref, str(i + 1)], (ref, i + 1, 'net mismatch')
    print('PASS: original circuit preserved; contours identical; mating pins aligned; electronics face upwards.')
    print('Mechanical heights and optical alignment still require a dry assembly with the actual parts.')


if __name__ == '__main__':
    main()
