import sys
import io
from ltp import LTP


def split_name(text):
    ltp = LTP()
    seg, hidden = ltp.seg([text])
    pos = ltp.pos(hidden)
    for index, item in enumerate(pos[0]):
        if item == 'nh':
            return seg[0][index]


if __name__ == '__main__':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    ret = split_name(sys.argv[1])
    if ret is None:
        print(' ')
    else:
        print(ret)
