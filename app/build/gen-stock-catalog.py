#!/usr/bin/env python3
"""Regenerate assets/resources/stock_catalog.json from the packed profiles.

The list the UI shows must be the list the APK ships, not the engine source
tree: the bundle carries a few profiles (B&W films, CineStill, Ilford paper)
that the open-source engine resources do not.

Each film record carries the profile's real scalar `info` fields and one
spectral series: `log_sensitivity` when it has samples (the complete series in
the sampled profiles), otherwise `channel_density` (which keeps its nulls as
gaps). `spectral_source` names which one was baked. Nothing is invented: a
profile without data emits null arrays.

Run from the APK project root; it writes both the source asset and the staging
copy that aapt packages. `--profiles` and `--out` point the same generator at a
fixture, which is how the unit check runs without the real bundle.
"""
import argparse
import glob
import json
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # app/
PROJECT = os.path.dirname(ROOT)
STAGED = os.path.join(PROJECT, 'app/build/apk/assets/resources/profiles')
SRC = os.path.join(PROJECT, 'app/src/main/assets/resources/profiles')
OUT = os.path.join(PROJECT, 'app/src/main/assets/resources/stock_catalog.json')
OUT_STAGED = os.path.join(PROJECT, 'app/build/apk/assets/resources/stock_catalog.json')

SCALAR_INFO = (str, int, float, bool)

# The paper a film is declared to print on. The profile carries it for most
# stocks (`target_print`); CineStill and the Vision3 negatives are aliases
# whose profile is a stub, and the macOS/iOS catalogue names 2383 for them.
FALLBACK_TARGET = {
    'cinestill_800t': 'kodak_2383',
    'kodak_verita_200d': 'kodak_2383',
    'kodak_vision3_200t': 'kodak_2383',
    'kodak_vision3_250d': 'kodak_2383',
    'kodak_vision3_500t': 'kodak_2383',
    'kodak_vision3_50d': 'kodak_2383',
}


def sanitize(value):
    """JSON that strict readers accept: non-finite floats become null."""
    if isinstance(value, float):
        return value if math.isfinite(value) else None
    if isinstance(value, list):
        return [sanitize(item) for item in value]
    if isinstance(value, dict):
        return {key: sanitize(item) for key, item in value.items()}
    return value


def scalar_info(info):
    """The real scalar fields, without the two the record already names."""
    fields = {}
    for key in sorted(info):
        if key in ('stock', 'name'):
            continue
        value = info[key]
        if value is None or isinstance(value, SCALAR_INFO):
            fields[key] = sanitize(value)
    return fields


def has_samples(series):
    """True when the series has at least two rows with a finite number.

    One measured point is not a curve: the Java seam would mark it unavailable,
    so the generator does not let it hide a complete fallback series.
    """
    if not isinstance(series, list):
        return False
    measured = 0
    for row in series:
        if not isinstance(row, list):
            continue
        for value in row:
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                if math.isfinite(value):
                    measured += 1
                    break
        if measured >= 2:
            return True
    return False


def film_spectrum(data):
    """The preferred real series, or Nones. Never a derived or typical curve."""
    data = data if isinstance(data, dict) else {}
    wavelengths = data.get('wavelengths')
    log_sensitivity = data.get('log_sensitivity')
    channel_density = data.get('channel_density')
    if has_samples(log_sensitivity):
        source, series = 'log_sensitivity', log_sensitivity
    elif has_samples(channel_density):
        source, series = 'channel_density', channel_density
    else:
        source, series = None, None
    return {
        'wavelengths': sanitize(wavelengths) if isinstance(wavelengths, list) else None,
        'log_sensitivity': sanitize(series) if source == 'log_sensitivity' else None,
        'channel_density': sanitize(series) if source == 'channel_density' else None,
        'spectral_source': source,
    }


def build_record(path):
    with open(path, encoding='utf-8') as handle:
        profile = json.load(handle)
    info = profile.get('info')
    info = info if isinstance(info, dict) else {}
    fallback = os.path.basename(path)[:-5]
    rec = {
        'id': info.get('stock') or fallback,
        'name': info.get('name') or info.get('stock') or fallback,
    }
    rec.update(scalar_info(info))
    target = info.get('target_print') or FALLBACK_TARGET.get(rec['id'])
    if target:
        rec['targetPrint'] = target
    for key, default in (('type', 'negative'), ('use', 'still'),
                         ('support', 'film'), ('stage', 'filming')):
        if not rec.get(key):
            rec[key] = default
    return rec, profile.get('data')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profiles', help='directory of profile JSON files')
    parser.add_argument('--out', help='write one catalogue here instead of the two bundle paths')
    args = parser.parse_args()

    profiles = args.profiles or (STAGED if os.path.isdir(STAGED) else SRC)
    films, prints = [], []
    for path in sorted(glob.glob(os.path.join(profiles, '*.json'))):
        rec, data = build_record(path)
        if rec['stage'] == 'printing':
            prints.append(rec)
        else:
            rec.update(film_spectrum(data))
            films.append(rec)

    films.sort(key=lambda r: (0 if r['use'] == 'still' else 1, r['name']))
    prints.sort(key=lambda r: (0 if r['use'] == 'still' else 1, r['name']))

    payload = {'schema_version': 1, 'films': films, 'prints': prints}
    destinations = [args.out] if args.out else [OUT, OUT_STAGED]
    for dest in destinations:
        os.makedirs(os.path.dirname(os.path.abspath(dest)), exist_ok=True)
        with open(dest, 'w', encoding='utf-8') as handle:
            json.dump(payload, handle, ensure_ascii=False, indent=1, allow_nan=False)
    with_spectra = sum(1 for film in films if film['spectral_source'])
    print('stocks: %d films (%d with spectra), %d prints -> %s'
          % (len(films), with_spectra, len(prints), destinations[0]))


if __name__ == '__main__':
    main()
