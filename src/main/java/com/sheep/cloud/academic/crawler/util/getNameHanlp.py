import sys
import io
from pyhanlp import HanLP
import re

def split_name(text):
    segment = HanLP.newSegment().enableNameRecognize(True)
    result = segment.seg(text)
    word_list = [str(i) for i in result]
    for item in word_list:
        if item.__contains__('/nr'):
            return re.sub('/nr([f]*)', '', item)


if __name__ == '__main__':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    ret = split_name(sys.argv[1])
    if ret is None:
        print(' ')
    else:
        print(ret)