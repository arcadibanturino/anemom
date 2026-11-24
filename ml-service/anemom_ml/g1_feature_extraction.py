import cv2
import numpy as np

SIZE = 256
CHANNELS = ["R", "G", "B", "L", "a", "b", "S", "V"]
LEGACY = [
    f"{c}_{s}"
    for c in CHANNELS
    for s in ["mean", "med", "std"]
] + ["R_over_G", "R_frac", "RG_norm"]

def load_pair(image_path,mask_path,size=SIZE):
    raw=cv2.imread(str(image_path),cv2.IMREAD_COLOR)
    seg=cv2.imread(str(mask_path),cv2.IMREAD_UNCHANGED)
    if raw is None or seg is None: raise ValueError('Foto / mask tidak terbaca')
    if seg.ndim!=3 or seg.shape[2]<3: raise ValueError('Segmentasi tidak RGB(A)')
    hr,wr=raw.shape[:2];hs,ws=seg.shape[:2]
    if abs((wr/hr)/(ws/hs)-1)>0.015:raise ValueError('Rasio foto-mask tidak cocok')
    if seg.shape[2]==4 and np.ptp(seg[:,:,3])>0: m=seg[:,:,3]>127
    else: m=np.min(seg[:,:,:3],axis=2)<245
    nc,cc,stats,_=cv2.connectedComponentsWithStats(m.astype('uint8'),8)
    if nc>1:m=cc==(1+np.argmax(stats[1:,cv2.CC_STAT_AREA]))
    fraction=float(m.mean())
    if not (0.002<fraction<0.70):raise ValueError(f'Fraksi mask tak wajar {fraction:.4f}')
    rgb=cv2.cvtColor(cv2.resize(raw,(size,size),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)
    m=cv2.resize(m.astype('uint8'),(size,size),interpolation=cv2.INTER_NEAREST).astype(bool)
    if m.sum()<80:raise ValueError('Mask terlalu kecil setelah resize')
    return rgb,m


def get_features(rgb, mask):
    assert rgb.ndim==3 and rgb.shape[2]==3 and rgb.shape[:2]==mask.shape
    lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB)
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    colors={'R':rgb[:,:,0],'G':rgb[:,:,1],'B':rgb[:,:,2],
            'L':lab[:,:,0],'a':lab[:,:,1],'b':lab[:,:,2],
            'S':hsv[:,:,1],'V':hsv[:,:,2]}
    result={}
    for ch,plane in colors.items():
        v=plane[mask].astype(np.float32)
        result[f'{ch}_mean']=float(v.mean())
        result[f'{ch}_med']=float(np.median(v))
        result[f'{ch}_std']=float(v.std())
    R,G,B=[result[c+'_mean'] for c in 'RGB']
    result.update(R_over_G=R/(G+1e-6),R_frac=R/(R+G+B+1e-6),RG_norm=(R-G)/(R+G+1e-6))
    # Raw brightness & saturation are preserved for QC, not taken as labels.
    rawpix=rgb[mask].astype(np.float32)
    white=np.all(rawpix>=248,axis=1)
    clipped=np.any(rawpix>=253,axis=1)
    dark=np.max(rawpix,axis=1)<35
    # Conservative robust subset; do not select based on anemia label.
    good=~(white|clipped|dark)
    use=good if good.sum()>=max(80,int(mask.sum()*0.55)) else np.ones(mask.sum(),dtype=bool)
    r,g,b=rawpix[use].T
    result['bright_fraction']=float(white.mean())
    result['clip_fraction']=float(clipped.mean())
    result['valid_fraction']=float(good.mean())
    result['mask_fraction']=float(mask.mean())
    result['rob_R_over_G']=float(np.median(r/(g+1)))
    result['rob_RG_norm']=float(np.median((r-g)/(r+g+1)))
    result['rob_R_frac']=float(np.median(r/(r+g+b+1)))
    for ch,plane in colors.items():
        v=plane[mask].astype(np.float32)[use]
        lo,hi=np.percentile(v,[10,90])
        trimmed=v[(v>=lo)&(v<=hi)]
        result[f'rob_{ch}_median']=float(np.median(v))
        result[f'rob_{ch}_p10']=float(lo)
        result[f'rob_{ch}_p90']=float(hi)
        result[f'rob_{ch}_trimmed']=float(trimmed.mean()) if len(trimmed) else float(v.mean())
    return result
