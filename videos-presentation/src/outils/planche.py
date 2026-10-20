#!/usr/bin/env python3
"""Assemble des captures en planche-contact (2 colonnes × 3 lignes par planche) : planche.py <dossier> [colonnes=2]"""
import glob, os, sys
from PIL import Image, ImageDraw, ImageFont
dossier = sys.argv[1]; cols = int(sys.argv[2]) if len(sys.argv) > 2 else 2
fichiers = sorted(glob.glob(f"{dossier}/*.png"))
fichiers = [f for f in fichiers if "planche" not in os.path.basename(f)]
TW, TH = (640, 360) if cols == 2 else (480, 270)
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 14)
par = cols * 3
for k in range(0, len(fichiers), par):
    morceau = fichiers[k:k + par]
    feuille = Image.new("RGB", (TW * cols, (TH + 20) * 3), (30, 30, 30)); d = ImageDraw.Draw(feuille)
    for i, f in enumerate(morceau):
        im = Image.open(f).convert("RGB").resize((TW, TH), Image.LANCZOS)
        x, y = (i % cols) * TW, (i // cols) * (TH + 20)
        feuille.paste(im, (x, y + 20)); d.text((x + 6, y + 3), os.path.basename(f)[:-4], fill="white", font=font)
    sortie = f"{dossier}/planche-{k // par + 1}.png"; feuille.save(sortie); print(sortie)
