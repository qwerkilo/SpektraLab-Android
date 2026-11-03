#!/usr/bin/env python3
"""Check gen-stock-catalog.py against a temporary fixture.

The real profile bundle is not part of this checkout, so this builds three
stand-ins: one profile with both spectral series, one with only the density
series and real nulls (including a NaN literal), and one with no data at all.
It also checks that scalar info survives and non-scalar info does not.

Run from the project root:

    python app/build/test-gen-stock-catalog.py
"""
import json
import os
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # app/
GENERATOR = os.path.join(ROOT, "build", "gen-stock-catalog.py")


def write_profile(directory, filename, profile):
    path = os.path.join(directory, filename)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(profile, handle, allow_nan=True)
    return path


def record(catalog, stock_id):
    for film in catalog["films"]:
        if film["id"] == stock_id:
            return film
    raise AssertionError("film %s missing from catalogue" % stock_id)


def main():
    with tempfile.TemporaryDirectory() as tmp:
        profiles = os.path.join(tmp, "profiles")
        os.makedirs(profiles)
        out = os.path.join(tmp, "stock_catalog.json")

        write_profile(profiles, "log.json", {
            "info": {
                "stock": "fixture_log",
                "name": "Fixture Log 400",
                "type": "negative",
                "use": "still",
                "support": "film",
                "stage": "filming",
                "iso": 400,
                "balance": "daylight",
                "description": "A fixture description.",
                "antihalation": "strong",
                "target_print": "fixture_paper",
                "nested": {"dropped": True},
            },
            "data": {
                "wavelengths": [380.0, 385.0, 390.0],
                "log_sensitivity": [
                    [1.0, 2.0, 3.0],
                    [1.5, 2.5, 3.5],
                    [2.0, 3.0, 4.0],
                ],
                "channel_density": [[9.0, 9.0, 9.0]] * 3,
            },
        })
        write_profile(profiles, "density.json", {
            "info": {
                "stock": "fixture_density",
                "name": "Fixture Density 100",
                "type": "negative",
                "stage": "filming",
            },
            "data": {
                "wavelengths": [380, 385, 390, 395],
                "channel_density": [
                    [1.0, None, 3.0],
                    [None, None, None],
                    [4.0, 5.0, float("nan")],
                    [None, 7.0, 8.0],
                ],
            },
        })
        write_profile(profiles, "nodata.json", {
            "info": {"stock": "fixture_nodata", "name": "Fixture No Data"},
        })
        write_profile(profiles, "fallback.json", {
            "info": {
                "stock": "fixture_fallback",
                "name": "Fixture Fallback 200",
                "type": "negative",
                "stage": "filming",
            },
            "data": {
                "wavelengths": [380, 385, 390],
                "log_sensitivity": [[None, None, None]] * 3,
                "channel_density": [[1.0, 2.0, 3.0], [2.0, 3.0, 4.0],
                                    [3.0, 4.0, 5.0]],
            },
        })
        write_profile(profiles, "sparse.json", {
            "info": {
                "stock": "fixture_sparse",
                "name": "Fixture Sparse 300",
                "type": "negative",
                "stage": "filming",
            },
            "data": {
                "wavelengths": [380, 385, 390],
                "log_sensitivity": [[1.0, 2.0, 3.0], [None, None, None],
                                    [None, None, None]],
                "channel_density": [[4.0, 5.0, 6.0], [5.0, 6.0, 7.0],
                                    [6.0, 7.0, 8.0]],
            },
        })
        write_profile(profiles, "paper.json", {
            "info": {
                "stock": "fixture_paper",
                "name": "Fixture Paper",
                "type": "negative",
                "stage": "printing",
                "support": "paper",
                "iso": 100,
            },
        })
        write_profile(profiles, "cinestill_800t.json", {
            "info": {
                "stock": "cinestill_800t",
                "name": "CineStill 800T",
                "type": "negative",
                "stage": "filming",
            },
            "data": {
                "wavelengths": [380, 385, 390],
                "channel_density": [[1.0, 2.0, 3.0], [2.0, 3.0, 4.0],
                                    [3.0, 4.0, 5.0]],
            },
        })
        write_profile(profiles, "kodak_vision3_500t.json", {
            "info": {
                "stock": "kodak_vision3_500t",
                "name": "Kodak Vision3 500T",
                "type": "negative",
                "stage": "filming",
                "target_print": "kodak_portra_endura",
            },
            "data": {
                "wavelengths": [380, 385, 390],
                "log_sensitivity": [[1.0, 2.0, 3.0], [2.0, 3.0, 4.0],
                                    [3.0, 4.0, 5.0]],
            },
        })

        result = subprocess.run(
            [sys.executable, GENERATOR, "--profiles", profiles, "--out", out],
            capture_output=True, text=True)
        if result.returncode != 0:
            raise AssertionError("generator failed: %s\n%s" % (result.stdout, result.stderr))
        with open(out, encoding="utf-8") as handle:
            catalog = json.load(handle)

        assert catalog["schema_version"] == 1, "schema_version changed"

        log = record(catalog, "fixture_log")
        assert log["iso"] == 400, "integer info is not preserved"
        assert log["balance"] == "daylight", "text info is not preserved"
        assert log["description"] == "A fixture description.", "description is not preserved"
        assert log["antihalation"] == "strong", "antihalation is not preserved"
        assert "nested" not in log, "non-scalar info leaked into the record"
        assert log["wavelengths"] == [380.0, 385.0, 390.0], "wavelengths changed"
        assert len(log["log_sensitivity"]) == 3, "log_sensitivity was not baked"
        assert log["spectral_source"] == "log_sensitivity", "log_sensitivity is not preferred"
        assert log["channel_density"] is None, "the fallback series should not be shipped too"

        density = record(catalog, "fixture_density")
        assert density["wavelengths"] == [380, 385, 390, 395], "density wavelengths changed"
        assert density["log_sensitivity"] is None, "log_sensitivity should be null when absent"
        assert density["spectral_source"] == "channel_density", "density fallback not declared"
        rows = density["channel_density"]
        assert rows[0] == [1.0, None, 3.0], "a null sample was not preserved"
        assert rows[1] == [None, None, None], "an unmeasured row was not preserved"
        assert rows[2] == [4.0, 5.0, None], "NaN was not preserved as JSON null"
        assert rows[3] == [None, 7.0, 8.0], "a partial row was not preserved"

        nodata = record(catalog, "fixture_nodata")
        assert nodata["wavelengths"] is None, "missing wavelengths should be null"
        assert nodata["log_sensitivity"] is None, "missing log_sensitivity should be null"
        assert nodata["channel_density"] is None, "missing channel_density should be null"
        assert nodata["spectral_source"] is None, "missing data should declare no source"

        fallback = record(catalog, "fixture_fallback")
        assert fallback["spectral_source"] == "channel_density", \
            "an all-null log_sensitivity should fall back to density"
        assert fallback["log_sensitivity"] is None, "the empty series should not be shipped"
        assert fallback["channel_density"][0] == [1.0, 2.0, 3.0], \
            "the density fallback was not baked"

        sparse = record(catalog, "fixture_sparse")
        assert sparse["spectral_source"] == "channel_density", \
            "a one-point log_sensitivity is not a curve and should fall back"
        assert sparse["log_sensitivity"] is None, "the one-point series should not be shipped"

        papers = [p for p in catalog["prints"] if p["id"] == "fixture_paper"]
        assert len(papers) == 1, "the printing profile did not land in prints"
        assert papers[0]["iso"] == 100, "print info is not preserved"
        assert "wavelengths" not in papers[0], "prints should not carry film spectra"

        # `targetPrint` is the paper a film declares it prints on. It comes
        # from the profile when it is there, and the CineStill/Vision3 alias
        # fallback when it is not; the real spectral series must survive.
        assert log["target_print"] == "fixture_paper", "the profile target is not preserved"
        assert log["targetPrint"] == "fixture_paper", "the profile target did not become targetPrint"
        assert log["spectral_source"] == "log_sensitivity", "targetPrint dropped the series"
        assert log["log_sensitivity"] is not None, "targetPrint nulled the film spectrum"

        cinestill = record(catalog, "cinestill_800t")
        assert cinestill["targetPrint"] == "kodak_2383", \
            "the CineStill alias should fall back to 2383"
        assert cinestill["spectral_source"] == "channel_density", \
            "the CineStill spectral series was dropped"
        assert cinestill["channel_density"][0] == [1.0, 2.0, 3.0], \
            "the CineStill spectral data changed"

        vision = record(catalog, "kodak_vision3_500t")
        assert vision["targetPrint"] == "kodak_portra_endura", \
            "an explicit profile target must win over the alias fallback"
        assert vision["spectral_source"] == "log_sensitivity", \
            "the Vision3 spectral series was dropped"

        assert "targetPrint" not in nodata, "targetPrint was invented for a no-data stock"
        assert "targetPrint" not in papers[0], "prints should not gain a film target"

    print("gen-stock-catalog test: fixture checks passed")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except AssertionError as error:
        print("FAIL: %s" % error)
        sys.exit(1)
