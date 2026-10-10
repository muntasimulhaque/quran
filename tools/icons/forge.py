# The mark's generator: the app's drawing lives once here and every surface
# that carries it is regenerated from it, so every surface shows one drawing
# drawn once, never its own drifting copy.
#
# Run from the repository with a Python that has Pillow, on the owner's
# machine only; the build does not depend on it an inch:
#   python tools/icons/forge.py preview   # comparison sheets into TEMP
#   python tools/icons/forge.py emit      # writes the repo's surface files
#
# emit writes: app/src/main/res/drawable/ic_launcher_foreground.xml and
# ic_launcher_monochrome.xml, the five mipmap legacy tiles, ic_share_mark
# and ic_daily at mdpi through xxxhdpi, play-store/icon-512.png, and
# play-store/feature-graphic-1024x500.png, rebuilt with the repo's own
# Literata and Inter over the paper field and the gold hairline frame.
# The drawing traces the owner's reference set: the pages are sail arcs
# diving into a spine that ends at the V where the two pages meet, each
# plank folds onto the page's outer edge, and each lower end meets the
# closing arc at one shared foot tip.

import math, os, sys
from PIL import Image, ImageDraw, ImageFont

INK   = (28, 27, 24)      # #1C1B18
GOLD  = (133, 100, 17)    # #856411
PAPER = (248, 245, 239)   # #F8F5EF
MUTED = (95, 91, 83)
WHITE = (255, 255, 255)
HAIR  = GOLD              # the banner's gold hairline

# ------------------------------------------------------------ trace geometry
AX_X, AX_Y = 85.5, 98.5
S_FG   = 0.415
STROKE = 1.5
HALO   = 1.5

PAGE_N   = (85.5, 63.2)
PAGE_P   = (56.5, 52.15)
PAGE_E1  = (39.6, 57.4)
PAGE_E2  = (38.6, 101.4)
PAGE_J   = (85.5, 113.7)

CROSS     = (85.5, 128.9)   # where the two planks cross
PLANK_END = (143.8, 148.8)   # the leg ends on its own line at the wedge's mouth
                             # and out to the mirrored (right) foot's corner
WEDGE_O   = (54.15, 118.25)
# the closing sweep now lands on the plank's own tip, so each foot is one
# clean tapered face with no open mouth: the sliver between arc and leg was
# the only spot of the mark that turned to mud at home-screen sizes
WEDGE_C1  = (44.0, 126.0)
WEDGE_C2  = (25.6, 137.4)
WEDGE_F   = (28.5, 149.0)

ARM_P_F, BAR_P_F = 3.0, 4.6
RING_F, JUNC_F   = 3.0, 1.2
CAP_TIP_X, CAP_BACK_X = 39.35, 35.6

# ---------------------------------------------------------------------- math
def norm(v):
    l = math.hypot(*v)
    return (v[0] / l, v[1] / l) if l else (0.0, 0.0)

def line_dir(p0, p1):
    return norm((p1[0] - p0[0], p1[1] - p0[1]))

def line_ix(p0, d0, p1, d1):
    t = ((p1[0] - p0[0]) * d1[1] - (p1[1] - p0[1]) * d1[0]) / (d0[0] * d1[1] - d0[1] * d1[0])
    return (p0[0] + d0[0] * t, p0[1] + d0[1] * t)

def cross(a, b):
    return a[0] * b[1] - a[1] * b[0]

def _perps(d):
    return ((-d[1], d[0]), (d[1], -d[0]))

def fillet(V, A, B, r):
    d1 = line_dir(A, V)
    d2 = line_dir(V, B)
    cosang = max(-1.0, min(1.0, d1[0] * d2[0] + d1[1] * d2[1]))
    turn = 180 - math.degrees(math.acos(cosang))
    d = r / math.tan(math.radians(turn / 2))
    t1 = (V[0] - d1[0] * d, V[1] - d1[1] * d)
    t2 = (V[0] + d2[0] * d, V[1] + d2[1] * d)
    # the center lies at distance r from each line, on the perpendicular at the
    # tangent point: solve t1 + n1*x = t2 + n2*y and take the right-sign pair
    b0 = t2[0] - t1[0]; b1 = t2[1] - t1[1]
    for n1 in _perps(d1):
        for n2 in _perps(d2):
            det = -n1[0]*n2[1] + n1[1]*n2[0]     # cross(n1, n2)
            if abs(det) < 1e-9:
                continue
            x = (n2[0]*b1 - b0*n2[1]) / det
            y = (n1[0]*b1 - n1[1]*b0) / det
            if abs(abs(x) - r) > 1e-6:
                continue
            c = (t1[0] + n1[0]*x, t1[1] + n1[1]*x)
            if abs(math.hypot(c[0]-t2[0], c[1]-t2[1]) - r) < 1e-5:
                return t1, c, t2, d1, d2
    raise ValueError('no fillet center found; lines nearly parallel')

def arc_cubics(c, t1, t2, d1, d2, tol=0.2):
    """circular arc about c from t1 to t2 made tangent to d1 at t1 and d2 at t2."""
    r = math.hypot(t1[0] - c[0], t1[1] - c[1])
    a1 = math.atan2(t1[1] - c[1], t1[0] - c[0])
    a2 = math.atan2(t2[1] - c[1], t2[0] - c[0])
    tw = a2 - a1
    while tw > math.pi: tw -= 2 * math.pi
    while tw <= -math.pi: tw += 2 * math.pi
    sgn = 1.0 if tw > 0 else -1.0
    n = 1
    while r * (4 / 3 * math.tan(abs(tw) / (4 * n)))**3 / 27 > tol and n < 4:
        n += 1
    segs = []
    a_prev = a1
    for i in range(1, n + 1):
        a_i = a1 + tw * i / n
        p0 = (c[0] + r * math.cos(a_prev), c[1] + r * math.sin(a_prev))
        p1 = (c[0] + r * math.cos(a_i), c[1] + r * math.sin(a_i))
        k = 4.0 / 3.0 * math.tan(abs(tw) / (4 * n)) * r
        tx0, ty0 = -math.sin(a_prev) * sgn, math.cos(a_prev) * sgn
        tx1, ty1 = -math.sin(a_i) * sgn, math.cos(a_i) * sgn
        segs.append(('C', (p0[0] + tx0 * k, p0[1] + ty0 * k), (p1[0] - tx1 * k, p1[1] - ty1 * k), p1))
        a_prev = a_i
    return segs

def cub_pts(p0, c1, c2, p1, n):
    out = []
    for i in range(1, n + 1):
        t = i / n; mt = 1 - t
        out.append((mt**3 * p0[0] + 3 * mt * mt * t * c1[0] + 3 * mt * t * t * c2[0] + t**3 * p1[0],
                    mt**3 * p0[1] + 3 * mt * mt * t * c1[1] + 3 * mt * t * t * c2[1] + t**3 * p1[1]))
    return out

# ---------------------------------------------------------------------- paths
def mirp(p):
    return (2 * AX_X - p[0], p[1])

def mir_segs(segs):
    out = []
    for s in segs:
        if s[0] == 'Z':
            out.append(s)
        elif s[0] == 'C':
            out.append(('C', mirp(s[1]), mirp(s[2]), mirp(s[3])))
        else:
            out.append((s[0], mirp(s[1])))
    return out

def page():
    d_edge = line_dir(PAGE_E1, PAGE_E2)
    d_bot  = line_dir(PAGE_E2, PAGE_J)
    t1E2, cE2, t2E2, _, _ = fillet(PAGE_E2, PAGE_E1, PAGE_J, RING_F)
    t1J, cJ, t2J, _, _ = fillet(PAGE_J, PAGE_E2, (85.5, PAGE_J[1] - 44.0), JUNC_F)
    P = [('M', PAGE_N)]
    P += [('C', (75.5, 54.2), (64.0, 52.05), PAGE_P)]
    # the sail cubic flows straight into the edge's own direction at the top
    # corner: tangent continuous by construction, so the outline never kinks
    c2top = (PAGE_E1[0] - d_edge[0]*5.2, PAGE_E1[1] - d_edge[1]*5.2)
    P += [('C', (49.5, 52.2), c2top, PAGE_E1)]
    P += [('L', t1E2)]
    P += arc_cubics(cE2, t1E2, t2E2, d_edge, d_bot)
    P += [('C', (54.0, 104.4), (73.5, 109.4), t1J)]
    P += arc_cubics(cJ, t1J, t2J, d_bot, (0.0, -1.0))
    P += [('L', PAGE_N), ('Z', None)]
    return P

def plank():
    bar_dir   = line_dir((30.5, 64.0), (28.6, 109.2))
    plank_dir = norm((2.93, 1.0))
    capd      = (-1.0, 0.0)
    v1 = line_ix((CAP_BACK_X, 61.8), capd, (30.5, 64.0), bar_dir)
    t1a, c1, t2a, _, _ = fillet(v1, (CAP_TIP_X, 61.8), (v1[0] + bar_dir[0] * 10, v1[1] + bar_dir[1] * 10), ARM_P_F)
    v2 = line_ix((30.5, 64.0), bar_dir, CROSS, plank_dir)
    t1b, c2, t2b, _, _ = fillet(v2, (v1[0] + bar_dir[0] * 10, v1[1] + bar_dir[1] * 10),
                                   (v2[0] + plank_dir[0] * 10, v2[1] + plank_dir[1] * 10), BAR_P_F)
    S_ = [('M', (CAP_TIP_X, 61.8))]
    S_ += [('L', t1a)]
    S_ += arc_cubics(c1, t1a, t2a, capd, bar_dir)
    S_ += [('L', t1b)]
    S_ += arc_cubics(c2, t1b, t2b, bar_dir, plank_dir)
    S_ += [('L', PLANK_END)]
    return S_

def wedge_arc():
    # the face's closing sweep: it leaves the plank's upper line past the
    # knee and closes into the leg's own tip, so the mouth is shut
    return [('M', WEDGE_O), ('C', WEDGE_C1, WEDGE_C2, WEDGE_F)]

def cap_redraw():
    return [('M', (CAP_TIP_X, 61.8)), ('L', (CAP_BACK_X, 61.8))]

# silhouette helpers ---------------------------------------------------------
def reverse_segs(segs):
    walks = []
    cur = segs[0][1]
    for s in segs[1:]:
        if s[0] == 'C':
            walks.append(('C', cur, (s[1], s[2]), s[3]))
            cur = s[3]
        elif s[0] == 'L':
            walks.append(('L', cur, s[1]))
            cur = s[1]
        else:
            break
    out = [('M', cur)]
    for w in reversed(walks):
        if w[0] == 'L':
            out.append(('L', w[1]))
        else:
            out.append(('C', w[2][1], w[2][0], w[1]))
    return out

def book_silhouette():
    body = page()[:-2]  # M(N) .. J fillet exit at t2J
    b2 = body + [('Z', None)]
    mb = mir_segs(b2)
    m_rev = reverse_segs(mb)     # M at the mirrored walk's end
    out = [('M', body[0][1])]
    out += body[1:-0] if False else body[1:]
    # drop m_rev's 'M' and the trailing Z, then close at the start point
    m_rev2 = [s for s in m_rev if s[0] != 'Z'][1:]
    out += m_rev2
    out.append(('Z', None))
    return out

def wedge_face_pts():
    pts = [CROSS, WEDGE_O]
    pts += cub_pts(WEDGE_O, WEDGE_C1, WEDGE_C2, WEDGE_F, 24)
    pts += [CROSS]
    return pts

# ------------------------------------------------------------- flatten / RDP
def flatten(path, step=40):
    pts = []; cur = None
    for seg in path:
        k = seg[0]
        if k == 'M':
            cur = seg[1]; pts.append(cur)
        elif k == 'L':
            a, b = cur, seg[1]
            n = max(2, int(math.dist(a, b) * step))
            for i in range(1, n + 1):
                t = i / n
                pts.append((a[0] + (b[0]-a[0])*t, a[1] + (b[1]-a[1])*t))
            cur = b
        elif k == 'C':
            p0 = cur; c1, c2, p1 = seg[1], seg[2], seg[3]
            pts += cub_pts(p0, c1, c2, p1, step)
            cur = p1
    return pts

def as_pts(x, step=40):
    """accept segments or a polyline."""
    if x and isinstance(x[0][0], str):
        return flatten(x, step)
    return list(x)

def pt_line_dist(p, a, b):
    if math.dist(a, b) < 1e-9:
        return math.dist(p, a)
    return abs((p[0]-a[0])*(b[1]-a[1]) - (p[1]-a[1])*(b[0]-a[0])) / math.dist(a, b)

def rdp(pts, eps):
    if len(pts) < 3:
        return pts[:]
    dmax, idx = 0.0, 0
    for i in range(1, len(pts) - 1):
        d = pt_line_dist(pts[i], pts[0], pts[-1])
        if d > dmax:
            dmax, idx = d, i
    if dmax <= eps:
        return [pts[0], pts[-1]]
    left = rdp(pts[:idx+1], eps)
    right = rdp(pts[idx:], eps)
    return left[:-1] + right

def resample(pts, every):
    out = pts[:1]
    for p in pts[1:]:
        if math.dist(p, out[-1]) >= every:
            out.append(p)
    if math.dist(pts[-1], out[-1]) > 0.06:
        out.append(pts[-1])
    return out

def _dir_at(pts, i):
    a = pts[max(0, i - 1)]; b = pts[min(len(pts) - 1, i + 1)]
    return norm((b[0]-a[0], b[1]-a[1]))

def band_outline(pts, half, cap_pts=10):
    pts = resample(pts, 1.4)
    n = len(pts)
    left, right = [], []
    for i, p in enumerate(pts):
        d = _dir_at(pts, i)
        left.append((p[0] - d[1] * half, p[1] + d[0] * half))
        right.append((p[0] + d[1] * half, p[1] - d[0] * half))
    d0 = _dir_at(pts, 0); d1 = _dir_at(pts, n - 1)
    capE = []
    base = math.atan2(d1[1], d1[0])
    for a in range(cap_pts + 1):
        ang = base + math.pi + math.pi * a / cap_pts
        capE.append((pts[n-1][0] + half * math.cos(ang), pts[n-1][1] + half * math.sin(ang)))
    capS = []
    base = math.atan2(d0[1], d0[0])
    for a in range(cap_pts + 1):
        ang = base + math.pi * a / cap_pts
        capS.append((pts[0][0] + half * math.cos(ang), pts[0][1] + half * math.sin(ang)))
    return right + capE + left[::-1] + capS

def expand_closed(pts, m):
    # offset both directions, keep the one with the larger area (outward)
    def offset(poly, mm):
        out = []
        n = len(poly)
        for i in range(n):
            a = poly[(i - 1) % n]; b = poly[(i + 1) % n]; p = poly[i]
            d1 = norm((p[0]-a[0], p[1]-a[1]))
            d2 = norm((b[0]-p[0], b[1]-p[1]))
            sgn = 1.0 if cross(d1, d2) > 0 else -1.0
            n1 = (-d1[1]*sgn, d1[0]*sgn); n2 = (-d2[1]*sgn, d2[0]*sgn)
            if abs(cross(d1, d2)) < 0.08:
                out.append((p[0] + n1[0]*mm, p[1] + n1[1]*mm))
                continue
            u = line_ix((p[0]+n1[0]*mm, p[1]+n1[1]*mm), d1, (p[0]+n2[0]*mm, p[1]+n2[1]*mm), d2)
            if math.hypot(u[0]-p[0], u[1]-p[1]) > 3.5*mm:
                s = n1[0]+n2[0], n1[1]+n2[1]
                ln = math.hypot(*s)
                u = (p[0] + s[0]/ln*mm/abs(math.sin(math.radians(60))) * 0.7,
                     p[1] + s[1]/ln*mm/abs(math.sin(math.radians(60))) * 0.7)
            out.append(u)
        return out
    A = area(pts)
    pa = offset(pts, m)
    pb = offset(pts, -m)
    if area(pa) > A and area(pa) > area(pb):
        out = pa
    elif area(pb) > A and area(pb) > area(pa):
        out = pb
    else:
        out = pa
    return rdp(out, 0.18)

def area(pts):
    s = 0.0
    for i in range(len(pts)):
        a = pts[i]; b = pts[(i+1) % len(pts)]
        s += a[0]*b[1] - b[0]*a[1]
    return abs(s) / 2

# ---------------------------------------------------------------- svg strings
def fmt(v):
    s = f'{v:.3f}'.rstrip('0').rstrip('.')
    return '0' if s in ('', '-0') else s

def tr(p, scale, cx, cy):
    return (cx + (p[0]-AX_X)*scale, cy + (p[1]-AX_Y)*scale)

def svg_segs(segs, scale, cx, cy):
    out = []
    for s in segs:
        if s[0] == 'C':
            a = tr(s[1], scale, cx, cy); b = tr(s[2], scale, cx, cy); e = tr(s[3], scale, cx, cy)
            out.append('C' + fmt(a[0])+','+fmt(a[1]) + ' ' + fmt(b[0])+','+fmt(b[1]) + ' ' + fmt(e[0])+','+fmt(e[1]))
        elif s[0] == 'L':
            p = tr(s[1], scale, cx, cy)
            out.append('L' + fmt(p[0])+','+fmt(p[1]))
        elif s[0] == 'M':
            p = tr(s[1], scale, cx, cy)
            out.append('M' + fmt(p[0])+','+fmt(p[1]))
        elif s[0] == 'Z':
            out.append('Z')
    return ' '.join(out)

def svg_poly(pts, scale, cx, cy, close=True):
    s = [fmt(tr(p, scale, cx, cy)[0]) + ',' + fmt(tr(p, scale, cx, cy)[1]) for p in pts]
    return 'M' + ' L'.join(s) + (' Z' if close else '')

# ------------------------------------------------------------------ fg layers
def fg_pieces():
    pl = plank(); pm = mir_segs(pl)
    wa = wedge_arc(); wb = mir_segs(wa)
    pc = page(); pcM = mir_segs(pc)
    ca = cap_redraw(); cb = mir_segs(ca)
    return dict(pl=pl, pm=pm, wa=wa, wb=wb, pc=pc, pcM=pcM, ca=ca, cb=cb)

# the themed silhouette's own weights: the band's half-width and the margin
# of air cut around the book, both in trace units, tuned against the old set
MONO_BAND = 7.6
MONO_CUT  = 7.0

def mono_shapes():
    p = fg_pieces()
    cla = as_pts(p['pl'], 60)
    bandA = band_outline(cla, MONO_BAND)
    bandB = band_outline([mirp(x) for x in cla], MONO_BAND)
    cut = expand_closed(as_pts(book_silhouette(), 24), MONO_CUT)
    faceL = wedge_face_pts()
    faceR = [mirp(x) for x in faceL]
    book = as_pts(book_silhouette(), 40)
    return dict(bandA=bandA, bandB=bandB, cut=cut, faceL=faceL, faceR=faceR, book=book)

def mono_pathdata():
    d = mono_shapes()
    scale, cx, cy = S_FG, 54.0, 54.0
    bandA = svg_poly(d['bandA'], scale, cx, cy) + ' ' + svg_poly(d['cut'], scale, cx, cy)
    bandB = svg_poly(d['bandB'], scale, cx, cy) + ' ' + svg_poly(d['cut'], scale, cx, cy)
    faces = svg_poly(d['faceL'], scale, cx, cy) + ' ' + svg_poly(d['faceR'], scale, cx, cy)
    book  = svg_segs(book_silhouette(), scale, cx, cy)
    return bandA, bandB, faces, book

def fg_xml(note):
    scale, cx, cy = S_FG, 54.0, 54.0
    p = fg_pieces()
    halo_w = STROKE + 2 * HALO
    parts = []
    def P(fill, stroke, sw, data):
        attrs = []
        if fill: attrs.append(f'android:fillColor="{fill}"')
        if stroke:
            attrs.append(f'android:strokeColor="{stroke}" android:strokeWidth="{fmt(sw)}" '
                         'android:strokeLineJoin="round" android:strokeLineCap="round"')
        attrs.append('android:pathData="%s"' % data)
        return '    <path\n        ' + '\n        '.join(attrs) + ' />'
    for k in ('pl', 'pm', 'wa', 'wb'):
        parts.append(P(None, '@color/icon_gold', STROKE, svg_segs(p[k], scale, cx, cy)))
    for k in ('pc', 'pcM'):
        parts.append(P('@color/paper', '@color/paper', halo_w, svg_segs(p[k], scale, cx, cy)))
    for k in ('pc', 'pcM'):
        parts.append(P('@color/paper', '@color/icon_ink', STROKE, svg_segs(p[k], scale, cx, cy)))
    for k in ('ca', 'cb'):
        parts.append(P(None, '@color/icon_gold', STROKE, svg_segs(p[k], scale, cx, cy)))
    body = '\n'.join(parts)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!--\n  ' + note + '\n-->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            + body + '\n</vector>\n')

def mono_xml(note):
    bandA, bandB, faces, book = mono_pathdata()
    parts = []
    for d in (bandA, bandB):
        parts.append('    <path\n        android:fillColor="#000000"\n        android:fillType="evenOdd"\n        android:pathData="%s" />' % d)
    parts.append('    <path\n        android:fillColor="#000000"\n        android:pathData="%s" />' % faces)
    parts.append('    <path\n        android:fillColor="#000000"\n        android:pathData="%s" />' % book)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!--\n  ' + note + '\n-->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            + '\n'.join(parts) + '\n</vector>\n')

# ------------------------------------------------------------------- rasters
SS = 4

def stroke_pts(draw, pts, color, width_px, cap=True):
    if len(pts) > 1:
        draw.line(pts, fill=color, width=width_px, joint='curve')
        if cap:
            r = width_px / 2
            for e in (pts[0], pts[-1]):
                draw.ellipse([e[0] - r, e[1] - r, e[0] + r, e[1] + r], fill=color)

def fill_pts(draw, pts, color):
    if len(pts) > 2:
        draw.polygon(pts, fill=color)

def raster_canvas(size, ss=SS):
    return Image.new('RGBA', (size*ss, size*ss), (0, 0, 0, 0))

def draw_fg(im, scale, cx, cy, ss=SS, dy=0.0):
    """draw the full fg layer set on any canvas."""
    p = fg_pieces()
    dr = ImageDraw.Draw(im)
    halo_w = STROKE + 2 * HALO
    def m(raw, off=0.0):
        return [(cx + (q[0]-AX_X)*scale*ss, cy + (q[1]-AX_Y+off)*scale*ss) for q in as_pts(raw, 60)]
    lw = STROKE * scale * ss
    for k in ('pl', 'pm', 'wa', 'wb'):
        stroke_pts(dr, m(p[k]), GOLD, max(1, round(lw)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, PAPER, max(1, round(halo_w*scale*ss)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, INK, max(1, round(lw)))
    for k in ('ca', 'cb'):
        stroke_pts(dr, m(p[k]), GOLD, max(1, round(lw)))
    return im

def draw_mono(im, scale, cx, cy, ycen, ss=SS, color=WHITE):
    d = mono_shapes()
    dr = ImageDraw.Draw(im)
    def m(raw):
        return [(cx + (q[0]-AX_X)*scale*ss, cy + (q[1]-ycen)*scale*ss) for q in raw]
    cut = m(d['cut'])
    empty = (0, 0, 0, 0)
    for k in ('bandA', 'bandB'):
        fill_pts(dr, m(d[k]), color)
        fill_pts(dr, cut, empty)
    for k in ('faceL', 'faceR'):
        fill_pts(dr, m(d[k]), color)
    fill_pts(dr, m(d['book']), color)
    return im

def trace_bbox(items):
    xs, ys = [], []
    for it in items:
        for p in as_pts(it, 40):
            xs.append(p[0]); ys.append(p[1])
    return min(xs), min(ys), max(xs), max(ys)

def paper_bg(im, color=PAPER):
    d = ImageDraw.Draw(im)
    w, h = im.size
    opaque = (color[0], color[1], color[2], 255)
    d.rectangle([0, 0, w, h], fill=opaque)
    return im

# ------------------------------------------------------------------- surfaces
def fit_for(item_lists, box, span_w=None, ycen=None):
    """scale + offsets so the items' ink bbox centered lands on box center."""
    x0, y0, x1, y1 = trace_bbox(item_lists)
    w = x1 - x0; h = y1 - y0
    if span_w:
        scale = span_w / w
    else:
        scale = min(2*box[2] / w, 2*box[3] / h)
    if ycen is None:
        ycen = (y0 + y1) / 2
    return scale, (x0+x1)/2, ycen

def render_store(size=512, out=None, ss=4):
    im = raster_canvas(size, ss)
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    scale, xc, yc = fit_for(ids, None, span_w=size*0.498)
    paper_bg(im)
    draw_fg(im, scale, size/2*ss - (xc-AX_X)*scale*ss, size/2*ss + (AX_Y - yc)*scale*ss, ss)
    if out:
        im.convert('RGB').resize((size, size), Image.LANCZOS).save(out)
    return im

DENS = [('mdpi', 48, 48), ('hdpi', 72, 72), ('xhdpi', 96, 96),
        ('xxhdpi', 144, 144), ('xxxhdpi', 192, 192)]



def legacy_png(size, out=None):
    im = raster_canvas(size, 4)
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    scale, xc, yc = fit_for(ids, None, span_w=size*0.495)
    paper_bg(im)
    draw_fg(im, scale, size/2*4 - (xc-AX_X)*scale*4, size/2*4 + (AX_Y-yc)*scale*4, 4)
    im.convert('RGB').resize((size, size), Image.LANCZOS).save(out)

def share_png(size, out=None):
    im = raster_canvas(size, 4)
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    scale, xc, yc = fit_for(ids, None, span_w=size*0.855)
    dr = ImageDraw.Draw(im)
    ink_w = 3.59*scale      # the mark's own stroke, in trace units
    halo_w = (3.59 + 2*5.1) * scale
    cx = size/2.0*4 - (xc-AX_X)*scale*4
    cy = size/2.0*4 + (AX_Y - yc)*scale*4
    lw = ink_w*4
    def m(raw):
        return [(cx + (q[0]-AX_X)*scale*4, cy + (q[1]-AX_Y)*scale*4) for q in as_pts(raw, 60)]
    for k in ('pl', 'pm', 'wa', 'wb', 'ca', 'cb'):
        stroke_pts(dr, m(p[k]), WHITE, max(2, round(halo_w*4)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, WHITE); stroke_pts(dr, pts, WHITE, max(2, round(halo_w*4)))
    for k in ('pl', 'pm', 'wa', 'wb', 'ca', 'cb'):
        stroke_pts(dr, m(p[k]), GOLD, max(1, round(lw)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, INK, max(1, round(lw)))
    im2 = im.resize((size, size), Image.LANCZOS)
    if out:
        im2.save(out)
    return im2

def daily_png(size, out=None):
    im = raster_canvas(size, 4)
    scale, xc, yc = fit_for(daily_ids(), None, span_w=size*0.916)
    draw_mono(im, scale, size/2*4 - (xc-AX_X)*scale*4, size/2*4, yc, 4, WHITE)
    im2 = im.resize((size, size), Image.LANCZOS)
    if out:
        im2.save(out)
    return im2

def daily_ids():
    return [plank(), mir_segs(plank()), wedge_arc(), mir_segs(wedge_arc()), book_silhouette()]

# --------------------------------------------------------------------- banner
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LITERATA = os.path.join(ROOT, 'content-assets/src/main/res/font/literata_variable.ttf')
INTERF    = os.path.join(ROOT, 'content-assets/src/main/res/font/inter_variable.ttf')

def _font(path, size, weight=None):
    try:
        f = ImageFont.truetype(path, size)
        if weight:
            try:
                f.set_variation_by_name(weight)
            except Exception:
                pass
        return f
    except Exception:
        return None

def banner_png(out=None, ss=1):
    W, H = 1024, 500
    big = (W*ss, H*ss)
    im = Image.new('RGBA', big, (0, 0, 0, 0))
    paper_bg(im)
    dr = ImageDraw.Draw(im)
    # the mark as a seal over the name, ink width 196, centered on the axis
    # at the banner's optical center (512, 176)
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    scale, xc, yc = fit_for(ids, None, span_w=196.0)
    cx, cy = 512*ss, 176*ss + (AX_Y-yc)*scale*ss
    s = ss
    lw = STROKE*scale
    def m(raw, step=60):
        return [(cx + (q[0]-AX_X)*scale*s, cy + (q[1]-AX_Y)*scale*s) for q in as_pts(raw, step)]
    for k in ('pl', 'pm', 'wa', 'wb'):
        stroke_pts(dr, m(p[k]), GOLD, max(1, round(lw*s)))
    halo_w = (STROKE + 2*HALO)*scale
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, PAPER, max(1, round(halo_w*s)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, INK, max(1, round(lw*s)))
    for k in ('ca', 'cb'):
        stroke_pts(dr, m(p[k]), GOLD, max(1, round(lw*s)))
    # the bookmaker's double rule: a fine line at the margin, the page's own
    # frame inside it
    dr.rectangle([58*ss, 54*ss, 966*ss, 446*ss], outline=HAIR, width=max(1, round(1.4*ss)))
    fr = [80*ss, 76*ss, 944*ss, 424*ss]
    dr.rectangle(fr, outline=HAIR, width=max(1, round(2.2*ss)))
    # texts
    im2 = banner_text(im)
    if out:
        im2.convert('RGB').resize((W, H), Image.LANCZOS).save(out)
    return im2

def banner_text(im):
    # the name and the caption as one centered stack, the gaps between them
    # held tight so the caption never crowds the frame's foot: the title's ink
    # ends 367, the rule sits 9 under it, and the caption's ink ends 401 with
    # 23 of air above the inner frame at 424 (measured at ss=1)
    dr = ImageDraw.Draw(im)
    s = im.size[0] // 1024
    title = 'Quran'
    f = _font(LITERATA, 120*s, 'Regular')
    bb = _inner_bbox(title, f)
    size = int(round(120*s * 300.0/max(1, bb[2]-bb[0])))
    f = _font(LITERATA, size, 'Regular')
    bb = _inner_bbox(title, f)
    dr.text((512*s - (bb[0]+bb[2])/2, 284*s - bb[1]), title, font=f, fill=INK)
    # the short gold rule between the name and the caption
    dr.rectangle([467*s, 376*s, 557*s, 379*s], fill=HAIR)
    # the caption in tracked small caps, centered on the axis
    cap = 'THE NOBLE BOOK'
    f2 = _font(INTERF, 21*s, 'Regular')
    track = 5.0*s
    widths = [dr.textlength(c, font=f2) for c in cap]
    x = 512*s - (sum(widths) + track*(len(cap)-1)) / 2
    for c, w in zip(cap, widths):
        dr.text((x, 386*s), c, font=f2, fill=MUTED)
        x += w + track
    return im

def _inner_bbox(text, font):
    tmp = Image.new('L', (2400, 600))
    d = ImageDraw.Draw(tmp)
    d.text((80, 80), text, font=font, fill=255)
    b = tmp.point(lambda v: 255 if v > 60 else 0).getbbox()
    return (b[0]-80, b[1]-80, b[2]-80, b[3]-80) if b else (0, 0, 0, 0)

# ------------------------------------------------------------------- preview
def preview():
    """renders surface families of the current drawing into TEMP, for the
    owner's own eyes: nothing is written into the repository here."""
    out = os.environ.get('TEMP', r'C:/Users/zn/AppData/Local/Temp')
    def W(name):
        return os.path.join(out, name)
    # the mark on paper, with an optional reference sheet beside it when the
    # session carries one: point QURAN_MARK_REFERENCE at the source image
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    panels = []
    overlays = os.environ.get('QURAN_MARK_REFERENCE')
    if overlays and os.path.exists(overlays):
        ref = Image.open(overlays).convert('RGB')
        ref = ref.crop((0, 40, ref.width // 3, 196)).resize((512, 394), Image.LANCZOS)
        panels.append(ref)
    scale, xc, yc = fit_for(ids, None, span_w=512*0.495)
    im = raster_canvas(512, 4)
    paper_bg(im)
    draw_fg(im, scale, 1024 - (xc-AX_X)*scale*4, 1024 + (AX_Y-yc)*scale*4, 4)
    panels.append(im.convert('RGB').resize((512, 512), Image.LANCZOS))
    sheet = Image.new('RGB', (512*len(panels) + 16*(len(panels)+1), 536), (230, 230, 235))
    x = 16
    for t in panels:
        sheet.paste(t, (x, 12)); x += 512 + 16
    sheet.save(W('mark_preview.png'))
    # small sizes
    tiles = []
    for size in (192, 96, 48, 24):
        small = raster_canvas(size, 8)
        s2, xc2, yc2 = fit_for(ids, None, span_w=size*0.495)
        paper_bg(small)
        draw_fg(small, s2, size/2*8 - (xc2-AX_X)*s2*8, size/2*8 + (AX_Y-yc2)*s2*8, 8)
        tiles.append(small.resize((size, size), Image.LANCZOS).convert('RGB'))
    cols = Image.new('RGB', (sum(t.size[0] for t in tiles) + 30, 200), (230, 230, 235))
    x = 10
    for t in tiles:
        cols.paste(t, (x, 10)); x += t.size[0] + 30
    cols.save(W('mark_small.png'))
    # the silhouette, the share mark, the legacy tile, the banner
    s3, xc3, yc3 = fit_for(daily_ids(), None, span_w=256*0.9)
    mono = raster_canvas(256, 8)
    draw_mono(mono, s3, 128*8 - (xc3-AX_X)*s3*8, 128*8, yc3, 8, INK)
    mono.convert('RGB').resize((256, 256), Image.LANCZOS).save(W('mark_mono.png'))
    share_png(96, W('mark_share.png'))
    legacy_png(192, W('mark_legacy.png'))
    banner_png(out=W('mark_banner.png'), ss=2)
    print('previews written:', out)

def emit():
    root = ROOT
    fg_note = ('The app\'s mark: an open book on its rehal, redrawn from the\n'
               '  owner\'s reference as uniform line art: the pages are sail arcs\n'
               '  diving into a spine that hangs a short tail; each plank folds\n'
               '  onto the page\'s edge, runs down beside the book, and its lower\n'
               '  end fans into a closed wedge foot. The book is in the theme\'s\n'
               '  ink on the theme\'s paper, the stand in the ornament gold; the\n'
               '  paper strokes keep air between the stand and the book\'s edges.')
    mono_note = ('The themed launcher layer: the same book on its stand as a solid\n'
                 '  silhouette, one band per plank, each lower end closed as a wedge,\n'
                 '  with the margin around the book cut out of the bands so the shape\n'
                 '  reads at 24 dp.')
    open(os.path.join(root, 'app/src/main/res/drawable/ic_launcher_foreground.xml'), 'w', newline='\n').write(fg_xml(fg_note))
    open(os.path.join(root, 'app/src/main/res/drawable/ic_launcher_monochrome.xml'), 'w', newline='\n').write(mono_xml(mono_note))
    for name, size, _ in DENS:
        legacy_png(size, os.path.join(root, f'app/src/main/res/mipmap-{name}/ic_launcher.png'))
        share_png(size // 2, os.path.join(root, f'app/src/main/res/drawable-{name}/ic_share_mark.png'))
        daily_png(size // 2, os.path.join(root, f'app/src/main/res/drawable-{name}/ic_daily.png'))
    render_store(512, os.path.join(root, 'play-store/icon-512.png'))
    banner_png(out=os.path.join(root, 'play-store/feature-graphic-1024x500.png'), ss=2)
    print('emitted')

if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'emit':
        emit()
    else:
        preview()
