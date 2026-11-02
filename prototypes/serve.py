#!/usr/bin/env python3
"""Throwaway prototype server. Run: python prototypes/serve.py"""
import http.server
import os
import socketserver
import webbrowser

ROOT = os.path.dirname(os.path.abspath(__file__))
PORT = 8788


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)


socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("127.0.0.1", PORT), Handler) as httpd:
    url = f"http://127.0.0.1:{PORT}/ui-instrument.html?variant=A"
    print(f"prototype: {url}")
    print("variants: ?variant=A (Console) | B (Ledger) | C (Scan)")
    print("theme:    pill in the phone header, ?theme=light|dark, or press T")
    webbrowser.open(url)
    httpd.serve_forever()
