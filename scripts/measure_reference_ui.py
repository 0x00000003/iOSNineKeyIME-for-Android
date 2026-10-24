#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Compare SHA-bound private reference/capture geometry; outputs numbers, never image text."""
import argparse, hashlib, json, math, re, statistics
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
STATIC=re.compile(r'(layout-(primary|symbols|latin|return)|emoji|more|space|pinyin|delete|enter|zero|number-punctuation|shift|symbol-page|text-[0-9]+|digit-[2-9])')

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def glyph_box(image,box):
    l,t,r,b=map(int,box);l=max(0,l+3);t=max(0,t+3);r=min(image.width,r-3);b=min(image.height,b-3)
    if r<=l or b<=t:return None
    patch=image.crop((l,t,r,b)).convert('RGB');points=[]
    for y in range(patch.height):
        for x in range(patch.width):
            rgb=patch.getpixel((x,y))
            if min(rgb)>200 and max(rgb)-min(rgb)<35:points.append((x,y))
    if not points:return None
    return [max(p[0] for p in points)-min(p[0] for p in points)+1,max(p[1] for p in points)-min(p[1] for p in points)+1]
def median_patch(image,x,y):
    patch=image.crop((max(0,int(x)-2),max(0,int(y)-2),min(image.width,int(x)+3),min(image.height,int(y)+3))).convert('RGB')
    return [statistics.median(p[i] for p in patch.getdata()) for i in range(3)]

def measure(evidence):
    record=json.loads(evidence.read_text());spec_path=ROOT/'spec/reference/ios-dark-v1.json';spec=json.loads(spec_path.read_text())
    captures={c['id'].split('-')[0].upper():c for c in record.get('reference_captures',[]) if not c['id'].endswith('-pressed')}
    rows=[]
    for reference in spec['reference_rows']:
        ident=reference['id'];capture=captures.get(ident)
        if capture is None:rows.append(dict(id=ident,status='NOT_CAPTURED'));continue
        source=ROOT/'.private/references/ios-20261023'/(ident+'-original.jpg')
        actual=ROOT/'artifacts/testhost'/(evidence.stem+'-'+capture['id']+'.png')
        if digest(source)!=reference['source_sha256'] or digest(actual)!=capture['png_sha256']:raise ValueError('E_IMAGE_HASH')
        crop=reference['keyboard_crop'];scale=reference['source_dimensions'][0]/capture['width']
        with Image.open(source) as source_image,Image.open(actual) as actual_image:
            if actual_image.size!=(capture['width'],capture['height']):raise ValueError('E_CAPTURE_SIZE')
            boxes={key:[v*scale for v in box] for key,box in capture['keys'].items() if STATIC.fullmatch(key)}
            matched=[];used=set()
            for item in reference.get('detected_key_rectangles',[]):
                l,t,r,b=item['box_source_px'];target=[l,t-crop[1],r,b-crop[1]];cx=(l+r)/2;cy=(t+b)/2-crop[1]
                candidates=[(math.hypot((v[0]+v[2])/2-cx,(v[1]+v[3])/2-cy),key,v) for key,v in boxes.items() if key not in used]
                if not candidates:continue
                distance,key,observed=min(candidates)
                if distance>30:continue
                used.add(key);delta=[round(a-e,3) for a,e in zip(observed,target)]
                ref_glyph=glyph_box(source_image,[l,t,r,b]);actual_glyph=glyph_box(actual_image,[v/scale for v in observed])
                matched.append(dict(static_key_id=key,reference_rectangle=target,actual_rectangle_normalized=[round(v,3) for v in observed],edge_delta_px=delta,
                    reference_white_bbox=ref_glyph,actual_white_bbox_normalized=None if actual_glyph is None else [round(v*scale,3) for v in actual_glyph]))
            colors={}
            for name,item in reference.get('color_measurements',{}).items():
                x,y=item['sample_center_source_px'];rgb=median_patch(actual_image,x/scale,(y-crop[1])/scale)
                colors[name]=dict(reference_rgb=item['median_rgb'],actual_rgb=rgb,delta=[a-e for a,e in zip(rgb,item['median_rgb'])])
            rows.append(dict(id=ident,status='MEASURED',source_sha256=reference['source_sha256'],capture_sha256=capture['png_sha256'],
                reference_height=crop[3]-crop[1],actual_height_normalized=round(actual_image.height*scale,3),
                height_delta_px=round(actual_image.height*scale-(crop[3]-crop[1]),3),reference_rectangles=len(reference.get('detected_key_rectangles',[])),matched_rectangles=len(matched),
                max_abs_edge_delta_px=max((abs(v) for m in matched for v in m['edge_delta_px']),default=None),colors=colors,keys=matched))
    return dict(status='MEASUREMENTS_ONLY',scope='width-normalized static keyboard geometry/color/white-pixel bounds; NOT visual acceptance, OCR, font equality or complete functional validation',
        input_scope_pass=record.get('pass',False),input_evidence_sha256=digest(evidence),spec_sha256=digest(spec_path),ime_apk_sha256=record.get('ime_apk_sha256'),
        limitations=['JPEG edges approximate +/-2 source pixels','white bounding boxes combine all white glyph/icon pixels in a static key','platform fonts/emoji/gesture area differ; no Apple assets packaged','only fresh nonce/SHA-bound captures present in input are compared'],references=rows)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--record',required=True);parser.add_argument('--output',required=True);args=parser.parse_args()
    if not re.fullmatch('[a-z0-9-]+',args.record) or not re.fullmatch('[a-z0-9-]+',args.output):raise ValueError('E_RECORD_NAME')
    output=ROOT/'docs/testing/evidence/m5'/(args.output+'.json');output.parent.mkdir(parents=True,exist_ok=True)
    result=measure(ROOT/'docs/testing/evidence/m2'/(args.record+'.json'))
    with output.open('x')as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(status=result['status'],rows=[dict(id=r['id'],status=r['status'],height_delta_px=r.get('height_delta_px'),max_abs_edge_delta_px=r.get('max_abs_edge_delta_px')) for r in result['references']])))
