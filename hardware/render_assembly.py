#!/usr/bin/env python3
"""Render individual boards and provisional 14 mm stack. Design: Ing. Alex M. Rivera.
Run with KiCad Python; modifies no active PCB. Requires the bundled Node/sharp for captions.
"""
from pathlib import Path
import base64
import json
import math
import subprocess
import tempfile
import pcbnew

HW = Path(__file__).resolve().parent
CLI = '/Applications/KiCad/KiCad.app/Contents/MacOS/kicad-cli'
MODEL_LIB = Path('/Applications/KiCad/KiCad.app/Contents/SharedSupport/3dmodels')


def command(*args):
    result = subprocess.run([CLI, *map(str, args)], capture_output=True, text=True)
    assert result.returncode == 0, result.stdout+result.stderr


def resolve_models(part):
    for index in range(len(part.Models())):
        model = part.Models()[index]
        model.m_Filename = model.m_Filename.replace('${TURBIMETER_3DMODEL_DIR}', str(HW/'3dmodels')).replace('${KICAD9_3DMODEL_DIR}', str(MODEL_LIB))
        assert Path(model.m_Filename).exists(), model.m_Filename


def compose_preview():
    """Keep the two PCB views first, followed by all individual/assembled renders."""
    def image(name, x, y, width=500):
        data = base64.b64encode((HW/'renders'/(name+'.png')).read_bytes()).decode('ascii')
        return f'<image x="{x}" y="{y}" width="{width}" height="{width*1100/1500}" href="data:image/png;base64,{data}"/>'

    def document(height, title, content):
        return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="{height}" viewBox="0 0 1100 {height}">
<title>{title} — Ing. Alex M. Rivera</title><rect width="1100" height="{height}" fill="white"/>
<g font-family="Arial, sans-serif" fill="#17283e">{content}
<text x="40" y="{height-25}" font-size="17" font-weight="bold">Design: Ing. Alex M. Rivera</text>
<text x="1060" y="{height-25}" text-anchor="end" font-size="15">Upper rev. 0.10 · 13 October 2026 · base as supplied / provisional fit</text>
</g></svg>'''

    body = ['<text x="40" y="42" font-size="27" font-weight="bold">Turbidimeter · upper PCB matched to routed base</text>',
            '<text x="40" y="72" font-size="17">Common KiCad coordinates · same scale · dashed circle: 44 mm enclosure</text>']
    with tempfile.TemporaryDirectory(prefix='turbimeter-preview-') as tmp:
        for i, (path, title) in enumerate([(HW/'esque.kicad_pcb', '1 · Sensor / cable entry'),
                                         (HW/'controller/controller.kicad_pcb', '2 · XIAO + upright ADS1115')]):
            exported = Path(tmp)/(str(i)+'.svg')
            command('pcb', 'export', 'svg', path, '-o', exported, '--layers', 'F.Cu,B.Cu,F.SilkS,B.SilkS,Edge.Cuts', '--mode-single', '--page-size-mode', '0', '--exclude-drawing-sheet')
            svg = exported.read_text(); svg = svg[svg.index('<g '):svg.rindex('</svg>')]
            svg = '\n'.join(line.rstrip() for line in svg.splitlines())
            for old, new in [('#F2EDA1','#665b18'),('#F0F084','#665b18'),('#E8B2A7','#A26253'),('#D0D2CD','#718392')]:
                svg = svg.replace(old,new)
            x = 40+520*i
            body.append(f'<text x="{x}" y="116" font-size="23" font-weight="bold">{title}</text><svg x="{x}" y="140" width="475" height="475" viewBox="11.278 10.2356 46 46">')
            body.append('<circle cx="34.278" cy="33.2356" r="22" fill="none" stroke="#42859c" stroke-width=".13" stroke-dasharray=".6 .4"/>')
            if i:
                board = pcbnew.LoadBoard(str(path))
                for ref, bounds, fill, stroke in [('A1', (-8.95,8.95,-10.65,11.95), '#dcecfb', '#3575a7'),
                                                  ('U2', (-2.62,25.53,-7.1,-5.5), '#f5e2b5', '#a67c28')]:
                    part = board.FindFootprintByReference(ref); angle = math.radians(part.GetOrientationDegrees())
                    ox,oy = pcbnew.ToMM(part.GetPosition().x),pcbnew.ToMM(part.GetPosition().y)
                    x0,x1,y0,y1 = bounds
                    y0,y1 = (-y0,-y1) if part.IsFlipped() else (y0,y1)
                    points = ' '.join(f'{ox+math.cos(angle)*x+math.sin(angle)*y:.4f},{oy-math.sin(angle)*x+math.cos(angle)*y:.4f}' for x,y in [(x0,y0),(x1,y0),(x1,y1),(x0,y1)])
                    body.append(f'<polygon points="{points}" fill="{fill}" fill-opacity=".55" stroke="{stroke}" stroke-width=".08" stroke-dasharray=".3 .2"/>')
            body.append(svg+'</svg>')
    body.extend(['<text x="40" y="650" font-size="17">Physical UP = B: THT + wire heads · SOLDER SIDE = F: SMD + upper sockets</text>',
                 '<text x="40" y="679" font-size="17" font-weight="bold">Upper: route F.Cu · Base routing preserved as supplied · Upper traces pending</text>',
                 '<text x="40" y="708" font-size="16">Base J2: 1 D− / 2 D+ / 3 VBUS / 4 GND · Upper J5: 1 VBUS / 2 GND / 3 D− / 4 D+</text>'])
    for y, title, names in [(755, 'Individual boards · TOP', ['base-top','upper-top']),
                            (1175, 'Individual boards · BOTTOM / solder side', ['base-bottom','upper-bottom']),
                            (1595, 'Connected boards · 14 mm top-to-top spacing (provisional)', ['assembly','assembly-front'])]:
        body.append(f'<text x="40" y="{y}" font-size="23" font-weight="bold">{title}</text>')
        body.extend(image(name,40+520*i,y+18) for i,name in enumerate(names))
    body.append('<text x="40" y="2020" font-size="16">Render models show assembly intent. Confirm actual module pinout, connector reach, tail trimming and enclosure fit.</text>')
    (HW/'pcb-top-preview.svg').write_text(document(2090,'Two-floor PCB placement and assembly renders',''.join(body)))
    assembly = '<text x="40" y="45" font-size="27" font-weight="bold">Turbidimeter · connected boards</text>'
    assembly += image('assembly',40,80)+image('assembly-front',560,80)
    for y, text in [(500,'Base retained: THT parts and male J1/J4 on B; SMD on F. Existing copper unchanged.'),
                    (535,'Upper: XIAO + ADS1115 + J5 on B (UP); female SMD sockets and C5 on F (BELOW).'),
                    (570,'Upper routing: F.Cu only. Base has user routing on F.Cu and B.Cu; no edits made.'),
                    (620,'14 mm PCB top-to-top spacing; approximately 35.6 mm total including 1 mm below base.'),
                    (655,'35 mm fit is unverified. Allow 36–38 mm before lid and cable-bend allowances.'),
                    (690,'Stock THT tails shown untrimmed. Select compatible long pins; support both boards and ADC.'),
                    (740,'USB: phone → cut cable → base J2 → J4 → upper J5 → USB-C male pigtail → XIAO.'),
                    (775,'Four wires: VBUS / GND / D− / D+. Phone-side OTG/Rd and micro-side legacy plug required.'),
                    (825,'USB remains toward the left in shared coordinates; verify compact plug clearance physically.'),
                    (860,'Supplied ADS model prints A3/A2/A1/A0; verify the real module before manufacture.')]:
        assembly += f'<text x="40" y="{y}" font-size="17">{text}</text>'
    (HW/'stack-assembly.svg').write_text(document(950,'Connected board assembly',assembly))


def main(names=None):
    output = HW/'renders'; output.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='turbimeter-render-') as tmp:
        tmp = Path(tmp)
        upper_carrier = tmp/'upper-carrier.step'
        command('pcb', 'export', 'step', HW/'controller/controller.kicad_pcb', '-o', upper_carrier, '--no-components', '--include-pads', '--include-silkscreen', '--include-soldermask', '--user-origin', '0x0mm')
        base = pcbnew.LoadBoard(str(HW/'esque.kicad_pcb'))
        upper = pcbnew.LoadBoard(str(HW/'controller/controller.kicad_pcb'))
        direction = -1 if base.FindFootprintByReference('J1').IsFlipped() else 1
        for part in base.GetFootprints():
            resolve_models(part)
        base_render = tmp/'base.kicad_pcb'; pcbnew.SaveBoard(str(base_render), base)
        for part in upper.GetFootprints():
            resolve_models(part)
        upper_render = tmp/'upper.kicad_pcb'; pcbnew.SaveBoard(str(upper_render), upper)
        for part in upper.GetFootprints():
            # A model-only anchor avoids drilling the upper module holes into the base.
            clone = pcbnew.FOOTPRINT(base); clone.SetReference('UPPER_'+part.GetReference())
            clone.SetPosition(part.GetPosition()); clone.SetLayer(part.GetLayer())
            clone.SetOrientationDegrees(part.GetOrientationDegrees()); base.Add(clone)
            clone.Reference().SetVisible(False); clone.Value().SetVisible(False)
            for model in part.Models():
                clone.Add3DModel(model)
            assert not len(clone.Pads()), 'upper model must not add holes to base'
            resolve_models(clone)
            for index in range(len(clone.Models())):
                model = clone.Models()[index]
                model.m_Offset.z += direction*(-14 if clone.IsFlipped() else 14)
        carrier = pcbnew.FOOTPRINT(base); carrier.SetReference('UPPER_CARRIER'); base.Add(carrier)
        model = pcbnew.FP_3DMODEL(); model.m_Filename = str(upper_carrier)
        # STEP bottom Z=0; offset is relative to the base F mount plane at Z=1.6.
        model.m_Offset.z = direction*14-1.6; carrier.Add3DModel(model)
        combined = tmp/'assembly.kicad_pcb'; pcbnew.SaveBoard(str(combined), base)
        jobs = [('base-top', base_render, 'bottom', '45,0,30', '.8'),
                ('upper-top', upper_render, 'bottom', '45,0,30', '.65'),
                ('base-bottom', base_render, 'top', '35,0,30', '.8'),
                ('upper-bottom', upper_render, 'top', '35,0,30', '.6'),
                ('assembly', combined, 'bottom', '45,0,30', '.5'),
                ('assembly-front', combined, 'front', '0,180,0', '.4')]
        if names is not None:
            jobs = [job for job in jobs if job[0] in names]
            assert {job[0] for job in jobs} == set(names), 'unknown render name'
        for name, path, side, rotation, zoom in jobs:
            command('pcb', 'render', path, '-o', output/(name+'.png'), '--width', '1500', '--height', '1100', '--side', side, '--rotate', rotation, '--zoom', zoom, '--background', 'opaque', '--quality', 'high', '--preset', 'follow_pcb_editor')
            print(name, 'rendered', flush=True)
    node = '/Users/alex/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node'
    sharp = '/Users/alex/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp'
    titles = {'base-top': 'Base | TOP: through-hole components', 'upper-top': 'Upper PCB | TOP: XIAO + upright ADS1115',
              'base-bottom': 'Base | BOTTOM: SMD + all solder joints', 'upper-bottom': 'Upper PCB | BOTTOM: SMD sockets + C5; J5 solder joints',
              'assembly': 'Connected boards | provisional 14 mm top-to-top spacing', 'assembly-front': 'Connected boards | front view of mating connectors'}
    titles = {job[0]: titles[job[0]] for job in jobs}
    script = """const sharp=require(process.argv[1]), fs=require('fs');
(async()=>{for(const [name,title] of Object.entries(JSON.parse(process.argv[3]))){
 const p=process.argv[2]+'/'+name+'.png', raw=fs.readFileSync(p);
 const svg=Buffer.from('<svg width="1500" height="1100"><rect y="1000" width="1500" height="100" fill="white"/><g font-family="Arial" fill="#17283e"><text x="30" y="1034" font-size="26">'+title+'</text><text x="30" y="1073" font-size="22">Design: Ing. Alex M. Rivera · Assembly 0.10 · base preserved / provisional fit</text></g></svg>');
 // ponytail: front crop is tied to the fixed native 1500x1100 render; adjust if camera settings change.
 let render=sharp(raw); if(name==="assembly-front") render=render.extract({left:470,top:100,width:560,height:540});
 await render.resize(1500,1000,{fit:"contain",background:"white"}).extend({bottom:100,background:"white"}).composite([{input:svg}]).png().toFile(p);}})();"""
    subprocess.run([node, '--input-type=commonjs', '-e', script, sharp, str(output), json.dumps(titles)], check=True)
    compose_preview()


if __name__ == '__main__':
    main()
