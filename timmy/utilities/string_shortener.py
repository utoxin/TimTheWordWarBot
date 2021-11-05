import textwrap


def shorten_to_bytes_width(text: str, width: int) -> str:
    # Ref: https://stackoverflow.com/a/56401167/
    width = max(10, width)  # This prevents ValueError if width < _MIN_WIDTH
    text = textwrap.shorten(text, width)  # After this line, len(text.encode()) >= width
    while len(text.encode()) > width:
        text = textwrap.shorten(text, len(text) - 1)
    assert len(text.encode()) <= width
    return text