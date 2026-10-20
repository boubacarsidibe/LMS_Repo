#!/usr/bin/env python3
"""Contrôle objectif de la piste sonore d'une vidéo produite.

On ne peut pas « écouter » une vidéo dans une chaîne automatisée ; on la fait donc transcrire par un modèle de
reconnaissance vocale (Whisper) et l'on compare le résultat au texte DIT :
  - intelligibilité : un sigle mal prononcé ou une phrase tronquée apparaît comme un écart de transcription ;
  - calage : l'instant où chaque passage est entendu doit coïncider avec celui où le scénario l'a déclenché.

    python3 verifier-audio.py sortie/admission/admission.mp4 [modèle=small]
"""
import difflib, json, re, subprocess, sys, tempfile, unicodedata, os

video = sys.argv[1]
modele = sys.argv[2] if len(sys.argv) > 2 else "small"
minutage = json.load(open(re.sub(r"\.mp4$", ".minutage.json", video), encoding="utf-8"))

def normaliser(texte):
    t = unicodedata.normalize("NFD", texte.lower())
    t = "".join(c for c in t if unicodedata.category(c) != "Mn")
    t = re.sub(r"[^a-z0-9' ]+", " ", t).replace("'", " ")
    mots = t.split()
    # « i n p g » (lettres épelées) et « inpg » doivent se valoir : on recolle les suites de lettres isolées.
    sortie, lettres = [], []
    for m in mots:
        if len(m) == 1 and m.isalpha():
            lettres.append(m)
            continue
        if lettres:
            sortie.append("".join(lettres)); lettres = []
        sortie.append(m)
    if lettres:
        sortie.append("".join(lettres))
    return sortie

with tempfile.TemporaryDirectory() as tmp:
    wav = os.path.join(tmp, "audio.wav")
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", video, "-ar", "16000", "-ac", "1", wav], check=True)
    from faster_whisper import WhisperModel
    m = WhisperModel(modele, device="cpu", compute_type="int8")
    # On fournit le signal déjà décodé : cela évite la dépendance de faster-whisper à PyAV, dont les versions divergent.
    import wave, numpy as np
    with wave.open(wav, "rb") as w:
        signal = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32768.0
    segs, info = m.transcribe(signal, language="fr", word_timestamps=True, vad_filter=False, beam_size=5)
    mots_asr = []
    for s in segs:
        for w in s.words:
            for n in normaliser(w.word):
                mots_asr.append((n, w.start, w.end))

attendu = []   # (mot, index_segment)
for i, s in enumerate(minutage):
    for n in normaliser(s["dit"]):
        attendu.append((n, i))

a = [m for m, _ in attendu]; b = [m for m, _, _ in mots_asr]
sm = difflib.SequenceMatcher(None, a, b, autojunk=False)
correspondances = {}   # index attendu -> index asr
for bloc in sm.get_matching_blocks():
    for k in range(bloc.size):
        correspondances[bloc.a + k] = bloc.b + k

total = len(a)
ok = len(correspondances)
print(f"Reconnaissance : {ok}/{total} mots retrouvés ({100*ok/total:.1f} %)")

ecarts = []
print("\nCalage par passage (écart entre l'instant prévu et l'instant entendu) :")
debut_seg = {}
for idx, (mot, i) in enumerate(attendu):
    debut_seg.setdefault(i, []).append(idx)
for i, s in enumerate(minutage):
    idxs = debut_seg.get(i, [])
    trouves = [correspondances[x] for x in idxs if x in correspondances]
    part = len(trouves) / max(1, len(idxs))
    if trouves:
        t_asr = mots_asr[min(trouves)][1]
        ecart = t_asr - s["debutVoix"]
        ecarts.append(ecart)
        drapeau = "" if abs(ecart) < 0.6 and part > 0.8 else "  ⚠"
        print(f"  {i:2d} {s['debutVoix']:7.2f}s  entendu à {t_asr:7.2f}s  Δ {ecart:+.2f}s  mots {100*part:3.0f} %{drapeau}  « {s['texte'][:60]} »")
    else:
        print(f"  {i:2d} {s['debutVoix']:7.2f}s  ⚠ rien retrouvé  « {s['texte'][:60]} »")

if ecarts:
    ecarts.sort()
    print(f"\nÉcart médian {ecarts[len(ecarts)//2]:+.2f} s, extrêmes {ecarts[0]:+.2f} / {ecarts[-1]:+.2f} s")

# Mots attendus non retrouvés : repère les sigles ou noms mal prononcés.
manques = [attendu[x][0] for x in range(total) if x not in correspondances]
if manques:
    from collections import Counter
    print("\nMots attendus mais non entendus (à surveiller) :", ", ".join(f"{m}×{n}" if n > 1 else m for m, n in Counter(manques).most_common(25)))
