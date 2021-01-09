import sys
import io
from ltp import LTP


def split_name(text):
    ltp = LTP()
    seg, hidden = ltp.seg([text])
    pos = ltp.pos(hidden)
    name_list = []
    for index, item in enumerate(pos[0]):
        if item == 'nh':
            name_list.append(seg[0][index])

    name_str = ' '.join(str(i) for i in name_list)
    return name_str


if __name__ == '__main__':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    ret = split_name(sys.argv[1])
    if ret == '':
        print(' ')
    else:
        print(ret)
