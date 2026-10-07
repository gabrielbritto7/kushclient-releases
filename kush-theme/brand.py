"""Replace display strings in class constant pools without changing identifiers."""
import struct
import re

def brand_class(data):
    if data[:4] != b'\xca\xfe\xba\xbe':
        return data
    result = bytearray(data[:10])
    count = struct.unpack_from('>H', data, 8)[0]
    offset, index = 10, 1
    lengths = {3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
    while index < count:
        tag = data[offset]
        result.append(tag)
        offset += 1
        if tag == 1:
            size = struct.unpack_from('>H', data, offset)[0]
            raw = data[offset+2:offset+2+size]
            # Namespace, class references, module IDs, endpoints and licenses
            # remain compatible. Only literal display/log messages are changed.
            if b'/' not in raw and b'net.fastclient' not in raw and b'http' not in raw:
                if raw in (b'FastClientHUD', b'FastClient', b'FastClient Store') or b' ' in raw or b'[' in raw:
                    raw = re.sub(b'Fast[Cc]lient(?:HUD)?', b'Kush', raw)
            result.extend(struct.pack('>H', len(raw)))
            result.extend(raw)
            offset += size + 2
        else:
            size = lengths[tag]
            result.extend(data[offset:offset+size])
            offset += size
            if tag in (5,6): index += 1
        index += 1
    result.extend(data[offset:])
    return bytes(result)
