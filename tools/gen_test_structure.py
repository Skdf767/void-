#!/usr/bin/env python3
"""Writes data/void_ascension/structures/empty.nbt - a 3x3x3 air structure used as the GameTest template."""
import gzip, os, struct

def s(v): b = v.encode(); return struct.pack(">H", len(b)) + b
def named(tag_id, name, payload): return bytes([tag_id]) + s(name) + payload
def i32(v): return struct.pack(">i", v)
def lst(tag_id, items): return bytes([tag_id]) + i32(len(items)) + b"".join(items)
def comp(entries): return b"".join(entries) + b"\x00"

blocks = [comp([named(9, "pos", lst(3, [i32(x), i32(y), i32(z)])), named(3, "state", i32(0))])
          for x in range(3) for y in range(3) for z in range(3)]
root = named(10, "", comp([
    named(3, "DataVersion", i32(3465)),             # 1.20.1
    named(9, "size", lst(3, [i32(3), i32(3), i32(3)])),
    named(9, "palette", lst(10, [comp([named(8, "Name", s("minecraft:air"))])])),
    named(9, "blocks", lst(10, blocks)),
    named(9, "entities", lst(10, [])),
]))
out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/data/void_ascension/structures/empty.nbt")
with gzip.open(out, "wb") as f: f.write(root)
print("wrote", os.path.normpath(out))
