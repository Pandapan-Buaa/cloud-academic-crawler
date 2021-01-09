import chardet
import urllib.request
import sys
import io


def get_charset(url):
    try:
        html = urllib.request.urlopen(url, timeout=15).read()
        charset = chardet.detect(html)['encoding']
        if 'utf-8' in charset.lower():
            return 'UTF-8'
        else:
            return charset
    except Exception:
        return 'UTF-8'


if __name__ == '__main__':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    ret = get_charset(sys.argv[1])
    print(ret)
