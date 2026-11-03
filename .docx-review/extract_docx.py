from pathlib import Path
from docx import Document
from docx.oxml.ns import qn

src = Path(r"C:\Users\mateo\Downloads\Marco_Teorico_Proyecto_Consumo_Energetico.docx")
out = Path(r"C:\Users\mateo\Downloads\sistema-turnos\sistema-turnos\.docx-review\content.txt")
doc = Document(src)

lines = []
lines.append(f"SECTIONS: {len(doc.sections)}")
lines.append(f"PARAGRAPHS: {len(doc.paragraphs)}")
lines.append(f"TABLES: {len(doc.tables)}")
lines.append("")

for i, p in enumerate(doc.paragraphs, 1):
    text = p.text.replace("\t", " [TAB] ").strip()
    if not text:
        continue
    lines.append(f"P{i:03d} | {p.style.name} | {text}")

for ti, table in enumerate(doc.tables, 1):
    lines.append("")
    lines.append(f"TABLE {ti}")
    for ri, row in enumerate(table.rows, 1):
        cells = [c.text.replace("\n", " / ").strip() for c in row.cells]
        lines.append(f"R{ri:02d} | " + " || ".join(cells))

lines.append("")
lines.append("STYLES USED")
counts = {}
for p in doc.paragraphs:
    counts[p.style.name] = counts.get(p.style.name, 0) + 1
for name, count in sorted(counts.items(), key=lambda x: (-x[1], x[0])):
    lines.append(f"{name}: {count}")

lines.append("")
lines.append("SECTION SETTINGS")
for i, s in enumerate(doc.sections, 1):
    lines.append(
        f"S{i}: page={s.page_width.inches:.2f}x{s.page_height.inches:.2f}in "
        f"margins L{s.left_margin.inches:.2f} R{s.right_margin.inches:.2f} "
        f"T{s.top_margin.inches:.2f} B{s.bottom_margin.inches:.2f}"
    )

out.write_text("\n".join(lines), encoding="utf-8")
print(out)
