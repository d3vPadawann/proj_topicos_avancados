import os
import fitz  # PyMuPDF

def extract_ucsal_logo():
    pdf_path = r"c:\Users\eduar\projeto elton\PROJETO 2 UNIDADE\TopicosAvancadosDocUnidade1 (1).pdf"
    out_dir = r"c:\Users\eduar\projeto elton\PROJETO 2 UNIDADE\overleaf\figuras"
    os.makedirs(out_dir, exist_ok=True)
    doc = fitz.open(pdf_path)
    page = doc[0]
    images = page.get_images(full=True)
    for idx, img in enumerate(images):
        xref = img[0]
        base_image = doc.extract_image(xref)
        image_bytes = base_image["image"]
        ext = base_image["ext"]
        logo_path = os.path.join(out_dir, f"logo_ucsal.{ext}")
        with open(logo_path, "wb") as f:
            f.write(image_bytes)
        print(f"Extracted logo to {logo_path} ({len(image_bytes)} bytes)")
        break

    # Also extract the 6 figures from Unidade 1 so Overleaf has all figures if needed
    for pno in range(len(doc)):
        p = doc[pno]
        for idx, img in enumerate(p.get_images(full=True)):
            xref = img[0]
            base_image = doc.extract_image(xref)
            ext = base_image["ext"]
            fname = os.path.join(out_dir, f"u1_p{pno+1}_img{idx+1}.{ext}")
            with open(fname, "wb") as f:
                f.write(base_image["image"])

if __name__ == "__main__":
    extract_ucsal_logo()
