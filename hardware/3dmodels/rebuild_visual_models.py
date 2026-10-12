#!/usr/bin/env python3
"""Derived viewer meshes from supplied STEP files. Design: Ing. Alex M. Rivera.
Run using KiCad Python. Originals remain unchanged; these are visual assembly aids.
"""
import json
import math
from pathlib import Path
import struct
import subprocess
import tempfile
import pcbnew

ROOT = Path(__file__).resolve().parent
CLI = '/Applications/KiCad/KiCad.app/Contents/MacOS/kicad-cli'


def matrix(node):
    x, y, z, w = node.get('rotation', [0, 0, 0, 1])
    s = node.get('scale', [1, 1, 1]); t = node.get('translation', [0, 0, 0])
    return [[(1-2*y*y-2*z*z)*s[0], (2*x*y-2*z*w)*s[1], (2*x*z+2*y*w)*s[2], t[0]],
            [(2*x*y+2*z*w)*s[0], (1-2*x*x-2*z*z)*s[1], (2*y*z-2*x*w)*s[2], t[1]],
            [(2*x*z-2*y*w)*s[0], (2*y*z+2*x*w)*s[1], (1-2*x*x-2*y*y)*s[2], t[2]],
            [0, 0, 0, 1]]


def multiply(a, b):
    return [[sum(a[i][k]*b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def clip_lead(triangle):
    """Keep the BPW34 body and short lead stubs; formed legs are separate models."""
    out = []
    for a, b in zip(triangle, triangle[1:] + triangle[:1]):
        if a[2] >= -.7:
            out.append(a)
        if (a[2] >= -.7) != (b[2] >= -.7):
            f = (-.7-a[2])/(b[2]-a[2])
            out.append([a[i]+f*(b[i]-a[i]) for i in range(3)])
    return [out[:1]+out[i:i+2] for i in range(1, len(out)-1)]


def convert(glb, output, remove_header=False, clip_bpw=False):
    data = glb.read_bytes()
    assert data[:4] == b'glTF'
    size = struct.unpack_from('<I', data, 12)[0]
    doc = json.loads(data[20:20+size]); binary = data[28+size:]
    groups = {}; removed = []

    def accessor(index):
        a = doc['accessors'][index]; view = doc['bufferViews'][a['bufferView']]
        fmt, length = {5126: ('f', 4), 5125: ('I', 4), 5123: ('H', 2), 5121: ('B', 1)}[a['componentType']]
        width = {'SCALAR': 1, 'VEC3': 3}[a['type']]
        stride = view.get('byteStride', length*width)
        start = view.get('byteOffset', 0)+a.get('byteOffset', 0)
        return [struct.unpack_from('<'+fmt*width, binary, start+i*stride) for i in range(a['count'])]

    def walk(index, parent):
        node = doc['nodes'][index]
        if remove_header and node.get('name', '').startswith('2.54mm x 10'):
            removed.append(node['name']); return
        transform = multiply(parent, matrix(node))
        if 'mesh' in node:
            for primitive in doc['meshes'][node['mesh']]['primitives']:
                assert primitive.get('mode', 4) == 4
                points = []
                for point in accessor(primitive['attributes']['POSITION']):
                    p = [sum(transform[i][k]*(*point, 1)[k] for k in range(4))*1000 for i in range(3)]
                    # Native GLTF is Y-up; component mount plane is at board Z=1.595 mm.
                    points.append([p[0], -p[2], p[1]-1.595])
                indices = [v[0] for v in accessor(primitive['indices'])]
                triangles = groups.setdefault(primitive.get('material', 0), [])
                for i in range(0, len(indices), 3):
                    triangle = [points[k] for k in indices[i:i+3]]
                    triangles.extend(clip_lead(triangle) if clip_bpw else [triangle])
        for child in node.get('children', []):
            walk(child, transform)

    for node in doc['scenes'][0]['nodes']:
        walk(node, matrix({}))
    assert not remove_header or len(removed) == 1, removed
    assert sum(map(len, groups.values())) > 100
    result = ['#VRML V2.0 utf8', '# Design: Ing. Alex M. Rivera.', '# Derived supplied model; KiCad VRML units are 2.54 mm. Not a measured fit certificate.']
    for material, triangles in groups.items():
        if not triangles:
            continue
        color = doc['materials'][material]['pbrMetallicRoughness'].get('baseColorFactor', [.5, .5, .5, 1])
        color = ' '.join(f'{max(0, v)**(1/2.2):.5f}' for v in color[:3])
        points = ',\n'.join(' '.join(f'{v/2.54:.7f}' for v in p) for t in triangles for p in t)
        indices = ', '.join(f'{i} {i+1} {i+2} -1' for i in range(0, len(triangles)*3, 3))
        result.append(f'Shape {{ appearance Appearance {{ material Material {{ diffuseColor {color} }} }} geometry IndexedFaceSet {{ solid FALSE coord Coordinate {{ point [{points}] }} coordIndex [{indices}] }} }}')
    output.write_text('\n'.join(result)+'\n')


def main():
    # One check for the clipping boundary used to remove long unformed leads.
    triangles = clip_lead([[0, 0, -1], [1, 0, 0], [0, 1, 0]])
    assert len(triangles) == 2 and all(p[2] >= -.700001 for t in triangles for p in t)
    with tempfile.TemporaryDirectory(prefix='turbimeter-models-') as tmp:
        tmp = Path(tmp)
        for source, output, remove_header, clip_bpw in [('ADS1115_supplied.step', 'ADS1115_body.wrl', True, False), ('BPW34.step', 'BPW34_body.wrl', False, True)]:
            board = pcbnew.BOARD(); part = pcbnew.FOOTPRINT(board); part.SetReference('MODEL'); board.Add(part)
            model = pcbnew.FP_3DMODEL(); model.m_Filename = str(ROOT/source); part.Add3DModel(model)
            edge = pcbnew.PCB_SHAPE(); edge.SetShape(pcbnew.S_RECT)
            edge.SetStart(pcbnew.VECTOR2I(-20000000, -20000000)); edge.SetEnd(pcbnew.VECTOR2I(20000000, 20000000))
            edge.SetLayer(pcbnew.Edge_Cuts); edge.SetWidth(50000); board.Add(edge)
            pcb = tmp/'model.kicad_pcb'; glb = tmp/'model.glb'; pcbnew.SaveBoard(str(pcb), board)
            result = subprocess.run([CLI, 'pcb', 'export', 'glb', str(pcb), '-o', str(glb), '--force', '--no-board-body'], capture_output=True, text=True)
            assert result.returncode == 0 and glb.exists(), result.stdout+result.stderr
            convert(glb, ROOT/output, remove_header, clip_bpw)
            print(output, 'created')
    def box(center, size, color):
        return ('Transform { translation %s children [ Shape { appearance Appearance { material Material { diffuseColor %s } } geometry Box { size %s } } ] }'
                % (' '.join(str(v/2.54) for v in center), color, ' '.join(str(v/2.54) for v in size)))
    header = '#VRML V2.0 utf8\n# Design: Ing. Alex M. Rivera. Provisional formed / long-reach leads; select and measure actual hardware.\n'
    legs = []
    for x in (0, 5.08):
        legs.extend([box((x, 0, .65), (.4, .4, 4.1), '.75 .75 .78'),
                     box((x, .35, 2.7), (.4, 1.1, .4), '.75 .75 .78'),
                     box((.12 if x == 0 else 4.96, .7, 2.7), (.64, .4, .4), '.75 .75 .78')])
    (ROOT/'BPW34_formed_leads.wrl').write_text(header+'\n'.join(legs)+'\n')
    for name, pins in [('StackPins_1x06_P2mm', [(0, -2*i) for i in range(6)]),
                       ('StackPins_2x02_P1.27mm', [(1.27*(i % 2), -1.27*(i//2)) for i in range(4)]),
                       ('StackPins_2x03_P1.27mm', [(1.27*(i % 2), -1.27*(i//2)) for i in range(6)])]:
        (ROOT/(name+'.wrl')).write_text(header+'\n'.join(box((x, y, 8.4), (.4, .4, 6.8), '.82 .68 .32') for x, y in pins)+'\n')


if __name__ == '__main__':
    main()
