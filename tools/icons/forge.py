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
# ic_launcher_monochrome.xml, ic_shortcut_mark.xml, the five mipmap legacy
# tiles, ic_share_mark and ic_book_mark at mdpi through xxxhdpi,
# play-store/icon-512.png, and play-store/feature-graphic-1024x500.png.
# The icon is rebuilt with the repo's own ink on its paper field; the feature
# graphic is the lapis reading card, set in the repo's own Literata and
# carrying a real capture from the committed tablet frames.
# The drawing traces the owner's reference set: the pages are sail arcs
# diving into a spine that ends at the V where the two pages meet, each
# plank folds onto the page's outer edge, and each lower end meets the
# closing arc at one shared foot tip.

import math, os, sys
from PIL import Image, ImageDraw, ImageFont

INK   = (41, 38, 34)      # #292622
GOLD  = (133, 110, 24)    # #856E18
PAPER = (251, 249, 244)   # #FBF9F4
LAPIS = (31, 61, 118)     # #1F3D76
NIGHT_GOLD = (220, 184, 112)  # #DCB870, the night page's gold
MUTED = (93, 90, 85)      # #5D5A55
WHITE = (255, 255, 255)

# ------------------------------------------------------------ trace geometry
AX_X, AX_Y = 85.5, 98.5
S_FG   = 0.415
# The standalone drawing's own hand: the mark at 210 px carries the icon's
# weight and the icon's air, so the card and the icon are one drawing seen at
# two sizes, not two weights of the same drawing.
STROKE = 3.0
HALO   = 1.4
# The icon's own line, in trace units: one weight and one ink for the
# launcher's layers, the legacy tiles, and the store icon. The launcher
# vector had been taking STROKE as canvas units, 2.4 times heavier than the
# rasters' STROKE * S_FG, and the gold stand at 6.1:1 against the paper was
# the first thing lost from the small sizes; the drawing keeps its geometry
# and the icon keeps one ink at 14.3:1, while the feature graphic keeps the
# ornament gold it draws well at its own size (owner decision, this
# session).
# The line weight. 2.6 was the drawing's own hand and read well at 512, but
# the adaptive icon is rasterized from 72 dp up, and at the bottom of that
# range 2.6 units is one pixel of gray. 3.2 holds from 72 px to 512 and still
# keeps the sail curves; 4.4 flattens them (owner decision, the any-size
# session).
STROKE_ICON = 3.8
# The air around the book: the paper stroke that keeps the stand's line clear
# of the page's edge, so the two never touch. 1.9 left the mark reading as a
# diagram of two parts; 1.4 seats the book in its stand as one object, and it
# stays 1.4 when the line grows, which seats it tighter still (owner
# decision, the feet session).
HALO_ICON   = 1.4
STROKE_CANVAS = STROKE_ICON * S_FG
HALO_CANVAS   = (STROKE_ICON + 2 * HALO_ICON) * S_FG
# The store and legacy tiles put the mark at 64 percent of the tile. The
# previous 60 percent left the drawing quieter than the adaptive foreground,
# whose visible book fills about 66 percent of the 72 dp viewport; 64 is the
# one step that reaches that presence without crowding a mask (owner
# decision, 22.9).
ICON_SPAN = 0.64

PAGE_N   = (85.5, 63.2)
PAGE_P   = (56.5, 52.15)
PAGE_E1  = (39.6, 57.4)
PAGE_E2  = (38.6, 101.4)
PAGE_J   = (85.5, 113.7)

CROSS     = (85.5, 128.9)   # where the two planks cross
# The foot is one point, not two. The leg used to end at (143.8, 148.8) and
# the closing sweep at (28.5, 149.0): 1.2 trace units apart, which is half
# the line's own width, so each round cap poked a small beak past the joint
# and the mark grew a spur at both feet. Both strokes now stop where their
# centerlines cross, and the corner stays (owner report, the feet session).
FOOT_TIP  = (28.354, 148.406)
PLANK_END = (2 * AX_X - FOOT_TIP[0], FOOT_TIP[1])   # the mirrored foot's corner
WEDGE_O   = (54.15, 118.25)
# the closing sweep lands on the plank's own tip, so each foot is one clean
# tapered face with no open mouth: the sliver between arc and leg was the
# only spot of the mark that turned to mud at home-screen sizes
WEDGE_C1  = (44.0, 126.0)
WEDGE_C2  = (25.6, 137.4)
WEDGE_F   = FOOT_TIP

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

# The themed silhouette's own weights: the band's half width, the clean
# negative space around the book, and the polygon tolerance of the stand.
# The old cutout was an offset of a densely sampled silhouette, which left a
# 41 KB path of stair steps and hairlines down the book's edges. A clean
# scaled copy of the book's own cubics keeps the same air at 24 dp and reads
# as one shape at every size (owner decision, 22.9).
MONO_BAND = 7.6
MONO_CUT_SCALE = 1.18
MONO_CUT_CENTER = (AX_X, 100.0)
MONO_BAND_RDP = 0.12

def scale_trace_path(segs, factor, center):
    """Scale every move, line, and cubic about center, preserving closes."""
    cx, cy = center
    def scaled(p):
        return (cx + (p[0] - cx) * factor, cy + (p[1] - cy) * factor)
    out = []
    for seg in segs:
        if seg[0] == 'C':
            out.append(('C', scaled(seg[1]), scaled(seg[2]), scaled(seg[3])))
        elif seg[0] in ('M', 'L'):
            out.append((seg[0], scaled(seg[1])))
        else:
            out.append(seg)
    return out

def mono_shapes():
    p = fg_pieces()
    cla = as_pts(p['pl'], 60)
    bandA = rdp(band_outline(cla, MONO_BAND), MONO_BAND_RDP)
    bandB = rdp(
        band_outline([mirp(x) for x in cla], MONO_BAND),
        MONO_BAND_RDP,
    )
    cut_segs = scale_trace_path(
        book_silhouette(),
        MONO_CUT_SCALE,
        MONO_CUT_CENTER,
    )
    # The raster fill still needs points; the vector keeps the clean cubics.
    cut = as_pts(cut_segs, 60)
    faceL = wedge_face_pts()
    faceR = [mirp(x) for x in faceL]
    book = as_pts(book_silhouette(), 40)
    return dict(
        bandA=bandA,
        bandB=bandB,
        cut=cut,
        cut_segs=cut_segs,
        faceL=faceL,
        faceR=faceR,
        book=book,
    )

def mono_pathdata():
    d = mono_shapes()
    scale, cx, cy = S_FG, 54.0, 54.0
    bandA = svg_poly(d['bandA'], scale, cx, cy) + ' ' + svg_segs(d['cut_segs'], scale, cx, cy)
    bandB = svg_poly(d['bandB'], scale, cx, cy) + ' ' + svg_segs(d['cut_segs'], scale, cx, cy)
    faces = svg_poly(d['faceL'], scale, cx, cy) + ' ' + svg_poly(d['faceR'], scale, cx, cy)
    book  = svg_segs(book_silhouette(), scale, cx, cy)
    return bandA, bandB, faces, book

def fg_xml(note):
    return mark_xml(note, 108, S_FG, 54.0, 54.0, STROKE_CANVAS, HALO_CANVAS)


def mark_xml(note, dp, scale, cx, cy, stroke, halo_w):
    """The same mark on any canvas: the launcher's 108 dp and the shortcut
    door's 48 dp are one drawing at two scales, never two drawings."""
    p = fg_pieces()
    parts = []
    def P(fill, stroke_c, sw, data):
        attrs = []
        if fill: attrs.append('android:fillColor="%s"' % fill)
        if stroke_c:
            attrs.append('android:strokeColor="%s" android:strokeWidth="%s" '
                         'android:strokeLineJoin="round" android:strokeLineCap="round"'
                         % (stroke_c, fmt(sw)))
        attrs.append('android:pathData="%s"' % data)
        return '    <path\n        ' + '\n        '.join(attrs) + ' />'
    for k in ('pl', 'pm', 'wa', 'wb'):
        parts.append(P(None, '@color/icon_ink', stroke, svg_segs(p[k], scale, cx, cy)))
    for k in ('pc', 'pcM'):
        parts.append(P('@color/paper', '@color/paper', halo_w, svg_segs(p[k], scale, cx, cy)))
    for k in ('pc', 'pcM'):
        parts.append(P('@color/paper', '@color/icon_ink', stroke, svg_segs(p[k], scale, cx, cy)))
    for k in ('ca', 'cb'):
        parts.append(P(None, '@color/icon_ink', stroke, svg_segs(p[k], scale, cx, cy)))
    body = '\n'.join(parts)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!--\n  ' + note + '\n-->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="%ddp" android:height="%ddp"\n'
            '    android:viewportWidth="%d" android:viewportHeight="%d">\n' % (dp, dp, dp, dp)
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

def stroke_icon_pts(draw, pts, color, width_px, cap=True):
    """Draw the icon's own line with explicit round joins and caps.

    Pillow's joint='curve' samples a dense joint path, and on the page's
    long shallow arcs that sampling left light notches and hairlines in the
    finished tile. Circles at the joins keep the line's one weight and give
    the curves a clean edge at every size (owner decision, 22.9).
    """
    if len(pts) <= 1:
        return
    draw.line(pts, fill=color, width=width_px)
    radius = width_px / 2
    joins = pts if cap else pts[1:-1]
    for x, y in joins:
        draw.ellipse(
            [x - radius, y - radius, x + radius, y + radius],
            fill=color,
        )

def fill_pts(draw, pts, color):
    if len(pts) > 2:
        draw.polygon(pts, fill=color)

def raster_canvas(size, ss=SS):
    return Image.new('RGBA', (size*ss, size*ss), (0, 0, 0, 0))

def draw_fg(im, scale, cx, cy, ss=SS, dy=0.0, stroke=None, halo=None, stand=None):
    """draw the full fg layer set on any canvas; the icon's surfaces pass the
    icon's one weight and ink, the banner keeps the defaults."""
    stroke = STROKE if stroke is None else stroke
    halo = HALO if halo is None else halo
    stand = GOLD if stand is None else stand
    p = fg_pieces()
    dr = ImageDraw.Draw(im)
    halo_w = stroke + 2 * halo
    def m(raw, off=0.0):
        return [(cx + (q[0]-AX_X)*scale*ss, cy + (q[1]-AX_Y+off)*scale*ss) for q in as_pts(raw, 60)]
    lw = stroke * scale * ss
    for k in ('pl', 'pm', 'wa', 'wb'):
        stroke_icon_pts(dr, m(p[k]), stand, max(1, round(lw)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_icon_pts(dr, pts, PAPER, max(1, round(halo_w*scale*ss)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_icon_pts(dr, pts, INK, max(1, round(lw)))
    for k in ('ca', 'cb'):
        stroke_icon_pts(dr, m(p[k]), stand, max(1, round(lw)))
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
    scale, xc, yc = fit_for(ids, None, span_w=size*ICON_SPAN)
    paper_bg(im)
    draw_fg(im, scale, size/2*ss - (xc-AX_X)*scale*ss, size/2*ss + (AX_Y - yc)*scale*ss, ss,
            stroke=STROKE_ICON, halo=HALO_ICON, stand=INK)
    result = im.convert('RGBA').resize((size, size), Image.LANCZOS)
    if out:
        result.save(out)
    return result

DENS = [('mdpi', 48, 48), ('hdpi', 72, 72), ('xhdpi', 96, 96),
        ('xxhdpi', 144, 144), ('xxxhdpi', 192, 192)]

# The optical ladder. Even at 3.2, a legacy tile at 48 is under a pixel, so the
# two smallest tiles take more weight and more air; 96 and up are the base
# weight, and the geometry never moves. The launcher's vector and the store icon
# keep one weight because the vector is rasterized at 72 dp and up and cannot
# change weight per density (owner decision, the any-size session).
OPTICAL = {48: (4.6, 2.3), 72: (3.8, 1.9)}


def optical(size):
    return OPTICAL.get(size, (STROKE_ICON, HALO_ICON))


def legacy_png(size, out=None):
    im = raster_canvas(size, 4)
    p = fg_pieces()
    ids = [p[k] for k in ('pl', 'pm', 'wa', 'wb', 'pc', 'pcM')]
    scale, xc, yc = fit_for(ids, None, span_w=size*ICON_SPAN)
    paper_bg(im)
    stroke, halo = optical(size)
    draw_fg(im, scale, size/2*4 - (xc-AX_X)*scale*4, size/2*4 + (AX_Y-yc)*scale*4, 4,
            stroke=stroke, halo=halo, stand=INK)
    result = im.convert('RGB').resize((size, size), Image.LANCZOS)
    if out:
        result.save(out)
    return result

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
        stroke_pts(dr, m(p[k]), INK, max(1, round(lw)))
    for k in ('pc', 'pcM'):
        pts = m(p[k]); fill_pts(dr, pts, PAPER); stroke_pts(dr, pts, INK, max(1, round(lw)))
    im2 = im.resize((size, size), Image.LANCZOS)
    if out:
        im2.save(out)
    return im2

def book_mark_png(size, out=None):
    im = raster_canvas(size, 4)
    scale, xc, yc = fit_for(book_mark_ids(), None, span_w=size*0.916)
    draw_mono(im, scale, size/2*4 - (xc-AX_X)*scale*4, size/2*4, yc, 4, WHITE)
    im2 = im.resize((size, size), Image.LANCZOS)
    if out:
        im2.save(out)
    return im2

def book_mark_ids():
    return [plank(), mir_segs(plank()), wedge_arc(), mir_segs(wedge_arc()), book_silhouette()]

# --------------------------------------------------------------------- banner
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LITERATA = os.path.join(ROOT, 'content-assets/src/main/res/font/literata_variable.ttf')
INTERF    = os.path.join(ROOT, 'content-assets/src/main/res/font/inter_variable.ttf')
READING_FRAME = os.path.join(ROOT, 'play-store/frames/tablet10/01-reading.png')
STORE_ICON = os.path.join(ROOT, 'play-store/icon-512.png')
# Ayat al-Kursi's ink inside the ten inch capture, its mark's own ink box, and
# the column the verse may not leave: the 16:9 centre crop keeps 68 to 956, so
# the verse is held inside 856 and no line is ever cut (owner decision, 22.9).
BANNER_VERSE_BOX = (672, 336, 1888, 744)
BANNER_VERSE_WIDTH = 856
BANNER_MARK_BOX = (86, 113, 426, 399)
BANNER_MARK_HEIGHT = 50
BANNER_LOCK_TOP = 20
BANNER_NAME_SIZE = 40
BANNER_CAPTION = 'THE NOBLE BOOK'

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

def ink_alpha(crop, floor=246, gain=1.75, cut=10):
    """A captured page's luminance as ink, with nothing re-set or reshaped.

    The ten inch frame already holds the QPC Hafs print, its diacritics, its
    tajweed shapes, and the closing medallion. Reading each pixel's luminance
    into one alpha field lets the banner paint the verse in the theme's own
    ink while every glyph keeps the shape the app drew (owner decision, 22.9).
    """
    alpha = Image.new('L', crop.size)
    src = crop.load()
    dst = alpha.load()
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b = src[x, y]
            luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            value = round(max(0, min(255, (floor - luminance) * gain)))
            dst[x, y] = 0 if value < cut else value
    return alpha

def banner_lockup(canvas, draw):
    """The colophon: the app's own mark beside the app's own name, one line.

    The store graphic is a printed page, so its head is set the way a page's
    head is: the mark and the name at one optical size, the book's own name
    tracked beneath, and a single flat gold rule. Nothing here is a copy of a
    Material header; it is the icon's drawing and the theme's type
    (owner decision, 22.9).
    """
    icon = Image.open(STORE_ICON).convert('RGB')
    mark = icon.crop(BANNER_MARK_BOX)
    title = _font(LITERATA, BANNER_NAME_SIZE, 'Regular')
    if title is None:
        raise RuntimeError('the display typeface is missing: ' + LITERATA)
    cap = _font(INTERF, 12, 'Regular')
    if cap is None:
        raise RuntimeError('the label typeface is missing: ' + INTERF)

    bbox = draw.textbbox((0, 0), 'Quran', font=title)
    name_w, name_h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    mark_h = BANNER_MARK_HEIGHT
    mark_w = round(mark.width * mark_h / mark.height)
    gap = 18
    left = (canvas.width - (mark_w + gap + name_w)) / 2
    mark = mark.resize((mark_w, mark_h), Image.LANCZOS)
    canvas.paste(mark, (round(left), round(BANNER_LOCK_TOP + (name_h - mark_h) / 2)))
    draw.text(
        (left + mark_w + gap - bbox[0], BANNER_LOCK_TOP - bbox[1]),
        'Quran',
        font=title,
        fill=INK,
    )

    cap_top = BANNER_LOCK_TOP + name_h + 16
    track = 5.0
    widths = [draw.textlength(c, font=cap) for c in BANNER_CAPTION]
    total = sum(widths) + track * (len(BANNER_CAPTION) - 1)
    x = (canvas.width - total) / 2
    for c, w in zip(BANNER_CAPTION, widths):
        draw.text((x, cap_top), c, font=cap, fill=(120, 104, 72))
        x += w + track

    rule_top = cap_top + 20
    draw.rectangle(
        (canvas.width / 2 - 34, rule_top, canvas.width / 2 + 34, rule_top + 2),
        fill=GOLD,
    )
    return rule_top

def banner_png(out=None):
    """The page is the store's face: Ayat al-Kursi, set as a reading.

    The graphic is one printed page, not a card of the app. The head is the
    colophon and the body is the whole Ayat al-Kursi at the width Play's
    commonest crop keeps, in the theme's ink on the theme's paper, with no
    gradient, panel, device, or slogan anywhere: the Book is the promise and
    the verse is the proof (owner decision, 22.9).
    """
    W, H = 1024, 500
    canvas = Image.new('RGB', (W, H), PAPER)
    draw = ImageDraw.Draw(canvas)
    rule_top = banner_lockup(canvas, draw)

    if not os.path.isfile(READING_FRAME):
        raise RuntimeError('the committed reading frame is missing: ' + READING_FRAME)
    with Image.open(READING_FRAME) as opened:
        source = opened.convert('RGB')
    if source.size != (2560, 1800):
        raise RuntimeError(
            'the committed reading frame is %dx%d, not 2560x1800: %s'
            % (source.size[0], source.size[1], READING_FRAME)
        )
    crop = source.crop(BANNER_VERSE_BOX)
    if crop.size != (1216, 408):
        raise RuntimeError('the Ayat al-Kursi crop is not 1216x408')

    band_top = rule_top + 20
    fit = min(BANNER_VERSE_WIDTH / crop.width, (H - band_top - 22) / crop.height)
    width = int(crop.width * fit)
    height = int(crop.height * fit)
    x = (W - width) // 2
    y = round(band_top + (H - band_top - 22 - height) / 2)

    verse = Image.new('RGB', crop.size, INK)
    verse.putalpha(ink_alpha(crop))
    verse = verse.resize((width, height), Image.LANCZOS)

    canvas = canvas.convert('RGBA')
    canvas.alpha_composite(verse, (x, y))
    result = canvas.convert('RGB')
    if out:
        result.save(out)
    return result

# ------------------------------------------------------------------- preview
def banner_crops(im, out):
    """The banner on the shapes Play and a thumbnail can give it."""
    full = im
    views = [
        ('16:9 center', full.crop((68, 0, 956, 500)).resize((480, 270), Image.LANCZOS)),
        ('4:3 center', full.crop((179, 0, 845, 500)).resize((360, 270), Image.LANCZOS)),
        ('1:1 stress', full.crop((262, 0, 762, 500)).resize((250, 250), Image.LANCZOS)),
        ('320x156 thumbnail', full.resize((320, 156), Image.LANCZOS)),
    ]
    play = full.convert('RGBA')
    overlay = Image.new('RGBA', play.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.ellipse((464, 202, 560, 298), fill=(255, 255, 255, 205))
    od.polygon([(498, 226), (498, 274), (542, 250)], fill=(30, 30, 30, 225))
    play = Image.alpha_composite(play, overlay).convert('RGB').resize((640, 312), Image.LANCZOS)

    sheet = Image.new('RGB', (1600, 1220), (229, 230, 232))
    draw = ImageDraw.Draw(sheet)
    draw.text((20, 18), 'Full 1024x500', fill=(30, 30, 30))
    sheet.paste(full, (20, 42))
    draw.text((20, 568), 'Play button at center, scaled', fill=(30, 30, 30))
    sheet.paste(play, (20, 592))
    x = 1070
    y = 42
    for label, view in views:
        draw.text((x, y - 24), label, fill=(30, 30, 30))
        sheet.paste(view, (x, y))
        y += view.height + 62
    sheet.save(out)


def preview():
    """Render every surface family into TEMP. Nothing lands in the repo."""
    out = os.environ.get('TEMP', r'C:/Users/zn/AppData/Local/Temp')
    def W(name):
        return os.path.join(out, name)

    # The mark on paper, with an optional reference sheet beside it.
    panels = []
    overlays = os.environ.get('QURAN_MARK_REFERENCE')
    if overlays and os.path.exists(overlays):
        ref = Image.open(overlays).convert('RGB')
        ref = ref.crop((0, 0, min(ref.width, ref.height), min(ref.width, ref.height)))
        panels.append(ref.resize((512, 512), Image.LANCZOS))
    panels.append(render_store(512).convert('RGB'))
    sheet = Image.new('RGB', (512*len(panels) + 16*(len(panels)+1), 536), (230, 230, 235))
    x = 16
    for panel in panels:
        sheet.paste(panel, (x, 12))
        x += 512 + 16
    sheet.save(W('mark_preview.png'))

    # Store icon at its practical sizes. The labels sit below each tile and
    # the canvas is computed from the tiles, so no last tile is clipped.
    sizes = (192, 96, 64, 48, 32, 24, 16)
    column = 210
    gap = 8
    margin = 16
    width = margin * 2 + column * len(sizes) + gap * (len(sizes) - 1)
    small = Image.new('RGB', (width, 250), (229, 230, 232))
    sd = ImageDraw.Draw(small)
    x = margin
    for size in sizes:
        tile = render_store(size).convert('RGB')
        small.paste(tile, (x + (column - size) // 2, 28))
        sd.text((x + 4, 226), str(size) + ' px', fill=(30, 30, 30))
        x += column + gap
    small.save(W('mark_small.png'))

    zoom_sizes = (48, 32, 24, 16)
    zoom = Image.new('RGB', (4 * 152 + 20, 206), (229, 230, 232))
    zd = ImageDraw.Draw(zoom)
    x = 10
    for size in zoom_sizes:
        tile = render_store(size).convert('RGB').resize((128, 128), Image.NEAREST)
        zoom.paste(tile, (x, 24))
        zd.text((x + 20, 166), str(size) + ' px, 8x', fill=(30, 30, 30))
        x += 152
    zoom.save(W('mark_small_zoom.png'))

    # The actual legacy tiles at their five densities.
    legacy = Image.new('RGB', (5 * 224 + 20, 250), (229, 230, 232))
    ld = ImageDraw.Draw(legacy)
    x = 10
    for name, size, _ in DENS:
        tile = legacy_png(size).convert('RGB')
        legacy.paste(tile, (x + (208 - size) // 2, 24))
        ld.text((x + 12, 224), name + ' ' + str(size), fill=(30, 30, 30))
        x += 224
    legacy.save(W('mark_legacy.png'))

    # The themed silhouette on the paper it is meant to read on.
    mono_sizes = (192, 96, 48, 32, 24)
    mono = Image.new('RGB', (260 + len(mono_sizes) * 200 + 20, 300), (229, 230, 232))
    md = ImageDraw.Draw(mono)
    ids = book_mark_ids()
    scale, xc, yc = fit_for(ids, None, span_w=224 * 0.8)
    mono_tile = raster_canvas(224, 8)
    paper_bg(mono_tile)
    draw_mono(mono_tile, scale, 112 * 8 - (xc - AX_X) * scale * 8,
              112 * 8, yc, 8, INK)
    mono.paste(mono_tile.convert('RGB').resize((224, 224), Image.LANCZOS), (10, 28))
    md.text((10, 258), '224 px', fill=(30, 30, 30))
    x = 260
    for size in mono_sizes:
        scale, xc, yc = fit_for(ids, None, span_w=size * 0.66)
        tile = raster_canvas(size, 8)
        paper_bg(tile)
        draw_mono(tile, scale, size/2*8 - (xc - AX_X) * scale * 8,
                  size/2*8, yc, 8, INK)
        tile = tile.convert('RGB').resize((size, size), Image.LANCZOS)
        mono.paste(tile, (x + (200 - size) // 2, 28 + (192 - size) // 2))
        md.text((x + 76, 258), str(size) + ' px', fill=(30, 30, 30))
        x += 200
    mono.save(W('mark_mono.png'))

    share_png(96, W('mark_share.png'))
    banner = banner_png(W('mark_banner.png'))
    banner_crops(banner, W('mark_banner_crops.png'))
    print('previews written:', out)

def emit():
    root = ROOT
    fg_note = ('The app\'s mark: an open book on its rehal, redrawn from the\n'
               '  owner\'s reference as uniform line art: the pages are sail arcs\n'
               '  diving into a spine that hangs a short tail; each plank folds\n'
               '  onto the page\'s edge, runs down beside the book, and its lower\n'
               '  end fans into a closed wedge foot. The whole drawing is one ink\n'
               '  on the theme\'s paper at one line weight on every icon surface,\n'
               '  and the paper strokes keep air between the stand and the book\'s\n'
               '  edges.')
    mono_note = ('The themed launcher layer: the same book on its stand as a solid\n'
                 '  silhouette, one band per plank, each lower end closed as a wedge,\n'
                 '  with a clean scaled copy of the book cut out of the bands so the shape\n'
                 '  reads at 24 dp without a stair-stepped margin.')
    open(os.path.join(root, 'app/src/main/res/drawable/ic_launcher_foreground.xml'), 'w', newline='\n').write(fg_xml(fg_note))
    open(os.path.join(root, 'app/src/main/res/drawable/ic_launcher_monochrome.xml'), 'w', newline='\n').write(mono_xml(mono_note))
    # The shortcut door: the launcher foreground is a 108 dp canvas with the
    # mark at 41.5 percent, which is right under a mask and too small in a 48 dp
    # shortcut tile. The door gets the same drawing at 86 percent of a 48 dp
    # canvas, so the mark stands at the tile's own size (owner report, the
    # surfaces session).
    door = 48.0
    door_scale = 0.86 * door / 115.0
    door_stroke, door_halo = optical(48)
    open(os.path.join(root, 'app/src/main/res/drawable/ic_shortcut_mark.xml'), 'w',
         newline='\n').write(
        mark_xml('The shortcut door: the app\'s mark at 86 percent of a 48 dp canvas,\n'
                 '  one ink on the theme\'s paper, drawn from the same geometry as\n'
                 '  the launcher, the share mark, and the store icon.',
                 door, door_scale, door / 2, door / 2,
                 door_stroke * door_scale,
                 (door_stroke + 2 * door_halo) * door_scale))
    for name, size, _ in DENS:
        legacy_png(size, os.path.join(root, f'app/src/main/res/mipmap-{name}/ic_launcher.png'))
        share_png(size // 2, os.path.join(root, f'app/src/main/res/drawable-{name}/ic_share_mark.png'))
        book_mark_png(size // 2, os.path.join(root, f'ui-kit/src/main/res/drawable-{name}/ic_book_mark.png'))
    render_store(512, os.path.join(root, 'play-store/icon-512.png'))
    banner_png(out=os.path.join(root, 'play-store/feature-graphic-1024x500.png'))
    print('emitted')

if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'emit':
        emit()
    else:
        preview()
